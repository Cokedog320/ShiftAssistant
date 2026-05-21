package com.qiuye.calendarkotlin.diary.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qiuye.calendarkotlin.diary.data.DiaryEntity
import com.qiuye.calendarkotlin.diary.data.DiaryRepository
import com.qiuye.calendarkotlin.tasks.TasksGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TasksGraph.diaryRepository(application)

    /** 全部日记，按日期降序 */
    val allEntries: StateFlow<List<DiaryEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 所有有日记的日期 key 集合，用于日历格子指示器 */
    val diaryDateKeys: StateFlow<Set<String>> = repository.observeAllDateKeys()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** 搜索 */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<DiaryEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    /** 加载指定日期的日记 */
    fun observeByDate(dateKey: String): Flow<DiaryEntity?> = repository.observeByDate(dateKey)

    suspend fun getByDate(dateKey: String): DiaryEntity? = repository.getByDate(dateKey)

    /** 保存日记（新建或更新） */
    fun saveDiary(dateKey: String, content: String, mood: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getByDate(dateKey)
            val entry = DiaryEntity(
                id = existing?.id ?: 0,
                dateKey = dateKey,
                content = content.trim(),
                mood = mood,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )
            repository.save(entry)
        }
    }

    /** 删除指定日期的日记 */
    fun deleteDiary(dateKey: String) {
        viewModelScope.launch { repository.deleteByDate(dateKey) }
    }
}
