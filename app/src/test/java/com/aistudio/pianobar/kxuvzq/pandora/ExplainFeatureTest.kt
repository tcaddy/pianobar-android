package com.aistudio.pianobar.kxuvzq.pandora

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aistudio.pianobar.kxuvzq.model.LogType
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.model.Track
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import com.aistudio.pianobar.kxuvzq.ui.components.ExplainSongDialog
import com.aistudio.pianobar.kxuvzq.ui.components.NowPlayingCard
import com.aistudio.pianobar.kxuvzq.ui.theme.MyApplicationTheme
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExplainFeatureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var server: HttpServer? = null

    @After
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    private fun startMockPandoraServer(
        onExplainRequest: (callCount: Int, query: String, decryptedBody: JSONObject) -> Pair<Int, String>
    ): String {
        val explainCounter = AtomicInteger(0)
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/services/json/") { exchange ->
            val query = exchange.requestURI.rawQuery ?: ""
            val bodyStr = exchange.requestBody.bufferedReader().readText()
            val method = Regex("(?:^|&)method=([^&]+)").find(query)?.groupValues?.get(1) ?: ""

            val (statusCode, responseBody) = when (method) {
                "auth.partnerLogin" -> {
                    // syncTime: 4 garbage bytes + unix timestamp digits, Blowfish-encrypted with DECRYPT_KEY
                    val syncTimeCipherHex = encryptSyncTimeForTest(System.currentTimeMillis() / 1000L)
                    200 to JSONObject().apply {
                        put("stat", "ok")
                        put("result", JSONObject().apply {
                            put("partnerId", "partner-42")
                            put("partnerAuthToken", "partner-auth-token")
                            put("syncTime", syncTimeCipherHex)
                        })
                    }.toString()
                }
                "auth.userLogin" -> {
                    200 to JSONObject().apply {
                        put("stat", "ok")
                        put("result", JSONObject().apply {
                            put("userAuthToken", "user-auth-token-v${explainCounter.get()}")
                            put("userId", "user-7")
                        })
                    }.toString()
                }
                "user.getStationList" -> {
                    200 to JSONObject().apply {
                        put("stat", "ok")
                        put("result", JSONObject().apply {
                            put("stations", JSONArray())
                        })
                    }.toString()
                }
                "station.getPlaylist" -> {
                    200 to JSONObject().apply {
                        put("stat", "ok")
                        put("result", JSONObject().apply {
                            put("items", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("songName", "Clair de Lune")
                                    put("artistName", "Claude Debussy")
                                    put("albumName", "Suite bergamasque")
                                    put("trackToken", "tok-123")
                                    put("songDuration", 300L)
                                    put("audioUrl", "https://audio.example.com/track.aac")
                                })
                            })
                        })
                    }.toString()
                }
                "track.explainTrack" -> {
                    val decryptedRaw = decryptClientPayloadForTest(bodyStr).trimEnd('\u0000')
                    val decryptedJson = JSONObject(decryptedRaw)
                    onExplainRequest(explainCounter.incrementAndGet(), query, decryptedJson)
                }
                else -> 404 to """{"stat":"fail","code":0,"message":"Unknown method $method"}"""
            }

            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.start()
        server = httpServer
        return "http://127.0.0.1:${httpServer.address.port}/services/json/"
    }

    private fun encryptSyncTimeForTest(unixSec: Long): String {
        val plain = "abcd$unixSec".toByteArray(Charsets.UTF_8)
        val paddedLen = if (plain.size % 8 == 0) plain.size else plain.size + (8 - plain.size % 8)
        val padded = ByteArray(paddedLen)
        System.arraycopy(plain, 0, padded, 0, plain.size)
        val cipher = javax.crypto.Cipher.getInstance("Blowfish/ECB/NoPadding")
        val key = javax.crypto.spec.SecretKeySpec("R=U!LH\$O2B#".toByteArray(Charsets.UTF_8), "Blowfish")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(padded).joinToString("") { "%02x".format(it) }
    }

    private fun decryptClientPayloadForTest(cipherHex: String): String {
        val len = cipherHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cipherHex[i], 16) shl 4) +
                Character.digit(cipherHex[i + 1], 16)).toByte()
            i += 2
        }
        val cipher = javax.crypto.Cipher.getInstance("Blowfish/ECB/NoPadding")
        val key = javax.crypto.spec.SecretKeySpec("6#26FRL\$ZWD".toByteArray(Charsets.UTF_8), "Blowfish")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key)
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    private fun explainJson(vararg traits: String): String {
        val arr = JSONArray()
        traits.forEach { trait ->
            arr.put(JSONObject().put("focusTraitName", trait))
        }
        return JSONObject().apply {
            put("stat", "ok")
            put("result", JSONObject().put("explanations", arr))
        }.toString()
    }

    @Test
    fun apiClient_explainTrack_encryptsPayloadAndFormatsTraits() = runBlocking {
        val capturedTrackToken = AtomicReference<String>()
        val capturedQuery = AtomicReference<String>()
        val endpoint = startMockPandoraServer { _, query, body ->
            capturedQuery.set(query)
            capturedTrackToken.set(body.optString("trackToken"))
            200 to explainJson("impressionistic harmonies", "delicate piano phrasing", "modal tonalities")
        }

        val api = PandoraApiClient(baseEndpoint = endpoint)
        val auth = api.loginAndFetchStations("listener@example.com", "secret")
        assertTrue(auth is PandoraAuthResult.Success)

        val tracks = api.fetchStationPlaylist("station-tok")
        assertEquals(1, tracks.size)
        assertEquals("", tracks.first().musicGenomeExplanation)

        val explanation = api.explainTrack(tracks.first().id)
        assertEquals("tok-123", capturedTrackToken.get())
        assertTrue(capturedQuery.get().contains("method=track.explainTrack"))
        assertEquals(
            "We're playing this track because it features impressionistic harmonies, delicate piano phrasing and modal tonalities.",
            explanation
        )
    }

    @Test
    fun apiClient_explainTrack_reauthenticatesWhenSessionExpired() = runBlocking {
        val endpoint = startMockPandoraServer { attempt, _, _ ->
            if (attempt == 1) {
                200 to """{"stat":"fail","code":1001,"message":"Invalid auth token"}"""
            } else {
                200 to explainJson("acoustic piano", "slow tempo")
            }
        }

        val api = PandoraApiClient(baseEndpoint = endpoint)
        api.loginAndFetchStations("listener@example.com", "secret")

        val explanation = api.explainTrack("tok-expired-test")
        assertEquals(
            "We're playing this track because it features acoustic piano and slow tempo.",
            explanation
        )
    }

    @Test
    fun apiClient_explainTrack_returnsNullOnHttpFailureOrEmptyTraits() = runBlocking {
        val endpoint = startMockPandoraServer { attempt, _, _ ->
            if (attempt == 1) {
                500 to "Server Error"
            } else {
                200 to """{"stat":"ok","result":{"explanations":[]}}"""
            }
        }

        val api = PandoraApiClient(baseEndpoint = endpoint)
        api.loginAndFetchStations("listener@example.com", "secret")

        assertNull(api.explainTrack("tok-1"))
        assertNull(api.explainTrack("tok-2"))
    }

    @Test
    fun playerManager_requestExplanation_updatesTrackHistoryAndLogs() = runBlocking {
        val endpoint = startMockPandoraServer { _, _, _ ->
            200 to explainJson("romantic counterpoint")
        }
        val api = PandoraApiClient(baseEndpoint = endpoint)
        api.loginAndFetchStations("listener@example.com", "secret")

        val manager = PianobarPlayerManager.createForTesting(api)
        val track = Track(
            id = "tok-cli",
            title = "Nocturne in E-flat",
            artist = "Chopin",
            album = "Nocturnes",
            artworkUrl = "",
            streamUrl = "https://audio.example.com/nocturne.aac",
            stationId = "st-1",
            musicGenomeExplanation = ""
        )
        manager.setStateForTesting {
            it.copy(currentTrack = track, history = listOf(track), isUserLoggedIn = true)
        }

        val latch = CountDownLatch(1)
        val callbackResult = AtomicReference<String>()
        manager.executeTerminalCommand("e")
        manager.requestExplanation { msg ->
            callbackResult.set(msg)
            latch.countDown()
        }

        val deadline = System.currentTimeMillis() + 5_000L
        while (latch.count > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue("Timed out waiting for explain callback", latch.await(100, TimeUnit.MILLISECONDS))

        val expected = "We're playing this track because it features romantic counterpoint."
        assertEquals(expected, callbackResult.get())
        assertEquals(expected, manager.state.value.currentTrack?.musicGenomeExplanation)
        assertEquals(expected, manager.state.value.history.firstOrNull()?.musicGenomeExplanation)
        assertEquals(false, manager.state.value.isExplaining)
        assertNull(manager.state.value.explainError)
        assertTrue(manager.state.value.logs.any { it.prefix == "[e]" && it.message == expected })
    }

    @Test
    fun playerManager_requestExplanation_failureSetsExplainErrorWithoutFakePlaceholder() = runBlocking {
        val endpoint = startMockPandoraServer { _, _, _ ->
            500 to "Service Unavailable"
        }
        val api = PandoraApiClient(baseEndpoint = endpoint)
        api.loginAndFetchStations("listener@example.com", "secret")

        val manager = PianobarPlayerManager.createForTesting(api)
        val track = Track(
            id = "tok-fail",
            title = "Prelude",
            artist = "Bach",
            album = "WTC",
            artworkUrl = "",
            streamUrl = "https://audio.example.com/prelude.aac",
            stationId = "st-1",
            musicGenomeExplanation = ""
        )
        manager.setStateForTesting {
            it.copy(currentTrack = track, isUserLoggedIn = true)
        }

        val latch = CountDownLatch(1)
        manager.requestExplanation { latch.countDown() }

        val deadline = System.currentTimeMillis() + 5_000L
        while (latch.count > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue("Timed out waiting for explain failure callback", latch.await(100, TimeUnit.MILLISECONDS))

        assertEquals(false, manager.state.value.isExplaining)
        assertNotNull(manager.state.value.explainError)
        assertEquals("", manager.state.value.currentTrack?.musicGenomeExplanation)
        assertTrue(manager.state.value.logs.any { it.type == LogType.WARNING && it.message.contains("Unable to retrieve") })
    }

    @Test
    fun narrowPhoneWidth_explainAndTiredButtonsRemainVisibleAndClickable() {
        var explainClicks = 0
        val track = Track(
            id = "t-ui",
            title = "Gymnopédie No. 1",
            artist = "Erik Satie",
            album = "Piano Works",
            artworkUrl = "",
            streamUrl = "",
            stationId = "s-1"
        )
        composeTestRule.setContent {
            MyApplicationTheme(themeId = "midnight") {
                Box(modifier = Modifier.width(360.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        NowPlayingCard(
                            state = PlayerState(currentTrack = track, isPlaying = true),
                            onTogglePlayPause = {},
                            onSkipNext = {},
                            onLoveTrack = {},
                            onBanTrack = {},
                            onBookmarkTrack = {},
                            onExplainTrack = { explainClicks++ },
                            onSeek = {},
                            onOpenStationSelect = {}
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("explain_song_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, explainClicks)

        composeTestRule.onNodeWithTag("tired_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun nowPlayingCard_explainButtonDisabledWithoutTrack() {
        composeTestRule.setContent {
            MyApplicationTheme(themeId = "midnight") {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NowPlayingCard(
                        state = PlayerState(currentTrack = null, isPlaying = false),
                        onTogglePlayPause = {},
                        onSkipNext = {},
                        onLoveTrack = {},
                        onBanTrack = {},
                        onBookmarkTrack = {},
                        onExplainTrack = {},
                        onSeek = {},
                        onOpenStationSelect = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("explain_song_button")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun explainDialog_rendersLoadingResultAndRetryStates() {
        val loadingState = androidx.compose.runtime.mutableStateOf(true)
        val errorState = androidx.compose.runtime.mutableStateOf<String?>(null)
        val explanationState = androidx.compose.runtime.mutableStateOf("")
        var retries = 0

        composeTestRule.setContent {
            MyApplicationTheme(themeId = "midnight") {
                ExplainSongDialog(
                    track = Track(
                        id = "t-dlg",
                        title = "Blue in Green",
                        artist = "Miles Davis",
                        album = "Kind of Blue",
                        artworkUrl = "",
                        streamUrl = "",
                        stationId = "s-jazz",
                        musicGenomeExplanation = explanationState.value
                    ),
                    stationName = "Jazz Classics",
                    isLoading = loadingState.value,
                    errorMessage = errorState.value,
                    onRetry = { retries++ },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("explain_loading_indicator").assertIsDisplayed()

        // Transition to error state and test Retry
        composeTestRule.runOnUiThread {
            loadingState.value = false
            errorState.value = "Unable to retrieve Music Genome explanation from Pandora."
        }
        composeTestRule.onNodeWithTag("explain_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("explain_retry_button").assertIsDisplayed().performClick()
        assertEquals(1, retries)

        // Transition to real Music Genome explanation
        composeTestRule.runOnUiThread {
            errorState.value = null
            explanationState.value = "We're playing this track because it features modal improvisation and muted trumpet."
        }
        composeTestRule.onNodeWithTag("explain_result_text")
            .assertIsDisplayed()
            .assertTextContains("modal improvisation", substring = true)
    }
}
