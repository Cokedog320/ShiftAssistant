package com.qiuye.calendarkotlin.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.model.ShiftProfile
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CalendarRepositoryTest : BaseUnitTest() {

    private lateinit var testDataStore: androidx.datastore.core.DataStore<Preferences>
    private lateinit var repository: CalendarRepository
    private val defaultProfileName = "默认方案"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "test_repository_store.preferences_pb") }
        )
        runBlocking {
            testDataStore.edit { it.clear() }
        }
        repository = CalendarRepository(defaultProfileName = defaultProfileName, dataStore = testDataStore)
    }

    @Test
    fun testDecodeProfilesAndNotesNormal() = runBlocking {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val profilesKey = stringPreferencesKey("profiles")
        val notesKey = stringPreferencesKey("notes")

        val expectedProfiles = listOf(
            ShiftProfile(
                id = "p1",
                name = "Profile 1",
                cycleStartDate = "2026-06-01",
                pattern = listOf(ShiftDefinition("1", "Day", ShiftColorOption.BLUE)),
                overrides = mapOf("2026-06-05" to ShiftDefinition("3", "Rest", ShiftColorOption.GREEN))
            )
        )
        val expectedNotes = mapOf("2026-06-02" to "My special note")

        testDataStore.edit { prefs ->
            prefs[profilesKey] = json.encodeToString(expectedProfiles)
            prefs[notesKey] = json.encodeToString(expectedNotes)
        }

        val data = repository.getCurrentData()
        assertEquals(expectedProfiles, data.profiles)
        assertEquals(expectedNotes, data.notes)
    }

    @Test
    fun testDecodePatternNormalizesDuplicateShiftIds() = runBlocking {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val profilesKey = stringPreferencesKey("profiles")

        val legacyProfiles = listOf(
            ShiftProfile(
                id = "p1",
                name = "Profile 1",
                pattern = listOf(
                    ShiftDefinition("3", "Rest", ShiftColorOption.GREEN),
                    ShiftDefinition("4", "Rest", ShiftColorOption.GREEN),
                    ShiftDefinition("1", "Day", ShiftColorOption.BLUE),
                ),
            )
        )

        testDataStore.edit { prefs ->
            prefs[profilesKey] = json.encodeToString(legacyProfiles)
        }

        val data = repository.getCurrentData()
        assertEquals(listOf("3", "3", "1"), data.activeProfile.pattern.map { it.id })
    }

    @Test
    fun testDecodeProfilesAndNotesFallbackWhenProfilesEmpty() = runBlocking {
        val data = repository.getCurrentData()
        assertEquals(1, data.profiles.size)
        val defaultProfile = data.profiles.first()
        assertEquals("default", defaultProfile.id)
        assertEquals(defaultProfileName, defaultProfile.name)
    }

    @Test
    fun testUpdateDetailWritesNotesAndOverrides() = runBlocking {
        val dateKey = "2026-06-15"
        val note = "Meeting day"
        val overrideShift = ShiftDefinition("override_id", "Override Shift", ShiftColorOption.RED)

        repository.updateDetail(dateKey, note, overrideShift)

        val data = repository.getCurrentData()
        assertEquals(note, data.notes[dateKey])
        assertEquals(overrideShift, data.activeProfile.overrides[dateKey])
    }

    @Test
    fun testReplaceAllDataRoundtrip() = runBlocking {
        val customData = CalendarData(
            activeProfileId = "p2",
            profiles = listOf(
                ShiftProfile(
                    id = "p2",
                    name = "Profile 2",
                    cycleStartDate = "2026-07-01",
                    pattern = listOf(ShiftDefinition("2", "Night", ShiftColorOption.INDIGO)),
                    overrides = mapOf("2026-07-05" to ShiftDefinition("3", "Rest", ShiftColorOption.GREEN))
                )
            ),
            showLunar = false,
            notes = mapOf("2026-07-02" to "Another note")
        )

        repository.replaceAllData(customData)

        val retrieved = repository.getCurrentData()
        assertEquals(customData.activeProfileId, retrieved.activeProfileId)
        assertEquals(customData.profiles, retrieved.profiles)
        assertEquals(customData.showLunar, retrieved.showLunar)
        assertEquals(customData.notes, retrieved.notes)
    }

    @Test
    fun testUpdateSettingsModifiesActiveProfile() = runBlocking {
        val newPattern = listOf(ShiftDefinition("new_id", "New Shift", ShiftColorOption.ORANGE))
        
        repository.updateSettings(
            cycleStartDate = "2026-08-01",
            cycleEndDate = "2026-08-31",
            pattern = newPattern,
            showLunar = false
        )

        val data = repository.getCurrentData()
        assertFalse(data.showLunar)
        val activeProfile = data.activeProfile
        assertEquals("2026-08-01", activeProfile.cycleStartDate)
        assertEquals("2026-08-31", activeProfile.cycleEndDate)
        assertEquals(newPattern, activeProfile.pattern)
    }
}
