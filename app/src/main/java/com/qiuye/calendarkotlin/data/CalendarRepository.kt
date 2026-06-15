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
import com.qiuye.calendarkotlin.model.ShiftProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.calendarDataStore by preferencesDataStore(name = "calendar_store")

interface CalendarDataStore {
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
        val activeProfileId = stringPreferencesKey("active_profile_id")
        val profiles = stringPreferencesKey("profiles")
        val showLunar = booleanPreferencesKey("show_lunar")

        // Old keys for backward compatibility migration
        val cycleStartDate = stringPreferencesKey("cycle_start_date")
        val cycleEndDate = stringPreferencesKey("cycle_end_date")
        val pattern = stringPreferencesKey("pattern")
        val notes = stringPreferencesKey("notes")
        val overrides = stringPreferencesKey("overrides")
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
                    activeProfileId = preferences[Keys.activeProfileId] ?: "default",
                    profiles = decodeProfiles(preferences),
                    showLunar = preferences[Keys.showLunar] ?: true,
                )
            }

    override suspend fun updateDetail(dateKey: String, note: String, overrideShift: ShiftDefinition?) {
        context.calendarDataStore.edit { preferences ->
            val profiles = decodeProfiles(preferences).toMutableList()
            val activeId = preferences[Keys.activeProfileId] ?: "default"
            val activeIndex = profiles.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
            if (activeIndex < profiles.size) {
                val activeProfile = profiles[activeIndex]
                val notes = activeProfile.notes.toMutableMap()
                if (note.isBlank()) {
                    notes.remove(dateKey)
                } else {
                    notes[dateKey] = note.trim()
                }
                val overrides = activeProfile.overrides.toMutableMap()
                if (overrideShift == null) {
                    overrides.remove(dateKey)
                } else {
                    overrides[dateKey] = overrideShift
                }
                profiles[activeIndex] = activeProfile.copy(notes = notes, overrides = overrides)
            }
            preferences[Keys.profiles] = json.encodeToString(profiles)
        }
    }

    override suspend fun updateSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        context.calendarDataStore.edit { preferences ->
            val profiles = decodeProfiles(preferences).toMutableList()
            val activeId = preferences[Keys.activeProfileId] ?: "default"
            val activeIndex = profiles.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
            if (activeIndex < profiles.size) {
                val activeProfile = profiles[activeIndex]
                profiles[activeIndex] = activeProfile.copy(
                    cycleStartDate = cycleStartDate?.takeIf { it.isNotBlank() },
                    cycleEndDate = cycleEndDate?.takeIf { it.isNotBlank() },
                    pattern = pattern.ifEmpty { CalendarData().pattern }
                )
            }
            preferences[Keys.profiles] = json.encodeToString(profiles)
            preferences[Keys.showLunar] = showLunar
        }
    }

    override suspend fun clearOverrides() {
        context.calendarDataStore.edit { preferences ->
            val profiles = decodeProfiles(preferences).toMutableList()
            val activeId = preferences[Keys.activeProfileId] ?: "default"
            val activeIndex = profiles.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
            if (activeIndex < profiles.size) {
                profiles[activeIndex] = profiles[activeIndex].copy(overrides = emptyMap())
            }
            preferences[Keys.profiles] = json.encodeToString(profiles)
        }
    }

    override suspend fun clearAll() {
        context.calendarDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun replaceAllData(data: CalendarData) {
        context.calendarDataStore.edit { preferences ->
            preferences[Keys.activeProfileId] = data.activeProfileId
            preferences[Keys.profiles] = json.encodeToString(data.profiles)
            preferences[Keys.showLunar] = data.showLunar
        }
    }

    override suspend fun getCurrentData(): CalendarData {
        return context.calendarDataStore.data.map { preferences ->
            CalendarData(
                activeProfileId = preferences[Keys.activeProfileId] ?: "default",
                profiles = decodeProfiles(preferences),
                showLunar = preferences[Keys.showLunar] ?: true,
            )
        }.first()
    }

    private fun decodeProfiles(preferences: Preferences): List<ShiftProfile> {
        val raw = preferences[Keys.profiles]
        if (raw != null) {
            return runCatching { json.decodeFromString<List<ShiftProfile>>(raw) }
                .getOrElse { emptyList() }
                .ifEmpty { listOf(ShiftProfile(id = "default", name = "默认方案")) }
        }

        // Old keys migration:
        val cycleStartDate = preferences[Keys.cycleStartDate]
        val cycleEndDate = preferences[Keys.cycleEndDate]
        val pattern = decodePattern(preferences)
        val notes = decodeNotes(preferences)
        val overrides = decodeOverrides(preferences)

        return listOf(
            ShiftProfile(
                id = "default",
                name = "默认方案",
                cycleStartDate = cycleStartDate,
                cycleEndDate = cycleEndDate,
                pattern = pattern,
                overrides = overrides,
                notes = notes
            )
        )
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


