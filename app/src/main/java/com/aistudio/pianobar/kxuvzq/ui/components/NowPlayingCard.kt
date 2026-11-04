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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoAmber
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoCyan
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoDarkBg
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoPurple
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoRed
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurface
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceBorder
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceElevated
import com.aistudio.pianobar.kxuvzq.ui.theme.TextPrimary
import com.aistudio.pianobar.kxuvzq.ui.theme.TextSecondary
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(24.dp)),
        color = PianoSurface,
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
                color = PianoSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, PianoCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
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
                        text = "• ${state.currentStation?.genre ?: "Pianobar"}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Artwork Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .scale(artworkScale)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(12.dp))
                    .background(PianoSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!track?.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track?.artworkUrl)
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
                            imageVector = Icons.Default.MusicNote,
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
                        .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = track?.audioBitrate ?: "AAC 192k",
                        color = TextSecondary,
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
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
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
                        imageVector = if (isBanned) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
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
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PianoCyan,
                        contentColor = Color.Black
                    ),
                    shape = CircleShape
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                        imageVector = Icons.Default.SkipNext,
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
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark (b)",
                        tint = if (isBookmarked) PianoAmber else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Explain (e) Music Genome Button
                IconButton(
                    onClick = onExplainTrack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("explain_song_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Why is this playing? (e)",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
