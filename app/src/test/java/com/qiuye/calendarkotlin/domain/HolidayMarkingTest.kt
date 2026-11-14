package com.qiuye.calendarkotlin.domain

import android.content.Context
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.utils.MockHelpers
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDate
import java.util.Locale

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
          },
          {
            "year": 2027,
            "holidays": [
              {
                "name": "春节",
                "start": "2027-02-05",
                "end": "2027-02-13"
              },
              {
                "name": "端午节",
                "start": "2027-06-09",
                "end": "2027-06-09"
              }
            ],
            "workdays": [
              {
                "date": "2027-01-31",
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

    @Test
    fun `getHolidayMarker should support multi-year 2027 holiday and workday`() {
        val springFestival2027 = ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 2, 5))
        assertNotNull(springFestival2027)
        assertEquals("休", springFestival2027?.label)
        assertEquals("春节", springFestival2027?.name)
        assertEquals(false, springFestival2027?.isWorkday)

        val dragonBoat2027 = ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 6, 9))
        assertNotNull(dragonBoat2027)
        assertEquals("休", dragonBoat2027?.label)
        assertEquals("端午节", dragonBoat2027?.name)
        assertEquals(false, dragonBoat2027?.isWorkday)

        val makeupWorkday2027 = ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 1, 31))
        assertNotNull(makeupWorkday2027)
        assertEquals("班", makeupWorkday2027?.label)
        assertEquals("春节调休", makeupWorkday2027?.name)
        assertEquals(true, makeupWorkday2027?.isWorkday)
    }

    @Test
    fun `real holidays json asset should parse and mark all 2027 holidays and workdays`() {
        val previousLocale = Locale.getDefault()
        val previousAppLocales = AppCompatDelegate.getApplicationLocales()
        try {
            Locale.setDefault(Locale.CHINA)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            ChineseCalendarInfo.resetHolidaysReadyForTesting()

            val assetBytes = loadRealAssetFile("holidays.json").readBytes()
            every { mockAssetManager.open("holidays.json") } returns ByteArrayInputStream(assetBytes)

            ChineseCalendarInfo.init(mockContext)
            assertTrue(ChineseCalendarInfo.holidaysReady.value)

            val expectedHolidayDates = listOf(
                LocalDate.of(2027, 1, 1) to "元旦",
                LocalDate.of(2027, 1, 3) to "元旦",
                LocalDate.of(2027, 2, 5) to "春节",
                LocalDate.of(2027, 2, 9) to "春节",
                LocalDate.of(2027, 2, 13) to "春节",
                LocalDate.of(2027, 4, 3) to "清明节",
                LocalDate.of(2027, 4, 5) to "清明节",
                LocalDate.of(2027, 5, 1) to "劳动节",
                LocalDate.of(2027, 5, 5) to "劳动节",
                LocalDate.of(2027, 6, 9) to "端午节",
                LocalDate.of(2027, 9, 15) to "中秋节",
                LocalDate.of(2027, 10, 1) to "国庆节",
                LocalDate.of(2027, 10, 7) to "国庆节",
            )
            for ((date, expectedName) in expectedHolidayDates) {
                val marker = ChineseCalendarInfo.getHolidayMarker(date)
                assertNotNull("Expected holiday on $date", marker)
                assertEquals("休", marker?.label)
                assertEquals(expectedName, marker?.name)
                assertEquals(false, marker?.isWorkday)
            }

            val expectedWorkdays = listOf(
                LocalDate.of(2027, 1, 31) to "春节调休",
                LocalDate.of(2027, 2, 14) to "春节调休",
                LocalDate.of(2027, 5, 8) to "劳动节调休",
                LocalDate.of(2027, 9, 26) to "国庆节调休",
                LocalDate.of(2027, 10, 9) to "国庆节调休",
            )
            for ((date, expectedName) in expectedWorkdays) {
                val marker = ChineseCalendarInfo.getHolidayMarker(date)
                assertNotNull("Expected workday on $date", marker)
                assertEquals("班", marker?.label)
                assertEquals(expectedName, marker?.name)
                assertEquals(true, marker?.isWorkday)
            }

            assertNull(ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 1, 4)))
            assertNull(ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 2, 4)))
            assertNull(ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 6, 10)))
        } finally {
            AppCompatDelegate.setApplicationLocales(previousAppLocales)
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun `real holidays_en json asset should parse and mark all 2027 holidays`() {
        val previousLocale = Locale.getDefault()
        val previousAppLocales = AppCompatDelegate.getApplicationLocales()
        try {
            Locale.setDefault(Locale.US)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            ChineseCalendarInfo.resetHolidaysReadyForTesting()

            val assetBytes = loadRealAssetFile("holidays_en.json").readBytes()
            every { mockAssetManager.open("holidays_en.json") } returns ByteArrayInputStream(assetBytes)

            ChineseCalendarInfo.init(mockContext)
            assertTrue(ChineseCalendarInfo.holidaysReady.value)

            val expectedEnglishHolidays = listOf(
                LocalDate.of(2027, 1, 1) to "New Year's Day",
                LocalDate.of(2027, 2, 14) to "Valentine's Day",
                LocalDate.of(2027, 5, 9) to "Mother's Day",
                LocalDate.of(2027, 6, 20) to "Father's Day",
                LocalDate.of(2027, 10, 31) to "Halloween",
                LocalDate.of(2027, 11, 25) to "Thanksgiving",
                LocalDate.of(2027, 12, 24) to "Christmas Eve",
                LocalDate.of(2027, 12, 25) to "Christmas Day",
            )
            for ((date, expectedName) in expectedEnglishHolidays) {
                val marker = ChineseCalendarInfo.getHolidayMarker(date)
                assertNotNull("Expected English holiday on $date", marker)
                assertEquals("Off", marker?.label)
                assertEquals(expectedName, marker?.name)
                assertEquals(false, marker?.isWorkday)
            }

            assertNull(ChineseCalendarInfo.getHolidayMarker(LocalDate.of(2027, 7, 4)))
        } finally {
            AppCompatDelegate.setApplicationLocales(previousAppLocales)
            Locale.setDefault(previousLocale)
        }
    }

    private fun loadRealAssetFile(fileName: String): File {
        val candidates = listOf(
            File("src/main/assets/$fileName"),
            File("app/src/main/assets/$fileName"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate asset file $fileName in ${File(".").absolutePath}")
    }
}
