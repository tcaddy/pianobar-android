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
    val year: String = "2024"
)

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
