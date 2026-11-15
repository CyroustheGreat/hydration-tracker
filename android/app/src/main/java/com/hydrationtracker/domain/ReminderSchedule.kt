package com.hydrationtracker.domain

import java.time.LocalDateTime
import java.time.LocalTime

object ReminderSchedule {
    /**
     * Active window is [start, end). Equal start and end means all day.
     * A window that ends before it starts wraps past midnight (for example 10 PM–8 AM).
     */
    fun isWithinWindow(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (start == end) return true
        return if (end.isAfter(start)) {
            !time.isBefore(start) && time.isBefore(end)
        } else {
            !time.isBefore(start) || time.isBefore(end)
        }
    }

    /**
     * Next time a reminder should fire.
     * Inside the window, that is one interval ahead when the result is still inside.
     * Otherwise it is the next window start.
     */
    fun nextTrigger(
        from: LocalDateTime,
        start: LocalTime,
        end: LocalTime,
        intervalMinutes: Int,
    ): LocalDateTime {
        require(intervalMinutes > 0) { "interval must be positive" }
        if (start == end) return from.plusMinutes(intervalMinutes.toLong())
        if (isWithinWindow(from.toLocalTime(), start, end)) {
            val candidate = from.plusMinutes(intervalMinutes.toLong())
            if (!leavesWindow(from, candidate, start, end)) return candidate
        }
        return upcomingStart(from, start)
    }

    private fun leavesWindow(
        from: LocalDateTime,
        candidate: LocalDateTime,
        start: LocalTime,
        end: LocalTime,
    ): Boolean {
        if (end.isAfter(start)) {
            if (candidate.toLocalDate() != from.toLocalDate()) return true
            val time = candidate.toLocalTime()
            return time.isBefore(start) || !time.isBefore(end)
        }
        val exit = if (!from.toLocalTime().isBefore(start)) {
            from.toLocalDate().plusDays(1).atTime(end)
        } else {
            from.toLocalDate().atTime(end)
        }
        return !candidate.isBefore(exit)
    }

    private fun upcomingStart(from: LocalDateTime, start: LocalTime): LocalDateTime {
        val todayStart = from.toLocalDate().atTime(start)
        return if (from.isBefore(todayStart)) todayStart else todayStart.plusDays(1)
    }
}
