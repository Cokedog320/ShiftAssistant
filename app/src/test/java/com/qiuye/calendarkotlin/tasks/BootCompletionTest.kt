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
        
        io.mockk.mockkObject(com.qiuye.calendarkotlin.tasks.TasksGraph)
        val mockService = io.mockk.mockk<com.qiuye.calendarkotlin.tasks.service.ReminderService>(relaxed = true)
        io.mockk.every { com.qiuye.calendarkotlin.tasks.TasksGraph.reminderService(any()) } returns mockService

        receiver.onReceive(context, intent)
        io.mockk.coVerify(exactly = 1) { mockService.restoreSchedules() }
        
        io.mockk.unmockkObject(com.qiuye.calendarkotlin.tasks.TasksGraph)
    }

    @Test
    fun `onReceive should ignore irrelevant intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = ReminderBootReceiver()
        val intent = Intent(Intent.ACTION_BATTERY_LOW)
        
        io.mockk.mockkObject(com.qiuye.calendarkotlin.tasks.TasksGraph)
        val mockService = io.mockk.mockk<com.qiuye.calendarkotlin.tasks.service.ReminderService>(relaxed = true)
        io.mockk.every { com.qiuye.calendarkotlin.tasks.TasksGraph.reminderService(any()) } returns mockService

        receiver.onReceive(context, intent)
        io.mockk.coVerify(exactly = 0) { mockService.restoreSchedules() }
        
        io.mockk.unmockkObject(com.qiuye.calendarkotlin.tasks.TasksGraph)
    }
}
