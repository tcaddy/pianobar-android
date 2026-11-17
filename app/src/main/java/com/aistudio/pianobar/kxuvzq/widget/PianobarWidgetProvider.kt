package com.aistudio.pianobar.kxuvzq.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aistudio.pianobar.kxuvzq.MainActivity
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PianobarWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(context.applicationContext)
        updateAllWidgets(context, playerManager.state.value, cachedArtworkFor(playerManager.state.value))
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(context.applicationContext)
        updateWidgetsForIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            state = playerManager.state.value,
            artworkBitmap = cachedArtworkFor(playerManager.state.value)
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
        updateWidgetsForIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = intArrayOf(appWidgetId),
            state = playerManager.state.value,
            artworkBitmap = cachedArtworkFor(playerManager.state.value)
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
                updateAllWidgets(appContext, playerManager.state.value, cachedArtworkFor(playerManager.state.value))
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
                updateAllWidgets(appContext, updated, cachedArtworkFor(updated))
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
                updateAllWidgets(appContext, updated, cachedArtworkFor(updated))
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
        val isUserLoggedIn: Boolean
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
        private var cachedArtworkUrl: String? = null

        @Volatile
        private var cachedRoundedArtwork: Bitmap? = null

        @Volatile
        private var inFlightArtworkUrl: String? = null

        private fun cachedArtworkFor(state: PlayerState): Bitmap? {
            val url = state.currentTrack?.artworkUrl
            return if (!url.isNullOrBlank() && url == cachedArtworkUrl) cachedRoundedArtwork else null
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
                isUserLoggedIn = state.isUserLoggedIn
            )
            if (key == lastRenderKey) return
            lastRenderKey = key
            updateAllWidgets(context, state, cachedArtworkFor(state))
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
            val roundedArt = if (artworkBitmap != null) {
                createRoundedWidgetBitmap(artworkBitmap).also {
                    if (trackArtworkUrl.isNotBlank()) {
                        cachedArtworkUrl = trackArtworkUrl
                        cachedRoundedArtwork = it
                    }
                }
            } else {
                cachedArtworkFor(state)
            }

            updateWidgetsForIds(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds,
                state = state,
                artworkBitmap = roundedArt
            )

            // If we don't yet have cached artwork for the active track URL, fetch asynchronously
            if (roundedArt == null && trackArtworkUrl.isNotBlank() && inFlightArtworkUrl != trackArtworkUrl) {
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
                            val rounded = createRoundedWidgetBitmap(bmp)
                            cachedArtworkUrl = trackArtworkUrl
                            cachedRoundedArtwork = rounded
                            val latestState = PianobarPlayerManager.getInstance().state.value
                            if (latestState.currentTrack?.artworkUrl == trackArtworkUrl) {
                                updateWidgetsForIds(
                                    context = appContext,
                                    appWidgetManager = appWidgetManager,
                                    appWidgetIds = appWidgetManager.getAppWidgetIds(componentName),
                                    state = latestState,
                                    artworkBitmap = rounded
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
            artworkBitmap: Bitmap?
        ) {
            if (appWidgetIds.isEmpty()) return
            for (appWidgetId in appWidgetIds) {
                val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val compact = buildRemoteViewsForState(context, state, artworkBitmap, isCompact = true)
                    val standard = buildRemoteViewsForState(context, state, artworkBitmap, isCompact = false)
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
                    buildRemoteViewsForState(context, state, artworkBitmap, isCompact = useCompact)
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
            val layoutId = if (isCompact) R.layout.widget_pianobar_compact else R.layout.widget_pianobar
            val views = RemoteViews(context.packageName, layoutId)

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
            views.setTextViewText(R.id.widget_subtitle, subtitle)

            if (!isCompact) {
                val subtext = "pianobar • ${track?.audioBitrate ?: "AAC 192k"}"
                views.setTextViewText(R.id.widget_subtext, subtext)
            }

            if (roundedArtwork != null) {
                views.setImageViewBitmap(R.id.widget_artwork, roundedArtwork)
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_pianobar_logo)
            }

            val isPlaying = state.isPlaying
            val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            val playPauseDesc = context.getString(
                if (isPlaying) R.string.widget_cd_pause else R.string.widget_cd_play
            )
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)
            views.setContentDescription(R.id.widget_btn_play_pause, playPauseDesc)

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
            cornerRadiusRatio: Float = 0.16f
        ): Bitmap {
            val safeSize = targetPx.coerceAtLeast(32)
            val minSide = minOf(source.width, source.height).coerceAtLeast(1)
            val cropX = (source.width - minSide) / 2
            val cropY = (source.height - minSide) / 2
            val square = if (source.width == source.height) {
                source
            } else {
                Bitmap.createBitmap(source, cropX, cropY, minSide, minSide)
            }
            val scaled = if (square.width == safeSize && square.height == safeSize) {
                square
            } else {
                Bitmap.createScaledBitmap(square, safeSize, safeSize, true)
            }

            val output = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            val radius = safeSize * cornerRadiusRatio
            canvas.drawRoundRect(
                RectF(0f, 0f, safeSize.toFloat(), safeSize.toFloat()),
                radius,
                radius,
                paint
            )
            if (scaled !== square && scaled !== source) scaled.recycle()
            if (square !== source) square.recycle()
            return output
        }
    }
}
