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

    @Query("SELECT * FROM reminders WHERE profileId = :profileId AND isArchived = 0")
    fun observeActive(profileId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE profileId = :profileId AND isArchived = 1")
    fun observeArchived(profileId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE profileId = :profileId AND isCompleted = 0 AND scheduledAtMillis > :now AND isArchived = 0")
    fun getFutureActiveReminders(profileId: String, now: Long): List<ReminderEntity>

    @Query(
        """
        SELECT * FROM reminders
        WHERE profileId = :profileId
          AND isArchived = 0
          AND (
              (isCompleted = 1 AND updatedAtMillis <= :completedCutoffMillis)
              OR (isCompleted = 0 AND scheduledAtMillis <= :overdueCutoffMillis)
          )
        """
    )
    fun findEligibleForAutoArchive(
        profileId: String,
        completedCutoffMillis: Long,
        overdueCutoffMillis: Long
    ): List<ReminderEntity>

    @Query(
        """
        UPDATE reminders
        SET isArchived = 1, archivedAtMillis = :archivedAtMillis
        WHERE id IN (:ids)
        """
    )
    fun archive(ids: List<Long>, archivedAtMillis: Long)

    @Query(
        """
        UPDATE reminders
        SET isArchived = 0, archivedAtMillis = NULL
        WHERE id = :id
        """
    )
    fun unarchive(id: Long)

    @Insert
    fun insert(reminder: ReminderEntity): Long

    @Update
    fun update(reminder: ReminderEntity)

    @Delete
    fun delete(reminder: ReminderEntity)
}


