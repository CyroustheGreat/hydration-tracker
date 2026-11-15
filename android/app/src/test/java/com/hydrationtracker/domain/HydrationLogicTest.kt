package com.hydrationtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationLogicTest {
    private val vancouver = ZoneId.of("America/Vancouver")

    @Test
    fun defaultsMatchTheWebApp() {
        assertEquals(2000, TrackerSettings.DEFAULT_GOAL_ML)
        assertEquals(250, TrackerSettings.DEFAULT_GLASS_ML)
        assertEquals(500, TrackerSettings.DEFAULT_BOTTLE_ML)
        assertEquals(listOf(100, 200, 330, 500), TrackerSettings.QUICK_ADD_ML)
        assertEquals(
            listOf("Water", "Sparkling", "Tea", "Coffee", "Juice", "Other"),
            DrinkType.entries.map { it.label },
        )
    }

    @Test
    fun dailyTotalSumsEveryEntry() {
        val today = LocalDate.of(2026, 11, 15)
        val entries = listOf(
            DrinkEntry(1, today, 250, DrinkType.WATER, 1),
            DrinkEntry(2, today, 500, DrinkType.COFFEE, 2),
            DrinkEntry(3, today, 100, DrinkType.TEA, 3),
        )
        assertEquals(850, HydrationMath.dailyTotalMl(entries))
        assertEquals(0, HydrationMath.dailyTotalMl(emptyList()))
    }

    @Test
    fun progressPercentRoundsHalfUpAndCapsAt100() {
        assertEquals(0, HydrationMath.progressPercent(0, 2000))
        assertEquals(0, HydrationMath.progressPercent(1, 2000))
        assertEquals(50, HydrationMath.progressPercent(1000, 2000))
        assertEquals(100, HydrationMath.progressPercent(1999, 2000))
        assertEquals(100, HydrationMath.progressPercent(2500, 2000))
        assertEquals(0, HydrationMath.progressPercent(500, 0))
    }

    @Test
    fun remainingAndGoalReached() {
        assertEquals(1500, HydrationMath.remainingMl(500, 2000))
        assertEquals(0, HydrationMath.remainingMl(2500, 2000))
        assertFalse(HydrationMath.goalReached(1999, 2000))
        assertTrue(HydrationMath.goalReached(2000, 2000))
        assertFalse(HydrationMath.goalReached(10, 0))
    }

    @Test
    fun unitConversionUsesUsFluidOunces() {
        assertEquals("250 ml", Volume.formatAmount(250, VolumeUnit.ML))
        assertEquals("0 ml", Volume.formatAmount(0, VolumeUnit.ML))
        assertEquals("3.4 oz", Volume.formatAmount(100, VolumeUnit.OZ))
        assertEquals("6.8 oz", Volume.formatAmount(200, VolumeUnit.OZ))
        assertEquals("8.5 oz", Volume.formatAmount(250, VolumeUnit.OZ))
        assertEquals("11.2 oz", Volume.formatAmount(330, VolumeUnit.OZ))
        assertEquals("16.9 oz", Volume.formatAmount(500, VolumeUnit.OZ))
        assertEquals("67.6 oz", Volume.formatAmount(2000, VolumeUnit.OZ))
        assertEquals("0.0 oz", Volume.formatAmount(0, VolumeUnit.OZ))

        assertEquals(2000, Volume.parseToMl("2000", VolumeUnit.ML))
        assertEquals(2001, Volume.parseToMl("2000.5", VolumeUnit.ML))
        assertEquals(251, Volume.parseToMl("8.5", VolumeUnit.OZ))
        assertNull(Volume.parseToMl("", VolumeUnit.ML))
        assertNull(Volume.parseToMl("0", VolumeUnit.ML))
        assertNull(Volume.parseToMl("-4", VolumeUnit.OZ))
        assertNull(Volume.parseToMl("abc", VolumeUnit.ML))
    }

    @Test
    fun settingsClampToTheRangesFromTheWebForm() {
        val clean = TrackerSettings(goalMl = 10, glassMl = 5, bottleMl = 50_000).sanitized()
        assertEquals(TrackerSettings.MIN_GOAL_ML, clean.goalMl)
        assertEquals(TrackerSettings.MIN_GLASS_ML, clean.glassMl)
        assertEquals(TrackerSettings.MAX_BOTTLE_ML, clean.bottleMl)
    }

    @Test
    fun vancouverSummerEveningStaysOnTheLocalDate() {
        // 5:00 PM PDT is 00:00 UTC the next day. The old web key would roll here.
        val instant = ZonedDateTime.of(2026, 7, 15, 17, 0, 0, 0, vancouver).toInstant()
        assertEquals(LocalDate.of(2026, 7, 15), LocalDates.dateOf(instant.toEpochMilli(), vancouver))
        assertEquals(LocalDate.of(2026, 7, 16), LocalDates.utcDate(instant.toEpochMilli()))
    }

    @Test
    fun vancouverWinterEveningStaysOnTheLocalDate() {
        val instant = ZonedDateTime.of(2026, 11, 15, 17, 30, 0, 0, vancouver).toInstant()
        assertEquals(LocalDate.of(2026, 11, 15), LocalDates.dateOf(instant.toEpochMilli(), vancouver))
        assertEquals(LocalDate.of(2026, 11, 16), LocalDates.utcDate(instant.toEpochMilli()))
    }

    @Test
    fun intakeRollsOverAtLocalMidnight() {
        val before = ZonedDateTime.of(2026, 11, 15, 23, 30, 0, 0, vancouver)
        val after = ZonedDateTime.of(2026, 11, 16, 0, 30, 0, 0, vancouver)
        val entries = listOf(entryAt(before, 250, 1), entryAt(after, 500, 2))

        assertEquals(250, HydrationMath.dailyTotalMl(HydrationMath.entriesForDate(entries, LocalDate.of(2026, 11, 15))))
        assertEquals(500, HydrationMath.dailyTotalMl(HydrationMath.entriesForDate(entries, LocalDate.of(2026, 11, 16))))
    }

    @Test
    fun todayLogIsNewestFirstAndSkipsOtherDays() {
        val date = LocalDate.of(2026, 11, 15)
        val entries = listOf(
            DrinkEntry(1, date, 100, DrinkType.WATER, 1_000),
            DrinkEntry(2, date, 200, DrinkType.TEA, 2_000),
            DrinkEntry(3, date.minusDays(1), 300, DrinkType.WATER, 3_000),
        )
        assertEquals(listOf(2L, 1L), HydrationMath.entriesForDate(entries, date).map { it.id })
    }

    @Test
    fun weekStatsCoverSevenLocalDaysIncludingZeros() {
        val today = LocalDate.of(2026, 11, 15)
        val entries = listOf(
            DrinkEntry(1, today.minusDays(6), 2000, DrinkType.WATER, 1),
            DrinkEntry(2, today.minusDays(5), 1999, DrinkType.TEA, 2),
            DrinkEntry(3, today.minusDays(2), 2500, DrinkType.COFFEE, 3),
            DrinkEntry(4, today, 300, DrinkType.JUICE, 4),
            DrinkEntry(5, today, 200, DrinkType.WATER, 5),
            DrinkEntry(6, today.minusDays(8), 9000, DrinkType.OTHER, 6),
            DrinkEntry(7, today.plusDays(1), 9000, DrinkType.OTHER, 7),
        )
        val stats = HydrationMath.last7Days(entries, today, goalMl = 2000)
        assertEquals(7, stats.days.size)
        assertEquals(today.minusDays(6), stats.days.first().date)
        assertEquals(today, stats.days.last().date)
        assertEquals(listOf(2000, 1999, 0, 0, 2500, 0, 500), stats.days.map { it.totalMl })
        assertEquals(1000, stats.averageMl)
        assertEquals(2, stats.daysAtGoal)
        assertEquals(2500, stats.bestMl)
    }

    @Test
    fun emptyWeekStillHasSevenDays() {
        val today = LocalDate.of(2026, 11, 15)
        val stats = HydrationMath.last7Days(emptyList(), today, goalMl = 2000)
        assertEquals(7, stats.days.size)
        assertEquals(0, stats.averageMl)
        assertEquals(0, stats.daysAtGoal)
        assertEquals(0, stats.bestMl)
        assertTrue(stats.days.all { it.totalMl == 0 })
    }

    @Test
    fun remindersStayInsideTheActiveWindow() {
        val start = LocalTime.of(8, 0)
        val end = LocalTime.of(22, 0)
        assertTrue(ReminderSchedule.isWithinWindow(LocalTime.of(8, 0), start, end))
        assertTrue(ReminderSchedule.isWithinWindow(LocalTime.of(21, 59), start, end))
        assertFalse(ReminderSchedule.isWithinWindow(LocalTime.of(22, 0), start, end))
        assertFalse(ReminderSchedule.isWithinWindow(LocalTime.of(7, 59), start, end))

        assertEquals(
            LocalDateTime.of(2026, 11, 15, 8, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 7, 15), start, end, 60),
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 15, 10, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 9, 0), start, end, 60),
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 16, 8, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 21, 30), start, end, 60),
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 16, 8, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 22, 30), start, end, 60),
        )
    }

    @Test
    fun overnightReminderWindowWrapsPastMidnight() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)
        assertEquals(
            LocalDateTime.of(2026, 11, 16, 1, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 23, 0), start, end, 120),
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 15, 22, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 12, 0), start, end, 60),
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 15, 22, 0),
            ReminderSchedule.nextTrigger(LocalDateTime.of(2026, 11, 15, 7, 30), start, end, 60),
        )
    }

    private fun entryAt(time: ZonedDateTime, ml: Int, id: Long): DrinkEntry {
        val millis = time.toInstant().toEpochMilli()
        return DrinkEntry(
            id = id,
            date = LocalDates.dateOf(millis, time.zone),
            ml = ml,
            drink = DrinkType.WATER,
            loggedAtEpochMillis = millis,
        )
    }
}
