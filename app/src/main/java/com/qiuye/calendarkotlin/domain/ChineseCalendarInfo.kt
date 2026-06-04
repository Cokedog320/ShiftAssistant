package com.qiuye.calendarkotlin.domain

import android.content.Context
import android.util.Log
import com.nlf.calendar.Solar
import com.qiuye.calendarkotlin.model.HolidayMarker
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON 反序列化模型 */
@Serializable
private data class YearHolidayData(
    val year: Int,
    val holidays: List<HolidayEntry>,
    val workdays: List<WorkdayEntry>,
)

@Serializable
private data class HolidayEntry(
    val name: String,
    val start: String,
    val end: String,
)

@Serializable
private data class WorkdayEntry(
    val date: String,
    val name: String,
)

/** 内部运行时模型 */
private data class HolidayPeriod(
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
)

data class LunarDisplay(
    val label: String,
    val fullText: String,
)

object ChineseCalendarInfo {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile
    private var holidayPeriods: List<HolidayPeriod> = emptyList()
    @Volatile
    private var workdays: Map<LocalDate, String> = emptyMap()

    /**
     * 从 assets/holidays.json 加载假日数据。
     * 应在 Application 或 MainActivity.onCreate 中调用一次。
     * 支持多年份数据，后续只需编辑 JSON 文件即可。
     */
    fun init(context: Context) {
        try {
            val raw = context.assets.open("holidays.json").bufferedReader().use { it.readText() }
            val yearDataList = json.decodeFromString<List<YearHolidayData>>(raw)

            holidayPeriods = yearDataList.flatMap { yearData ->
                yearData.holidays.map { entry ->
                    HolidayPeriod(
                        name = entry.name,
                        start = LocalDate.parse(entry.start),
                        end = LocalDate.parse(entry.end),
                    )
                }
            }

            workdays = yearDataList.flatMap { yearData ->
                yearData.workdays.map { entry ->
                    LocalDate.parse(entry.date) to entry.name
                }
            }.toMap()
        } catch (e: Exception) {
            Log.e("ChineseCalendarInfo", "加载 holidays.json 失败", e)
        }
    }

    fun getLunarDisplay(date: LocalDate): LunarDisplay {
        try {
            val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
            val lunar = solar.lunar
            val monthDay = "${lunar.monthInChinese}月${lunar.dayInChinese}"
            
            val validJieQi = lunar.jieQi?.takeIf { it.isNotBlank() }
            val validFestival = lunar.festivals?.firstOrNull { it.isNotBlank() } ?: solar.festivals?.firstOrNull { it.isNotBlank() }
            val defaultLunar = "${lunar.monthInChinese}月\n${lunar.dayInChinese}"
            
            val specialText = buildString {
                validJieQi?.let { append(it) }
                validFestival?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it)
                }
            }

            val shortLabel = validJieQi ?: validFestival ?: defaultLunar

            return LunarDisplay(
                label = shortLabel.takeIf { it.isNotBlank() } ?: defaultLunar,
                fullText =
                    buildString {
                        append("${lunar.yearInGanZhi}年 ")
                        append(monthDay)
                        if (specialText.isNotBlank()) {
                            append(" · ")
                            append(specialText)
                        }
                    },
            )
        } catch (e: Exception) {
            Log.e("ChineseCalendarInfo", "获取农历失败: date=$date", e)
            return LunarDisplay(label = "", fullText = "")
        }
    }

    fun getCleanLunarLabel(date: LocalDate): String {
        try {
            val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
            val lunar = solar.lunar
            val monthDay = "${lunar.monthInChinese}月${lunar.dayInChinese}"
            
            val validJieQi = lunar.jieQi?.takeIf { it.isNotBlank() }
            val validFestival = lunar.festivals?.firstOrNull { it.isNotBlank() } ?: solar.festivals?.firstOrNull { it.isNotBlank() }
            
            val specialText = buildString {
                validJieQi?.let { append(it) }
                validFestival?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it)
                }
            }
            
            return if (specialText.isNotBlank()) {
                "$monthDay · $specialText"
            } else {
                monthDay
            }
        } catch (e: Exception) {
            Log.e("ChineseCalendarInfo", "获取清洗后的农历标签失败: date=$date", e)
            return ""
        }
    }

    fun getHolidayMarker(date: LocalDate): HolidayMarker? {
        workdays[date]?.let { name ->
            return HolidayMarker(
                name = name,
                label = "班",
                isWorkday = true,
            )
        }

        val holiday =
            holidayPeriods.firstOrNull { period ->
                !date.isBefore(period.start) && !date.isAfter(period.end)
            } ?: return null

        return HolidayMarker(
            name = holiday.name,
            label = "休",
            isWorkday = false,
        )
    }
}


