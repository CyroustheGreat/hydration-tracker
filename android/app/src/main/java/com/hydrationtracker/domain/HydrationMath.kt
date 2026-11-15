package com.hydrationtracker.domain

import java.time.LocalDate
import kotlin.math.min

data class DayTotal(val date: LocalDate, val totalMl: Int)

data class WeekStats(
    val days: List<DayTotal>,
    val averageMl: Int,
    val daysAtGoal: Int,
    val bestMl: Int,
)

object HydrationMath {
    fun dailyTotalMl(entries: List<DrinkEntry>): Int = entries.sumOf { it.ml }

    /** Matches the web app: round half up, then cap at 100. */
    fun progressPercent(totalMl: Int, goalMl: Int): Int {
        if (goalMl <= 0 || totalMl <= 0) return 0
        val rounded = Math.round(totalMl * 100.0 / goalMl).toInt()
        return min(100, rounded)
    }

    fun remainingMl(totalMl: Int, goalMl: Int): Int = (goalMl - totalMl).coerceAtLeast(0)

    fun goalReached(totalMl: Int, goalMl: Int): Boolean = goalMl > 0 && totalMl >= goalMl

    fun entriesForDate(entries: List<DrinkEntry>, date: LocalDate): List<DrinkEntry> =
        entries.filter { it.date == date }
            .sortedWith(compareByDescending<DrinkEntry> { it.loggedAtEpochMillis }.thenByDescending { it.id })

    /**
     * The seven local dates ending on [today], oldest first.
     * Days with no drinks count as zero, including toward the average.
     */
    fun last7Days(entries: List<DrinkEntry>, today: LocalDate, goalMl: Int): WeekStats {
        val start = today.minusDays(6)
        val totals = entries.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.ml } }
        val days = (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            DayTotal(date, totals[date] ?: 0)
        }
        val sum = days.sumOf { it.totalMl }
        val average = Math.round(sum / 7.0).toInt()
        val daysAtGoal = if (goalMl <= 0) 0 else days.count { it.totalMl >= goalMl }
        val best = days.maxOf { it.totalMl }
        return WeekStats(days, average, daysAtGoal, best)
    }
}
