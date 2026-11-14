package com.aistudio.pianobar.kxuvzq.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val durationMs: Long = 180000L,
    val streamUrl: String,
    val stationId: String,
    val isLoved: Boolean = false,
    val isBanned: Boolean = false,
    val isBookmarked: Boolean = false,
    val musicGenomeExplanation: String = "Rich piano melodies, mellow acoustic accompaniment, modal harmonic progressions, dynamic touch and gentle syncopation.",
    val audioBitrate: String = "AAC 192 kbps",
    val year: String = "2024",
    val fetchedAtMs: Long = System.currentTimeMillis()
)

object PlaylistCachePolicy {
    // Pandora audio URLs are time-limited; treat batches older than 25 minutes as expired.
    const val PLAYLIST_MAX_AGE_MS = 25L * 60L * 1000L
    private const val EXPIRATION_SAFETY_MARGIN_SEC = 30L
    private val EXP_PARAM_REGEX = Regex("(?:^|[?&])(?:e|exp|expires)=(\\d+)(?:&|$)", RegexOption.IGNORE_CASE)

    fun isTrackExpired(track: Track, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (nowMs - track.fetchedAtMs >= PLAYLIST_MAX_AGE_MS) {
            return true
        }
        val queryStart = track.streamUrl.indexOf('?')
        if (queryStart >= 0 && queryStart < track.streamUrl.length - 1) {
            val query = track.streamUrl.substring(queryStart)
            val match = EXP_PARAM_REGEX.find(query)
            val rawExp = match?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (rawExp != null) {
                val expSec = if (rawExp > 10_000_000_000L) rawExp / 1000L else rawExp
                val nowSec = nowMs / 1000L
                if (nowSec >= expSec - EXPIRATION_SAFETY_MARGIN_SEC) {
                    return true
                }
            }
        }
        return false
    }

    fun isQueueExpired(tracks: Collection<Track>, nowMs: Long = System.currentTimeMillis()): Boolean {
        return tracks.any { isTrackExpired(it, nowMs) }
    }
}

data class Station(
    val id: String,
    val name: String,
    val description: String,
    val genre: String,
    val isQuickMix: Boolean = false,
    val isCustom: Boolean = false,
    val defaultTracks: List<Track> = emptyList()
)

enum class LogType {
    INFO,       // [i]
    PLAYING,    // [#]
    LOVE,       // [+]
    BAN,        // [-]
    WARNING,    // [!]
    PROMPT,     // [?]
    COMMAND     // [>]
}

private val logIdCounter = java.util.concurrent.atomic.AtomicLong(1L)

data class PianobarLog(
    val id: Long = logIdCounter.getAndIncrement(),
    val timeFormatted: String,
    val prefix: String,
    val message: String,
    val type: LogType
)

data class PlayerState(
    val currentStation: Station? = null,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 180000L,
    val volume: Float = 1.0f,
    val lovedTrackIds: Set<String> = emptySet(),
    val bannedTrackIds: Set<String> = emptySet(),
    val bookmarkedTrackIds: Set<String> = emptySet(),
    val history: List<Track> = emptyList(),
    val isCliMode: Boolean = false,
    val logs: List<PianobarLog> = emptyList(),
    val userEmail: String = "",
    val isUserLoggedIn: Boolean = false,
    val isHighQualityAudio: Boolean = true,
    val sleepTimerRemainingSeconds: Long = 0L,
    val isSleepTimerActive: Boolean = false,
    val error: String? = null
)
