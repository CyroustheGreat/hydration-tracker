package com.hydrationtracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hydrationtracker.R
import com.hydrationtracker.domain.TrackerSettings
import com.hydrationtracker.domain.Volume
import com.hydrationtracker.domain.VolumeUnit
import com.hydrationtracker.reminder.ReminderScheduler
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: TrackerSettings,
    onBack: () -> Unit,
    onChange: ((TrackerSettings) -> TrackerSettings) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var exactAllowed by remember { mutableStateOf(ReminderScheduler.canUseExactAlarms(context)) }
    val unitLabel = if (settings.unit == VolumeUnit.ML) "ml" else "oz"
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
                exactAllowed = ReminderScheduler.canUseExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                Text("Daily target", style = MaterialTheme.typography.titleMedium)
                AmountField(
                    label = "Daily goal ($unitLabel)",
                    ml = settings.goalMl,
                    unit = settings.unit,
                    range = TrackerSettings.MIN_GOAL_ML..TrackerSettings.MAX_GOAL_ML,
                    modifier = Modifier.padding(top = 8.dp),
                    onCommit = { ml -> onChange { it.copy(goalMl = ml) } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountField(
                        label = "Glass ($unitLabel)",
                        ml = settings.glassMl,
                        unit = settings.unit,
                        range = TrackerSettings.MIN_GLASS_ML..TrackerSettings.MAX_GLASS_ML,
                        modifier = Modifier.weight(1f),
                        onCommit = { ml -> onChange { it.copy(glassMl = ml) } },
                    )
                    AmountField(
                        label = "Bottle ($unitLabel)",
                        ml = settings.bottleMl,
                        unit = settings.unit,
                        range = TrackerSettings.MIN_BOTTLE_ML..TrackerSettings.MAX_BOTTLE_ML,
                        modifier = Modifier.weight(1f),
                        onCommit = { ml -> onChange { it.copy(bottleMl = ml) } },
                    )
                }
                ChoiceField(
                    label = "Units",
                    value = if (settings.unit == VolumeUnit.ML) "Millilitres (ml)" else "Fluid ounces (oz)",
                    options = listOf(
                        "Millilitres (ml)" to { onChange { it.copy(unit = VolumeUnit.ML) } },
                        "Fluid ounces (oz)" to { onChange { it.copy(unit = VolumeUnit.OZ) } },
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Amounts are stored in millilitres. Ounces are US fluid ounces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Drink reminders", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Nudges during the hours you choose. They come back after a reboot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.remindersEnabled,
                        onCheckedChange = { enabled ->
                            onChange { it.copy(remindersEnabled = enabled) }
                            if (enabled && !notificationsOn) onRequestNotifications()
                        },
                    )
                }
                if (settings.remindersEnabled) {
                    ChoiceField(
                        label = "Interval",
                        value = intervalLabel(settings.reminderIntervalMinutes),
                        options = TrackerSettings.REMINDER_INTERVALS_MIN.map { minutes ->
                            intervalLabel(minutes) to { onChange { it.copy(reminderIntervalMinutes = minutes) } }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onChange { it.copy(reminderStartMinuteOfDay = hour * 60 + minute) }
                                },
                                settings.reminderStartMinuteOfDay / 60,
                                settings.reminderStartMinuteOfDay % 60,
                                is24Hour,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Starts · ${formatMinute(settings.reminderStartMinuteOfDay, is24Hour)}")
                    }
                    OutlinedButton(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onChange { it.copy(reminderEndMinuteOfDay = hour * 60 + minute) }
                                },
                                settings.reminderEndMinuteOfDay / 60,
                                settings.reminderEndMinuteOfDay % 60,
                                is24Hour,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ends · ${formatMinute(settings.reminderEndMinuteOfDay, is24Hour)}")
                    }
                    Text(
                        "Same start and end means all day. If the end is earlier than the start, the window runs overnight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!notificationsOn) {
                        Text(
                            "Notifications are off, so reminders can't appear yet.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = onRequestNotifications) { Text("Allow notifications") }
                        TextButton(onClick = { openAppNotificationSettings(context) }) {
                            Text("Open notification settings")
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
                        Text(
                            "Exact alarms are off. Reminders still fire, but Android may deliver them a little late.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { openExactAlarmSettings(context) }) {
                            Text("Allow exact alarms")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    ml: Int,
    unit: VolumeUnit,
    range: IntRange,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(ml, unit) { mutableStateOf(Volume.editString(ml, unit)) }
    var error by remember(ml, unit) { mutableStateOf<String?>(null) }
    var touched by remember { mutableStateOf(false) }
    val hint = "Between ${Volume.formatAmount(range.first, unit)} and ${Volume.formatAmount(range.last, unit)}"

    fun commit() {
        val parsed = Volume.parseToMl(text, unit)
        if (parsed == null || parsed !in range) {
            error = hint
            return
        }
        error = null
        text = Volume.editString(parsed, unit)
        if (parsed != ml) onCommit(parsed)
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            error = null
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (state.isFocused) {
                    touched = true
                } else if (touched) {
                    touched = false
                    commit()
                }
            },
        label = { Text(label) },
        supportingText = { Text(error ?: hint) },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
    )
}

@Composable
private fun ChoiceField(
    label: String,
    value: String,
    options: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (option, select) ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        select()
                    },
                )
            }
        }
    }
}

private fun intervalLabel(minutes: Int): String = when (minutes) {
    30 -> "Every 30 minutes"
    45 -> "Every 45 minutes"
    60 -> "Every hour"
    90 -> "Every 1 hour 30 minutes"
    120 -> "Every 2 hours"
    180 -> "Every 3 hours"
    else -> "Every $minutes minutes"
}

private fun formatMinute(minuteOfDay: Int, is24Hour: Boolean): String {
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern).format(time)
}

private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

private fun openExactAlarmSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
