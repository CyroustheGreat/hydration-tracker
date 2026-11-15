package com.hydrationtracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

object LocalDates {
    /** Calendar date in [zone]. This is the day boundary the tracker uses. */
    fun dateOf(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * UTC date key, matching the web app's old `toISOString().slice(0, 10)`.
     * Kept so tests can show it diverges from the local calendar date.
     */
    fun utcDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
