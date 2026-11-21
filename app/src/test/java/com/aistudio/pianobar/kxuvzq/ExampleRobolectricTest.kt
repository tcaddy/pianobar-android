package com.aistudio.pianobar.kxuvzq

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.aistudio.pianobar.kxuvzq.data.PianobarPreferences
import com.aistudio.pianobar.kxuvzq.data.StationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pianobar", appName)
  }

  @Test
  fun defaultStationsEmptyUntilLogin() {
    val stations = StationRepository.defaultStations
    assertTrue(stations.isEmpty())
  }

  @Test
  fun themeId_isPersistedAcrossPreferenceInstances() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PianobarPreferences(context)
    assertEquals("midnight", prefs.themeId)

    prefs.themeId = "amber-crt"
    assertEquals("amber-crt", PianobarPreferences(context).themeId)

    prefs.themeId = "midnight"
  }

  @Test
  fun customStationCreation() {
    val custom = StationRepository.createCustomStation("Miles Davis Radio", "Miles Davis", "Jazz")
    assertEquals("Miles Davis Radio", custom.name)
    assertTrue(custom.isCustom)
  }

  @Test
  fun allReplacementIconDrawablesLoadCleanly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val iconIds = listOf(
      R.drawable.ic_icon_bookmark,
      R.drawable.ic_icon_bookmark_border,
      R.drawable.ic_icon_history,
      R.drawable.ic_icon_key,
      R.drawable.ic_icon_logout,
      R.drawable.ic_icon_radio,
      R.drawable.ic_icon_security,
      R.drawable.ic_icon_shuffle,
      R.drawable.ic_icon_terminal,
      R.drawable.ic_icon_thumb_down,
      R.drawable.ic_icon_thumb_down_outlined,
      R.drawable.ic_icon_visibility,
      R.drawable.ic_icon_visibility_off,
      R.drawable.ic_notif_music,
      R.drawable.ic_notif_pause,
      R.drawable.ic_notif_skip
    )
    iconIds.forEach { resId ->
      assertNotNull("Drawable $resId should inflate cleanly", ContextCompat.getDrawable(context, resId))
    }
  }
}
