package com.aistudio.pianobar.kxuvzq.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aistudio.pianobar.kxuvzq.MainActivity
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.model.Track
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PianobarPlaybackService : Service(), AudioManager.OnAudioFocusChangeListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSession? = null
    private val playerManager = PianobarPlayerManager.getInstance()
    private var currentArtworkBitmap: Bitmap? = null
    private var currentArtworkUrl: String? = null

    companion object {
        const val CHANNEL_ID = "pianobar_playback_channel"
        const val NOTIFICATION_ID = 4041

        const val ACTION_START = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_START"
        const val ACTION_PLAY = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_PLAY"
        const val ACTION_PAUSE = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_NEXT"
        const val ACTION_LOVE = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_LOVE"
        const val ACTION_BAN = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_BAN"
        const val ACTION_STOP = "com.aistudio.pianobar.kxuvzq.pianobar.ACTION_STOP"

        const val CUSTOM_ACTION_LOVE = "com.aistudio.pianobar.kxuvzq.pianobar.CUSTOM_ACTION_LOVE"
        const val CUSTOM_ACTION_BAN = "com.aistudio.pianobar.kxuvzq.pianobar.CUSTOM_ACTION_BAN"

        fun start(context: Context) {
            val intent = Intent(context, PianobarPlaybackService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("PianobarService", "Error starting playback service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initMediaSession()
        requestAudioFocus()

        // Sync with player state
        serviceScope.launch {
            playerManager.state.collect { state ->
                syncArtworkAndMetadata(state)
                updateMediaSessionPlaybackState(state)
                updateNotification(state)
            }
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "PianobarSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    playerManager.play()
                }

                override fun onPause() {
                    playerManager.pause()
                }

                override fun onSkipToNext() {
                    playerManager.skipNext()
                }

                override fun onSeekTo(pos: Long) {
                    playerManager.seekTo(pos)
                }

                override fun onSetRating(rating: Rating) {
                    if (rating.isThumbUp) {
                        playerManager.loveCurrentTrack()
                    } else {
                        playerManager.banCurrentTrack()
                    }
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        CUSTOM_ACTION_LOVE -> playerManager.loveCurrentTrack()
                        CUSTOM_ACTION_BAN -> playerManager.banCurrentTrack()
                    }
                }

                override fun onStop() {
                    playerManager.pause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }
    }

    private fun syncArtworkAndMetadata(state: PlayerState) {
        val track = state.currentTrack ?: return

        // Update basic metadata immediately
        updateMediaMetadata(track, state, currentArtworkBitmap)

        // Asynchronously load artwork if URL changed
        if (track.artworkUrl != currentArtworkUrl) {
            currentArtworkUrl = track.artworkUrl
            serviceScope.launch {
                val loadedBitmap = loadArtwork(track.artworkUrl)
                currentArtworkBitmap = loadedBitmap
                updateMediaMetadata(track, state, loadedBitmap)
                updateNotification(state)
            }
        }
    }

    private suspend fun loadArtwork(url: String): Bitmap = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(this@PianobarPlaybackService)
            val request = ImageRequest.Builder(this@PianobarPlaybackService)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } ?: BitmapFactory.decodeResource(resources, R.drawable.ic_pianobar_logo)
    }

    private fun updateMediaMetadata(track: Track, state: PlayerState, artwork: Bitmap?) {
        val stationName = state.currentStation?.name ?: "Pianobar Radio"
        val fallbackBitmap = artwork ?: BitmapFactory.decodeResource(resources, R.drawable.ic_pianobar_logo)

        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "${track.artist} • $stationName")
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, "Pianobar Pandora Client • ${track.audioBitrate}")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)

        if (fallbackBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, fallbackBitmap)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, fallbackBitmap)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, fallbackBitmap)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updateMediaSessionPlaybackState(state: PlayerState) {
        val playbackStateVal = when {
            state.isPlaying -> PlaybackState.STATE_PLAYING
            state.isBuffering -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_PAUSED
        }

        val playbackSpeed = if (state.isPlaying) 1.0f else 0.0f
        val pbState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SEEK_TO or
                        PlaybackState.ACTION_STOP
            )
            .setState(playbackStateVal, state.currentPositionMs, playbackSpeed)
            .build()
        mediaSession?.setPlaybackState(pbState)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundNotification()
            }
            ACTION_PLAY -> playerManager.play()
            ACTION_PAUSE -> playerManager.pause()
            ACTION_TOGGLE -> playerManager.togglePlayPause()
            ACTION_NEXT -> playerManager.skipNext()
            ACTION_LOVE -> playerManager.loveCurrentTrack()
            ACTION_BAN -> playerManager.banCurrentTrack()
            ACTION_STOP -> {
                playerManager.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification(playerManager.state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: PlayerState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
        } catch (e: Exception) {
            Log.e("PianobarService", "Error notifying status", e)
        }
    }

    private fun buildNotification(state: PlayerState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play / Pause toggle pending intent
        val toggleIntent = Intent(this, PianobarPlaybackService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Skip next pending intent
        val nextIntent = Intent(this, PianobarPlaybackService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop / Close pending intent
        val stopIntent = Intent(this, PianobarPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            5,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val track = state.currentTrack
        val title = track?.title ?: "Pianobar"
        val artist = track?.artist ?: "Pandora Radio Client"
        val stationName = state.currentStation?.name ?: "QuickMix"

        val isPlaying = state.isPlaying

        // Play/Pause action button
        val playPauseIconRes = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseLabel = if (isPlaying) "Pause (p)" else "Play (p)"
        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(this, playPauseIconRes),
            playPauseLabel,
            togglePendingIntent
        ).build()

        // Skip Next action button
        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_skip),
            "Skip (n)",
            nextPendingIntent
        ).build()

        // Stop / Close action
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_close),
            "Close",
            stopPendingIntent
        ).build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val mediaStyle = Notification.MediaStyle()
            // Compact view displays Play/Pause and Next
            .setShowActionsInCompactView(0, 1)
        mediaSession?.let { mediaStyle.setMediaSession(it.sessionToken) }

        val largeArt = currentArtworkBitmap
            ?: BitmapFactory.decodeResource(resources, R.drawable.ic_pianobar_logo)

        builder.setContentTitle(title)
            .setContentText("$artist • $stationName")
            .setSubText("pianobar • ${track?.audioBitrate ?: "AAC 192k"}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeArt)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            // 0: Play / Pause
            .addAction(playPauseAction)
            // 1: Skip Next
            .addAction(nextAction)
            // 2: Stop / Close
            .addAction(stopAction)
            .setStyle(mediaStyle)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestAudioFocus() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                playerManager.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                playerManager.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus regained
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
