package com.qiuye.calendarkotlin.domain

import android.content.Context
import android.content.res.AssetManager
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.utils.MockHelpers
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class HolidayMarkingTest : BaseUnitTest() {

    @MockK
    lateinit var mockContext: Context
    
    @MockK
    lateinit var mockAssetManager: AssetManager

    private val dummyHolidayJson = """
        [
          {
            "year": 2024,
            "holidays": [
              {
                "name": "春节",
                "start": "2024-02-10",
                "end": "2024-02-17"
              }
            ],
            "workdays": [
              {
                "date": "2024-02-18",
                "name": "春节调休"
              }
            ]
          }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        MockHelpers.initMocks(this)
        every { mockContext.assets } returns mockAssetManager
        every { mockAssetManager.open("holidays.json") } returns ByteArrayInputStream(dummyHolidayJson.toByteArray())
        
        ChineseCalendarInfo.init(mockContext)
    }

    @Test
    fun `getHolidayMarker should return statutory holiday`() {
        val holidayDate = LocalDate.of(2024, 2, 12) // Inside Spring Festival (2024-02-10 to 2024-02-17)
        val marker = ChineseCalendarInfo.getHolidayMarker(holidayDate)
        
        assertNotNull(marker)
        assertEquals("休", marker?.label)
        assertEquals("春节", marker?.name)
        assertEquals(false, marker?.isWorkday)
    }

    @Test
    fun `getHolidayMarker should return makeup workday`() {
        val workday = LocalDate.of(2024, 2, 18) // Makeup workday
        val marker = ChineseCalendarInfo.getHolidayMarker(workday)
        
        assertNotNull(marker)
        assertEquals("班", marker?.label)
        assertEquals("春节调休", marker?.name)
        assertEquals(true, marker?.isWorkday)
    }

    @Test
    fun `getHolidayMarker should return null for normal day`() {
        val normalDay = LocalDate.of(2024, 3, 1)
        val marker = ChineseCalendarInfo.getHolidayMarker(normalDay)
        
        assertNull("Normal day should not have a holiday marker", marker)
    }
}
