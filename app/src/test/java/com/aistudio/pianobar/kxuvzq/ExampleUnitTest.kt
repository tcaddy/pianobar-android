package com.aistudio.pianobar.kxuvzq

import com.aistudio.pianobar.kxuvzq.model.PlaylistCachePolicy
import com.aistudio.pianobar.kxuvzq.model.Track
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
}

