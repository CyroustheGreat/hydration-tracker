package com.hydrationtracker.domain

import java.time.LocalDate

enum class VolumeUnit {
    ML,
    OZ,
}

enum class DrinkType(val label: String) {
    WATER("Water"),
    SPARKLING("Sparkling"),
    TEA("Tea"),
    COFFEE("Coffee"),
    JUICE("Juice"),
    OTHER("Other"),
    ;

    companion object {
        fun fromLabel(label: String): DrinkType =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTHER
    }
}

data class DrinkEntry(
    val id: Long,
    val date: LocalDate,
    val ml: Int,
    val drink: DrinkType,
    val loggedAtEpochMillis: Long,
)

data class TrackerSettings(
    val goalMl: Int = DEFAULT_GOAL_ML,
    val glassMl: Int = DEFAULT_GLASS_ML,
    val bottleMl: Int = DEFAULT_BOTTLE_ML,
    val unit: VolumeUnit = VolumeUnit.ML,
    val remindersEnabled: Boolean = false,
    val reminderIntervalMinutes: Int = DEFAULT_REMINDER_INTERVAL_MIN,
    val reminderStartMinuteOfDay: Int = DEFAULT_REMINDER_START_MIN,
    val reminderEndMinuteOfDay: Int = DEFAULT_REMINDER_END_MIN,
) {
    companion object {
        const val DEFAULT_GOAL_ML = 2000
        const val DEFAULT_GLASS_ML = 250
        const val DEFAULT_BOTTLE_ML = 500
        const val DEFAULT_REMINDER_INTERVAL_MIN = 60
        const val DEFAULT_REMINDER_START_MIN = 8 * 60
        const val DEFAULT_REMINDER_END_MIN = 22 * 60
        val QUICK_ADD_ML = listOf(100, 200, 330, 500)
        val REMINDER_INTERVALS_MIN = listOf(30, 45, 60, 90, 120, 180)

        const val MIN_GOAL_ML = 500
        const val MAX_GOAL_ML = 10_000
        const val MIN_GLASS_ML = 50
        const val MAX_GLASS_ML = 1_000
        const val MIN_BOTTLE_ML = 100
        const val MAX_BOTTLE_ML = 2_000
        const val MIN_CUSTOM_ML = 1
        const val MAX_CUSTOM_ML = 5_000
    }
}

fun TrackerSettings.sanitized(): TrackerSettings = copy(
    goalMl = goalMl.coerceIn(TrackerSettings.MIN_GOAL_ML, TrackerSettings.MAX_GOAL_ML),
    glassMl = glassMl.coerceIn(TrackerSettings.MIN_GLASS_ML, TrackerSettings.MAX_GLASS_ML),
    bottleMl = bottleMl.coerceIn(TrackerSettings.MIN_BOTTLE_ML, TrackerSettings.MAX_BOTTLE_ML),
    reminderIntervalMinutes = reminderIntervalMinutes.coerceAtLeast(15),
    reminderStartMinuteOfDay = reminderStartMinuteOfDay.floorMod(24 * 60),
    reminderEndMinuteOfDay = reminderEndMinuteOfDay.floorMod(24 * 60),
)

private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod
