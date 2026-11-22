package com.aistudio.pianobar.kxuvzq.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.model.Station
import com.aistudio.pianobar.kxuvzq.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PianobarWidgetTest {

    @Test
    fun widgetDrawablesInflateCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            R.drawable.widget_bg,
            R.drawable.widget_bg_midnight,
            R.drawable.widget_bg_amber_crt,
            R.drawable.widget_bg_ice,
            R.drawable.widget_bg_solarized,
            R.drawable.widget_bg_newsprint,
            R.drawable.widget_bg_ai_slop,
            R.drawable.widget_play_btn_midnight,
            R.drawable.widget_play_btn_amber_crt,
            R.drawable.widget_play_btn_ice,
            R.drawable.widget_play_btn_solarized,
            R.drawable.widget_play_btn_newsprint,
            R.drawable.widget_play_btn_ai_slop,
            R.drawable.widget_btn_ripple,
            R.drawable.widget_play_btn_ripple,
            R.drawable.ic_icon_snooze
        ).forEach { resId ->
            assertNotNull("Widget drawable $resId should inflate", ContextCompat.getDrawable(context, resId))
        }
    }

    @Test
    fun standardWidgetLayoutFollowsNotificationMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val track = Track(
            id = "t-1",
            title = "Clair de Lune",
            artist = "Claude Debussy",
            album = "Suite bergamasque",
            artworkUrl = "",
            streamUrl = "",
            stationId = "s-1",
            audioBitrate = "AAC 192 kbps"
        )
        val station = Station(
            id = "s-1",
            name = "Solo Piano Radio",
            description = "",
            genre = "Classical"
        )
        val playingState = PlayerState(
            currentStation = station,
            currentTrack = track,
            isPlaying = true,
            isUserLoggedIn = true
        )

        val remoteViews = PianobarWidgetProvider.buildRemoteViewsForState(
            context = context,
            state = playingState,
            isCompact = false
        )
        val container = FrameLayout(context)
        val view = remoteViews.apply(context, container)

        assertEquals(
            "pianobar • AAC 192 kbps",
            view.findViewById<TextView>(R.id.widget_subtext).text.toString()
        )
        assertEquals(
            "Clair de Lune",
            view.findViewById<TextView>(R.id.widget_title).text.toString()
        )
        assertEquals(
            "Claude Debussy • Solo Piano Radio",
            view.findViewById<TextView>(R.id.widget_subtitle).text.toString()
        )

        val playPauseBtn = view.findViewById<ImageButton>(R.id.widget_btn_play_pause)
        assertEquals(context.getString(R.string.widget_cd_pause), playPauseBtn.contentDescription.toString())
        assertNotNull(view.findViewById<ImageButton>(R.id.widget_btn_next))
        assertNotNull(view.findViewById<ImageButton>(R.id.widget_btn_stop))
    }

    @Test
    fun compactWidgetLayoutRendersEssentialControls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val idleState = PlayerState(isPlaying = false, isUserLoggedIn = false)

        val remoteViews = PianobarWidgetProvider.buildRemoteViewsForState(
            context = context,
            state = idleState,
            isCompact = true
        )
        val view = remoteViews.apply(context, FrameLayout(context))

        assertEquals(
            context.getString(R.string.widget_default_title),
            view.findViewById<TextView>(R.id.widget_title).text.toString()
        )
        assertEquals(
            context.getString(R.string.widget_subtitle_sign_in),
            view.findViewById<TextView>(R.id.widget_subtitle).text.toString()
        )
        val playPauseBtn = view.findViewById<ImageButton>(R.id.widget_btn_play_pause)
        assertEquals(context.getString(R.string.widget_cd_play), playPauseBtn.contentDescription.toString())
        assertNull(view.findViewById<ImageButton>(R.id.widget_btn_stop))
    }

    @Test
    fun widgetAppliesAllThemesAndAiSlopSubtext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val track = Track(
            id = "t-2",
            title = "Gymnopédie No. 1",
            artist = "Erik Satie",
            album = "Piano Works",
            artworkUrl = "",
            streamUrl = "",
            stationId = "s-1",
            audioBitrate = "AAC 192k"
        )
        com.aistudio.pianobar.kxuvzq.ui.theme.PianobarThemes.all.forEach { palette ->
            val state = PlayerState(
                currentTrack = track,
                isPlaying = true,
                isUserLoggedIn = true,
                themeId = palette.id
            )
            val standardView = PianobarWidgetProvider.buildRemoteViewsForState(
                context = context,
                state = state,
                isCompact = false
            ).apply(context, FrameLayout(context))

            val titleView = standardView.findViewById<TextView>(R.id.widget_title)
            assertEquals(
                androidx.compose.ui.graphics.Color(titleView.currentTextColor),
                palette.textPrimary
            )
            val subtext = standardView.findViewById<TextView>(R.id.widget_subtext).text.toString()
            if (palette.id == "ai-slop") {
                assertTrue(subtext.startsWith("✨ pianobar"))
            } else {
                assertTrue(subtext.startsWith("pianobar"))
            }

            val compactView = PianobarWidgetProvider.buildRemoteViewsForState(
                context = context,
                state = state,
                isCompact = true
            ).apply(context, FrameLayout(context))
            assertNotNull(compactView.findViewById<ImageButton>(R.id.widget_btn_play_pause))

            val placeholder = com.aistudio.pianobar.kxuvzq.ui.theme.PianobarArtworkRenderer.renderThemedPlaceholder(
                context = context,
                palette = palette,
                targetPx = 160
            )
            assertEquals(160, placeholder.width)
            assertEquals(160, placeholder.height)
        }
    }

    @Test
    fun artworkIsScaledForBinderSafety() {
        val largeBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val rounded = PianobarWidgetProvider.createRoundedWidgetBitmap(largeBitmap, targetPx = 160)
        assertEquals(160, rounded.width)
        assertEquals(160, rounded.height)
    }

    @Test
    fun manifestRegistersAppWidgetReceiver() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            component = ComponentName(context, PianobarWidgetProvider::class.java)
        }
        val receivers = context.packageManager.queryBroadcastReceivers(intent, 0)
        assertTrue(
            "PianobarWidgetProvider must be registered in AndroidManifest",
            receivers.any { it.activityInfo.name == PianobarWidgetProvider::class.java.name }
        )
    }
}
