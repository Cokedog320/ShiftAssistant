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
import com.qiuye.calendarkotlin.model.businessTripShift
import com.qiuye.calendarkotlin.model.vacationShift
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.qiuye.calendarkotlin.model.defaultPattern

val Context.calendarDataStore by preferencesDataStore(name = "calendar_store")

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

class CalendarRepository(
    private val defaultProfileName: String,
    private val dataStore: androidx.datastore.core.DataStore<Preferences>
) : CalendarDataStore {
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
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val (profiles, notes) = decodeProfilesAndNotes(preferences)
                CalendarData(
                    activeProfileId = preferences[Keys.activeProfileId] ?: "default",
                    profiles = profiles,
                    showLunar = preferences[Keys.showLunar] ?: true,
                    notes = notes
                )
            }

    override suspend fun updateDetail(dateKey: String, note: String, overrideShift: ShiftDefinition?) {
        dataStore.edit { preferences ->
            val (profiles, globalNotesMutable) = decodeProfilesAndNotes(preferences)
            val profilesList = profiles.toMutableList()
            val activeId = preferences[Keys.activeProfileId] ?: "default"
            val activeIndex = profilesList.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
            if (activeIndex < profilesList.size) {
                val activeProfile = profilesList[activeIndex]
                val overrides = activeProfile.overrides.toMutableMap()
                if (overrideShift == null) {
                    overrides.remove(dateKey)
                } else {
                    overrides[dateKey] = overrideShift
                }
                profilesList[activeIndex] = activeProfile.copy(overrides = overrides)
            }
            
            val globalNotes = globalNotesMutable.toMutableMap()
            preferences[Keys.profiles] = json.encodeToString(profilesList)
            if (note.isBlank()) {
                globalNotes.remove(dateKey)
            } else {
                globalNotes[dateKey] = note.trim()
            }
            preferences[Keys.notes] = json.encodeToString(globalNotes)
        }
    }

    override suspend fun updateSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        dataStore.edit { preferences ->
            val (profiles, _) = decodeProfilesAndNotes(preferences)
            val profilesList = profiles.toMutableList()
            val activeId = preferences[Keys.activeProfileId] ?: "default"
            val activeIndex = profilesList.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
            if (activeIndex < profilesList.size) {
                val activeProfile = profilesList[activeIndex]
                profilesList[activeIndex] = activeProfile.copy(
                    cycleStartDate = cycleStartDate?.takeIf { it.isNotBlank() },
                    cycleEndDate = cycleEndDate?.takeIf { it.isNotBlank() },
                    pattern = pattern.ifEmpty { CalendarData().pattern }
                )
            }
            preferences[Keys.profiles] = json.encodeToString(profilesList)
            preferences[Keys.showLunar] = showLunar
        }
    }

    override suspend fun clearOverrides() {
        dataStore.edit { preferences ->
            val (profiles, _) = decodeProfilesAndNotes(preferences)
            val profilesList = profiles.toMutableList()
            val activeId = preferences[Keys.activeProfileId] ?: "default"
            val activeIndex = profilesList.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
            if (activeIndex < profilesList.size) {
                profilesList[activeIndex] = profilesList[activeIndex].copy(overrides = emptyMap())
            }
            preferences[Keys.profiles] = json.encodeToString(profilesList)
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun replaceAllData(data: CalendarData) {
        dataStore.edit { preferences ->
            preferences[Keys.activeProfileId] = data.activeProfileId
            preferences[Keys.profiles] = json.encodeToString(data.profiles)
            preferences[Keys.showLunar] = data.showLunar
            preferences[Keys.notes] = json.encodeToString(data.notes)
        }
    }

    override suspend fun getCurrentData(): CalendarData {
        return dataStore.data.map { preferences ->
            val (profiles, notes) = decodeProfilesAndNotes(preferences)
            CalendarData(
                activeProfileId = preferences[Keys.activeProfileId] ?: "default",
                profiles = profiles,
                showLunar = preferences[Keys.showLunar] ?: true,
                notes = notes
            )
        }.first()
    }

    private fun decodeProfilesAndNotes(preferences: Preferences): Pair<List<ShiftProfile>, Map<String, String>> {
        val rawProfiles = preferences[Keys.profiles]
        val rawNotes = preferences[Keys.notes]
        
        val globalNotes = if (rawNotes != null) {
            runCatching { json.decodeFromString<Map<String, String>>(rawNotes) }
                .getOrElse { emptyMap() }
        } else {
            emptyMap()
        }
        val mergedNotes = globalNotes.toMutableMap()
        val profilesList = mutableListOf<ShiftProfile>()

        if (rawProfiles != null) {
            val legacyProfiles = runCatching { json.decodeFromString<List<LegacyShiftProfile>>(rawProfiles) }
                .getOrElse { emptyList() }
            
            for (legacy in legacyProfiles) {
                for ((date, text) in legacy.notes) {
                    if (text.isNotBlank()) {
                        val existing = mergedNotes[date]
                        if (existing == null) {
                            mergedNotes[date] = text
                        } else if (!existing.contains(text)) {
                            mergedNotes[date] = "$existing\n$text"
                        }
                    }
                }
                profilesList.add(
                    ShiftProfile(
                        id = legacy.id,
                        name = legacy.name,
                        cycleStartDate = legacy.cycleStartDate,
                        cycleEndDate = legacy.cycleEndDate,
                        pattern = legacy.pattern.normalizeIdsByNameAndColor(),
                        overrides = legacy.overrides.normalizeBuiltinOverrideColors()
                    )
                )
            }
        } else {
            // Old keys migration:
            val cycleStartDate = preferences[Keys.cycleStartDate]
            val cycleEndDate = preferences[Keys.cycleEndDate]
            val pattern = decodePattern(preferences)
            val oldNotes = decodeNotes(preferences)
            val overrides = decodeOverrides(preferences)

            for ((date, text) in oldNotes) {
                if (text.isNotBlank()) {
                    val existing = mergedNotes[date]
                    if (existing == null) {
                        mergedNotes[date] = text
                    } else if (!existing.contains(text)) {
                        mergedNotes[date] = "$existing\n$text"
                    }
                }
            }

            profilesList.add(
                ShiftProfile(
                    id = "default",
                    name = defaultProfileName,
                    cycleStartDate = cycleStartDate,
                    cycleEndDate = cycleEndDate,
                    pattern = pattern,
                    overrides = overrides.normalizeBuiltinOverrideColors()
                )
            )
        }

        if (profilesList.isEmpty()) {
            profilesList.add(ShiftProfile(id = "default", name = defaultProfileName))
        }

        return Pair(profilesList, mergedNotes)
    }

    private fun decodePattern(preferences: Preferences): List<ShiftDefinition> {
        val raw = preferences[Keys.pattern] ?: return CalendarData().pattern
        return runCatching { json.decodeFromString<List<ShiftDefinition>>(raw) }
            .getOrElse { CalendarData().pattern }
            .ifEmpty { CalendarData().pattern }
            .normalizeIdsByNameAndColor()
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

    private fun List<ShiftDefinition>.normalizeIdsByNameAndColor(): List<ShiftDefinition> {
        val firstIdBySignature = mutableMapOf<String, String>()
        return map { shift ->
            val signature = "${shift.name}\u0000${shift.color.name}"
            val normalizedId = firstIdBySignature.getOrPut(signature) { shift.id }
            if (normalizedId == shift.id) shift else shift.copy(id = normalizedId)
        }
    }

    private fun Map<String, ShiftDefinition>.normalizeBuiltinOverrideColors(): Map<String, ShiftDefinition> =
        mapValues { (_, shift) ->
            when (shift.id) {
                vacationShift.id -> shift.copy(color = vacationShift.color)
                businessTripShift.id -> shift.copy(color = businessTripShift.color)
                else -> shift
            }
        }
}

@Serializable
private data class LegacyShiftProfile(
    val id: String,
    val name: String,
    val cycleStartDate: String? = null,
    val cycleEndDate: String? = null,
    val pattern: List<ShiftDefinition> = defaultPattern,
    val overrides: Map<String, ShiftDefinition> = emptyMap(),
    val notes: Map<String, String> = emptyMap(),
)
