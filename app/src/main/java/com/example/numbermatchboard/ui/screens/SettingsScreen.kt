package com.example.numbermatchboard.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.numbermatchboard.data.GameSettings
import com.example.numbermatchboard.data.ThemeMode
import com.example.numbermatchboard.game.model.Difficulty
import com.example.numbermatchboard.ui.NumberMatchViewModel
import kotlinx.coroutines.launch

private enum class ResetTarget { STATISTICS, SETTINGS }

@Composable
fun SettingsScreen(viewModel: NumberMatchViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingReset by rememberSaveable { mutableStateOf<ResetTarget?>(null) }
    val update: ((GameSettings) -> GameSettings) -> Unit = viewModel::updateSettings

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Text(
                    "SETTINGS",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(20.dp))

                SettingsSection("APPEARANCE") {
                    ChipRow(
                        title = "Theme",
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        label = { it.label },
                        onSelect = { mode -> update { it.copy(themeMode = mode) } }
                    )
                    SwitchRow(
                        title = "Dynamic Color",
                        subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Use colors from your wallpaper"
                        } else {
                            "Requires Android 12 or newer"
                        },
                        checked = settings.dynamicColor,
                        onChange = { value -> update { it.copy(dynamicColor = value) } }
                    )
                    SwitchRow(
                        title = "Animations",
                        checked = settings.animations,
                        onChange = { value -> update { it.copy(animations = value) } }
                    )
                }

                SettingsSection("GAMEPLAY") {
                    ChipRow(
                        title = "CPU Difficulty",
                        options = Difficulty.entries,
                        selected = settings.difficulty,
                        label = { it.label },
                        onSelect = { difficulty -> update { it.copy(difficulty = difficulty) } }
                    )
                    SwitchRow(
                        title = "Show Match Hints",
                        subtitle = "Outline numbers that match your selection",
                        checked = settings.showMatchHints,
                        onChange = { value -> update { it.copy(showMatchHints = value) } }
                    )
                    SwitchRow(
                        title = "Show Coordinates",
                        subtitle = "Row letters and column numbers",
                        checked = settings.showCoordinates,
                        onChange = { value -> update { it.copy(showCoordinates = value) } }
                    )
                    SwitchRow(
                        title = "Confirm Restart",
                        checked = settings.confirmRestart,
                        onChange = { value -> update { it.copy(confirmRestart = value) } }
                    )
                }

                SettingsSection("FEEDBACK") {
                    SwitchRow(
                        title = "Sound",
                        checked = settings.sound,
                        onChange = { value -> update { it.copy(sound = value) } }
                    )
                    SwitchRow(
                        title = "Haptic Feedback",
                        checked = settings.haptics,
                        onChange = { value -> update { it.copy(haptics = value) } }
                    )
                }

                SettingsSection("DATA") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { pendingReset = ResetTarget.STATISTICS },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Reset Statistics") }
                        OutlinedButton(
                            onClick = { pendingReset = ResetTarget.SETTINGS },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Reset Settings") }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    pendingReset?.let { target ->
        val isStats = target == ResetTarget.STATISTICS
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text(if (isStats) "Reset statistics?" else "Reset settings?") },
            text = {
                Text(
                    if (isStats) "Games, wins, best pairs, best score and streaks will be cleared."
                    else "All settings will return to their defaults."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingReset = null
                    if (isStats) viewModel.resetStatistics() else viewModel.resetSettings()
                    scope.launch {
                        snackbarHostState.showSnackbar(if (isStats) "Statistics reset" else "Settings reset")
                    }
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReset = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
