package com.hydrationtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hydrationtracker.domain.DrinkEntry
import com.hydrationtracker.domain.DrinkType
import com.hydrationtracker.domain.LocalDates
import com.hydrationtracker.domain.TrackerSettings
import com.hydrationtracker.domain.VolumeUnit
import com.hydrationtracker.domain.sanitized
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.hydrationStore: DataStore<Preferences> by preferencesDataStore(name = "hydration")

class HydrationRepository(
    private val dataStore: DataStore<Preferences>,
    private val onDataChanged: () -> Unit = {},
) {
    val settings: Flow<TrackerSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs.toSettings() }

    val entries: Flow<List<DrinkEntry>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> decodeEntries(prefs[Keys.entries]) }

    suspend fun addDrink(
        ml: Int,
        drink: DrinkType,
        atMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        if (ml <= 0 || ml > TrackerSettings.MAX_CUSTOM_ML) return
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.entries])
            val id = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val entry = DrinkEntry(
                id = id,
                date = LocalDates.dateOf(atMillis, zone),
                ml = ml,
                drink = drink,
                loggedAtEpochMillis = atMillis,
            )
            prefs[Keys.entries] = encodeEntries(current + entry)
        }
        onDataChanged()
    }

    suspend fun delete(id: Long) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.entries])
            prefs[Keys.entries] = encodeEntries(current.filter { it.id != id })
        }
        onDataChanged()
    }

    suspend fun clearDate(date: LocalDate) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.entries])
            prefs[Keys.entries] = encodeEntries(current.filter { it.date != date })
        }
        onDataChanged()
    }

    /**
     * Applies [transform] to the latest saved settings inside one DataStore edit,
     * so two quick changes cannot overwrite each other.
     */
    suspend fun updateSettings(transform: (TrackerSettings) -> TrackerSettings) {
        dataStore.edit { prefs ->
            val clean = transform(prefs.toSettings()).sanitized()
            prefs[Keys.goal] = clean.goalMl
            prefs[Keys.glass] = clean.glassMl
            prefs[Keys.bottle] = clean.bottleMl
            prefs[Keys.unit] = clean.unit.name
            prefs[Keys.reminders] = clean.remindersEnabled
            prefs[Keys.interval] = clean.reminderIntervalMinutes
            prefs[Keys.start] = clean.reminderStartMinuteOfDay
            prefs[Keys.end] = clean.reminderEndMinuteOfDay
        }
        onDataChanged()
    }

    private fun Preferences.toSettings(): TrackerSettings {
        val unitName = this[Keys.unit]
        val unit = VolumeUnit.entries.firstOrNull { it.name == unitName } ?: VolumeUnit.ML
        return TrackerSettings(
            goalMl = this[Keys.goal] ?: TrackerSettings.DEFAULT_GOAL_ML,
            glassMl = this[Keys.glass] ?: TrackerSettings.DEFAULT_GLASS_ML,
            bottleMl = this[Keys.bottle] ?: TrackerSettings.DEFAULT_BOTTLE_ML,
            unit = unit,
            remindersEnabled = this[Keys.reminders] ?: false,
            reminderIntervalMinutes = this[Keys.interval] ?: TrackerSettings.DEFAULT_REMINDER_INTERVAL_MIN,
            reminderStartMinuteOfDay = this[Keys.start] ?: TrackerSettings.DEFAULT_REMINDER_START_MIN,
            reminderEndMinuteOfDay = this[Keys.end] ?: TrackerSettings.DEFAULT_REMINDER_END_MIN,
        ).sanitized()
    }

    private object Keys {
        val goal = intPreferencesKey("goal_ml")
        val glass = intPreferencesKey("glass_ml")
        val bottle = intPreferencesKey("bottle_ml")
        val unit = stringPreferencesKey("unit")
        val reminders = booleanPreferencesKey("reminders_enabled")
        val interval = intPreferencesKey("reminder_interval_min")
        val start = intPreferencesKey("reminder_start_min")
        val end = intPreferencesKey("reminder_end_min")
        val entries = stringPreferencesKey("entries_json")
    }
}

internal fun encodeEntries(entries: List<DrinkEntry>): String {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject()
                .put("id", entry.id)
                .put("date", entry.date.toString())
                .put("ml", entry.ml)
                .put("drink", entry.drink.label)
                .put("at", entry.loggedAtEpochMillis),
        )
    }
    return array.toString()
}

internal fun decodeEntries(json: String?): List<DrinkEntry> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val date = try {
                    LocalDate.parse(obj.optString("date"))
                } catch (_: Exception) {
                    continue
                }
                val ml = obj.optInt("ml", 0)
                if (ml <= 0) continue
                add(
                    DrinkEntry(
                        id = obj.optLong("id"),
                        date = date,
                        ml = ml,
                        drink = DrinkType.fromLabel(obj.optString("drink")),
                        loggedAtEpochMillis = obj.optLong("at"),
                    ),
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
