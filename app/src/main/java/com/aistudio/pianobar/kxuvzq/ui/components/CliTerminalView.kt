package com.aistudio.pianobar.kxuvzq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.pianobar.kxuvzq.model.LogType
import com.aistudio.pianobar.kxuvzq.model.PianobarLog
import com.aistudio.pianobar.kxuvzq.model.PlayerState
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoCyan
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoPurple
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalBg
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalError
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalPrompt
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalText
import com.aistudio.pianobar.kxuvzq.ui.theme.TerminalWarn
import java.util.Locale

@Composable
fun CliTerminalView(
    state: PlayerState,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new logs
    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    val currentSeconds = state.currentPositionMs / 1000
    val totalSeconds = state.durationMs / 1000
    val timeFormatted = String.format(
        Locale.US,
        "-%02d:%02d/%02d:%02d",
        (totalSeconds - currentSeconds).coerceAtLeast(0) / 60,
        (totalSeconds - currentSeconds).coerceAtLeast(0) % 60,
        totalSeconds / 60,
        totalSeconds % 60
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Authentic ASCII Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, PianoCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            color = Color(0xFF030508)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = """
  ____  _                 _
 |  _ \(_) __ _ _ __  ___| |__   __ _ _ __
 | |_) | |/ _` | '_ \/ _ \ '_ \ / _` | '__|
 |  __/| | (_| | | |  __/ |_) | (_| | |
 |_|   |_|\__,_|_| |_|\___|_.__/ \__,_|_|  v2026.10
                    """.trimIndent(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = PianoCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val stationName = state.currentStation?.name ?: "No Station"
                val track = state.currentTrack
                val trackTitle = track?.title ?: "Ready"
                val trackArtist = track?.artist ?: "Pianobar"
                val trackAlbum = track?.album ?: "Pandora Radio"

                Text(
                    text = "|> Station: \"$stationName\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = PianoGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "|> \"$trackTitle\" by \"$trackArtist\" on \"$trackAlbum\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TerminalText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "#   $timeFormatted  [${if (state.isPlaying) "PLAYING" else if (state.isBuffering) "BUFFER" else "PAUSED"}]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = PianoPurple
                    )
                    Text(
                        text = track?.audioBitrate ?: "AAC 192k",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalPrompt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CLI Quick Action Shortcut Bar
        val shortcuts = listOf(
            "p" to "play/pause",
            "n" to "next",
            "+" to "love",
            "-" to "ban",
            "t" to "tired",
            "b" to "bookmark",
            "e" to "explain",
            "s" to "stations",
            "h" to "history",
            "help" to "help"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shortcuts.forEach { (key, label) ->
                ElevatedButton(
                    onClick = { onExecuteCommand(key) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF131924),
                        contentColor = PianoCyan
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("cli_btn_$key")
                ) {
                    Text(
                        text = "[$key] $label",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Live Log Terminal Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, Color(0xFF1C2538), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = state.logs.size,
                    key = { index -> "${state.logs[index].id}_$index" }
                ) { index ->
                    val log = state.logs[index]
                    LogLineItem(log = log)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Command Prompt Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("cli_command_input"),
                placeholder = {
                    Text(
                        text = "Type pianobar command (p, n, +, -, s, e, help)...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF4A5568)
                    )
                },
                leadingIcon = {
                    Text(
                        text = ">_",
                        fontFamily = FontFamily.Monospace,
                        color = PianoCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TerminalText
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PianoCyan,
                    unfocusedBorderColor = Color(0xFF26334D),
                    focusedContainerColor = Color(0xFF080C14),
                    unfocusedContainerColor = Color(0xFF080C14)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank()) {
                            onExecuteCommand(commandInput)
                            commandInput = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onExecuteCommand(commandInput)
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PianoCyan)
                    .testTag("cli_submit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun LogLineItem(log: PianobarLog) {
    val color = when (log.type) {
        LogType.INFO -> Color(0xFF8899A6)
        LogType.PLAYING -> TerminalText
        LogType.LOVE -> PianoCyan
        LogType.BAN -> TerminalError
        LogType.WARNING -> TerminalWarn
        LogType.PROMPT -> TerminalPrompt
        LogType.COMMAND -> PianoPurple
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp)
    ) {
        Text(
            text = "${log.timeFormatted} ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF4A5568)
        )
        Text(
            text = "${log.prefix} ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = log.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = if (log.type == LogType.PLAYING) TerminalText else color.copy(alpha = 0.9f)
        )
    }
}
