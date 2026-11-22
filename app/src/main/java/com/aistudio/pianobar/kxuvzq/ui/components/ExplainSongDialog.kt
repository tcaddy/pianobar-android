package com.aistudio.pianobar.kxuvzq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.pianobar.kxuvzq.model.Track
import com.aistudio.pianobar.kxuvzq.ui.theme.BodyFontFamily
import com.aistudio.pianobar.kxuvzq.ui.theme.DisplayFontFamily
import com.aistudio.pianobar.kxuvzq.ui.theme.MonoFontFamily
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoCyan
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoDarkBg
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoPurple
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurface
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceBorder
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceElevated
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalBg
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalPrimary
import com.aistudio.pianobar.kxuvzq.ui.theme.TextPrimary
import com.aistudio.pianobar.kxuvzq.ui.theme.TextSecondary

@Composable
fun ExplainSongDialog(
    track: Track?,
    stationName: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (track == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(16.dp))
                .testTag("explain_song_dialog"),
            color = PianoSurface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with clean terminal [e] icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PianoSurfaceElevated)
                                .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "e",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = PianoGreen,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Song Explanation",
                                fontFamily = DisplayFontFamily,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Music Genome Project attributes",
                                fontFamily = BodyFontFamily,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("explain_close_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Track details card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(8.dp)),
                    color = PianoSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "\"${track.title}\"",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Station: $stationName",
                            color = PianoCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Genome Attributes & Selection Reasoning:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(8.dp)),
                    color = TerminalBg
                ) {
                    val explanationText = track.musicGenomeExplanation
                    when {
                        isLoading && explanationText.isBlank() -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .testTag("explain_loading_indicator"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PianoCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Querying Pandora Music Genome...",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = MonoFontFamily
                                )
                            }
                        }
                        explanationText.isNotBlank() -> {
                            Text(
                                text = explanationText,
                                color = TerminalPrimary,
                                fontSize = 13.5.sp,
                                lineHeight = 21.sp,
                                fontFamily = BodyFontFamily,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .testTag("explain_result_text")
                            )
                        }
                        else -> {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = errorMessage ?: "No explanation provided by Pandora for this track.",
                                    fontFamily = BodyFontFamily,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.testTag("explain_error_text")
                                )
                                if (onRetry != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onRetry,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PianoSurfaceElevated,
                                            contentColor = PianoCyan
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PianoSurfaceBorder),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("explain_retry_button")
                                    ) {
                                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CLI terminal command hint
                Text(
                    text = "$ pianobar --explain",
                    fontFamily = MonoFontFamily,
                    color = PianoGreen,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PianoSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PianoSurfaceBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
