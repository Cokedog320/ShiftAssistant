package com.qiuye.calendarkotlin.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE profileId = :profileId")
    fun observeAll(profileId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE profileId = :profileId AND isCompleted = 0 AND scheduledAtMillis > :now")
    fun getFutureActiveReminders(profileId: String, now: Long): List<ReminderEntity>

    @Insert
    fun insert(reminder: ReminderEntity): Long

    @Update
    fun update(reminder: ReminderEntity)

    @Delete
    fun delete(reminder: ReminderEntity)
}


