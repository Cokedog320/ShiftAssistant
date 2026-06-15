package com.qiuye.calendarkotlin.tasks

import android.content.Context
import com.qiuye.calendarkotlin.diary.data.DiaryRepository
import com.qiuye.calendarkotlin.tasks.data.ReminderDatabase
import com.qiuye.calendarkotlin.tasks.data.ReminderRepository
import com.qiuye.calendarkotlin.tasks.notification.ReminderNotifier
import com.qiuye.calendarkotlin.tasks.scheduler.AlarmReminderScheduler
import com.qiuye.calendarkotlin.tasks.scheduler.ReminderScheduler
import com.qiuye.calendarkotlin.tasks.service.ReminderService
import com.qiuye.calendarkotlin.data.CalendarRepository

object TasksGraph {
    @Volatile
    private var applicationContext: Context? = null

    @Volatile
    private var database: ReminderDatabase? = null

    @Volatile
    private var repository: ReminderRepository? = null

    @Volatile
    private var diaryRepository: DiaryRepository? = null

    @Volatile
    private var scheduler: ReminderScheduler? = null

    @Volatile
    private var service: ReminderService? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        applicationContext = appContext
        ReminderNotifier.createChannel(appContext)
    }

    fun reminderService(context: Context? = null): ReminderService {
        if (context != null) {
            initialize(context)
        }
        return service ?: synchronized(this) {
            val appContext = requireNotNull(applicationContext) {
                "TasksGraph must be initialized before use"
            }
            val currentDatabase = database ?: ReminderDatabase.getInstance(appContext).also { database = it }
            val currentRepository = repository ?: ReminderRepository(currentDatabase.reminderDao()).also { repository = it }
            val currentScheduler = scheduler ?: AlarmReminderScheduler(appContext).also { scheduler = it }
            val calendarRepo = CalendarRepository(appContext)
            service ?: ReminderService(
                repository = currentRepository,
                scheduler = currentScheduler,
                context = appContext,
                calendarRepository = calendarRepo
            ).also { service = it }
        }
    }

    fun diaryRepository(context: Context? = null): DiaryRepository {
        if (context != null) {
            initialize(context)
        }
        return diaryRepository ?: synchronized(this) {
            val appContext = requireNotNull(applicationContext) {
                "TasksGraph must be initialized before use"
            }
            val currentDatabase = database ?: ReminderDatabase.getInstance(appContext).also { database = it }
            diaryRepository ?: DiaryRepository(currentDatabase.diaryDao()).also { diaryRepository = it }
        }
    }
}


