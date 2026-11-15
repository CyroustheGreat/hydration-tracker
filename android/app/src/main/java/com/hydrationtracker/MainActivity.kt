package com.hydrationtracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hydrationtracker.reminder.ReminderScheduler
import com.hydrationtracker.ui.HomeScreen
import com.hydrationtracker.ui.HydrationViewModel
import com.hydrationtracker.ui.HydrationViewModelFactory
import com.hydrationtracker.ui.SettingsScreen
import com.hydrationtracker.ui.theme.HydrationTheme
import com.hydrationtracker.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    private val viewModel: HydrationViewModel by viewModels {
        HydrationViewModelFactory((application as HydrationApplication).repository)
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Settings reads the current grant the next time it resumes. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HydrationTheme {
                HydrationApp(
                    viewModel = viewModel,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WidgetUpdater.updateAll(this)
    }
}

@Composable
private fun HydrationApp(
    viewModel: HydrationViewModel,
    onRequestNotifications: () -> Unit,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }

    val settings = state.settings
    androidx.compose.runtime.LaunchedEffect(
        state.ready,
        settings.remindersEnabled,
        settings.reminderIntervalMinutes,
        settings.reminderStartMinuteOfDay,
        settings.reminderEndMinuteOfDay,
    ) {
        if (state.ready) {
            ReminderScheduler.apply(context, settings)
        }
    }

    if (!state.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onBack = { showSettings = false },
            onChange = viewModel::updateSettings,
            onRequestNotifications = onRequestNotifications,
        )
    } else {
        HomeScreen(
            state = state,
            onSelectDrink = viewModel::selectDrink,
            onAdd = viewModel::add,
            onDelete = viewModel::delete,
            onClearToday = viewModel::clearToday,
            onOpenSettings = { showSettings = true },
        )
    }
}
