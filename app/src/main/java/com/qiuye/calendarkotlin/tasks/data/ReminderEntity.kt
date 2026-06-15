package com.qiuye.calendarkotlin.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val note: String,
    val scheduledAtMillis: Long,
    val isCompleted: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val profileId: String = "default"
)


