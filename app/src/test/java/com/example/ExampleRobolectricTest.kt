package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.location.LocationService
import com.example.data.seed.KenyaPropertySeed
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
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Makao", appName)
  }

  @Test
  fun `verify Kenya property seed listings and geospatial distance`() {
    val properties = KenyaPropertySeed.getInitialProperties()
    assertTrue("Should have initial Kenya properties", properties.isNotEmpty())

    val first = properties.first()
    assertNotNull(first.title)
    assertTrue("Should format KES millions", first.formattedPrice.startsWith("KES"))

    // Distance calculation test from Westlands to Karen
    val distance = LocationService.calculateDistanceKm(-1.2683, 36.8070, -1.3197, 36.7067)
    assertTrue("Distance should be around 12-14km", distance in 8.0..20.0)
  }
}
