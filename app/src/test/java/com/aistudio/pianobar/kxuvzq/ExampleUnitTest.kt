package com.aistudio.pianobar.kxuvzq

import com.aistudio.pianobar.kxuvzq.model.PlaylistCachePolicy
import com.aistudio.pianobar.kxuvzq.model.Track
import com.aistudio.pianobar.kxuvzq.ui.theme.PianobarThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for local playlist cache expiration and helper logic.
 */
class ExampleUnitTest {

    private fun testTrack(
        streamUrl: String = "https://audio.pandora.com/access/?version=4&lid=123",
        fetchedAtMs: Long = 1_700_000_000_000L
    ): Track {
        return Track(
            id = "track_1",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            artworkUrl = "",
            durationMs = 180_000L,
            streamUrl = streamUrl,
            stationId = "station_1",
            fetchedAtMs = fetchedAtMs
        )
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun themes_haveUniqueIdsAndSafeFallback() {
        val ids = PianobarThemes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.contains(PianobarThemes.DEFAULT_ID))
        assertEquals(PianobarThemes.DEFAULT_ID, PianobarThemes.fromId(null).id)
        assertEquals(PianobarThemes.DEFAULT_ID, PianobarThemes.fromId("unknown").id)
        assertEquals("ice", PianobarThemes.fromId("ice").id)
        assertEquals("ai-slop", PianobarThemes.fromId("ai-slop").id)
    }

    @Test
    fun freshTrackWithoutExpiryParam_isNotExpired() {
        val fetchedAt = 1_700_000_000_000L
        val now = fetchedAt + (10L * 60L * 1000L) // 10 minutes old
        val track = testTrack(fetchedAtMs = fetchedAt)

        assertFalse(PlaylistCachePolicy.isTrackExpired(track, nowMs = now))
    }

    @Test
    fun trackOlderThanMaxAge_isExpired() {
        val fetchedAt = 1_700_000_000_000L
        val now = fetchedAt + PlaylistCachePolicy.PLAYLIST_MAX_AGE_MS
        val track = testTrack(fetchedAtMs = fetchedAt)

        assertTrue(PlaylistCachePolicy.isTrackExpired(track, nowMs = now))
    }

    @Test
    fun trackWithSecondsExpirationParams_respects30sSafetyMargin() {
        val nowSec = 1_700_000_000L
        val nowMs = nowSec * 1000L
        val fetchedAt = nowMs - 60_000L

        // Expires in 60s -> outside 30s safety margin -> still valid
        val validE = testTrack(
            streamUrl = "https://audio.pandora.com/stream?e=${nowSec + 60}&token=abc",
            fetchedAtMs = fetchedAt
        )
        assertFalse(PlaylistCachePolicy.isTrackExpired(validE, nowMs = nowMs))

        // Expires in 25s -> inside 30s safety margin -> treated as expired
        val expiringSoonExp = testTrack(
            streamUrl = "https://audio.pandora.com/stream?foo=1&exp=${nowSec + 25}",
            fetchedAtMs = fetchedAt
        )
        assertTrue(PlaylistCachePolicy.isTrackExpired(expiringSoonExp, nowMs = nowMs))

        // Already expired using EXPIRES (case-insensitive)
        val alreadyExpired = testTrack(
            streamUrl = "https://audio.pandora.com/stream?EXPIRES=${nowSec - 5}",
            fetchedAtMs = fetchedAt
        )
        assertTrue(PlaylistCachePolicy.isTrackExpired(alreadyExpired, nowMs = nowMs))
    }

    @Test
    fun trackWithMillisExpirationParam_isHandledCorrectly() {
        val nowMs = 1_700_000_000_000L
        val fetchedAt = nowMs - 60_000L

        val validMs = testTrack(
            streamUrl = "https://audio.pandora.com/stream?expires=${nowMs + 120_000L}",
            fetchedAtMs = fetchedAt
        )
        assertFalse(PlaylistCachePolicy.isTrackExpired(validMs, nowMs = nowMs))

        val expiredMs = testTrack(
            streamUrl = "https://audio.pandora.com/stream?expires=${nowMs + 10_000L}",
            fetchedAtMs = fetchedAt
        )
        assertTrue(PlaylistCachePolicy.isTrackExpired(expiredMs, nowMs = nowMs))
    }

    @Test
    fun isQueueExpired_returnsTrueWhenAnyTrackIsExpired() {
        val nowMs = 1_700_000_000_000L
        val fresh = testTrack(fetchedAtMs = nowMs - 60_000L)
        val stale = testTrack(fetchedAtMs = nowMs - PlaylistCachePolicy.PLAYLIST_MAX_AGE_MS - 1_000L)

        assertFalse(PlaylistCachePolicy.isQueueExpired(listOf(fresh), nowMs = nowMs))
        assertTrue(PlaylistCachePolicy.isQueueExpired(listOf(fresh, stale), nowMs = nowMs))
        assertFalse(PlaylistCachePolicy.isQueueExpired(emptyList(), nowMs = nowMs))
    }

    private fun relativeLuminance(color: androidx.compose.ui.graphics.Color): Double {
        fun linearize(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }

    private fun contrastRatio(
        fg: androidx.compose.ui.graphics.Color,
        bg: androidx.compose.ui.graphics.Color
    ): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun seriousPalettes_meetWcagAaTextAndActionContrast() {
        val serious = PianobarThemes.all.filter { it.id != "ai-slop" }
        assertEquals(5, serious.size)
        serious.forEach { palette ->
            assertTrue(
                "${palette.id} textPrimary on surface must meet 7.0:1 AAA",
                contrastRatio(palette.textPrimary, palette.surface) >= 7.0
            )
            assertTrue(
                "${palette.id} textSecondary on surface must meet 4.5:1 AA",
                contrastRatio(palette.textSecondary, palette.surface) >= 4.5
            )
            assertTrue(
                "${palette.id} textTertiary on surface must meet 4.5:1 AA",
                contrastRatio(palette.textTertiary, palette.surface) >= 4.5
            )
            assertTrue(
                "${palette.id} onAccent on accent must meet 4.5:1 AA",
                contrastRatio(palette.onAccent, palette.accent) >= 4.5
            )
        }
    }
}

