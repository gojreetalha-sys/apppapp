package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Voice Assistant", appName)
    }

    @Test
    fun `test iso timestamp parser`() {
        val iso = "2026-10-13T17:00:00Z"
        val epoch = ReminderScheduler.parseIsoTimestamp(iso)
        assertNotNull(epoch)
    }
}
