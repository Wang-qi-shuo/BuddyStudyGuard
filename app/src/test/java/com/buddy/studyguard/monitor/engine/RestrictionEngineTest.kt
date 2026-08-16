package com.buddy.studyguard.monitor.engine

import com.buddy.studyguard.common.data.db.dao.AppLimitRuleDao
import com.buddy.studyguard.common.data.db.dao.AppLockStateDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.data.db.dao.BlockedTimeWindowDao
import com.buddy.studyguard.common.data.db.entity.AppLimitRuleEntity
import com.buddy.studyguard.common.data.db.entity.AppLockStateEntity
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowEntity
import com.buddy.studyguard.common.util.TimeUtil
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * RestrictionEngine 单元测试：用 MockK mock 四个 DAO，并固定 TimeUtil 时间相关方法，
 * 覆盖即时锁定、每日时长、禁用时段（含跨天/星期掩码/指定应用）的评估顺序与边界。
 */
class RestrictionEngineTest {

    private val appLockStateDao = mockk<AppLockStateDao>()
    private val appLimitRuleDao = mockk<AppLimitRuleDao>()
    private val blockedTimeWindowDao = mockk<BlockedTimeWindowDao>()
    private val appUsageRecordDao = mockk<AppUsageRecordDao>()

    private lateinit var engine: RestrictionEngine

    private val pkg = "com.example.game"

