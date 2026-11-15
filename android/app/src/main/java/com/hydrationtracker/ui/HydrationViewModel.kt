package com.hydrationtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hydrationtracker.data.HydrationRepository
import com.hydrationtracker.domain.DrinkEntry
import com.hydrationtracker.domain.DrinkType
import com.hydrationtracker.domain.HydrationMath
import com.hydrationtracker.domain.TrackerSettings
import com.hydrationtracker.domain.WeekStats
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val ready: Boolean,
    val today: LocalDate,
    val settings: TrackerSettings,
    val totalMl: Int,
    val percent: Int,
    val remainingMl: Int,
    val goalReached: Boolean,
    val todayEntries: List<DrinkEntry>,
    val week: WeekStats,
    val selectedDrink: DrinkType,
)

class HydrationViewModel(
    private val repository: HydrationRepository,
    private val clockZone: ZoneId? = null,
) : ViewModel() {
    /** Read on each refresh so a timezone change is not stuck on the zone from process start. */
    private fun zone(): ZoneId = clockZone ?: ZoneId.systemDefault()

    private val selectedDrink = MutableStateFlow(DrinkType.WATER)
    private val ticks = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDate.now(zone()))
            delay(15_000)
        }
    }

    val ui: StateFlow<HomeUiState> = combine(
        repository.settings,
        repository.entries,
        selectedDrink,
        ticks,
    ) { settings, entries, drink, today ->
        val todayEntries = HydrationMath.entriesForDate(entries, today)
        val total = HydrationMath.dailyTotalMl(todayEntries)
        HomeUiState(
            ready = true,
            today = today,
            settings = settings,
            totalMl = total,
            percent = HydrationMath.progressPercent(total, settings.goalMl),
            remainingMl = HydrationMath.remainingMl(total, settings.goalMl),
            goalReached = HydrationMath.goalReached(total, settings.goalMl),
            todayEntries = todayEntries,
            week = HydrationMath.last7Days(entries, today, settings.goalMl),
            selectedDrink = drink,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    fun selectDrink(drink: DrinkType) {
        selectedDrink.value = drink
    }

    fun add(ml: Int) {
        val drink = selectedDrink.value
        val zoneNow = zone()
        val atMillis = System.currentTimeMillis()
        viewModelScope.launch {
            repository.addDrink(ml, drink, atMillis, zoneNow)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearToday() {
        val zoneNow = zone()
        viewModelScope.launch { repository.clearDate(LocalDate.now(zoneNow)) }
    }

    fun updateSettings(transform: (TrackerSettings) -> TrackerSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    private fun initialState(): HomeUiState {
        val today = LocalDate.now(zone())
        val settings = TrackerSettings()
        return HomeUiState(
            ready = false,
            today = today,
            settings = settings,
            totalMl = 0,
            percent = 0,
            remainingMl = settings.goalMl,
            goalReached = false,
            todayEntries = emptyList(),
            week = HydrationMath.last7Days(emptyList(), today, settings.goalMl),
            selectedDrink = DrinkType.WATER,
        )
    }
}

class HydrationViewModelFactory(
    private val repository: HydrationRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HydrationViewModel::class.java)) {
            return HydrationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
