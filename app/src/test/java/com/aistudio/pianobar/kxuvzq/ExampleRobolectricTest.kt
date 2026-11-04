package com.aistudio.pianobar.kxuvzq

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.pianobar.kxuvzq.data.StationRepository
import org.junit.Assert.assertEquals
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
  fun customStationCreation() {
    val custom = StationRepository.createCustomStation("Miles Davis Radio", "Miles Davis", "Jazz")
    assertEquals("Miles Davis Radio", custom.name)
    assertTrue(custom.isCustom)
  }
}
