package com.aistudio.pianobar.kxuvzq.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import com.aistudio.pianobar.kxuvzq.data.StationRepository
import com.aistudio.pianobar.kxuvzq.model.LogType
import com.aistudio.pianobar.kxuvzq.model.PianobarLog
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.model.PlaylistCachePolicy
import com.aistudio.pianobar.kxuvzq.model.Station
import com.aistudio.pianobar.kxuvzq.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PianobarPlayerManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var streamWatchdogJob: Job? = null
    private var bufferingWatchdogJob: Job? = null
    private var networkRecoveryJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var appContext: Context? = null
    private var preferences: com.aistudio.pianobar.kxuvzq.data.PianobarPreferences? = null
    private val pandoraApiClient = com.aistudio.pianobar.kxuvzq.pandora.PandoraApiClient()

    private val _stations = MutableStateFlow<List<Station>>(StationRepository.defaultStations)
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    // Live FIFO track queue per station token/id (matching pianobar's linked-list playlist)
    private val stationPlaylistQueues = mutableMapOf<String, ArrayDeque<Track>>()
    private var isFetchingPlaylist = false

    // Network failure & expired playlist detection state
    private var currentPlaybackAttemptId: Long = 0L
    private var consecutivePlaybackFailures: Int = 0
    private var hasTriedFreshPlaylistInFailureStreak: Boolean = false
    @Volatile
    private var networkInvalidatedPlaylist: Boolean = false
    @Volatile
    private var waitingForNetworkRecovery: Boolean = false
    private var needsInitialLoginSync: Boolean = false
    private var pendingLoginUser: String? = null
    private var pendingLoginPass: String? = null
    private var pendingLoginHq: Boolean = true
    private var pendingLoginBg: Boolean = true

    // Callbacks for service synchronization
    var onStateChanged: ((PlayerState) -> Unit)? = null

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            val prefs = com.aistudio.pianobar.kxuvzq.data.PianobarPreferences(context.applicationContext)
            preferences = prefs

            val savedUser = prefs.username
            val savedPass = prefs.password
            val isUserLoggedIn = prefs.isLoggedIn && savedUser.isNotBlank()
            val isHq = prefs.isHighQualityAudio

            _state.update {
                it.copy(
                    userEmail = savedUser,
                    isUserLoggedIn = isUserLoggedIn,
                    isHighQualityAudio = isHq
                )
            }

            addLog("[i]", "pianobar v2026.10 (Pandora CLI Client) initialized.", LogType.INFO)
            registerNetworkMonitor(context.applicationContext)

            if (isUserLoggedIn && savedPass.isNotBlank()) {
                addLog("[i]", "Authenticating with stored credentials for $savedUser...", LogType.INFO)
                saveAndLoginAccount(savedUser, savedPass, isHq, prefs.isBackgroundStreaming)
            } else {
                addLog("[?]", "No credentials configured. Please sign in to Pandora to load stations.", LogType.PROMPT)
            }
        }
    }

    private fun registerNetworkMonitor(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i("PianobarPlayer", "Network interface available / re-established. Invalidating cached playlist & resetting HTTP pool.")
                    networkInvalidatedPlaylist = true
                    pandoraApiClient.resetNetworkConnections()

                    scope.launch {
                        networkRecoveryJob?.cancel()
                        networkRecoveryJob = scope.launch {
                            // Brief delay so Android routing table and DNS settle after VPN/Tailscale toggle
                            delay(600L)
                            if (needsInitialLoginSync && !pendingLoginUser.isNullOrBlank() && !pendingLoginPass.isNullOrBlank()) {
                                addLog("[#]", "Network restored. Retrying Pandora authentication...", LogType.INFO)
                                saveAndLoginAccount(pendingLoginUser!!, pendingLoginPass!!, pendingLoginHq, pendingLoginBg)
                                return@launch
                            }
                            if (_state.value.isUserLoggedIn && !_state.value.isPlaying && (waitingForNetworkRecovery || _state.value.isBuffering)) {
                                waitingForNetworkRecovery = false
                                consecutivePlaybackFailures = 0
                                hasTriedFreshPlaylistInFailureStreak = false
                                val station = _state.value.currentStation ?: _stations.value.firstOrNull()
                                if (station != null) {
                                    stopPlayback()
                                    stationPlaylistQueues[station.id]?.clear()
                                    networkInvalidatedPlaylist = false
                                    addLog("[#]", "Network restored. Requesting fresh playlist for \"${station.name}\"...", LogType.INFO)
                                    fetchPlaylistAndPlay(station, autoPlay = true)
                                }
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    Log.w("PianobarPlayer", "Network lost. Marking cached playlists for refresh.")
                    networkInvalidatedPlaylist = true
                    pandoraApiClient.resetNetworkConnections()
                }
            })
        } catch (e: Exception) {
            Log.w("PianobarPlayer", "Could not register network callback: ${e.message}")
        }
    }

    private fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun addLog(prefix: String, message: String, type: LogType) {
        val log = PianobarLog(
            timeFormatted = getFormattedTime(),
            prefix = prefix,
            message = message,
            type = type
        )
        _state.update { current ->
            val updatedLogs = (current.logs + log).takeLast(100)
            current.copy(logs = updatedLogs)
        }
        notifyState()
    }

    private fun notifyState() {
        val currentState = _state.value
        onStateChanged?.invoke(currentState)
        appContext?.let {
            com.aistudio.pianobar.kxuvzq.widget.PianobarWidgetProvider.updateIfStateChanged(it, currentState)
        }
    }

    private fun clearExpiredOrInvalidatedQueue(stationId: String): Boolean {
        val queue = stationPlaylistQueues[stationId] ?: return false
        if (queue.isEmpty()) return false

        if (networkInvalidatedPlaylist) {
            queue.clear()
            networkInvalidatedPlaylist = false
            return true
        }

        var removedAny = false
        while (queue.isNotEmpty() && PlaylistCachePolicy.isTrackExpired(queue.first())) {
            queue.removeFirst()
            removedAny = true
        }
        return removedAny && queue.isEmpty()
    }

    fun selectStation(station: Station, autoPlay: Boolean = true) {
        addLog("[s]", "Selected station: \"${station.name}\" (${station.genre})", LogType.COMMAND)
        stopPlayback()
        consecutivePlaybackFailures = 0
        hasTriedFreshPlaylistInFailureStreak = false
        waitingForNetworkRecovery = false
        preferences?.lastStationId = station.id

        val queue = stationPlaylistQueues.getOrPut(station.id) { ArrayDeque() }
        val clearedExpired = clearExpiredOrInvalidatedQueue(station.id)
        if (clearedExpired) {
            addLog("[i]", "Cached playlist for \"${station.name}\" expired. Fetching fresh tracks...", LogType.INFO)
        }

        _state.update {
            it.copy(
                currentStation = station,
                currentTrack = null,
                isBuffering = true
            )
        }
        notifyState()

        if (queue.isNotEmpty()) {
            val firstTrack = queue.removeFirst()
            _state.update {
                it.copy(
                    currentStation = station,
                    currentTrack = firstTrack,
                    isBuffering = false,
                    currentPositionMs = 0L,
                    durationMs = firstTrack.durationMs
                )
            }
            notifyState()
            if (autoPlay) playTrack(firstTrack)
            // Pre-fetch if queue is low
            checkAndReplenishPlaylist(station.id)
            return
        }

        // Queue is empty: fetch initial batch from Pandora
        fetchPlaylistAndPlay(station, autoPlay = autoPlay)
    }

    private fun fetchPlaylistAndPlay(station: Station, autoPlay: Boolean) {
        if (!_state.value.isUserLoggedIn || isFetchingPlaylist) return
        isFetchingPlaylist = true
        _state.update { it.copy(isBuffering = true) }
        notifyState()

        addLog("[i]", "Receiving new playlist for \"${station.name}\"...", LogType.INFO)
        scope.launch {
            try {
                val liveTracks = pandoraApiClient.fetchStationPlaylist(station.id)
                val queue = stationPlaylistQueues.getOrPut(station.id) { ArrayDeque() }
                queue.addAll(liveTracks)

                if (queue.isNotEmpty()) {
                    waitingForNetworkRecovery = false
                    networkInvalidatedPlaylist = false
                    val nextTrack = queue.removeFirst()
                    _state.update {
                        it.copy(
                            currentStation = station,
                            currentTrack = nextTrack,
                            isBuffering = false,
                            currentPositionMs = 0L,
                            durationMs = nextTrack.durationMs
                        )
                    }
                    addLog("[#]", "Loaded ${liveTracks.size} tracks from Pandora Music Genome.", LogType.INFO)
                    notifyState()
                    if (autoPlay) playTrack(nextTrack)
                } else {
                    waitingForNetworkRecovery = true
                    networkInvalidatedPlaylist = true
                    addLog("[!]", "Could not load tracks for \"${station.name}\". Waiting for network or tap Skip [n] to retry.", LogType.WARNING)
                    _state.update { it.copy(isBuffering = false, isPlaying = false) }
                    notifyState()
                }
            } catch (e: Exception) {
                Log.e("PianobarPlayer", "Failed to fetch playlist", e)
                waitingForNetworkRecovery = true
                networkInvalidatedPlaylist = true
                addLog("[!]", "Network error fetching playlist: ${e.message}. Tuner connection reset.", LogType.WARNING)
                pandoraApiClient.resetNetworkConnections()
                _state.update { it.copy(isBuffering = false, isPlaying = false) }
                notifyState()
            } finally {
                isFetchingPlaylist = false
            }
        }
    }

    private fun checkAndReplenishPlaylist(stationId: String) {
        val queue = stationPlaylistQueues[stationId] ?: return
        if (queue.size <= 2 && !isFetchingPlaylist && _state.value.isUserLoggedIn) {
            isFetchingPlaylist = true
            scope.launch {
                try {
                    val moreTracks = pandoraApiClient.fetchStationPlaylist(stationId)
                    if (moreTracks.isNotEmpty()) {
                        queue.addAll(moreTracks)
                        addLog("[i]", "Replenished playlist with ${moreTracks.size} additional tracks from Pandora.", LogType.INFO)
                    }
                } catch (e: Exception) {
                    Log.w("PianobarPlayer", "Replenish error: ${e.message}")
                } finally {
                    isFetchingPlaylist = false
                }
            }
        }
    }

    fun playTrack(track: Track) {
        stopPlayback()
        val attemptId = currentPlaybackAttemptId
        var isPreparedForAttempt = false

        appContext?.let { com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService.start(it) }
        _state.update {
            it.copy(
                currentTrack = track,
                isBuffering = true,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = track.durationMs
            )
        }
        notifyState()

        addLog("[#]", "Station \"${_state.value.currentStation?.name}\" (0)", LogType.INFO)
        addLog("[#]", "|> \"${track.title}\" by \"${track.artist}\" on \"${track.album}\"", LogType.PLAYING)
        addLog("[i]", "Stream: ${track.audioBitrate} • Genome ID: ${track.id}", LogType.INFO)

        // 5-second watchdog: if MediaPlayer stalls while preparing, abort and skip/refresh
        streamWatchdogJob = scope.launch {
            delay(STREAM_TIMEOUT_MS)
            if (isActive && attemptId == currentPlaybackAttemptId && !isPreparedForAttempt) {
                Log.w("PianobarPlayer", "5s stream prepare timeout for \"${track.title}\"")
                addLog("[!]", "Stream timed out after 5s on \"${track.title}\". Skipping to next track...", LogType.WARNING)
                handleStreamFailure(track, "5s prepare timeout", attemptId)
            }
        }

        try {
            val player = MediaPlayer().apply {
                appContext?.let { setWakeMode(it, PowerManager.PARTIAL_WAKE_LOCK) }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                val rawUrl = track.streamUrl
                // Pandora media servers support HTTPS; HTTPS also ensures Android network security policy compliance
                val streamUri = android.net.Uri.parse(if (rawUrl.startsWith("http://")) rawUrl.replaceFirst("http://", "https://") else rawUrl)
                val context = appContext
                if (context != null) {
                    setDataSource(context, streamUri)
                } else {
                    setDataSource(streamUri.toString())
                }

                setOnPreparedListener { mp ->
                    if (attemptId != currentPlaybackAttemptId) return@setOnPreparedListener
                    isPreparedForAttempt = true
                    streamWatchdogJob?.cancel()
                    waitingForNetworkRecovery = false

                    _state.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = true,
                            durationMs = if (mp.duration > 0) mp.duration.toLong() else track.durationMs
                        )
                    }
                    mp.start()
                    startProgressTracker(attemptId, track)
                    notifyState()
                    addLog("[#]", "Started streaming playback.", LogType.INFO)
                }
                setOnInfoListener { _, what, _ ->
                    if (attemptId != currentPlaybackAttemptId) return@setOnInfoListener false
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                            _state.update { it.copy(isBuffering = true) }
                            notifyState()
                            bufferingWatchdogJob?.cancel()
                            bufferingWatchdogJob = scope.launch {
                                delay(STREAM_TIMEOUT_MS)
                                if (isActive && attemptId == currentPlaybackAttemptId && _state.value.isBuffering) {
                                    Log.w("PianobarPlayer", "5s buffering stall timeout for \"${track.title}\"")
                                    addLog("[!]", "Stream stalled buffering for 5s on \"${track.title}\". Skipping...", LogType.WARNING)
                                    handleStreamFailure(track, "5s buffering stall", attemptId)
                                }
                            }
                        }
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                            bufferingWatchdogJob?.cancel()
                            _state.update { it.copy(isBuffering = false) }
                            notifyState()
                        }
                    }
                    false
                }
                setOnCompletionListener {
                    if (attemptId != currentPlaybackAttemptId) return@setOnCompletionListener
                    consecutivePlaybackFailures = 0
                    hasTriedFreshPlaylistInFailureStreak = false
                    addLog("[i]", "Track finished. Transitioning to next song...", LogType.INFO)
                    skipNext()
                }
                setOnErrorListener { _, what, extra ->
                    if (attemptId != currentPlaybackAttemptId) return@setOnErrorListener true
                    Log.e("PianobarPlayer", "Playback error: what=$what, extra=$extra")
                    addLog("[!]", "Streaming error ($what, $extra) on \"${track.title}\".", LogType.WARNING)
                    streamWatchdogJob?.cancel()
                    bufferingWatchdogJob?.cancel()
                    scope.launch {
                        delay(300)
                        handleStreamFailure(track, "MediaPlayer error ($what, $extra)", attemptId)
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("PianobarPlayer", "Failed to start media player", e)
            addLog("[!]", "Error initializing player: ${e.message}", LogType.WARNING)
            handleStreamFailure(track, "Player init error: ${e.message}", attemptId)
        }

        // Add to history
        _state.update { current ->
            val updatedHistory = listOf(track) + current.history.filter { it.id != track.id }
            current.copy(history = updatedHistory.take(50))
        }
    }

    private fun handleStreamFailure(failedTrack: Track, reason: String, attemptId: Long) {
        if (attemptId != currentPlaybackAttemptId) return
        stopPlayback()
        _state.update { it.copy(isBuffering = false, isPlaying = false) }
        notifyState()

        consecutivePlaybackFailures++
        val station = _state.value.currentStation ?: _stations.value.firstOrNull() ?: return
        val queue = stationPlaylistQueues.getOrPut(station.id) { ArrayDeque() }

        // Prune any time-expired tracks from the cached queue
        while (queue.isNotEmpty() && PlaylistCachePolicy.isTrackExpired(queue.first())) {
            queue.removeFirst()
        }

        if (hasTriedFreshPlaylistInFailureStreak && consecutivePlaybackFailures >= 3) {
            waitingForNetworkRecovery = true
            networkInvalidatedPlaylist = true
            addLog("[!]", "Stream failed ($reason) after refreshing playlist. Waiting for stable network or tap Skip [n] to retry.", LogType.WARNING)
            return
        }

        // Detect when cached playlist is stale/expired:
        // - Network changed/dropped since fetch, OR
        // - Queue was expired/emptied, OR
        // - 2 consecutive tracks failed (proving the cached batch URLs are dead)
        val shouldInvalidateCache = networkInvalidatedPlaylist || queue.isEmpty() || consecutivePlaybackFailures >= 2

        if (shouldInvalidateCache && !hasTriedFreshPlaylistInFailureStreak) {
            if (queue.isNotEmpty()) {
                addLog("[!]", "Cached playlist expired/stale ($reason). Discarding ${queue.size} cached tracks and requesting fresh playlist from Pandora...", LogType.WARNING)
            } else {
                addLog("[!]", "Requesting fresh playlist from Pandora after stream failure ($reason)...", LogType.WARNING)
            }
            queue.clear()
            networkInvalidatedPlaylist = false
            hasTriedFreshPlaylistInFailureStreak = true
            pandoraApiClient.resetNetworkConnections()
            fetchPlaylistAndPlay(station, autoPlay = true)
        } else if (queue.isNotEmpty()) {
            val nextTrack = queue.removeFirst()
            addLog("[n]", "Skipping failed track -> Next: \"${nextTrack.title}\"", LogType.COMMAND)
            playTrack(nextTrack)
            checkAndReplenishPlaylist(station.id)
        } else {
            waitingForNetworkRecovery = true
            networkInvalidatedPlaylist = true
            addLog("[!]", "No playable tracks available ($reason). Waiting for network or tap Skip [n] to retry.", LogType.WARNING)
        }
    }

    fun togglePlayPause() {
        if (needsInitialLoginSync && !pendingLoginUser.isNullOrBlank() && !pendingLoginPass.isNullOrBlank()) {
            saveAndLoginAccount(pendingLoginUser!!, pendingLoginPass!!, pendingLoginHq, pendingLoginBg)
            return
        }
        val player = mediaPlayer
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.pause()
                    _state.update { it.copy(isPlaying = false) }
                    addLog("[p]", "Playback paused.", LogType.COMMAND)
                    notifyState()
                } else {
                    val track = _state.value.currentTrack
                    val station = _state.value.currentStation ?: _stations.value.firstOrNull()
                    if (station != null && (networkInvalidatedPlaylist || (track != null && PlaylistCachePolicy.isTrackExpired(track)))) {
                        stopPlayback()
                        waitingForNetworkRecovery = false
                        clearExpiredOrInvalidatedQueue(station.id)
                        addLog("[i]", "Cached stream expired after pause/network change. Fetching fresh tracks...", LogType.INFO)
                        fetchPlaylistAndPlay(station, autoPlay = true)
                    } else {
                        appContext?.let { com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService.start(it) }
                        player.start()
                        _state.update { it.copy(isPlaying = true) }
                        startProgressTracker(currentPlaybackAttemptId, track)
                        addLog("[p]", "Playback resumed.", LogType.COMMAND)
                        notifyState()
                    }
                }
            } catch (e: Exception) {
                Log.w("PianobarPlayer", "Toggle play error: ${e.message}")
                _state.value.currentTrack?.let { playTrack(it) }
            }
        } else {
            val track = _state.value.currentTrack
            val station = _state.value.currentStation ?: _stations.value.firstOrNull()
            if (track != null && !waitingForNetworkRecovery && !networkInvalidatedPlaylist && !PlaylistCachePolicy.isTrackExpired(track)) {
                playTrack(track)
            } else if (station != null) {
                waitingForNetworkRecovery = false
                clearExpiredOrInvalidatedQueue(station.id)
                fetchPlaylistAndPlay(station, autoPlay = true)
            }
        }
    }

    fun play() {
        if (needsInitialLoginSync && !pendingLoginUser.isNullOrBlank() && !pendingLoginPass.isNullOrBlank()) {
            saveAndLoginAccount(pendingLoginUser!!, pendingLoginPass!!, pendingLoginHq, pendingLoginBg)
            return
        }
        try {
            val player = mediaPlayer
            if (player != null && !player.isPlaying) {
                val track = _state.value.currentTrack
                val station = _state.value.currentStation ?: _stations.value.firstOrNull()
                if (station != null && (networkInvalidatedPlaylist || (track != null && PlaylistCachePolicy.isTrackExpired(track)))) {
                    stopPlayback()
                    waitingForNetworkRecovery = false
                    clearExpiredOrInvalidatedQueue(station.id)
                    addLog("[i]", "Cached stream expired after pause/network change. Fetching fresh tracks...", LogType.INFO)
                    fetchPlaylistAndPlay(station, autoPlay = true)
                } else {
                    appContext?.let { com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService.start(it) }
                    player.start()
                    _state.update { it.copy(isPlaying = true) }
                    startProgressTracker(currentPlaybackAttemptId, track)
                    notifyState()
                    addLog("[p]", "Playback started.", LogType.COMMAND)
                }
            } else if (player == null) {
                val track = _state.value.currentTrack
                val station = _state.value.currentStation ?: _stations.value.firstOrNull()
                if (track != null && !waitingForNetworkRecovery && !networkInvalidatedPlaylist && !PlaylistCachePolicy.isTrackExpired(track)) {
                    playTrack(track)
                } else if (station != null) {
                    waitingForNetworkRecovery = false
                    clearExpiredOrInvalidatedQueue(station.id)
                    fetchPlaylistAndPlay(station, autoPlay = true)
                }
            }
        } catch (e: Exception) {
            Log.w("PianobarPlayer", "Play error: ${e.message}")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _state.update { s -> s.copy(isPlaying = false) }
                    notifyState()
                    addLog("[p]", "Playback paused.", LogType.COMMAND)
                }
            }
        } catch (e: Exception) {
            Log.w("PianobarPlayer", "Pause error: ${e.message}")
        }
    }

    fun skipNext() {
        if (needsInitialLoginSync && !pendingLoginUser.isNullOrBlank() && !pendingLoginPass.isNullOrBlank()) {
            addLog("[n]", "Retrying Pandora login & station sync...", LogType.COMMAND)
            saveAndLoginAccount(pendingLoginUser!!, pendingLoginPass!!, pendingLoginHq, pendingLoginBg)
            return
        }
        val station = _state.value.currentStation ?: _stations.value.firstOrNull() ?: return
        // Stop current media player immediately to release any hanging network socket
        stopPlayback()
        waitingForNetworkRecovery = false

        val queue = stationPlaylistQueues.getOrPut(station.id) { ArrayDeque() }

        // If network was invalidated or previous track had just failed, discard stale cached queue immediately
        if (queue.isNotEmpty() && (networkInvalidatedPlaylist || consecutivePlaybackFailures > 0)) {
            addLog("[!]", "Discarding stale cached playlist (${queue.size} tracks) after network/stream failure...", LogType.WARNING)
            queue.clear()
            networkInvalidatedPlaylist = false
        } else {
            val clearedExpired = clearExpiredOrInvalidatedQueue(station.id)
            if (clearedExpired) {
                addLog("[i]", "Cached playlist expired. Fetching fresh songs from Pandora...", LogType.INFO)
            }
        }

        consecutivePlaybackFailures = 0
        hasTriedFreshPlaylistInFailureStreak = false

        if (queue.isNotEmpty()) {
            val nextTrack = queue.removeFirst()
            addLog("[n]", "Skipping track -> Next: \"${nextTrack.title}\"", LogType.COMMAND)
            playTrack(nextTrack)
            checkAndReplenishPlaylist(station.id)
        } else {
            // Queue empty or stale after network outage -> evict dead sockets and re-fetch from scratch
            pandoraApiClient.resetNetworkConnections()
            addLog("[n]", "Requesting fresh tracks from Pandora tuner...", LogType.COMMAND)
            fetchPlaylistAndPlay(station, autoPlay = true)
        }
    }

    fun loveCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        val isCurrentlyLoved = _state.value.lovedTrackIds.contains(track.id)
        val newLovedState = !isCurrentlyLoved

        _state.update { current ->
            val set = current.lovedTrackIds.toMutableSet()
            if (newLovedState) set.add(track.id) else set.remove(track.id)
            current.copy(lovedTrackIds = set)
        }
        if (newLovedState) {
            addLog("[+]", "Loved song \"${track.title}\" by \"${track.artist}\"!", LogType.LOVE)
            val stId = _state.value.currentStation?.id
            if (stId != null) {
                scope.launch { pandoraApiClient.addFeedback(stId, track.id, true) }
            }
        } else {
            addLog("[i]", "Unloved song \"${track.title}\"", LogType.INFO)
        }
        notifyState()
    }

    fun banCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        _state.update { current ->
            val set = current.bannedTrackIds.toMutableSet()
            set.add(track.id)
            current.copy(bannedTrackIds = set)
        }
        addLog("[-]", "Banned song \"${track.title}\" by \"${track.artist}\". Skipping now...", LogType.BAN)
        val stId = _state.value.currentStation?.id
        if (stId != null) {
            scope.launch { pandoraApiClient.addFeedback(stId, track.id, false) }
        }
        skipNext()
    }

    fun tiredOfTrack() {
        val track = _state.value.currentTrack ?: return
        addLog("[t]", "Tired of song \"${track.title}\". Snoozing from playlist for 30 days.", LogType.COMMAND)
        skipNext()
    }

    fun bookmarkTrack() {
        val track = _state.value.currentTrack ?: return
        _state.update { current ->
            val set = current.bookmarkedTrackIds.toMutableSet()
            val added = set.add(track.id)
            if (!added) set.remove(track.id)
            current.copy(bookmarkedTrackIds = set)
        }
        addLog("[b]", "Bookmarked song \"${track.title}\" by \"${track.artist}\".", LogType.COMMAND)
        scope.launch { pandoraApiClient.addSongBookmark(track.id) }
        notifyState()
    }

    fun requestExplanation(onComplete: ((String) -> Unit)? = null) {
        val track = _state.value.currentTrack
        if (track == null) {
            addLog("[!]", "No track playing to explain.", LogType.WARNING)
            onComplete?.invoke("No track currently playing.")
            return
        }

        addLog("[i]", "Receiving explanation for \"${track.title}\" from Music Genome...", LogType.INFO)
        scope.launch {
            val liveExplanation = pandoraApiClient.explainTrack(track.id)
            val explanation = if (!liveExplanation.isNullOrBlank()) {
                liveExplanation
            } else if (track.musicGenomeExplanation.isNotBlank()) {
                track.musicGenomeExplanation
            } else {
                "No explanation provided by Pandora for this track."
            }

            // Update current track explanation in player state
            _state.update { current ->
                if (current.currentTrack?.id == track.id) {
                    current.copy(currentTrack = track.copy(musicGenomeExplanation = explanation))
                } else {
                    current
                }
            }
            addLog("[e]", explanation, LogType.INFO)
            notifyState()
            onComplete?.invoke(explanation)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let {
                it.seekTo(positionMs.toInt())
                _state.update { s -> s.copy(currentPositionMs = positionMs) }
                notifyState()
            }
        } catch (e: Exception) {
            Log.w("PianobarPlayer", "Seek error: ${e.message}")
        }
    }

    fun toggleCliMode() {
        _state.update { it.copy(isCliMode = !it.isCliMode) }
        val mode = if (_state.value.isCliMode) "Terminal CLI Mode" else "Material 3 UI Mode"
        addLog("[m]", "Switched view mode to: $mode", LogType.INFO)
        notifyState()
    }

    fun createCustomStation(name: String, seed: String, genre: String) {
        val newStation = StationRepository.createCustomStation(name, seed, genre)
        _stations.update { it + newStation }
        addLog("[c]", "Station created: \"${newStation.name}\" seeded from \"$seed\"", LogType.COMMAND)
        selectStation(newStation, autoPlay = true)
    }

    fun deleteStation(stationId: String) {
        val station = _stations.value.find { it.id == stationId } ?: return
        if (station.isQuickMix) {
            addLog("[!]", "Cannot delete default QuickMix station.", LogType.WARNING)
            return
        }
        _stations.update { list -> list.filter { it.id != stationId } }
        addLog("[d]", "Deleted station \"${station.name}\"", LogType.COMMAND)
        if (_state.value.currentStation?.id == stationId) {
            _stations.value.firstOrNull()?.let { selectStation(it, autoPlay = true) }
        }
    }

    fun toggleStationQuickMix(stationId: String) {
        _stations.update { list ->
            list.map { st ->
                if (st.id == stationId) st.copy(isQuickMix = !st.isQuickMix) else st
            }
        }
        addLog("[q]", "Toggled QuickMix inclusion for station.", LogType.COMMAND)
    }

    fun saveAndLoginAccount(user: String, pass: String, highQuality: Boolean, backgroundStreaming: Boolean) {
        preferences?.saveAccount(user, pass, highQuality, backgroundStreaming)
        val trimmedUser = user.trim()
        val isNowLoggedIn = trimmedUser.isNotBlank()

        if (!isNowLoggedIn) {
            networkRecoveryJob?.cancel()
            networkRecoveryJob = null
            needsInitialLoginSync = false
            pendingLoginUser = null
            pendingLoginPass = null
            waitingForNetworkRecovery = false
            consecutivePlaybackFailures = 0
            hasTriedFreshPlaylistInFailureStreak = false
            networkInvalidatedPlaylist = false
            pandoraApiClient.clearSession()
            _state.update {
                it.copy(
                    userEmail = "",
                    isUserLoggedIn = false,
                    isHighQualityAudio = highQuality
                )
            }
            _stations.value = StationRepository.defaultStations
            addLog("[i]", "Credentials cleared. Switched to Guest Mode.", LogType.INFO)
            notifyState()
            return
        }

        pendingLoginUser = trimmedUser
        pendingLoginPass = pass.trim()
        pendingLoginHq = highQuality
        pendingLoginBg = backgroundStreaming

        _state.update {
            it.copy(
                userEmail = trimmedUser,
                isUserLoggedIn = true,
                isHighQualityAudio = highQuality,
                isBuffering = true,
                error = null
            )
        }
        addLog("[i]", "Initiating Pandora JSON-RPC v5 partner handshake (android)...", LogType.INFO)
        addLog("[i]", "Connecting to tuner.pandora.com...", LogType.INFO)
        notifyState()

        scope.launch {
            when (val authResult = pandoraApiClient.loginAndFetchStations(trimmedUser, pass.trim())) {
                is com.aistudio.pianobar.kxuvzq.pandora.PandoraAuthResult.Success -> {
                    needsInitialLoginSync = false
                    waitingForNetworkRecovery = false
                    networkInvalidatedPlaylist = false
                    addLog("[#]", "Pandora authentication succeeded (User ID: ${authResult.userId})", LogType.INFO)
                    addLog("[#]", "Blowfish session established. Syncing station list...", LogType.INFO)

                    if (authResult.stations.isNotEmpty()) {
                        val sortedStations = authResult.stations.sortedWith(
                            compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                        )
                        _stations.value = sortedStations
                        addLog("[#]", "Loaded ${sortedStations.size} personalized Pandora stations into tuner (alphabetized).", LogType.INFO)

                        val savedStationId = preferences?.lastStationId
                        val initial = (if (!savedStationId.isNullOrBlank()) {
                            sortedStations.firstOrNull { it.id == savedStationId }
                        } else null)
                            ?: sortedStations.firstOrNull { !it.isQuickMix }
                            ?: sortedStations.first()

                        addLog("[i]", "Resuming station: \"${initial.name}\"", LogType.INFO)
                        selectStation(initial, autoPlay = true)
                    } else {
                        addLog("[i]", "No custom stations found on account. Created Pandora Personal station.", LogType.INFO)
                        val personalName = "${trimmedUser.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }}'s Radio"
                        createCustomStation(personalName, trimmedUser.substringBefore("@"), "Pandora Personal")
                    }
                    _state.update { it.copy(isBuffering = false) }
                }
                is com.aistudio.pianobar.kxuvzq.pandora.PandoraAuthResult.Error -> {
                    if (authResult.code != null) {
                        // Explicit Pandora API auth rejection (e.g. invalid credentials)
                        needsInitialLoginSync = false
                        addLog("[!]", "Pandora Login Error: ${authResult.message}", LogType.WARNING)
                        _state.update { it.copy(isBuffering = false, isUserLoggedIn = false, error = authResult.message) }
                    } else {
                        // Transient network failure (e.g. Tailscale/VPN blocking connection) - preserve logged-in state & retry automatically
                        needsInitialLoginSync = true
                        waitingForNetworkRecovery = true
                        addLog("[!]", "Network unavailable during login (${authResult.message}). Will auto-retry when network recovers or tap Skip [n].", LogType.WARNING)
                        _state.update { it.copy(isBuffering = false, isUserLoggedIn = true, error = authResult.message) }
                    }
                }
            }
            notifyState()
        }
    }

    fun logoutAccount() {
        stopPlayback()
        networkRecoveryJob?.cancel()
        networkRecoveryJob = null
        needsInitialLoginSync = false
        pendingLoginUser = null
        pendingLoginPass = null
        waitingForNetworkRecovery = false
        consecutivePlaybackFailures = 0
        hasTriedFreshPlaylistInFailureStreak = false
        networkInvalidatedPlaylist = false
        isFetchingPlaylist = false
        pandoraApiClient.clearSession()
        preferences?.clearAccount()
        _stations.value = emptyList()
        stationPlaylistQueues.clear()
        _state.update {
            it.copy(
                userEmail = "",
                isUserLoggedIn = false,
                currentStation = null,
                currentTrack = null,
                isPlaying = false,
                isBuffering = false,
                error = null
            )
        }
        addLog("[i]", "Signed out from Pandora account.", LogType.INFO)
        notifyState()
    }

    fun getPreferences(): com.aistudio.pianobar.kxuvzq.data.PianobarPreferences? = preferences

    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim().lowercase()
        addLog("[>]", "> $trimmed", LogType.COMMAND)
        when (trimmed) {
            "p", "pause", "play" -> togglePlayPause()
            "n", "next", "skip" -> skipNext()
            "+", "love" -> loveCurrentTrack()
            "-", "ban" -> banCurrentTrack()
            "t", "tired" -> tiredOfTrack()
            "b", "bookmark" -> bookmarkTrack()
            "e", "explain" -> {
                requestExplanation()
            }
            "h", "history" -> {
                addLog("[h]", "Recent Song History (${_state.value.history.size} items):", LogType.INFO)
                _state.value.history.take(5).forEachIndexed { i, t ->
                    addLog("[h]", "${i + 1}. \"${t.title}\" - ${t.artist}", LogType.INFO)
                }
            }
            "s", "station" -> {
                addLog("[s]", "Available Stations:", LogType.INFO)
                _stations.value.forEachIndexed { idx, st ->
                    val activeMark = if (st.id == _state.value.currentStation?.id) "*" else " "
                    addLog("[s]", "[$idx] $activeMark ${st.name} (${st.genre})", LogType.INFO)
                }
                addLog("[?]", "To switch station, tap station chip or type station index e.g. '0'", LogType.PROMPT)
            }
            "q", "quit" -> {
                togglePlayPause()
                addLog("[q]", "Stopping playback session.", LogType.INFO)
            }
            "sleep" -> {
                if (_state.value.isSleepTimerActive) {
                    cancelSleepTimer()
                } else {
                    startSleepTimer(30)
                }
            }
            "help", "?" -> {
                addLog("[i]", "Pianobar CLI Commands:", LogType.INFO)
                addLog("[i]", "p: Play/Pause | n: Next song | +: Love song | -: Ban song", LogType.INFO)
                addLog("[i]", "t: Tired (snooze) | b: Bookmark | e: Explain song | s: Stations", LogType.INFO)
                addLog("[i]", "h: Song History | sleep: Toggle 30m Sleep Timer | q: Stop playback", LogType.INFO)
            }
            else -> {
                if (trimmed.startsWith("sleep ")) {
                    val mins = trimmed.removePrefix("sleep ").trim().toIntOrNull()
                    if (mins != null && mins > 0) {
                        startSleepTimer(mins)
                    } else {
                        addLog("[!]", "Usage: sleep <minutes> (e.g., 'sleep 45') or 'sleep' to toggle", LogType.WARNING)
                    }
                } else {
                    val index = trimmed.toIntOrNull()
                    if (index != null && index in 0 until _stations.value.size) {
                        selectStation(_stations.value[index], autoPlay = true)
                    } else {
                        addLog("[!]", "Unknown command: '$trimmed'. Type 'help' for shortcut list.", LogType.WARNING)
                    }
                }
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer(notify = false)
        val totalSeconds = minutes * 60L
        _state.update {
            it.copy(
                isSleepTimerActive = true,
                sleepTimerRemainingSeconds = totalSeconds
            )
        }
        addLog("[z]", "Sleep timer activated: stopping playback in $minutes minutes.", LogType.INFO)
        notifyState()

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _state.update {
                    it.copy(sleepTimerRemainingSeconds = remaining)
                }
                notifyState()
            }
            if (isActive && remaining <= 0L) {
                _state.update {
                    it.copy(
                        isSleepTimerActive = false,
                        sleepTimerRemainingSeconds = 0L
                    )
                }
                addLog("[z]", "Sleep timer expired: Stopping music playback now. Goodnight!", LogType.INFO)
                pausePlayback()
                notifyState()
            }
        }
    }

    fun cancelSleepTimer(notify: Boolean = true) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        val wasActive = _state.value.isSleepTimerActive
        _state.update {
            it.copy(
                isSleepTimerActive = false,
                sleepTimerRemainingSeconds = 0L
            )
        }
        if (notify && wasActive) {
            addLog("[z]", "Sleep timer cancelled.", LogType.INFO)
        }
        notifyState()
    }

    fun pausePlayback() {
        if (_state.value.isPlaying) {
            togglePlayPause()
        }
    }

    private fun startProgressTracker(attemptId: Long, track: Track?) {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastObservedPos = -1L
            var stallElapsedMs = 0L
            while (isActive && attemptId == currentPlaybackAttemptId) {
                val player = mediaPlayer
                if (player != null) {
                    try {
                        if (player.isPlaying) {
                            val currentPos = player.currentPosition.toLong()
                            _state.update { it.copy(currentPositionMs = currentPos) }
                            notifyState()

                            if (currentPos > 1500L && (consecutivePlaybackFailures > 0 || hasTriedFreshPlaylistInFailureStreak)) {
                                consecutivePlaybackFailures = 0
                                hasTriedFreshPlaylistInFailureStreak = false
                                networkInvalidatedPlaylist = false
                            }

                            val duration = _state.value.durationMs
                            if (track != null && currentPos == lastObservedPos && (duration <= 0L || currentPos < duration - 2000L)) {
                                stallElapsedMs += 500L
                                if (stallElapsedMs >= STREAM_TIMEOUT_MS) {
                                    Log.w("PianobarPlayer", "5s playback stall for \"${track.title}\" at ${currentPos}ms")
                                    addLog("[!]", "Playback stalled for 5s on \"${track.title}\". Skipping to next track...", LogType.WARNING)
                                    handleStreamFailure(track, "5s playback stall", attemptId)
                                    return@launch
                                }
                            } else {
                                stallElapsedMs = 0L
                                lastObservedPos = currentPos
                            }
                        } else {
                            stallElapsedMs = 0L
                        }
                    } catch (e: Exception) {
                        Log.w("PianobarPlayer", "Progress tracker error: ${e.message}")
                    }
                }
                delay(500)
            }
        }
    }

    fun stopPlayback() {
        currentPlaybackAttemptId++
        streamWatchdogJob?.cancel()
        streamWatchdogJob = null
        bufferingWatchdogJob?.cancel()
        bufferingWatchdogJob = null
        progressJob?.cancel()
        progressJob = null
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                player.reset()
                player.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        const val STREAM_TIMEOUT_MS = 5_000L

        @Volatile
        private var instance: PianobarPlayerManager? = null

        fun getInstance(): PianobarPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PianobarPlayerManager().also { instance = it }
            }
        }
    }
}
