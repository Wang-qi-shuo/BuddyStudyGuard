package com.buddy.studyguard.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * TimeUtil 单元测试：覆盖时间计算、星期掩码、跨天窗口、格式化等正常与边界路径。
 */
class TimeUtilTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    /** 2026-08-16 是周日；用 UTC 中午的 epoch，保证任何系统时区下都不跨天。 */
    private val sundayEpoch: Long =
        LocalDateTime.of(2026, 8, 16, 12, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** 2026-08-17 是周一。 */
    private val mondayEpoch: Long =
        LocalDateTime.of(2026, 8, 17, 12, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    // ---------- dayString ----------

    @Test
    fun dayString_formatsDate() {
        val epoch = LocalDateTime.of(2026, 8, 16, 12, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals("2026-08-16", TimeUtil.dayString(epoch, zone))
    }

    @Test
    fun dayString_handlesMonthBoundary() {
        val epoch = LocalDateTime.of(2026, 8, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals("2026-08-01", TimeUtil.dayString(epoch, zone))
    }

    // ---------- minuteOfDay ----------

    @Test
    fun minuteOfDay_midnightIsZero() {
        val epoch = LocalDateTime.of(2026, 8, 16, 0, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(0, TimeUtil.minuteOfDay(epoch, zone))
    }

    @Test
    fun minuteOfDay_tenThirtyIs630() {
        val epoch = LocalDateTime.of(2026, 8, 16, 10, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals(630, TimeUtil.minuteOfDay(epoch, zone))
    }

    @Test
    fun minuteOfDay_lastMinuteIs1439() {
        val epoch = LocalDateTime.of(2026, 8, 16, 23, 59).atZone(zone).toInstant().toEpochMilli()
        assertEquals(1439, TimeUtil.minuteOfDay(epoch, zone))
    }

    // ---------- dayOfWeek ----------

    @Test
    fun dayOfWeek_sundayIs7() {
        assertEquals(7, TimeUtil.dayOfWeek(sundayEpoch, zone))
    }

    @Test
    fun dayOfWeek_mondayIs1() {
        assertEquals(1, TimeUtil.dayOfWeek(mondayEpoch, zone))
    }

    // ---------- isTodayInMask（bit0=周日 … bit6=周六） ----------

    @Test
    fun isTodayInMask_sundayMatchesBit0() {
        assertTrue(TimeUtil.isTodayInMask(0x01, sundayEpoch))
    }

    @Test
    fun isTodayInMask_sundayNotMatchMondayBit() {
        assertFalse(TimeUtil.isTodayInMask(0x02, sundayEpoch))
    }

    @Test
    fun isTodayInMask_mondayMatchesBit1() {
        assertTrue(TimeUtil.isTodayInMask(0x02, mondayEpoch))
    }

    @Test
    fun isTodayInMask_mondayNotMatchSundayBit() {
        assertFalse(TimeUtil.isTodayInMask(0x01, mondayEpoch))
    }

    @Test
    fun isTodayInMask_everydayMaskMatches() {
        assertTrue(TimeUtil.isTodayInMask(0x7F, sundayEpoch))
        assertTrue(TimeUtil.isTodayInMask(0x7F, mondayEpoch))
    }

    @Test
    fun isTodayInMask_zeroMaskNeverMatches() {
        assertFalse(TimeUtil.isTodayInMask(0, sundayEpoch))
        assertFalse(TimeUtil.isTodayInMask(0, mondayEpoch))
    }

    // ---------- isMinuteInWindow（含跨天） ----------

    @Test
    fun isMinuteInWindow_withinWindow() {
        assertTrue(TimeUtil.isMinuteInWindow(600, 600, 1200))
        assertTrue(TimeUtil.isMinuteInWindow(1199, 600, 1200))
    }

    @Test
    fun isMinuteInWindow_startInclusiveEndExclusive() {
        assertTrue(TimeUtil.isMinuteInWindow(600, 600, 1200))
        assertFalse(TimeUtil.isMinuteInWindow(1200, 600, 1200))
        assertFalse(TimeUtil.isMinuteInWindow(599, 600, 1200))
    }

    @Test
    fun isMinuteInWindow_crossMidnight() {
        // 22:00(1320) - 06:00(360)
        assertTrue(TimeUtil.isMinuteInWindow(1320, 1320, 360))
        assertTrue(TimeUtil.isMinuteInWindow(1439, 1320, 360))
        assertTrue(TimeUtil.isMinuteInWindow(0, 1320, 360))
        assertTrue(TimeUtil.isMinuteInWindow(359, 1320, 360))
        assertFalse(TimeUtil.isMinuteInWindow(360, 1320, 360))
        assertFalse(TimeUtil.isMinuteInWindow(600, 1320, 360))
    }

    @Test
    fun isMinuteInWindow_startEqualsEnd() {
        // start == end：空窗口，不命中
        assertFalse(TimeUtil.isMinuteInWindow(600, 600, 600))
        assertFalse(TimeUtil.isMinuteInWindow(500, 600, 600))
    }

    // ---------- currentWeekRange ----------

    @Test
    fun currentWeekRange_startsMondayEndsNextMonday() {
        val (start, end) = TimeUtil.currentWeekRange(zone)
        val startDate = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(start), zone)
        val endDate = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(end), zone)
        // 周一 00:00 起，下周一 00:00 止，跨度 7 天
        assertEquals(1, startDate.dayOfWeek.value)
        assertEquals(0, startDate.hour)
        assertEquals(0, startDate.minute)
        assertEquals(1, endDate.dayOfWeek.value)
        assertEquals(7L * 24 * 3600 * 1000, end - start)
    }

    // ---------- dayRange ----------

    @Test
    fun dayRange_spans24Hours() {
        val epoch = LocalDateTime.of(2026, 8, 16, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val (start, end) = TimeUtil.dayRange(epoch, zone)
        val startDate = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(start), zone)
        val endDate = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(end), zone)
        assertEquals("2026-08-16", startDate.toLocalDate().toString())
        assertEquals("2026-08-17", endDate.toLocalDate().toString())
        assertEquals(24L * 3600 * 1000, end - start)
    }

    // ---------- minuteToHm ----------

    @Test
    fun minuteToHm_formats() {
        assertEquals("00:00", TimeUtil.minuteToHm(0))
        assertEquals("00:59", TimeUtil.minuteToHm(59))
        assertEquals("10:00", TimeUtil.minuteToHm(600))
        assertEquals("23:59", TimeUtil.minuteToHm(1439))
    }

    // ---------- msToReadable ----------

    @Test
    fun msToReadable_zeroOrNegative() {
        assertEquals("0分", TimeUtil.msToReadable(0))
        assertEquals("0分", TimeUtil.msToReadable(-1000))
    }

    @Test
    fun msToReadable_minutesOnly() {
        assertEquals("1分", TimeUtil.msToReadable(60_000))
    }

    @Test
    fun msToReadable_hoursAndMinutes() {
        assertEquals("1小时", TimeUtil.msToReadable(3_600_000))
        assertEquals("1小时1分", TimeUtil.msToReadable(3_660_000))
        assertEquals("2小时", TimeUtil.msToReadable(7_200_000))
    }

    // ---------- formatGameTime（30s 四舍五入到分钟） ----------

    @Test
    fun formatGameTime_zeroOrNegative() {
        assertEquals("0分钟", TimeUtil.formatGameTime(0))
        assertEquals("0分钟", TimeUtil.formatGameTime(-5000))
    }

    @Test
    fun formatGameTime_roundsToNearestMinute() {
        assertEquals("1分钟", TimeUtil.formatGameTime(30_000))
        assertEquals("1分钟", TimeUtil.formatGameTime(45_000))
        assertEquals("0分钟", TimeUtil.formatGameTime(29_999))
    }

    @Test
    fun formatGameTime_minutesOnly() {
        assertEquals("30分钟", TimeUtil.formatGameTime(1_800_000))
    }

    @Test
    fun formatGameTime_hoursOnly() {
        assertEquals("1小时", TimeUtil.formatGameTime(3_600_000))
    }

    @Test
    fun formatGameTime_hoursAndMinutes() {
        assertEquals("1小时1分钟", TimeUtil.formatGameTime(3_660_000))
        assertEquals("1小时5分钟", TimeUtil.formatGameTime(3_900_000))
        assertEquals("2小时", TimeUtil.formatGameTime(7_200_000))
    }
}
