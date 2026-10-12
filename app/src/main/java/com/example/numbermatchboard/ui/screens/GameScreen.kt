package com.example.numbermatchboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.numbermatchboard.data.GameSettings
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.ui.GameEvent
import com.example.numbermatchboard.ui.GameUiState
import com.example.numbermatchboard.ui.NumberMatchViewModel
import com.example.numbermatchboard.ui.components.LastEventLine
import com.example.numbermatchboard.ui.components.NumberBoard
import com.example.numbermatchboard.ui.components.RuleCard
import com.example.numbermatchboard.ui.components.Scoreboard
import com.example.numbermatchboard.ui.components.TurnIndicator
import com.example.numbermatchboard.util.rememberGameFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: NumberMatchViewModel,
    onExit: () -> Unit,
    onGameFinished: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentSettings by rememberUpdatedState(settings)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val feedback = rememberGameFeedback()
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.Feedback ->
                    feedback.play(event.kind, currentSettings.sound, currentSettings.haptics)
                is GameEvent.Message -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.text, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    LaunchedEffect(ui.gameId, ui.game.phase) {
        if (ui.game.phase == GamePhase.GAME_OVER) {
            delay(if (settings.animations) 900 else 300)
            if (viewModel.consumeResultNavigation(ui.gameId)) onGameFinished()
        }
    }

    val requestRestart = {
        if (settings.confirmRestart) showRestartDialog = true else viewModel.restart()
    }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight || maxWidth >= 600.dp
            Column(Modifier.fillMaxSize()) {
                GameHeader(
                    difficultyLabel = settings.difficulty.label,
                    restartEnabled = ui.game.phase != GamePhase.GAME_OVER,
                    onBack = onExit,
                    onRestart = requestRestart
                )
                if (wide) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        GameBoard(ui, settings, viewModel, Modifier.weight(1f).fillMaxHeight())
                        Column(
                            Modifier
                                .widthIn(min = 280.dp, max = 380.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                        ) {
                            GameScoreboard(ui, settings)
                            RuleCard()
                            TurnIndicator(
                                phase = ui.game.phase,
                                currentSide = ui.game.currentSide,
                                selectedCount = ui.game.selectedCellIds.size,
                                cpuThinking = ui.cpuThinking,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            LastEventLine(ui.game.pendingPair, ui.game.lastEvent)
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GameScoreboard(ui, settings)
                        Spacer(Modifier.height(10.dp))
                        RuleCard()
                        Spacer(Modifier.height(10.dp))
                        GameBoard(ui, settings, viewModel, Modifier.weight(1f).fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        TurnIndicator(
                            phase = ui.game.phase,
                            currentSide = ui.game.currentSide,
                            selectedCount = ui.game.selectedCellIds.size,
                            cpuThinking = ui.cpuThinking
                        )
                        Spacer(Modifier.height(8.dp))
                        LastEventLine(ui.game.pendingPair, ui.game.lastEvent)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart this board?") },
            text = { Text("A new randomized board will be generated. This game will not be recorded.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    viewModel.restart()
                }) { Text("Restart") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GameHeader(
    difficultyLabel: String,
    restartEnabled: Boolean,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NUMBER MATCH BOARD",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "vs CPU · ${difficultyLabel.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestart, enabled = restartEnabled) {
            Icon(Icons.Filled.Refresh, contentDescription = "Restart board")
        }
    }
}

@Composable
private fun GameScoreboard(ui: GameUiState, settings: GameSettings) {
    Scoreboard(
        playerPairs = ui.game.playerPairs,
        playerScore = ui.game.playerScore,
        cpuPairs = ui.game.cpuPairs,
        cpuScore = ui.game.cpuScore,
        pairsRemaining = ui.game.pairsRemaining,
        currentSide = ui.game.currentSide,
        gameOver = ui.game.phase == GamePhase.GAME_OVER,
        animationsEnabled = settings.animations
    )
}

@Composable
private fun GameBoard(
    ui: GameUiState,
    settings: GameSettings,
    viewModel: NumberMatchViewModel,
    modifier: Modifier
) {
    NumberBoard(
        cells = ui.game.cells,
        currentSide = ui.game.currentSide,
        hintCellIds = ui.hintCellIds,
        pendingPair = ui.game.pendingPair,
        showCoordinates = settings.showCoordinates,
        animationsEnabled = settings.animations,
        inputEnabled = ui.acceptsPlayerInput,
        onCellClick = viewModel::selectCell,
        modifier = modifier
    )
}
