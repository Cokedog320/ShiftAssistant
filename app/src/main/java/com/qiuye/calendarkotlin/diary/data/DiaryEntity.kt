package com.qiuye.calendarkotlin.diary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["dateKey"], unique = true)]
)
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String,           // "2026-05-21" 格式，和备注的 dateKey 一致
    val content: String,           // 日记正文
    val mood: String = "",         // emoji 心情标签，如 "😊"，空表示未选
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
