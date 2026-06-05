package com.qiuye.calendarkotlin.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftDefinition
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.calendarDataStore by preferencesDataStore(name = "calendar_store")

internal interface CalendarDataStore {
    val calendarData: Flow<CalendarData>
    suspend fun getCurrentData(): CalendarData

    suspend fun updateDetail(dateKey: String, note: String, overrideShift: ShiftDefinition?)

    suspend fun updateSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    )

    suspend fun clearOverrides()

    suspend fun clearAll()

    suspend fun replaceAllData(data: CalendarData)
}

class CalendarRepository(private val context: Context) : CalendarDataStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val cycleStartDate = stringPreferencesKey("cycle_start_date")
        val cycleEndDate = stringPreferencesKey("cycle_end_date")
        val pattern = stringPreferencesKey("pattern")
        val notes = stringPreferencesKey("notes")
        val overrides = stringPreferencesKey("overrides")
        val showLunar = booleanPreferencesKey("show_lunar")
    }

    override val calendarData: Flow<CalendarData> =
        context.calendarDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                CalendarData(
                    cycleStartDate = preferences[Keys.cycleStartDate],
                    cycleEndDate = preferences[Keys.cycleEndDate],
                    pattern = decodePattern(preferences),
                    notes = decodeNotes(preferences),
                    overrides = decodeOverrides(preferences),
                    showLunar = preferences[Keys.showLunar] ?: true,
                )
            }

    override suspend fun updateDetail(dateKey: String, note: String, overrideShift: ShiftDefinition?) {
        context.calendarDataStore.edit { preferences ->
            val notes = decodeNotes(preferences).toMutableMap()
            if (note.isBlank()) {
                notes.remove(dateKey)
            } else {
                notes[dateKey] = note.trim()
            }
            if (notes.isEmpty()) {
                preferences.remove(Keys.notes)
            } else {
                preferences[Keys.notes] = json.encodeToString(notes)
            }

            val overrides = decodeOverrides(preferences).toMutableMap()
            if (overrideShift == null) {
                overrides.remove(dateKey)
            } else {
                overrides[dateKey] = overrideShift
            }
            if (overrides.isEmpty()) {
                preferences.remove(Keys.overrides)
            } else {
                preferences[Keys.overrides] = json.encodeToString(overrides)
            }
        }
    }

    override suspend fun updateSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        context.calendarDataStore.edit { preferences ->
            if (cycleStartDate.isNullOrBlank()) {
                preferences.remove(Keys.cycleStartDate)
            } else {
                preferences[Keys.cycleStartDate] = cycleStartDate
            }

            if (cycleEndDate.isNullOrBlank()) {
                preferences.remove(Keys.cycleEndDate)
            } else {
                preferences[Keys.cycleEndDate] = cycleEndDate
            }

            preferences[Keys.pattern] = json.encodeToString(pattern.ifEmpty { CalendarData().pattern })
            preferences[Keys.showLunar] = showLunar
        }
    }

    override suspend fun clearOverrides() {
        context.calendarDataStore.edit { preferences ->
            preferences.remove(Keys.overrides)
        }
    }

    override suspend fun clearAll() {
        context.calendarDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun replaceAllData(data: CalendarData) {
        context.calendarDataStore.edit { preferences ->
            if (data.cycleStartDate != null) {
                preferences[Keys.cycleStartDate] = data.cycleStartDate
            } else {
                preferences.remove(Keys.cycleStartDate)
            }
            if (data.cycleEndDate != null) {
                preferences[Keys.cycleEndDate] = data.cycleEndDate
            } else {
                preferences.remove(Keys.cycleEndDate)
            }
            preferences[Keys.pattern] = json.encodeToString(data.pattern)
            preferences[Keys.notes] = json.encodeToString(data.notes)
            preferences[Keys.overrides] = json.encodeToString(data.overrides)
            preferences[Keys.showLunar] = data.showLunar
        }
    }

    override suspend fun getCurrentData(): CalendarData {
        return context.calendarDataStore.data.map { preferences ->
            CalendarData(
                cycleStartDate = preferences[Keys.cycleStartDate],
                cycleEndDate = preferences[Keys.cycleEndDate],
                pattern = decodePattern(preferences),
                notes = decodeNotes(preferences),
                overrides = decodeOverrides(preferences),
                showLunar = preferences[Keys.showLunar] ?: true,
            )
        }.first()
    }

    private fun decodePattern(preferences: Preferences): List<ShiftDefinition> {
        val raw = preferences[Keys.pattern] ?: return CalendarData().pattern
        return runCatching { json.decodeFromString<List<ShiftDefinition>>(raw) }
            .getOrElse { CalendarData().pattern }
            .ifEmpty { CalendarData().pattern }
    }

    private fun decodeNotes(preferences: Preferences): Map<String, String> {
        val raw = preferences[Keys.notes] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrElse { emptyMap() }
    }

    private fun decodeOverrides(preferences: Preferences): Map<String, ShiftDefinition> {
        val raw = preferences[Keys.overrides] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, ShiftDefinition>>(raw) }
            .getOrElse { emptyMap() }
    }
}


