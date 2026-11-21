package com.aistudio.pianobar.kxuvzq.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.pianobar.kxuvzq.R
import com.aistudio.pianobar.kxuvzq.model.Station
import com.aistudio.pianobar.kxuvzq.model.Track
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService
import com.aistudio.pianobar.kxuvzq.ui.components.CliTerminalView
import com.aistudio.pianobar.kxuvzq.ui.components.ExplainSongDialog
import com.aistudio.pianobar.kxuvzq.ui.components.HistorySheet
import com.aistudio.pianobar.kxuvzq.ui.components.NowPlayingCard
import com.aistudio.pianobar.kxuvzq.ui.components.SettingsSheet
import com.aistudio.pianobar.kxuvzq.ui.components.StationSelectionSheet
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoCyan
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoDarkBg
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoOnAccent
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoPurple
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurface
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceBorder
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoSurfaceElevated
import com.aistudio.pianobar.kxuvzq.ui.theme.TextPrimary
import com.aistudio.pianobar.kxuvzq.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianobarScreen(
    playerManager: PianobarPlayerManager = PianobarPlayerManager.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by playerManager.state.collectAsState()
    val stations by playerManager.stations.collectAsState()

    val stationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showStationSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showExplainDialog by remember { mutableStateOf(false) }

    fun ensureServiceStarted() {
        PianobarPlaybackService.start(context)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PianoDarkBg),
        containerColor = PianoDarkBg,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = PianoDarkBg,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo + Branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_branding_header")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PianoSurfaceElevated)
                                .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ">_",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PianoGreen,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "pianobar",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLI",
                                    color = PianoGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Pandora Radio Client",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Mode Switch & Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // CLI / Material Toggle Button
                        ElevatedButton(
                            onClick = { playerManager.toggleCliMode() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (state.isCliMode) PianoCyan else PianoSurfaceElevated,
                                contentColor = if (state.isCliMode) PianoOnAccent else PianoCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("toggle_cli_mode_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_icon_terminal),
                                contentDescription = "CLI Toggle",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isCliMode) "M3 UI" else "Terminal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("open_history_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_icon_history),
                                contentDescription = "History",
                                tint = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("open_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            AnimatedContent(
                targetState = state.isCliMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "mode_switch_anim"
            ) { isCli ->
                if (isCli) {
                    CliTerminalView(
                        state = state,
                        onExecuteCommand = { cmd ->
                            ensureServiceStarted()
                            playerManager.executeTerminalCommand(cmd)
                        }
                    )
                } else {
                    // Material 3 UI Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Quick Station Selector Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stations.forEach { station ->
                                val isSelected = station.id == state.currentStation?.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PianoCyan else PianoSurfaceBorder,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            ensureServiceStarted()
                                            playerManager.selectStation(station, autoPlay = true)
                                        }
                                        .testTag("station_chip_${station.id}"),
                                    color = if (isSelected) PianoCyan.copy(alpha = 0.15f) else PianoSurfaceElevated
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                if (station.isQuickMix) R.drawable.ic_icon_shuffle else R.drawable.ic_icon_radio
                                            ),
                                            contentDescription = null,
                                            tint = if (isSelected) PianoCyan else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = station.name,
                                            color = if (isSelected) PianoCyan else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hero Now Playing Card
                        NowPlayingCard(
                            state = state,
                            onTogglePlayPause = {
                                ensureServiceStarted()
                                playerManager.togglePlayPause()
                            },
                            onSkipNext = {
                                ensureServiceStarted()
                                playerManager.skipNext()
                            },
                            onLoveTrack = {
                                playerManager.loveCurrentTrack()
                            },
                            onBanTrack = {
                                playerManager.banCurrentTrack()
                            },
                            onBookmarkTrack = {
                                playerManager.bookmarkTrack()
                            },
                            onExplainTrack = {
                                playerManager.requestExplanation()
                                showExplainDialog = true
                            },
                            onSeek = { targetMs ->
                                playerManager.seekTo(targetMs)
                            },
                            onOpenStationSelect = {
                                showStationSheet = true
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Station Selection Sheet
    if (showStationSheet) {
        StationSelectionSheet(
            sheetState = stationSheetState,
            stations = stations,
            currentStationId = state.currentStation?.id,
            isPlaying = state.isPlaying,
            onDismiss = { showStationSheet = false },
            onSelectStation = { station ->
                ensureServiceStarted()
                playerManager.selectStation(station, autoPlay = true)
            },
            onCreateStation = { name, seed, genre ->
                ensureServiceStarted()
                playerManager.createCustomStation(name, seed, genre)
            },
            onDeleteStation = { id ->
                playerManager.deleteStation(id)
            },
            onToggleQuickMix = { id ->
                playerManager.toggleStationQuickMix(id)
            }
        )
    }

    // History Sheet
    if (showHistorySheet) {
        HistorySheet(
            sheetState = historySheetState,
            history = state.history,
            lovedTrackIds = state.lovedTrackIds,
            bannedTrackIds = state.bannedTrackIds,
            onDismiss = { showHistorySheet = false },
            onPlayTrack = { track ->
                ensureServiceStarted()
                playerManager.playTrack(track)
            },
            onToggleLove = { track ->
                playerManager.loveCurrentTrack()
            }
        )
    }

    // Settings Sheet
    if (showSettingsSheet) {
        SettingsSheet(
            sheetState = settingsSheetState,
            onDismiss = { showSettingsSheet = false },
            onShowSavedToast = {
                val msg = if (state.isUserLoggedIn) {
                    "Connected to Pandora as ${state.userEmail}"
                } else {
                    "Pianobar settings saved"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Explain Dialog
    if (showExplainDialog) {
        ExplainSongDialog(
            track = state.currentTrack,
            stationName = state.currentStation?.name ?: "QuickMix",
            onDismiss = { showExplainDialog = false }
        )
    }

    // Require Login Dialog if not authenticated
    if (!state.isUserLoggedIn) {
        com.aistudio.pianobar.kxuvzq.ui.components.LoginDialog(
            isLoading = state.isBuffering,
            errorMessage = state.error,
            onLogin = { email, pass ->
                playerManager.saveAndLoginAccount(
                    user = email,
                    pass = pass,
                    highQuality = state.isHighQualityAudio,
                    backgroundStreaming = true
                )
            }
        )
    }
}
