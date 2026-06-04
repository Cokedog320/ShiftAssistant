package com.qiuye.calendarkotlin.diary.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qiuye.calendarkotlin.diary.data.DiaryEntity
import com.qiuye.calendarkotlin.diary.data.DiaryRepository
import com.qiuye.calendarkotlin.tasks.TasksGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DiaryViewModel internal constructor(
    private val repository: DiaryRepository,
) : ViewModel() {

    val allEntries: StateFlow<List<DiaryEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val diaryDateKeys: StateFlow<Set<String>> = repository.observeAllDateKeys()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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

    fun observeByDate(dateKey: String): Flow<DiaryEntity?> = repository.observeByDate(dateKey)

    suspend fun getByDate(dateKey: String): DiaryEntity? = repository.getByDate(dateKey)

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

    fun deleteDiary(dateKey: String) {
        viewModelScope.launch { repository.deleteByDate(dateKey) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DiaryViewModel(
                    repository = TasksGraph.diaryRepository(context.applicationContext),
                )
            }
        }
    }
}