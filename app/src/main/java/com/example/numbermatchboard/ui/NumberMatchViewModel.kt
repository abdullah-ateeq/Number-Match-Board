package com.example.numbermatchboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.numbermatchboard.data.FinishedGame
import com.example.numbermatchboard.data.GameSettings
import com.example.numbermatchboard.data.GameStatistics
import com.example.numbermatchboard.data.SettingsRepository
import com.example.numbermatchboard.data.StatisticsRepository
import com.example.numbermatchboard.game.ai.NumberMatchCpuStrategy
import com.example.numbermatchboard.game.logic.NumberMatchGameEngine
import com.example.numbermatchboard.game.logic.SelectionResult
import com.example.numbermatchboard.game.model.GameOutcome
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.GameSummary
import com.example.numbermatchboard.game.model.MatchEvent
import com.example.numbermatchboard.game.model.NumberMatchGameState
import com.example.numbermatchboard.game.model.PairType
import com.example.numbermatchboard.game.model.Side
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameUiState(
    val gameId: Int,
    val game: NumberMatchGameState,
    val hintCellIds: Set<Int> = emptySet(),
    val cpuThinking: Boolean = false,
    val summary: GameSummary? = null
) {
    val acceptsPlayerInput: Boolean
        get() = game.phase == GamePhase.PLAYER_TURN && game.currentSide == Side.PLAYER
}

enum class FeedbackKind {
    SELECT,
    IDENTICAL_MATCH,
    SUM_TO_TEN_MATCH,
    MISMATCH,
    CPU_ACTION,
    VICTORY,
    DEFEAT,
    DRAW
}

sealed interface GameEvent {
    data class Feedback(val kind: FeedbackKind) : GameEvent
    data class Message(val text: String) : GameEvent
}

/** Delays for animations and CPU pacing. Animations-off uses the reduced values. */
data class GameTiming(
    val resolveMillis: Long = 620,
    val reducedResolveMillis: Long = 260,
    val cpuThinkMinMillis: Long = 300,
    val cpuThinkMaxMillis: Long = 700,
    val cpuSecondPickMillis: Long = 280
)

