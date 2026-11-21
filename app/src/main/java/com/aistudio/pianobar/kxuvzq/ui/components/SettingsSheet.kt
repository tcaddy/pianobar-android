package com.aistudio.pianobar.kxuvzq.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoCyan
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoDarkBg
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoDragHandle
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoOnAccent
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoOnSuccess
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoPurple
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoRed
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurface
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceBorder
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceElevated
import com.aistudio.pianobar.kxuvzq.ui.theme.AiSlopGradients
import com.aistudio.pianobar.kxuvzq.ui.theme.AiSlopPalette
import com.aistudio.pianobar.kxuvzq.ui.theme.PianobarPalette
import com.aistudio.pianobar.kxuvzq.ui.theme.PianobarThemes
import com.aistudio.pianobar.kxuvzq.ui.theme.TextPrimary
import com.aistudio.pianobar.kxuvzq.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onShowSavedToast: () -> Unit
) {
    val playerManager = PianobarPlayerManager.getInstance()
    val state by playerManager.state.collectAsState()
    val prefs = playerManager.getPreferences()

    var pandoraUser by remember {
        mutableStateOf(prefs?.username ?: state.userEmail)
    }
    var pandoraPass by remember {
        mutableStateOf(prefs?.password ?: "")
    }
    var passwordVisible by remember { mutableStateOf(false) }

    var highQualityAudio by remember {
        mutableStateOf(prefs?.isHighQualityAudio ?: state.isHighQualityAudio)
    }
    var backgroundPlayback by remember {
        mutableStateOf(prefs?.isBackgroundStreaming ?: true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PianoDarkBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(PianoDragHandle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PianoSurfaceElevated)
                        .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pandora protocol & streaming configuration",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Active Account Status Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (state.isUserLoggedIn) PianoGreen.copy(alpha = 0.5f) else PianoSurfaceBorder,
                        RoundedCornerShape(16.dp)
                    ),
                color = PianoSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.isUserLoggedIn) Icons.Default.CheckCircle else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (state.isUserLoggedIn) PianoGreen else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isUserLoggedIn) "Pandora Account Active" else "Not Signed In",
                                color = if (state.isUserLoggedIn) PianoGreen else TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.isUserLoggedIn) {
                            OutlinedButton(
                                onClick = {
                                    playerManager.logoutAccount()
                                    pandoraUser = ""
                                    pandoraPass = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PianoRed)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_icon_logout),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", fontSize = 12.sp)
                            }
                        }
                    }

                    if (state.isUserLoggedIn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Logged in as: ${state.userEmail}",
                            color = PianoCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in below to sync your personalized stations and playlists from Pandora.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pandora Credentials Form
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(16.dp)),
                color = PianoSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pandora Account Credentials",
                        color = PianoCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter your Pandora email/username and password. Credentials are securely stored on your device.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Username / Email input
                    OutlinedTextField(
                        value = pandoraUser,
                        onValueChange = { pandoraUser = it },
                        label = { Text("Pandora Email / Username") },
                        placeholder = { Text("e.g. user@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            autoCorrectEnabled = false
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pandora_email_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PianoCyan,
                            unfocusedBorderColor = PianoSurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password input with visibility toggle and dictionary predictions disabled
                    OutlinedTextField(
                        value = pandoraPass,
                        onValueChange = { pandoraPass = it },
                        label = { Text("Password or Auth Token") },
                        placeholder = { Text("Enter account password") },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_icon_key),
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        trailingIcon = {
                            val icon = ImageVector.vectorResource(
                                if (passwordVisible) R.drawable.ic_icon_visibility else R.drawable.ic_icon_visibility_off
                            )
                            val description = if (passwordVisible) "Hide password" else "Show password"
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.testTag("toggle_password_visibility_btn")
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = description,
                                    tint = if (passwordVisible) PianoCyan else TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation('\u2022')
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pandora_password_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PianoCyan,
                            unfocusedBorderColor = PianoSurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_icon_security),
                            contentDescription = "Hardware Security",
                            tint = PianoGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Encrypted in Hardware Secure Enclave (StrongBox / TEE AES-256-GCM)",
                            color = PianoGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Audio & Playback Options
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(16.dp)),
                color = PianoSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Playback & Stream Quality",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("High Quality Audio (AAC 192 kbps)", color = TextPrimary, fontSize = 14.sp)
                            Text("Pandora One high bitrate audio streaming", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = highQualityAudio,
                            onCheckedChange = { highQualityAudio = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PianoOnSuccess,
                                checkedTrackColor = PianoGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = PianoSurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Streaming Service", color = TextPrimary, fontSize = 14.sp)
                            Text("Keep radio playing when app is minimized or screen off", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = backgroundPlayback,
                            onCheckedChange = { backgroundPlayback = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PianoOnSuccess,
                                checkedTrackColor = PianoGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = PianoSurfaceElevated
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Appearance
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(16.dp))
                    .testTag("theme_selector"),
                color = PianoSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Appearance",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Terminal palette · applies instantly",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PianobarThemes.all.forEach { palette ->
                            ThemeSwatch(
                                palette = palette,
                                selected = palette.id == state.themeId,
                                onClick = { playerManager.setTheme(palette.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sleep Timer Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (state.isSleepTimerActive) PianoCyan.copy(alpha = 0.6f) else PianoSurfaceBorder,
                        RoundedCornerShape(12.dp)
                    ),
                color = PianoSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sleep Timer",
                                color = if (state.isSleepTimerActive) PianoCyan else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isSleepTimerActive) {
                                    val remainingMins = state.sleepTimerRemainingSeconds / 60
                                    val remainingSecs = state.sleepTimerRemainingSeconds % 60
                                    String.format(java.util.Locale.US, "Stopping in %02d:%02d", remainingMins, remainingSecs)
                                } else {
                                    "Automatically stop playback after set duration"
                                },
                                color = if (state.isSleepTimerActive) PianoCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (state.isSleepTimerActive) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        if (state.isSleepTimerActive) {
                            OutlinedButton(
                                onClick = { playerManager.cancelSleepTimer() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PianoRed)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset buttons
                    val timerPresets = listOf(15, 30, 45, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timerPresets.forEach { minutes ->
                            val isSelected = state.isSleepTimerActive &&
                                    (state.sleepTimerRemainingSeconds <= minutes * 60L &&
                                     state.sleepTimerRemainingSeconds > (minutes - 15) * 60L)
                            Button(
                                onClick = { playerManager.startSleepTimer(minutes) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) PianoCyan else PianoSurfaceElevated,
                                    contentColor = if (isSelected) PianoOnAccent else TextPrimary
                                )
                            ) {
                                Text(
                                    text = "${minutes}m",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CLI Shortcuts Reference Guide
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(12.dp)),
            color = PianoSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pianobar CLI Shortcuts Reference",
                    color = PianoGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val shortcuts = listOf(
                    "p" to "Play or pause current song",
                    "n" to "Skip to next song",
                    "+" to "Love song (thumbs up)",
                    "-" to "Ban song (thumbs down)",
                    "t" to "Tired of song (snooze 1 month)",
                    "b" to "Bookmark song or artist",
                    "e" to "Explain why song is playing (Music Genome)",
                    "s" to "Select or switch station",
                    "h" to "Show recent play history",
                    "sleep" to "Toggle 30m sleep timer (or: sleep <mins>)",
                    "q" to "Quit playback"
                )
                shortcuts.forEach { (cmd, desc) ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "[$cmd] ",
                            fontFamily = FontFamily.Monospace,
                            color = PianoGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = desc,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Save & Load Account Button
        Button(
            onClick = {
                playerManager.saveAndLoginAccount(
                    user = pandoraUser,
                    pass = pandoraPass,
                    highQuality = highQualityAudio,
                    backgroundStreaming = backgroundPlayback
                )
                onShowSavedToast()
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_settings_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = PianoGreen,
                contentColor = PianoOnSuccess
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (pandoraUser.isNotBlank()) "Save & Connect Account" else "Save Settings",
                fontWeight = FontWeight.Bold
            )
        }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeSwatch(
    palette: PianobarPalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isSlopSwatch = palette.id == AiSlopPalette.id
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(112.dp)
            .height(76.dp)
            .testTag("theme_option_${palette.id}")
            .semantics { contentDescription = "${palette.name}: ${palette.caption}" },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.surfaceElevated else PianoSurfaceElevated,
        contentColor = TextPrimary,
        border = when {
            isSlopSwatch -> androidx.compose.foundation.BorderStroke(
                width = if (selected) 2.dp else 1.2.dp,
                brush = AiSlopGradients.borderBrush
            )
            selected -> androidx.compose.foundation.BorderStroke(2.dp, palette.accent)
            else -> androidx.compose.foundation.BorderStroke(1.dp, PianoSurfaceBorder)
        }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(palette.background, palette.surface, palette.accent, palette.success)
                    .forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(1.dp, palette.border, CircleShape)
                        )
                    }
            }
            Text(
                text = if (isSlopSwatch) "✨ ${palette.name}" else palette.name,
                color = if (selected) palette.accent else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
