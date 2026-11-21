package com.aistudio.pianobar.kxuvzq.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aistudio.pianobar.kxuvzq.MainActivity
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService
import com.aistudio.pianobar.kxuvzq.ui.theme.AiSlopPalette
import com.aistudio.pianobar.kxuvzq.ui.theme.AmberCrtPalette
import com.aistudio.pianobar.kxuvzq.ui.theme.IcePalette
import com.aistudio.pianobar.kxuvzq.ui.theme.NewsprintPalette
import com.aistudio.pianobar.kxuvzq.ui.theme.PianobarArtworkRenderer
import com.aistudio.pianobar.kxuvzq.ui.theme.PianobarThemes
import com.aistudio.pianobar.kxuvzq.ui.theme.SolarizedPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PianobarWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(context.applicationContext)
        updateAllWidgets(context, playerManager.state.value, cachedRawArtworkFor(playerManager.state.value))
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(context.applicationContext)
        val state = playerManager.state.value
        val themedArt = resolveThemedBitmap(context.applicationContext, state, cachedRawArtworkFor(state))
        updateWidgetsForIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            state = state,
            themedArtwork = themedArt
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(context.applicationContext)
        val state = playerManager.state.value
        val themedArt = resolveThemedBitmap(context.applicationContext, state, cachedRawArtworkFor(state))
        updateWidgetsForIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = intArrayOf(appWidgetId),
            state = state,
            themedArtwork = themedArt
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action !in setOf(ACTION_WIDGET_TOGGLE, ACTION_WIDGET_NEXT, ACTION_WIDGET_STOP)) {
            return
        }

        val appContext = context.applicationContext
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(appContext)

        when (action) {
            ACTION_WIDGET_STOP -> {
                playerManager.pause()
                try {
                    appContext.stopService(Intent(appContext, PianobarPlaybackService::class.java))
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to stop playback service from widget: ${e.message}")
                }
                val state = playerManager.state.value
                updateAllWidgets(appContext, state, cachedRawArtworkFor(state))
            }
            ACTION_WIDGET_TOGGLE -> {
                if (!playerManager.state.value.isUserLoggedIn) {
                    openMainActivity(appContext)
                    return
                }
                playerManager.togglePlayPause()
                val updated = playerManager.state.value
                if (updated.isPlaying || updated.isBuffering) {
                    PianobarPlaybackService.start(appContext)
                }
                updateAllWidgets(appContext, updated, cachedRawArtworkFor(updated))
            }
            ACTION_WIDGET_NEXT -> {
                if (!playerManager.state.value.isUserLoggedIn) {
                    openMainActivity(appContext)
                    return
                }
                playerManager.skipNext()
                val updated = playerManager.state.value
                if (updated.isPlaying || updated.isBuffering) {
                    PianobarPlaybackService.start(appContext)
                }
                updateAllWidgets(appContext, updated, cachedRawArtworkFor(updated))
            }
        }
    }

    private fun openMainActivity(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open MainActivity from widget: ${e.message}")
        }
    }

    private data class WidgetRenderKey(
        val trackId: String?,
        val title: String?,
        val artist: String?,
        val audioBitrate: String?,
        val artworkUrl: String?,
        val stationId: String?,
        val stationName: String?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val isUserLoggedIn: Boolean,
        val themeId: String
    )

    companion object {
        private const val TAG = "PianobarWidget"
        private const val WIDGET_ART_PX = 160

        const val ACTION_WIDGET_TOGGLE = "com.aistudio.pianobar.kxuvzq.widget.ACTION_TOGGLE"
        const val ACTION_WIDGET_NEXT = "com.aistudio.pianobar.kxuvzq.widget.ACTION_NEXT"
        const val ACTION_WIDGET_STOP = "com.aistudio.pianobar.kxuvzq.widget.ACTION_STOP"

        private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        @Volatile
        private var lastRenderKey: WidgetRenderKey? = null

        @Volatile
        private var cachedRawArtworkUrl: String? = null

        @Volatile
        private var cachedRawArtwork: Bitmap? = null

        @Volatile
        private var cachedThemedKey: Pair<String?, String>? = null

        @Volatile
        private var cachedThemedBitmap: Bitmap? = null

        @Volatile
        private var inFlightArtworkUrl: String? = null

        private fun cachedRawArtworkFor(state: PlayerState): Bitmap? {
            val url = state.currentTrack?.artworkUrl
            return if (!url.isNullOrBlank() && url == cachedRawArtworkUrl) {
                cachedRawArtwork?.takeIf { !it.isRecycled }
            } else {
                null
            }
        }

        private fun resolveThemedBitmap(
            context: Context,
            state: PlayerState,
            rawArtwork: Bitmap?
        ): Bitmap {
            val palette = PianobarThemes.fromId(state.themeId)
            val url = state.currentTrack?.artworkUrl.orEmpty()
            if (rawArtwork != null && !rawArtwork.isRecycled && url.isNotBlank()) {
                cachedRawArtworkUrl = url
                cachedRawArtwork = rawArtwork
            }
            val effectiveRaw = rawArtwork?.takeIf { !it.isRecycled } ?: cachedRawArtworkFor(state)
            val cacheKey = (if (effectiveRaw != null) url else "") to palette.id
            val existing = cachedThemedBitmap
            if (cachedThemedKey == cacheKey && existing != null && !existing.isRecycled) {
                return existing
            }
            val rendered = PianobarArtworkRenderer.renderThemedArtwork(
                context = context,
                rawArtwork = effectiveRaw,
                palette = palette,
                targetPx = WIDGET_ART_PX,
                cornerRadiusRatio = 0.16f
            )
            cachedThemedKey = cacheKey
            cachedThemedBitmap = rendered
            return rendered
        }

        fun updateIfStateChanged(context: Context, state: PlayerState) {
            val key = WidgetRenderKey(
                trackId = state.currentTrack?.id,
                title = state.currentTrack?.title,
                artist = state.currentTrack?.artist,
                audioBitrate = state.currentTrack?.audioBitrate,
                artworkUrl = state.currentTrack?.artworkUrl,
                stationId = state.currentStation?.id,
                stationName = state.currentStation?.name,
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                isUserLoggedIn = state.isUserLoggedIn,
                themeId = state.themeId
            )
            if (key == lastRenderKey) return
            lastRenderKey = key
            updateAllWidgets(context, state, cachedRawArtworkFor(state))
        }

        fun updateAllWidgets(
            context: Context,
            state: PlayerState,
            artworkBitmap: Bitmap? = null
        ) {
            val appContext = context.applicationContext
            val appWidgetManager = try {
                AppWidgetManager.getInstance(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "AppWidgetManager unavailable: ${e.message}")
                return
            }
            val componentName = ComponentName(appContext, PianobarWidgetProvider::class.java)
            val appWidgetIds = try {
                appWidgetManager.getAppWidgetIds(componentName)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to query widget IDs: ${e.message}")
                return
            }
            if (appWidgetIds.isEmpty()) return

            val trackArtworkUrl = state.currentTrack?.artworkUrl.orEmpty()
            val rawArt = artworkBitmap?.takeIf { !it.isRecycled } ?: cachedRawArtworkFor(state)
            val themedArt = resolveThemedBitmap(appContext, state, rawArt)

            updateWidgetsForIds(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds,
                state = state,
                themedArtwork = themedArt
            )

            // If we don't yet have cached raw artwork for the active track URL, fetch asynchronously
            if (rawArt == null && trackArtworkUrl.isNotBlank() && inFlightArtworkUrl != trackArtworkUrl) {
                inFlightArtworkUrl = trackArtworkUrl
                widgetScope.launch {
                    try {
                        val loader = ImageLoader(appContext)
                        val request = ImageRequest.Builder(appContext)
                            .data(trackArtworkUrl)
                            .allowHardware(false)
                            .size(WIDGET_ART_PX, WIDGET_ART_PX)
                            .build()
                        val result = loader.execute(request)
                        val loaded = (result as? SuccessResult)?.drawable as? BitmapDrawable
                        val bmp = loaded?.bitmap
                        if (bmp != null) {
                            cachedRawArtworkUrl = trackArtworkUrl
                            cachedRawArtwork = bmp
                            val latestState = PianobarPlayerManager.getInstance().state.value
                            if (latestState.currentTrack?.artworkUrl == trackArtworkUrl) {
                                val updatedThemed = resolveThemedBitmap(appContext, latestState, bmp)
                                updateWidgetsForIds(
                                    context = appContext,
                                    appWidgetManager = appWidgetManager,
                                    appWidgetIds = appWidgetManager.getAppWidgetIds(componentName),
                                    state = latestState,
                                    themedArtwork = updatedThemed
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Widget artwork fetch error: ${e.message}")
                    } finally {
                        if (inFlightArtworkUrl == trackArtworkUrl) {
                            inFlightArtworkUrl = null
                        }
                    }
                }
            }
        }

        private fun updateWidgetsForIds(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            state: PlayerState,
            themedArtwork: Bitmap?
        ) {
            if (appWidgetIds.isEmpty()) return
            for (appWidgetId in appWidgetIds) {
                val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val compact = buildRemoteViewsForState(context, state, themedArtwork, isCompact = true)
                    val standard = buildRemoteViewsForState(context, state, themedArtwork, isCompact = false)
                    RemoteViews(
                        mapOf(
                            SizeF(180f, 56f) to compact,
                            SizeF(250f, 110f) to standard
                        )
                    )
                } else {
                    val options = try {
                        appWidgetManager.getAppWidgetOptions(appWidgetId)
                    } catch (_: Exception) {
                        Bundle.EMPTY
                    }
                    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                    val useCompact = minHeightDp in 1..95
                    buildRemoteViewsForState(context, state, themedArtwork, isCompact = useCompact)
                }
                try {
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed updating widget $appWidgetId: ${e.message}")
                }
            }
        }

        @VisibleForTesting
        internal fun buildRemoteViewsForState(
            context: Context,
            state: PlayerState,
            roundedArtwork: Bitmap? = null,
            isCompact: Boolean = false
        ): RemoteViews {
            val palette = PianobarThemes.fromId(state.themeId)
            val isSlop = palette.id == AiSlopPalette.id
            val layoutId = if (isCompact) R.layout.widget_pianobar_compact else R.layout.widget_pianobar
            val views = RemoteViews(context.packageName, layoutId)

            // Theme card surface & Play button backgrounds
            views.setInt(R.id.widget_root, "setBackgroundResource", widgetBgResForTheme(palette.id))
            views.setInt(R.id.widget_btn_play_pause, "setBackgroundResource", playBtnBgResForTheme(palette.id))

            val track = state.currentTrack
            val stationName = state.currentStation?.name ?: "QuickMix"
            val title = track?.title ?: context.getString(R.string.widget_default_title)
            val artist = track?.artist ?: "Pandora Radio Client"
            val subtitle = when {
                track != null -> "$artist • $stationName"
                !state.isUserLoggedIn -> context.getString(R.string.widget_subtitle_sign_in)
                state.isBuffering -> "Tuning $stationName..."
                else -> "$artist • $stationName"
            }

            views.setTextViewText(R.id.widget_title, title)
            views.setTextColor(R.id.widget_title, palette.textPrimary.toArgb())

            views.setTextViewText(R.id.widget_subtitle, subtitle)
            views.setTextColor(R.id.widget_subtitle, palette.textSecondary.toArgb())

            if (!isCompact) {
                val subtextPrefix = if (isSlop) "✨ pianobar" else "pianobar"
                val subtext = "$subtextPrefix • ${track?.audioBitrate ?: "AAC 192k"}"
                views.setTextViewText(R.id.widget_subtext, subtext)
                views.setTextColor(
                    R.id.widget_subtext,
                    if (isSlop) palette.accent.toArgb() else palette.textSecondary.toArgb()
                )
                views.setInt(R.id.widget_music_icon, "setColorFilter", palette.accent.toArgb())
                views.setInt(R.id.widget_btn_stop, "setColorFilter", palette.textSecondary.toArgb())
            }

            val artworkToRender = roundedArtwork ?: PianobarArtworkRenderer.renderThemedPlaceholder(
                context = context,
                palette = palette,
                targetPx = WIDGET_ART_PX,
                cornerRadiusRatio = 0.16f
            )
            views.setImageViewBitmap(R.id.widget_artwork, artworkToRender)

            val isPlaying = state.isPlaying
            val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            val playPauseDesc = context.getString(
                if (isPlaying) R.string.widget_cd_pause else R.string.widget_cd_play
            )
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)
            views.setContentDescription(R.id.widget_btn_play_pause, playPauseDesc)
            views.setInt(
                R.id.widget_btn_play_pause,
                "setColorFilter",
                if (isSlop) palette.onAccent.toArgb() else palette.textPrimary.toArgb()
            )
            views.setInt(R.id.widget_btn_next, "setColorFilter", palette.textPrimary.toArgb())

            // Wire tap actions
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_content_area, openAppPendingIntent)

            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                createActionPendingIntent(context, ACTION_WIDGET_TOGGLE, 101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                createActionPendingIntent(context, ACTION_WIDGET_NEXT, 102)
            )
            if (!isCompact) {
                views.setOnClickPendingIntent(
                    R.id.widget_btn_stop,
                    createActionPendingIntent(context, ACTION_WIDGET_STOP, 103)
                )
            }

            return views
        }

        private fun widgetBgResForTheme(themeId: String): Int = when (themeId) {
            AmberCrtPalette.id -> R.drawable.widget_bg_amber_crt
            IcePalette.id -> R.drawable.widget_bg_ice
            SolarizedPalette.id -> R.drawable.widget_bg_solarized
            NewsprintPalette.id -> R.drawable.widget_bg_newsprint
            AiSlopPalette.id -> R.drawable.widget_bg_ai_slop
            else -> R.drawable.widget_bg_midnight
        }

        private fun playBtnBgResForTheme(themeId: String): Int = when (themeId) {
            AmberCrtPalette.id -> R.drawable.widget_play_btn_amber_crt
            IcePalette.id -> R.drawable.widget_play_btn_ice
            SolarizedPalette.id -> R.drawable.widget_play_btn_solarized
            NewsprintPalette.id -> R.drawable.widget_play_btn_newsprint
            AiSlopPalette.id -> R.drawable.widget_play_btn_ai_slop
            else -> R.drawable.widget_play_btn_midnight
        }

        private fun createActionPendingIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, PianobarWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        @VisibleForTesting
        internal fun createRoundedWidgetBitmap(
            source: Bitmap,
            targetPx: Int = WIDGET_ART_PX,
            cornerRadiusRatio: Float = 0.16f,
            themeId: String = PianobarThemes.DEFAULT_ID
        ): Bitmap {
            return PianobarArtworkRenderer.frameArtworkForTheme(
                source = source,
                palette = PianobarThemes.fromId(themeId),
                targetPx = targetPx,
                cornerRadiusRatio = cornerRadiusRatio
            )
        }
    }
}
