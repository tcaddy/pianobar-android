package com.aistudio.pianobar.kxuvzq.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.ui.components.NowPlayingCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiSlopThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aiSlopTheme_rendersSlopBentoAndStatusBanner() {
        composeTestRule.setContent {
            MyApplicationTheme(themeId = "ai-slop") {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AiSlopStatusBanner(isPlaying = true, isBuffering = false)
                    NowPlayingCard(
                        state = PlayerState(isPlaying = true, themeId = "ai-slop"),
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
        composeTestRule.onNodeWithTag("ai_slop_status_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_slop_bento_strip").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun midnightTheme_doesNotRenderAiSlopChrome() {
        composeTestRule.setContent {
            MyApplicationTheme(themeId = "midnight") {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NowPlayingCard(
                        state = PlayerState(isPlaying = true, themeId = "midnight"),
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
        composeTestRule.onNodeWithTag("ai_slop_bento_strip").assertDoesNotExist()
    }
}
