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

class ShiftConfigRepositoryTest : BaseUnitTest() {

    private lateinit var repository: CalendarRepository

    @Before
    fun setUp() = runBlocking {
        repository = CalendarRepository(ApplicationProvider.getApplicationContext())
        repository.clearAll()
    }

    @Test
    fun `updateSettings should save cycle dates and pattern`() = runBlocking {
        val pattern = listOf(ShiftDefinition("1", "Day", ShiftColorOption.BLUE))
        repository.updateSettings("2024-01-01", "2024-12-31", pattern, false)
        
        val data = repository.calendarData.first()
        assertEquals("2024-01-01", data.cycleStartDate)
        assertEquals("2024-12-31", data.cycleEndDate)
        assertEquals(false, data.showLunar)
        assertEquals(1, data.pattern.size)
        assertEquals("Day", data.pattern[0].name)
    }

    @Test
    fun `updateSettings with empty dates should remove them`() = runBlocking {
        val pattern = listOf(ShiftDefinition("1", "Day", ShiftColorOption.BLUE))
        repository.updateSettings("2024-01-01", "2024-12-31", pattern, true)
        
        repository.updateSettings("", null, pattern, true)
        
        val data = repository.calendarData.first()
        assertNull(data.cycleStartDate)
        assertNull(data.cycleEndDate)
    }

    @Test
    fun `updateSettings with empty pattern should use default`() = runBlocking {
        repository.updateSettings("2024-01-01", "2024-12-31", emptyList(), true)
        
        val data = repository.calendarData.first()
        assertTrue(data.pattern.isNotEmpty())
    }
}
