package com.qiuye.calendarkotlin.diary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY dateKey DESC")
    fun observeAll(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey LIMIT 1")
    fun observeByDate(dateKey: String): Flow<DiaryEntity?>

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey LIMIT 1")
    fun getByDate(dateKey: String): DiaryEntity?

    @Query("SELECT dateKey FROM diary_entries")
    fun observeAllDateKeys(): Flow<List<String>>

    @Query("""
        SELECT * FROM diary_entries
        WHERE content LIKE '%' || :query || '%'
           OR mood LIKE '%' || :query || '%'
           OR REPLACE(dateKey, '-', '') LIKE '%' || REPLACE(:query, '-', '') || '%'
        ORDER BY dateKey DESC
    """)
    fun search(query: String): Flow<List<DiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: DiaryEntity): Long

    @Delete
    fun delete(entry: DiaryEntity)

    @Query("DELETE FROM diary_entries WHERE dateKey = :dateKey")
    fun deleteByDate(dateKey: String)
}
