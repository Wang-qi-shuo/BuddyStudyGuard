package com.buddy.studyguard.common.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 时间工具：所有日期均按系统默认时区处理。
 * 最低 API 26 已内置 java.time，无需 desugaring。
 */
object TimeUtil {

    private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** 当前日期字符串 yyyy-MM-dd。 */
    fun todayDayString(zone: ZoneId = ZoneId.systemDefault()): String =
        dayString(System.currentTimeMillis(), zone)

    /** epoch 毫秒转日期字符串。 */
    fun dayString(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(DAY_FMT)

    /** epoch 毫秒 -> 当天分钟数 (0-1439)。 */
    fun minuteOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return dt.hour * 60 + dt.minute
    }

    /** ISO 周几：1=周一 … 7=周日（与 [CourseEntity.dayOfWeek] 一致）。 */
    fun dayOfWeek(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zone).dayOfWeek.value

    /** 今天是否在位掩码 daysOfWeek 中（bit0=周日, bit1=周一, …, bit6=周六）。 */
    fun isTodayInMask(daysOfWeek: Int, epochMillis: Long = System.currentTimeMillis()): Boolean {
        // java.time DayOfWeek: 1=Mon..7=Sun。转换为掩码位：周一=bit1, 周日=bit0
        val dow = dayOfWeek(epochMillis)
        val bit = if (dow == 7) 1 else (1 shl dow)
        return daysOfWeek and bit != 0
    }

    /** 当前分钟是否落在 [startMinute, endMinute) 区间内（支持跨天）。 */
    fun isMinuteInWindow(nowMinute: Int, startMinute: Int, endMinute: Int): Boolean {
        return if (endMinute > startMinute) {
            nowMinute in startMinute until endMinute
        } else {
            // 跨天，如 22:00(1320) - 06:00(360)
            nowMinute >= startMinute || nowMinute < endMinute
        }
    }

    /** 本周（周一 00:00 ~ 下周一 00:00）的起止 epoch 毫秒。 */
    fun currentWeekRange(zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextMonday = monday.plusWeeks(1)
        val start = LocalDateTime.of(monday, LocalTime.MIN).atZone(zone).toInstant().toEpochMilli()
        val end = LocalDateTime.of(nextMonday, LocalTime.MIN).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    /** 某天 00:00 ~ 次日 00:00 的起止 epoch 毫秒。 */
    fun dayRange(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val start = LocalDateTime.of(date, LocalTime.MIN).atZone(zone).toInstant().toEpochMilli()
        val end = LocalDateTime.of(date.plusDays(1), LocalTime.MIN).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    /** 分钟数转 HH:mm 字符串。 */
    fun minuteToHm(minute: Int): String {
        val h = minute / 60
        val m = minute % 60
        return "%02d:%02d".format(h, m)
    }

    /** 毫秒转可读时长，如 "1小时20分"。 */
    fun msToReadable(ms: Long): String {
        if (ms <= 0) return "0分"
        val totalMin = ms / 60000
        val h = totalMin / 60
        val m = totalMin % 60
        return buildString {
            if (h > 0) append("${h}小时")
            if (m > 0 || h == 0L) append("${m}分")
        }
    }

    /**
     * 游戏时长格式化：精确到分钟级，使用"分钟"替代"分"。
     *
     * @param millis 毫秒数
     * @return 格式化字符串。示例：3660000ms → "1小时1分钟"，1800000ms → "30分钟"，
     *         3600000ms → "1小时"，0ms → "0分钟"
     */
    fun formatGameTime(millis: Long): String {
        if (millis <= 0) return "0分钟"
        val totalMinutes = (millis + 30000) / 60000  // 30s 四舍五入到最近分钟
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return buildString {
            if (hours > 0) append("${hours}小时")
            if (hours > 0 && minutes > 0) append("${minutes}分钟")
            else if (hours == 0L) append("${minutes}分钟")
        }
    }
}
