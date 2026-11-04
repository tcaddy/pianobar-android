package com.aistudio.pianobar.kxuvzq.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.pianobar.kxuvzq.model.Station
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSelectionSheet(
    sheetState: SheetState,
    stations: List<Station>,
    currentStationId: String?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onSelectStation: (Station) -> Unit,
    onCreateStation: (name: String, seed: String, genre: String) -> Unit,
    onDeleteStation: (String) -> Unit,
    onToggleQuickMix: (String) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var newStationName by remember { mutableStateOf("") }
    var newStationSeed by remember { mutableStateOf("") }
    var newStationGenre by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Alphabetize stations (case-insensitive) and filter by searchQuery
    val filteredStations = remember(stations, searchQuery) {
        val sorted = stations.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        if (searchQuery.isBlank()) {
            sorted
        } else {
            val q = searchQuery.trim().lowercase()
            sorted.filter {
                it.name.lowercase().contains(q) ||
                        it.genre.lowercase().contains(q) ||
                        it.description.lowercase().contains(q)
            }
        }
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
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pandora Stations",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stations.size} stations available in Pianobar",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showImportDialog = !showImportDialog },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PianoCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (showImportDialog) "Close" else "Import")
                    }

                    Button(
                        onClick = {
                            showCreateForm = !showCreateForm
                            if (showCreateForm) showImportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showCreateForm) PianoSurfaceElevated else PianoCyan,
                            contentColor = if (showCreateForm) TextPrimary else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_station_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Station",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showCreateForm) "Cancel" else "Create")
                    }
                }
            }

            // Quick Batch Import Stations Form
            AnimatedVisibility(visible = showImportDialog) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, PianoPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    color = PianoSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Import Personal Stations",
                            color = PianoPurple,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste your Pandora station names or artist/genre seeds (separated by commas or new lines):",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            placeholder = { Text("e.g. Miles Davis Radio, Daft Punk, Lo-Fi Beats, Classical Piano") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PianoPurple,
                                unfocusedBorderColor = PianoSurfaceBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val seeds = importText.split(',', '\n').map { it.trim() }.filter { it.isNotBlank() }
                                seeds.forEach { seed ->
                                    val stationName = if (seed.endsWith("Radio", ignoreCase = true)) seed else "$seed Radio"
                                    onCreateStation(stationName, seed, "Custom")
                                }
                                importText = ""
                                showImportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PianoPurple, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add Stations to Tuner", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Inline Create Station Seed Form
            AnimatedVisibility(visible = showCreateForm) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, PianoCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    color = PianoSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "New Station Seed",
                            color = PianoCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newStationSeed,
                            onValueChange = {
                                newStationSeed = it
                                if (newStationName.isBlank() || newStationName.endsWith("Radio")) {
                                    newStationName = if (it.isNotBlank()) "$it Radio" else ""
                                }
                            },
                            label = { Text("Artist, Song, or Genre Seed") },
                            placeholder = { Text("e.g. Bill Evans, Debussy, Lo-Fi Piano") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("station_seed_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PianoCyan,
                                unfocusedBorderColor = PianoSurfaceBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newStationName,
                            onValueChange = { newStationName = it },
                            label = { Text("Station Label") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PianoCyan,
                                unfocusedBorderColor = PianoSurfaceBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newStationGenre,
                            onValueChange = { newStationGenre = it },
                            label = { Text("Genre Tag (Optional)") },
                            placeholder = { Text("Jazz / Classical / Ambient") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PianoCyan,
                                unfocusedBorderColor = PianoSurfaceBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newStationSeed.isNotBlank()) {
                                    onCreateStation(
                                        newStationName.ifBlank { "${newStationSeed.trim()} Radio" },
                                        newStationSeed.trim(),
                                        newStationGenre.trim()
                                    )
                                    newStationName = ""
                                    newStationSeed = ""
                                    newStationGenre = ""
                                    showCreateForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_station_submit"),
                            colors = ButtonDefaults.buttonColors(containerColor = PianoCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Initialize Station Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Station Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("station_search_input"),
                placeholder = { Text("Search stations by name or genre...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Stations",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PianoGreen,
                    unfocusedBorderColor = PianoSurfaceBorder,
                    focusedContainerColor = PianoSurfaceElevated,
                    unfocusedContainerColor = PianoSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Station List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredStations.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(12.dp)),
                            color = PianoSurface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No stations match \"$searchQuery\"",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                items(
                    count = filteredStations.size,
                    key = { index -> "${filteredStations[index].id}_$index" }
                ) { index ->
                    val station = filteredStations[index]
                    val isSelected = station.id == currentStationId

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PianoCyan else PianoSurfaceBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectStation(station)
                                onDismiss()
                            }
                            .testTag("station_card_${station.id}"),
                        color = if (isSelected) PianoSurfaceElevated else PianoSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) PianoCyan.copy(alpha = 0.2f) else Color(0xFF1E293B)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (station.isQuickMix) Icons.Default.Shuffle else Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = if (isSelected) PianoCyan else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = station.name,
                                        color = if (isSelected) PianoCyan else TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (station.isQuickMix) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "QuickMix",
                                            color = PianoPurple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = station.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            // Visualizer indicator if playing this station
                            if (isSelected) {
                                VisualizerBars(
                                    isPlaying = isPlaying,
                                    barCount = 6,
                                    maxHeight = 20.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (station.isCustom) {
                                IconButton(
                                    onClick = { onDeleteStation(station.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Station",
                                        tint = PianoRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
