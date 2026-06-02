package com.qiuye.calendarkotlin.data

import androidx.test.core.app.ApplicationProvider
import com.qiuye.calendarkotlin.BaseUnitTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NoteRepositoryTest : BaseUnitTest() {

    private lateinit var repository: CalendarRepository

    @Before
    fun setUp() = runBlocking {
        repository = CalendarRepository(ApplicationProvider.getApplicationContext())
        repository.clearAll() // Reset for each test
    }

    @Test
    fun `updateDetail should save and retrieve note correctly`() = runBlocking {
        repository.updateDetail("2024-01-01", "New Year Note", null)
        val data = repository.calendarData.first()
        assertEquals("New Year Note", data.notes["2024-01-01"])
    }

    @Test
    fun `updateDetail with empty note should remove the note`() = runBlocking {
        repository.updateDetail("2024-01-01", "Some Note", null)
        var data = repository.calendarData.first()
        assertEquals("Some Note", data.notes["2024-01-01"])
        
        // Remove
        repository.updateDetail("2024-01-01", "", null)
        data = repository.calendarData.first()
        assertNull(data.notes["2024-01-01"])
    }

    @Test
    fun `updateDetail should handle multiple dates`() = runBlocking {
        repository.updateDetail("2024-01-01", "Note 1", null)
        repository.updateDetail("2024-01-02", "Note 2", null)
        
        val data = repository.calendarData.first()
        assertEquals(2, data.notes.size)
        assertEquals("Note 1", data.notes["2024-01-01"])
        assertEquals("Note 2", data.notes["2024-01-02"])
    }
}