class NumberMatchViewModel(
    private val engine: NumberMatchGameEngine,
    private val cpuStrategy: NumberMatchCpuStrategy,
    private val settingsRepository: SettingsRepository,
    private val statisticsRepository: StatisticsRepository,
    private val timing: GameTiming = GameTiming(),
    private val random: Random = Random.Default
) : ViewModel() {

    val settings: StateFlow<GameSettings> = settingsRepository.settings
    val statistics: StateFlow<GameStatistics> = statisticsRepository.statistics

    private val _uiState = MutableStateFlow(GameUiState(gameId = 0, game = engine.newGame()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /** Resolution and CPU turns run in this single job; never more than one at a time. */
    private var turnJob: Job? = null
    private var recordedGameId = -1
    private var resultShownGameId = -1

    fun startNewGame() {
        turnJob?.cancel()
        turnJob = null
        val nextId = _uiState.value.gameId + 1
        _uiState.value = GameUiState(gameId = nextId, game = engine.newGame())
    }

    fun restart() = startNewGame()

    fun selectCell(cellId: Int) {
        val ui = _uiState.value
        if (!ui.acceptsPlayerInput || turnJob?.isActive == true) return

        when (val result = engine.selectCell(ui.game, cellId)) {
            SelectionResult.Ignored -> Unit
            SelectionResult.SameCell -> _events.tryEmit(GameEvent.Message("Choose another number."))
            is SelectionResult.FirstSelected -> {
                setGame(result.state)
                _events.tryEmit(GameEvent.Feedback(FeedbackKind.SELECT))
            }
            is SelectionResult.PairSelected -> {
                setGame(result.state)
                _events.tryEmit(GameEvent.Feedback(FeedbackKind.SELECT))
                val gameId = ui.gameId
                turnJob = viewModelScope.launch {
                    resolvePending(gameId)
                    runCpuTurns(gameId)
                }
            }
        }
    }

    /** Returns true exactly once per finished game so the UI navigates to the result only once. */
    fun consumeResultNavigation(gameId: Int): Boolean {
        val ui = _uiState.value
        if (ui.gameId != gameId || ui.game.phase != GamePhase.GAME_OVER || resultShownGameId == gameId) return false
        resultShownGameId = gameId
        return true
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        settingsRepository.update(transform)
        setGame(_uiState.value.game)
    }

    fun resetSettings() {
        settingsRepository.reset()
        setGame(_uiState.value.game)
    }

    fun resetStatistics() = statisticsRepository.reset()

    private suspend fun resolvePending(gameId: Int) {
        delay(if (settings.value.animations) timing.resolveMillis else timing.reducedResolveMillis)
        if (_uiState.value.gameId != gameId) return
        val resolution = engine.resolvePendingPair(_uiState.value.game) ?: return
        setGame(resolution.state)
        emitMatchFeedback(resolution.event)
        if (resolution.state.phase == GamePhase.GAME_OVER) onGameOver(gameId, resolution.state)
    }

    private suspend fun runCpuTurns(gameId: Int) {
        try {
            while (_uiState.value.gameId == gameId && _uiState.value.game.phase == GamePhase.CPU_TURN) {
                _uiState.update { it.copy(cpuThinking = true) }
                delay(random.nextLong(timing.cpuThinkMinMillis, timing.cpuThinkMaxMillis + 1))

                val pair = cpuStrategy.choosePair(_uiState.value.game, settings.value.difficulty) ?: return
                val first = engine.selectCell(_uiState.value.game, pair.first.id)
                    as? SelectionResult.FirstSelected ?: return
                setGame(first.state)
                _events.tryEmit(GameEvent.Feedback(FeedbackKind.CPU_ACTION))

                delay(timing.cpuSecondPickMillis)
                val second = engine.selectCell(_uiState.value.game, pair.second.id)
                    as? SelectionResult.PairSelected ?: return
                setGame(second.state)
                _uiState.update { it.copy(cpuThinking = false) }

                resolvePending(gameId)
            }
        } finally {
            _uiState.update { if (it.gameId == gameId) it.copy(cpuThinking = false) else it }
        }
    }

    private fun onGameOver(gameId: Int, game: NumberMatchGameState) {
        if (recordedGameId == gameId) return
        recordedGameId = gameId
        val outcome = game.outcome ?: return
        val previous = statisticsRepository.statistics.value
        val updated = statisticsRepository.record(
            FinishedGame(
                outcome = outcome,
                playerPairs = game.playerPairs,
                playerScore = game.playerScore,
                identicalMatches = game.playerIdenticalPairs,
                sumToTenMatches = game.playerSumToTenPairs
            )
        )
        val summary = GameSummary(
            gameId = gameId,
            outcome = outcome,
            isPerfectMatch = game.isPerfectMatch,
            playerPairs = game.playerPairs,
            cpuPairs = game.cpuPairs,
            playerScore = game.playerScore,
            cpuScore = game.cpuScore,
            playerIdenticalPairs = game.playerIdenticalPairs,
            playerSumToTenPairs = game.playerSumToTenPairs,
            difficulty = settings.value.difficulty,
            bestPairs = updated.bestPairs,
            isNewBestPairs = game.playerPairs > previous.bestPairs,
            isNewBestScore = game.playerScore > previous.bestScore
        )
        _uiState.update { it.copy(summary = summary, cpuThinking = false) }
        val kind = when (outcome) {
            GameOutcome.PLAYER_WIN -> FeedbackKind.VICTORY
            GameOutcome.CPU_WIN -> FeedbackKind.DEFEAT
            GameOutcome.DRAW -> FeedbackKind.DRAW
        }
        _events.tryEmit(GameEvent.Feedback(kind))
    }

    private fun emitMatchFeedback(event: MatchEvent) {
        val kind = when (event.pairType) {
            PairType.IDENTICAL -> FeedbackKind.IDENTICAL_MATCH
            PairType.SUM_TO_TEN -> FeedbackKind.SUM_TO_TEN_MATCH
            null -> FeedbackKind.MISMATCH
        }
        _events.tryEmit(GameEvent.Feedback(kind))
    }

    private fun setGame(game: NumberMatchGameState) {
        _uiState.update { it.copy(game = game, hintCellIds = computeHints(game)) }
    }

    /** Hints only appear for the player's own single selection, and only when enabled. */
    private fun computeHints(game: NumberMatchGameState): Set<Int> {
        if (!settings.value.showMatchHints) return emptySet()
        if (game.phase != GamePhase.PLAYER_TURN || game.selectedCellIds.size != 1) return emptySet()
        return engine.matchCandidates(game, game.selectedCellIds.first())
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val statisticsRepository: StatisticsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NumberMatchViewModel(
                engine = NumberMatchGameEngine(),
                cpuStrategy = NumberMatchCpuStrategy(),
                settingsRepository = settingsRepository,
                statisticsRepository = statisticsRepository
            ) as T
    }
}