    @Before
    fun setUp() {
        engine = RestrictionEngine(appLockStateDao, appLimitRuleDao, blockedTimeWindowDao, appUsageRecordDao)
        // 固定时间相关方法，避免测试依赖真实时钟
        mockkObject(TimeUtil)
        every { TimeUtil.todayDayString() } returns "2026-08-16"
        every { TimeUtil.minuteOfDay(any()) } returns 1320 // 22:00
        every { TimeUtil.isTodayInMask(any(), any()) } returns true
        every { TimeUtil.isMinuteInWindow(any(), any(), any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun mockNoLock() {
        coEvery { appLockStateDao.get(any()) } returns null
    }

    private fun mockNoRule() {
        coEvery { appLimitRuleDao.get(any()) } returns null
    }

    private fun mockNoWindows() {
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns emptyList()
    }

    // ---------- 1. 即时锁定 ----------

    @Test
    fun instantLocked_blocks() = runTest {
        coEvery { appLockStateDao.get(pkg) } returns AppLockStateEntity(pkg, locked = true)
        val decision = engine.evaluate(pkg)
        assertEquals(RestrictionDecision.Block(BlockReason.INSTANT_LOCKED, pkg), decision)
    }

    @Test
    fun instantLocked_takesPriorityOverTimeLimit() = runTest {
        coEvery { appLockStateDao.get(pkg) } returns AppLockStateEntity(pkg, locked = true)
        coEvery { appLimitRuleDao.get(pkg) } returns AppLimitRuleEntity(pkg, dailyLimitMs = 1, enabled = true)
        coEvery { appUsageRecordDao.sumForegroundMsForPackages(any(), any()) } returns 999_999L
        val decision = engine.evaluate(pkg)
        assertEquals(RestrictionDecision.Block(BlockReason.INSTANT_LOCKED, pkg), decision)
    }

    // ---------- 2. 每日时长限制 ----------

    @Test
    fun noRestriction_allows() = runTest {
        mockNoLock()
        mockNoRule()
        mockNoWindows()
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun timeLimitExceeded_blocks() = runTest {
        mockNoLock()
        coEvery { appLimitRuleDao.get(pkg) } returns AppLimitRuleEntity(pkg, dailyLimitMs = 3_600_000, enabled = true)
        coEvery { appUsageRecordDao.sumForegroundMsForPackages("2026-08-16", listOf(pkg)) } returns 3_600_000L
        val decision = engine.evaluate(pkg)
        assertEquals(RestrictionDecision.Block(BlockReason.TIME_LIMIT_EXCEEDED, pkg), decision)
    }

    @Test
    fun timeLimitNotExceeded_allows() = runTest {
        mockNoLock()
        coEvery { appLimitRuleDao.get(pkg) } returns AppLimitRuleEntity(pkg, dailyLimitMs = 3_600_000, enabled = true)
        coEvery { appUsageRecordDao.sumForegroundMsForPackages("2026-08-16", listOf(pkg)) } returns 3_599_999L
        mockNoWindows()
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun disabledRule_allows() = runTest {
        mockNoLock()
        coEvery { appLimitRuleDao.get(pkg) } returns AppLimitRuleEntity(pkg, dailyLimitMs = 1, enabled = false)
        mockNoWindows()
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun zeroLimitRule_allows() = runTest {
        mockNoLock()
        coEvery { appLimitRuleDao.get(pkg) } returns AppLimitRuleEntity(pkg, dailyLimitMs = 0, enabled = true)
        mockNoWindows()
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun timeLimitExceeded_takesPriorityOverWindow() = runTest {
        mockNoLock()
        coEvery { appLimitRuleDao.get(pkg) } returns AppLimitRuleEntity(pkg, dailyLimitMs = 1, enabled = true)
        coEvery { appUsageRecordDao.sumForegroundMsForPackages(any(), any()) } returns 999_999L
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns
            listOf(BlockedTimeWindowEntity(id = 1, startMinute = 0, endMinute = 1439, appliesToAllApps = true))
        val decision = engine.evaluate(pkg)
        assertEquals(RestrictionDecision.Block(BlockReason.TIME_LIMIT_EXCEEDED, pkg), decision)
    }

    // ---------- 3. 禁用时段 ----------

    @Test
    fun blockedWindow_allApps_blocks() = runTest {
        mockNoLock()
        mockNoRule()
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns
            listOf(BlockedTimeWindowEntity(id = 1, startMinute = 1320, endMinute = 360, appliesToAllApps = true))
        val decision = engine.evaluate(pkg)
        assertEquals(RestrictionDecision.Block(BlockReason.BLOCKED_TIME_WINDOW, pkg), decision)
    }

    @Test
    fun blockedWindow_specificApp_blocks() = runTest {
        mockNoLock()
        mockNoRule()
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns
            listOf(BlockedTimeWindowEntity(id = 1, startMinute = 1320, endMinute = 360, appliesToAllApps = false))
        coEvery { blockedTimeWindowDao.getAppsOf(1) } returns listOf(pkg)
        val decision = engine.evaluate(pkg)
        assertEquals(RestrictionDecision.Block(BlockReason.BLOCKED_TIME_WINDOW, pkg), decision)
    }

    @Test
    fun blockedWindow_specificApp_notInList_allows() = runTest {
        mockNoLock()
        mockNoRule()
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns
            listOf(BlockedTimeWindowEntity(id = 1, startMinute = 1320, endMinute = 360, appliesToAllApps = false))
        coEvery { blockedTimeWindowDao.getAppsOf(1) } returns listOf("com.other.app")
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun blockedWindow_dayNotMatch_allows() = runTest {
        mockNoLock()
        mockNoRule()
        every { TimeUtil.isTodayInMask(any(), any()) } returns false
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns
            listOf(BlockedTimeWindowEntity(id = 1, startMinute = 0, endMinute = 1439, daysOfWeek = 0x02, appliesToAllApps = true))
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun blockedWindow_minuteNotInWindow_allows() = runTest {
        mockNoLock()
        mockNoRule()
        every { TimeUtil.isMinuteInWindow(any(), any(), any()) } returns false
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns
            listOf(BlockedTimeWindowEntity(id = 1, startMinute = 0, endMinute = 1439, appliesToAllApps = true))
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }

    @Test
    fun disabledWindow_ignored() = runTest {
        mockNoLock()
        mockNoRule()
        coEvery { blockedTimeWindowDao.getAllEnabled() } returns emptyList() // 仅返回 enabled 的窗口
        assertEquals(RestrictionDecision.Allow, engine.evaluate(pkg))
    }
}
