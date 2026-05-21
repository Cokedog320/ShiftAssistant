package com.qiuye.calendarkotlin.diary.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DiaryRepository(private val diaryDao: DiaryDao) {

    fun observeAll(): Flow<List<DiaryEntity>> = diaryDao.observeAll()

    fun observeByDate(dateKey: String): Flow<DiaryEntity?> = diaryDao.observeByDate(dateKey)

    fun observeAllDateKeys(): Flow<List<String>> = diaryDao.observeAllDateKeys()

    fun search(query: String): Flow<List<DiaryEntity>> = diaryDao.search(query)

    suspend fun getByDate(dateKey: String): DiaryEntity? = withContext(Dispatchers.IO) {
        diaryDao.getByDate(dateKey)
    }

    suspend fun save(entry: DiaryEntity): Long = withContext(Dispatchers.IO) {
        diaryDao.upsert(entry)
    }

    suspend fun delete(entry: DiaryEntity) = withContext(Dispatchers.IO) {
        diaryDao.delete(entry)
    }

    suspend fun deleteByDate(dateKey: String) = withContext(Dispatchers.IO) {
        diaryDao.deleteByDate(dateKey)
    }
}
