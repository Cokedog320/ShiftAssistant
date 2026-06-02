package com.qiuye.calendarkotlin.data

import androidx.test.core.app.ApplicationProvider
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataStoreTest : BaseUnitTest() {

    private lateinit var repository: CalendarRepository

    @Before
    fun setUp() = runBlocking {
        repository = CalendarRepository(ApplicationProvider.getApplicationContext())
        repository.clearAll()
    }

    @Test
    fun `calendarData should emit default values initially`() = runBlocking {
        val data = repository.calendarData.first()
        assertTrue(data.showLunar)
        assertNull(data.cycleStartDate)
        assertNull(data.cycleEndDate)
        assertTrue(data.pattern.isNotEmpty()) // Fallbacks to default pattern
        assertTrue(data.notes.isEmpty())
        assertTrue(data.overrides.isEmpty())
    }

    @Test
    fun `clearOverrides should only remove overrides`() = runBlocking {
        repository.updateDetail("2024-01-01", "Note 1", ShiftDefinition("99", "Override", ShiftColorOption.RED))
        
        var data = repository.calendarData.first()
        assertEquals("Note 1", data.notes["2024-01-01"])
        assertNotNull(data.overrides["2024-01-01"])
        
        repository.clearOverrides()
        
        data = repository.calendarData.first()
        assertEquals("Note 1", data.notes["2024-01-01"]) // Note is kept
        assertNull(data.overrides["2024-01-01"]) // Override is cleared
    }

    @Test
    fun `clearAll should reset all to default`() = runBlocking {
        val pattern = listOf(ShiftDefinition("1", "Day", ShiftColorOption.BLUE))
        repository.updateSettings("2024-01-01", "2024-12-31", pattern, false)
        repository.updateDetail("2024-01-01", "Note 1", null)

        repository.clearAll()
        
        val data = repository.calendarData.first()
        assertTrue(data.showLunar) // default
        assertNull(data.cycleStartDate)
        assertNull(data.notes["2024-01-01"])
    }
}
