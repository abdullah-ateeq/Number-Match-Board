package com.example.numbermatchboard.ui

import com.example.numbermatchboard.data.InMemoryKeyValueStore
import com.example.numbermatchboard.data.SettingsRepository
import com.example.numbermatchboard.data.StatisticsRepository
import com.example.numbermatchboard.game.ai.NumberMatchCpuStrategy
import com.example.numbermatchboard.game.logic.NumberBoardGenerator
import com.example.numbermatchboard.game.logic.NumberMatchGameEngine
import com.example.numbermatchboard.game.logic.findInvalidPairs
import com.example.numbermatchboard.game.logic.findValidPairs
import com.example.numbermatchboard.game.model.Difficulty
import com.example.numbermatchboard.game.model.GameOutcome
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class NumberMatchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var settings: SettingsRepository
    private lateinit var statistics: StatisticsRepository
    private lateinit var viewModel: NumberMatchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = SettingsRepository(InMemoryKeyValueStore())
        statistics = StatisticsRepository(InMemoryKeyValueStore())
        viewModel = NumberMatchViewModel(
            engine = NumberMatchGameEngine(NumberBoardGenerator(Random(21))),
            cpuStrategy = NumberMatchCpuStrategy(Random(22)),
            settingsRepository = settings,
            statisticsRepository = statistics,
            random = Random(23)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val game get() = viewModel.uiState.value.game

    private fun validPair() = findValidPairs(game.availableCells).first()
    private fun invalidPair() = findInvalidPairs(game.availableCells).first()

    @Test
    fun validPlayerPairIsCapturedAfterAnimationAndPlayerKeepsTurn() = runTest(dispatcher) {
        val (a, b) = validPair()
        viewModel.selectCell(a.id)
        viewModel.selectCell(b.id)
        assertEquals(GamePhase.RESOLVING, game.phase)
        advanceUntilIdle()
        assertEquals(1, game.playerPairs)
        assertEquals(GamePhase.PLAYER_TURN, game.phase)
        assertEquals(17, game.pairsRemaining)
    }

    @Test
    fun rapidTapsDuringResolutionAreIgnoredAndScoreOnce() = runTest(dispatcher) {
        val (a, b) = validPair()
        viewModel.selectCell(a.id)
        viewModel.selectCell(b.id)
        val other = game.availableCells.first { it.id != a.id && it.id != b.id }
        repeat(5) { viewModel.selectCell(other.id) }
        viewModel.selectCell(a.id)
        assertEquals(listOf(a.id, b.id), game.selectedCellIds)
        advanceUntilIdle()
        assertEquals(1, game.playerPairs)
        assertTrue(game.playerScore in 11..12)
    }

    @Test
    fun sameCellShowsMessage() = runTest(dispatcher) {
        val events = mutableListOf<GameEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it } }
        runCurrent()
        val cell = game.availableCells.first()
        viewModel.selectCell(cell.id)
        viewModel.selectCell(cell.id)
        runCurrent()
        assertTrue(events.contains(GameEvent.Message("Choose another number.")))
        assertEquals(listOf(cell.id), game.selectedCellIds)
    }

    @Test
    fun matchHintsFollowSetting() = runTest(dispatcher) {
        val cell = game.availableCells.first()
        viewModel.selectCell(cell.id)
        val hints = viewModel.uiState.value.hintCellIds
        assertTrue(hints.isNotEmpty())
        assertTrue(hints.all { id ->
            val other = game.cell(id)!!
            other.value == cell.value || other.value + cell.value == 10
        })
        viewModel.updateSettings { it.copy(showMatchHints = false) }
        assertTrue(viewModel.uiState.value.hintCellIds.isEmpty())
    }

    @Test
    fun mismatchHandsTurnToCpuWhichPlaysAndBlocksInput() = runTest(dispatcher) {
        settings.update { it.copy(difficulty = Difficulty.HARD) }
        val (a, b) = invalidPair()
        viewModel.selectCell(a.id)
        viewModel.selectCell(b.id)
        advanceTimeBy(700)
        runCurrent()
        assertEquals(Side.CPU, game.currentSide)
        assertFalse(viewModel.uiState.value.acceptsPlayerInput)

        // Player taps are ignored during the CPU turn.
        val before = game
        viewModel.selectCell(game.availableCells.first().id)
        assertEquals(before.selectedCellIds, game.selectedCellIds)

        advanceUntilIdle()
        // Hard never misses, so it clears the board.
        assertEquals(GamePhase.GAME_OVER, game.phase)
        assertEquals(18, game.cpuPairs)
        val summary = viewModel.uiState.value.summary
        assertNotNull(summary)
        assertEquals(GameOutcome.CPU_WIN, summary!!.outcome)
        assertEquals(1, statistics.statistics.value.losses)
        assertEquals(1, statistics.statistics.value.gamesPlayed)
    }

    @Test
    fun resultNavigationIsConsumedOnce() = runTest(dispatcher) {
        playWholeBoardAsPlayer()
        val id = viewModel.uiState.value.gameId
        assertTrue(viewModel.consumeResultNavigation(id))
        assertFalse(viewModel.consumeResultNavigation(id))
    }

    @Test
    fun perfectGameRecordsWinBestScoreAndStreak() = runTest(dispatcher) {
        playWholeBoardAsPlayer()
        val summary = viewModel.uiState.value.summary!!
        assertTrue(summary.isPerfectMatch)
        assertEquals(18, summary.playerPairs)
        val stats = statistics.statistics.value
        assertEquals(1, stats.wins)
        assertEquals(1, stats.currentStreak)
        assertEquals(18, stats.bestPairs)
        assertEquals(game.playerScore, stats.bestScore)
        assertEquals(18, stats.totalIdenticalMatches + stats.totalSumToTenMatches)
    }

    @Test
    fun restartCreatesFreshBoardAndCancelsCpu() = runTest(dispatcher) {
        val (a, b) = invalidPair()
        viewModel.selectCell(a.id)
        viewModel.selectCell(b.id)
        advanceTimeBy(700)
        runCurrent()
        assertEquals(Side.CPU, game.currentSide)
        val oldId = viewModel.uiState.value.gameId
        viewModel.restart()
        advanceUntilIdle()
        assertEquals(oldId + 1, viewModel.uiState.value.gameId)
        assertEquals(GamePhase.PLAYER_TURN, game.phase)
        assertEquals(0, game.cpuPairs + game.playerPairs)
        assertEquals(36, game.cells.size)
        assertEquals(0, statistics.statistics.value.gamesPlayed)
    }

    private fun TestScope.playWholeBoardAsPlayer() {
        while (game.phase != GamePhase.GAME_OVER) {
            val (a, b) = validPair()
            viewModel.selectCell(a.id)
            viewModel.selectCell(b.id)
            advanceUntilIdle()
        }
    }
}
