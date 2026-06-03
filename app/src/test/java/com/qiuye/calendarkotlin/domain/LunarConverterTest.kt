package com.qiuye.calendarkotlin.domain

import com.qiuye.calendarkotlin.BaseUnitTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LunarConverterTest : BaseUnitTest() {

    @Test
    fun `getLunarDisplay should convert Gregorian to Lunar date correctly`() {
        // 2024-02-10 is Spring Festival (Chinese New Year)
        val date = LocalDate.of(2024, 2, 10)
        val display = ChineseCalendarInfo.getLunarDisplay(date)
        
        // Label should show festival and full text should contain correct year/month details
        assertTrue(display.label.contains("春节") || display.fullText.contains("春节"))
        assertTrue(display.fullText.contains("正月"))
    }
    
    @Test
    fun `getLunarDisplay should show JieQi (Solar Term)`() {
        // 2024-04-04 is Qingming
        val date = LocalDate.of(2024, 4, 4)
        val display = ChineseCalendarInfo.getLunarDisplay(date)
        
        assertTrue("Expected 清明 in label or full text", display.label.contains("清明") || display.fullText.contains("清明"))
    }

    @Test
    fun `getLunarDisplay should return basic month and day if no festival or JieQi`() {
        val date = LocalDate.of(2024, 3, 12) // Normal day
        val display = ChineseCalendarInfo.getLunarDisplay(date)
        
        assertNotNull(display.label)
        assertTrue(display.fullText.contains("年") && display.fullText.contains("月"))
    }

    @Test
    fun `getCleanLunarLabel should format lunar label correctly for various scenarios`() {
        // 1. Regular day (no festival, e.g. 2024-03-13 is Feb 4th in Lunar)
        val dateNormal = LocalDate.of(2024, 3, 13)
        val labelNormal = ChineseCalendarInfo.getCleanLunarLabel(dateNormal)
        assertEquals("二月初四", labelNormal)

        // 2. Solar Term (Qingming on 2024-04-04)
        val dateJieQi = LocalDate.of(2024, 4, 4) // Feb 26th in Lunar
        val labelJieQi = ChineseCalendarInfo.getCleanLunarLabel(dateJieQi)
        assertTrue("Label should contain lunar date and Qingming term: $labelJieQi", labelJieQi.contains("二月廿六") && labelJieQi.contains("清明"))

        // 3. Solar holiday (3.15 Consumer Rights Day)
        val dateSolarHoliday = LocalDate.of(2024, 3, 15) // Feb 6th in Lunar
        val labelSolarHoliday = ChineseCalendarInfo.getCleanLunarLabel(dateSolarHoliday)
        assertTrue("Label should contain lunar date and Consumer Rights Day: $labelSolarHoliday", labelSolarHoliday.contains("二月初六") && labelSolarHoliday.contains("消费者权益日"))
    }
}
