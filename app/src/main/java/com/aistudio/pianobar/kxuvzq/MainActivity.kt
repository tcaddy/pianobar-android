package com.aistudio.pianobar.kxuvzq

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.aistudio.pianobar.kxuvzq.player.PianobarPlayerManager
import com.aistudio.pianobar.kxuvzq.ui.PianobarScreen
import com.aistudio.pianobar.kxuvzq.ui.theme.MyApplicationTheme
import com.aistudio.pianobar.kxuvzq.ui.theme.PianobarThemes

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val playerManager = PianobarPlayerManager.getInstance()
        playerManager.initialize(this)

        // Match the pre-compose window to the saved palette (notably for light themes).
        val savedPalette = PianobarThemes.fromId(playerManager.state.value.themeId)
        window.setBackgroundDrawable(ColorDrawable(savedPalette.background.toArgb()))

        setContent {
            val state by playerManager.state.collectAsState()
            MyApplicationTheme(themeId = state.themeId) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle notification permission result */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                PianobarScreen()
            }
        }
    }
}
