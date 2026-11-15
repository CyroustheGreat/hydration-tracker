package com.hydrationtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hydrationtracker.R
import com.hydrationtracker.domain.DrinkType
import com.hydrationtracker.domain.TrackerSettings
import com.hydrationtracker.domain.Volume
import com.hydrationtracker.ui.theme.GoalGreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectDrink: (DrinkType) -> Unit,
    onAdd: (Int) -> Unit,
    onDelete: (Long) -> Unit,
    onClearToday: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val unit = state.settings.unit
    var custom by rememberSaveable { mutableStateOf("") }
    var customError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val dateLabel = DateTimeFormatter.ofPattern("EEEE, MMMM d").format(state.today)

    fun submitCustom() {
        val parsed = Volume.parseToMl(custom, unit)
        if (parsed == null || parsed !in TrackerSettings.MIN_CUSTOM_ML..TrackerSettings.MAX_CUSTOM_ML) {
            customError = "Enter ${Volume.formatAmount(TrackerSettings.MIN_CUSTOM_ML, unit)} to ${Volume.formatAmount(TrackerSettings.MAX_CUSTOM_ML, unit)}"
            return
        }
        customError = null
        custom = ""
        onAdd(parsed)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = dateLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionCard {
                ProgressRing(
                    percent = state.percent,
                    totalText = Volume.formatAmount(state.totalMl, unit),
                    goalText = stringResource(
                        R.string.percent_of_goal,
                        state.percent,
                        Volume.formatAmount(state.settings.goalMl, unit),
                    ),
                )
                if (state.goalReached) {
                    Text(
                        text = stringResource(R.string.goal_reached),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = GoalGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val amount = Volume.formatAmount(state.remainingMl, unit)
                    Text(
                        text = buildAnnotatedString {
                            append("You still need ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                                append(amount)
                            }
                            append(" to reach your goal.")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            SectionCard {
                Text("Add a drink", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DrinkType.entries.forEach { drink ->
                        FilterChip(
                            selected = drink == state.selectedDrink,
                            onClick = { onSelectDrink(drink) },
                            label = { Text(drink.label) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onAdd(state.settings.glassMl) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("+ Glass · ${Volume.formatAmount(state.settings.glassMl, unit)}", textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { onAdd(state.settings.bottleMl) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("+ Bottle · ${Volume.formatAmount(state.settings.bottleMl, unit)}", textAlign = TextAlign.Center)
                    }
                }
                TrackerSettings.QUICK_ADD_ML.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { amount ->
                            FilledTonalButton(
                                onClick = { onAdd(amount) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("+ ${Volume.formatAmount(amount, unit)}")
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = {
                            custom = it
                            customError = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = customError != null,
                        label = { Text("Custom (${if (unit == com.hydrationtracker.domain.VolumeUnit.ML) "ml" else "oz"})") },
                        supportingText = customError?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitCustom() }),
                    )
                    Button(
                        onClick = { submitCustom() },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Add")
                    }
                }
            }

            SectionCard {
                Text("Today's log", style = MaterialTheme.typography.titleMedium)
                if (state.todayEntries.isEmpty()) {
                    Text(
                        text = "No drinks logged yet today. Tap Glass to start.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    state.todayEntries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${entry.drink.label} — ${Volume.formatAmount(entry.ml, unit)}",
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    formatEntryTime(entry.loggedAtEpochMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDelete(entry.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete ${entry.drink.label}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { confirmClear = true }) {
                    Text("Clear today", color = MaterialTheme.colorScheme.error)
                }
            }

            SectionCard {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Stat(Volume.formatAmount(state.week.averageMl, unit), "7-day average", Modifier.weight(1f))
                    Stat("${state.week.daysAtGoal} / 7", "days at goal", Modifier.weight(1f))
                    Stat(Volume.formatAmount(state.week.bestMl, unit), "best day", Modifier.weight(1f))
                }
                WeekChart(stats = state.week, goalMl = state.settings.goalMl, unit = unit)
            }

            Text(
                text = "Saved on this device. A new day starts at local midnight.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear today?") },
            text = { Text("Clear all of today's drinks?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearToday()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

private fun formatEntryTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time)
}
