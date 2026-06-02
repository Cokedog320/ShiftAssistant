package com.qiuye.calendarkotlin.tasks

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.scheduler.AlarmReminderScheduler
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

class ReminderSchedulerTest : BaseUnitTest() {

    private lateinit var context: Context
    private lateinit var scheduler: AlarmReminderScheduler
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        scheduler = AlarmReminderScheduler(context)
    }

    @Test
    fun `schedule should set exact alarm if permission granted`() {
        // Given future time
        val triggerTime = System.currentTimeMillis() + 10000
        val reminder = ReminderEntity(id = 1L, title = "Test", note = "", scheduledAtMillis = triggerTime, isCompleted = false, createdAtMillis = 0L, updatedAtMillis = 0L)
        
        // When
        scheduler.schedule(reminder)
        
        // Then
        val scheduledAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(scheduledAlarm)
        assertEquals(triggerTime, scheduledAlarm!!.triggerAtTime)
    }

    @Test
    fun `schedule should cancel alarm if reminder is already completed`() {
        // Given
        val triggerTime = System.currentTimeMillis() + 10000
        val reminder = ReminderEntity(id = 2L, title = "Done", note = "", scheduledAtMillis = triggerTime, isCompleted = true, createdAtMillis = 0L, updatedAtMillis = 0L)
        
        // When
        scheduler.schedule(reminder)
        
        // Then
        val scheduledAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNull("Should not schedule alarm for completed reminder", scheduledAlarm)
    }

    @Test
    fun `schedule should not schedule if time is in the past`() {
        // Given past time
        val triggerTime = System.currentTimeMillis() - 10000
        val reminder = ReminderEntity(id = 3L, title = "Past", note = "", scheduledAtMillis = triggerTime, isCompleted = false, createdAtMillis = 0L, updatedAtMillis = 0L)
        
        // When
        scheduler.schedule(reminder)
        
        // Then
        val scheduledAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNull("Should not schedule alarm for past reminder", scheduledAlarm)
    }

    @Test
    fun `cancel should remove scheduled alarm`() {
        // Schedule first
        val triggerTime = System.currentTimeMillis() + 10000
        val reminder = ReminderEntity(id = 4L, title = "To Cancel", note = "", scheduledAtMillis = triggerTime, isCompleted = false, createdAtMillis = 0L, updatedAtMillis = 0L)
        scheduler.schedule(reminder)
        
        assertNotNull(shadowAlarmManager.nextScheduledAlarm)
        
        // Act: Cancel
        scheduler.cancel(4L)
        
        // The shadow removes it
        assertNull(shadowAlarmManager.nextScheduledAlarm)
    }
}
