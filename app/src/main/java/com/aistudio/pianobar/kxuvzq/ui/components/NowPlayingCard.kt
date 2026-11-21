package com.aistudio.pianobar.kxuvzq.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.ui.theme.AiSlopBentoStrip
import com.aistudio.pianobar.kxuvzq.ui.theme.AiSlopGradients
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoAmber
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoCyan
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoDarkBg
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoOnAccent
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoPurple
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoRed
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurface
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceBorder
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceElevated
import com.aistudio.pianobar.kxuvzq.ui.theme.TextPrimary
import com.aistudio.pianobar.kxuvzq.ui.theme.TextSecondary
import com.aistudio.pianobar.kxuvzq.ui.theme.isAiSlopTheme
import java.util.Locale

@Composable
fun NowPlayingCard(
    state: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onLoveTrack: () -> Unit,
    onBanTrack: () -> Unit,
    onBookmarkTrack: () -> Unit,
    onExplainTrack: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenStationSelect: () -> Unit,
    onTiredTrack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack
    val isLoved = track != null && state.lovedTrackIds.contains(track.id)
    val isBanned = track != null && state.bannedTrackIds.contains(track.id)
    val isBookmarked = track != null && state.bookmarkedTrackIds.contains(track.id)

    val currentSeconds = state.currentPositionMs / 1000
    val totalSeconds = (state.durationMs / 1000).coerceAtLeast(1)
    val currentFormatted = String.format(Locale.US, "%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val totalFormatted = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    var isDraggingSlider by remember { androidx.compose.runtime.mutableStateOf(false) }
    var dragSliderPosition by remember { mutableFloatStateOf(0f) }

    val sliderProgress = if (isDraggingSlider) {
        dragSliderPosition
    } else {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    }

    val artworkScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.0f else 0.94f,
        animationSpec = tween(durationMillis = 400),
        label = "artwork_scale"
    )

    val isSlop = isAiSlopTheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (isSlop) {
                    Modifier.border(1.5.dp, AiSlopGradients.borderBrush, RoundedCornerShape(24.dp))
                } else {
                    Modifier.border(1.dp, PianoSurfaceBorder, RoundedCornerShape(24.dp))
                }
            ),
        color = if (isSlop) PianoSurface.copy(alpha = 0.76f) else PianoSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Station Chip Header
            Surface(
                onClick = onOpenStationSelect,
                modifier = Modifier
                    .clip(CircleShape)
                    .testTag("station_selector_chip"),
                color = if (isSlop) PianoSurfaceElevated.copy(alpha = 0.85f) else PianoSurfaceElevated,
                border = if (isSlop) {
                    androidx.compose.foundation.BorderStroke(1.dp, AiSlopGradients.borderBrush)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, PianoCyan.copy(alpha = 0.3f))
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_icon_radio),
                        contentDescription = "Station",
                        tint = PianoCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.currentStation?.name ?: "Select Station",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSlop) "• ✨ ${state.currentStation?.genre ?: "Neural Mix"}" else "• ${state.currentStation?.genre ?: "Pianobar"}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Artwork Cover
            val glowColorPrimary = PianoCyan
            val glowColorSecondary = PianoPurple
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .scale(artworkScale)
                    .then(
                        if (isSlop) {
                            Modifier.drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            glowColorPrimary.copy(alpha = if (state.isPlaying) 0.34f else 0.18f),
                                            glowColorSecondary.copy(alpha = if (state.isPlaying) 0.22f else 0.10f),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = size.maxDimension * 0.64f
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSlop) {
                            Modifier.border(1.5.dp, AiSlopGradients.borderBrush, RoundedCornerShape(12.dp))
                        } else {
                            Modifier.border(1.dp, PianoSurfaceBorder, RoundedCornerShape(12.dp))
                        }
                    )
                    .background(PianoSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val artworkUrl = track?.artworkUrl
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Artwork",
                        modifier = Modifier
                            .matchParentSize()
                            .testTag("album_artwork_image"),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(PianoSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_notif_music),
                            contentDescription = "Generic Music Note",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // High Quality audio badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PianoDarkBg.copy(alpha = 0.9f))
                        .then(
                            if (isSlop) {
                                Modifier.border(1.dp, AiSlopGradients.borderBrush, RoundedCornerShape(6.dp))
                            } else {
                                Modifier.border(1.dp, PianoSurfaceBorder, RoundedCornerShape(6.dp))
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSlop) "⚡ ${track?.audioBitrate ?: "AAC 192k"}" else (track?.audioBitrate ?: "AAC 192k"),
                        color = if (isSlop) PianoPurple else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Visualizer overlay when playing
                if (state.isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(PianoDarkBg.copy(alpha = 0.85f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VisualizerBars(
                            isPlaying = state.isPlaying,
                            barCount = 16,
                            maxHeight = 20.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track metadata
            Text(
                text = track?.title ?: if (state.isBuffering) "Loading Track..." else (state.currentStation?.name ?: "Select a Station to Play"),
                style = if (isSlop) {
                    TextStyle(
                        brush = AiSlopGradients.textBrush,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    TextStyle(
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track?.artist ?: if (state.isBuffering) "Retrieving metadata from Pandora..." else "Pianobar",
                color = PianoCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = when {
                    track != null -> {
                        val album = track.album.trim()
                        val year = track.year.trim()
                        when {
                            album.isNotBlank() && year.isNotBlank() -> "$album ($year)"
                            album.isNotBlank() -> album
                            year.isNotBlank() -> year
                            else -> state.currentStation?.genre ?: "Music Genome Stream"
                        }
                    }
                    else -> state.currentStation?.genre ?: "Music Genome Stream"
                },
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scrubber Slider
            Slider(
                value = sliderProgress,
                onValueChange = {
                    isDraggingSlider = true
                    dragSliderPosition = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    val targetMs = (dragSliderPosition * state.durationMs).toLong()
                    onSeek(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = PianoCyan,
                    activeTrackColor = PianoCyan,
                    inactiveTrackColor = PianoSurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_progress_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentFormatted,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = totalFormatted,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback and Pianobar control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Love (+) Button
                IconButton(
                    onClick = onLoveTrack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("love_button")
                ) {
                    Icon(
                        imageVector = if (isLoved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Love Song (+)",
                        tint = if (isLoved) PianoRed else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Ban (-) Button
                IconButton(
                    onClick = onBanTrack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ban_button")
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (isBanned) R.drawable.ic_icon_thumb_down else R.drawable.ic_icon_thumb_down_outlined
                        ),
                        contentDescription = "Ban Song (-)",
                        tint = if (isBanned) PianoRed else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Main Play / Pause Button (Large Hero FAB)
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(68.dp)
                        .then(
                            if (isSlop) {
                                Modifier
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    glowColorPrimary.copy(alpha = 0.45f),
                                                    glowColorSecondary.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            radius = size.maxDimension * 0.72f
                                        )
                                    }
                                    .clip(CircleShape)
                                    .background(AiSlopGradients.vibrantBrush)
                            } else {
                                Modifier
                            }
                        )
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSlop) Color.Transparent else PianoCyan,
                        contentColor = PianoOnAccent
                    ),
                    shape = CircleShape
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = PianoOnAccent,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) {
                                ImageVector.vectorResource(R.drawable.ic_notif_pause)
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (state.isPlaying) "Pause (p)" else "Play (p)",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Skip Next (n) Button
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("skip_next_button")
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_notif_skip),
                        contentDescription = "Skip Song (n)",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Bookmark (b) Button
                IconButton(
                    onClick = onBookmarkTrack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bookmark_button")
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (isBookmarked) R.drawable.ic_icon_bookmark else R.drawable.ic_icon_bookmark_border
                        ),
                        contentDescription = "Bookmark (b)",
                        tint = if (isBookmarked) PianoAmber else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explain (e) Music Genome Action
            Surface(
                onClick = onExplainTrack,
                enabled = track != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("explain_song_button"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSlop) PianoSurfaceElevated.copy(alpha = 0.72f) else PianoSurfaceElevated,
                border = if (isSlop && track != null) {
                    androidx.compose.foundation.BorderStroke(1.dp, AiSlopGradients.borderBrush)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, PianoSurfaceBorder)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Why is this playing? (e)",
                        tint = if (track != null) PianoCyan else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Why is this playing?",
                        color = if (track != null) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(e)",
                        color = if (track != null) TextSecondary else TextSecondary.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Snooze / Tired of Song (t) Action
            Surface(
                onClick = onTiredTrack,
                enabled = track != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("tired_button"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSlop) PianoSurfaceElevated.copy(alpha = 0.72f) else PianoSurfaceElevated,
                border = if (isSlop && track != null) {
                    androidx.compose.foundation.BorderStroke(1.dp, AiSlopGradients.borderBrush)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, PianoSurfaceBorder)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_icon_snooze),
                        contentDescription = null,
                        tint = if (track != null) PianoAmber else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I'm tired of this song",
                        color = if (track != null) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(30d)",
                        color = if (track != null) TextSecondary else TextSecondary.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isSlop) {
                AiSlopBentoStrip()
            }
        }
    }
}
