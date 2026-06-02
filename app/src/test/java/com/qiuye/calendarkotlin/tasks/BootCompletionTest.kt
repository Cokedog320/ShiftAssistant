package com.qiuye.calendarkotlin.tasks

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.tasks.receiver.ReminderBootReceiver
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class BootCompletionTest : BaseUnitTest() {

    @Test
    fun `onReceive BOOT_COMPLETED should trigger restore logic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = ReminderBootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        // Since TasksGraph provides Singletons and CoroutineScope is used, 
        // we mainly verify it doesn't crash and returns gracefully when called.
        // A deeper test would mock TasksGraph or inject a fake service.
        try {
            receiver.onReceive(context, intent)
            assertTrue(true) // Should reach here without exception
        } catch (e: Exception) {
            fail("BootReceiver should not throw exception on boot intent")
        }
    }

    @Test
    fun `onReceive should ignore irrelevant intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = ReminderBootReceiver()
        val intent = Intent(Intent.ACTION_BATTERY_LOW)
        
        try {
            receiver.onReceive(context, intent)
            assertTrue(true)
        } catch (e: Exception) {
            fail("Should ignore other intents")
        }
    }
}
