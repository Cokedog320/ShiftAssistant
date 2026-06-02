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
}
