package com.example.numbermatchboard.game

import com.example.numbermatchboard.game.logic.NumberBoardGenerator
import com.example.numbermatchboard.game.logic.NumberMatchGameEngine
import com.example.numbermatchboard.game.logic.SelectionResult
import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.GameOutcome
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.NumberMatchGameState
import com.example.numbermatchboard.game.model.PairType
import com.example.numbermatchboard.game.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NumberMatchGameEngineTest {

    private val engine = NumberMatchGameEngine(NumberBoardGenerator(Random(3)))

    private fun play(state: NumberMatchGameState, a: Int, b: Int) = engine.playPair(state, a, b)!!

    @Test
    fun identicalPairIsCapturedAndPlayerKeepsTurn() {
        val start = engine.initialState(boardWithA1B4(7, 7))
        val result = play(start, A1, B4)
        val state = result.state
        assertEquals(PairType.IDENTICAL, result.event.pairType)
        assertEquals(1, state.playerPairs)
        assertEquals(12, state.playerScore)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(Side.PLAYER, state.currentSide)
        assertEquals(CellState.PLAYER_CAPTURED, state.cell(A1)!!.state)
        assertEquals(CellState.PLAYER_CAPTURED, state.cell(B4)!!.state)
        assertEquals(1, state.playerIdenticalPairs)
    }

    @Test
    fun sumToTenPairIsCapturedAndPlayerKeepsTurn() {
        val state = play(engine.initialState(boardWithA1B4(4, 6)), A1, B4).state
        assertEquals(PairType.SUM_TO_TEN, state.lastPairType)
        assertEquals(1, state.playerPairs)
        assertEquals(11, state.playerScore)
        assertEquals(1, state.playerSumToTenPairs)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
    }

    @Test
    fun invalidPairPassesTurnToCpuWithoutCapture() {
        val result = play(engine.initialState(boardWithA1B4(4, 7)), A1, B4)
        val state = result.state
        assertNull(result.event.pairType)
        assertEquals(0, state.playerPairs)
        assertEquals(0, state.playerScore)
        assertEquals(GamePhase.CPU_TURN, state.phase)
        assertEquals(Side.CPU, state.currentSide)
        assertEquals(CellState.ACTIVE, state.cell(A1)!!.state)
        assertEquals(CellState.ACTIVE, state.cell(B4)!!.state)
        assertEquals(false, state.lastMatch)
        assertTrue(state.selectedCellIds.isEmpty())
    }

    @Test
    fun fivePlusFiveScoresAsIdentical() {
        val state = play(engine.initialState(boardWithA1B4(5, 5)), A1, B4).state
        assertEquals(PairType.IDENTICAL, state.lastPairType)
        assertEquals(12, state.playerScore)
        assertEquals(1, state.playerIdenticalPairs)
        assertEquals(0, state.playerSumToTenPairs)
    }

    @Test
    fun selectingSameCellTwiceIsRejected() {
        val first = engine.selectCell(engine.initialState(boardWithA1B4(4, 6)), A1) as SelectionResult.FirstSelected
        assertSame(SelectionResult.SameCell, engine.selectCell(first.state, A1))
        assertEquals(listOf(A1), first.state.selectedCellIds)
        assertEquals(CellState.SELECTED, first.state.cell(A1)!!.state)
    }

    @Test
    fun tappingCapturedCellIsIgnoredAndDoesNotConsumeTurn() {
        val state = play(engine.initialState(boardWithA1B4(7, 7)), A1, B4).state
        assertSame(SelectionResult.Ignored, engine.selectCell(state, A1))
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(1, state.turnCount)
    }

    @Test
    fun selectionsAreIgnoredWhileResolving() {
        val start = engine.initialState(boardWithA1B4(4, 6))
        val first = engine.selectCell(start, A1) as SelectionResult.FirstSelected
        val pair = engine.selectCell(first.state, B4) as SelectionResult.PairSelected
        assertEquals(GamePhase.RESOLVING, pair.state.phase)
        assertSame(SelectionResult.Ignored, engine.selectCell(pair.state, 1))
        // Resolving twice cannot double-score.
        val resolved = engine.resolvePendingPair(pair.state)!!.state
        assertNull(engine.resolvePendingPair(resolved))
        assertEquals(11, resolved.playerScore)
    }

    @Test
    fun cpuCapturesCountForCpuAndCpuKeepsTurn() {
        val toCpu = play(engine.initialState(cellsOf(4, 7, 2, 8, 3, 3)), 0, 1).state
        assertEquals(Side.CPU, toCpu.currentSide)
        val cpuState = play(toCpu, 2, 3).state
        assertEquals(1, cpuState.cpuPairs)
        assertEquals(11, cpuState.cpuScore)
        assertEquals(0, cpuState.playerPairs)
        assertEquals(GamePhase.CPU_TURN, cpuState.phase)
        assertEquals(CellState.CPU_CAPTURED, cpuState.cell(2)!!.state)
        assertEquals(Side.CPU, cpuState.lastCapturedBy)
    }

    @Test
    fun turnSwitchesBackToPlayerWhenCpuMisses() {
        val toCpu = play(engine.initialState(cellsOf(4, 7, 2, 8, 3, 3)), 0, 1).state
        val back = play(toCpu, 0, 2).state
        assertEquals(GamePhase.PLAYER_TURN, back.phase)
        assertEquals(Side.PLAYER, back.currentSide)
        assertEquals(2, back.turnCount)
    }

    @Test
    fun pairsRemainingTracksAvailableCells() {
        val start = engine.initialState(cellsOf(1, 9, 2, 2, 5, 5))
        assertEquals(3, start.pairsRemaining)
        val next = play(start, 0, 1).state
        assertEquals(2, next.pairsRemaining)
        assertEquals(next.availableCells.size / 2, next.pairsRemaining)
    }

    @Test
    fun capturingFinalPairEndsTheGame() {
        var state = engine.initialState(cellsOf(1, 9, 2, 2))
        state = play(state, 0, 1).state
        state = play(state, 2, 3).state
        assertEquals(0, state.pairsRemaining)
        assertEquals(GamePhase.GAME_OVER, state.phase)
        assertSame(SelectionResult.Ignored, engine.selectCell(state, 0))
    }

    @Test
    fun playerWinsWithMorePairs() {
        // Player takes one, misses, CPU takes the other... then player wins 2–1.
        var state = engine.initialState(cellsOf(1, 9, 2, 3, 2, 3, 4, 4))
        state = play(state, 0, 1).state // player 1
        state = play(state, 2, 3).state // player miss
        state = play(state, 2, 4).state // cpu 1
        state = play(state, 3, 6).state // cpu miss
        state = play(state, 3, 5).state // player 2
        state = play(state, 6, 7).state // player 3
        assertEquals(GameOutcome.PLAYER_WIN, state.outcome)
        assertEquals(3, state.playerPairs)
        assertEquals(1, state.cpuPairs)
    }

    @Test
    fun cpuWinsWithMorePairs() {
        var state = engine.initialState(cellsOf(1, 2, 1, 2, 3, 3))
        state = play(state, 0, 1).state // player miss
        state = play(state, 0, 2).state
        state = play(state, 1, 3).state
        state = play(state, 4, 5).state
        assertEquals(GameOutcome.CPU_WIN, state.outcome)
        assertEquals(3, state.cpuPairs)
    }

    @Test
    fun winnerIsDecidedByPairsNotScore() {
        val state = NumberMatchGameState(
            cells = cellsOf(1, 1).map { it.copy(state = CellState.PLAYER_CAPTURED) },
            phase = GamePhase.GAME_OVER,
            currentSide = Side.PLAYER,
            playerPairs = 5,
            cpuPairs = 6,
            playerScore = 100,
            cpuScore = 66,
            pairsRemaining = 0
        )
        assertEquals(GameOutcome.CPU_WIN, state.outcome)
    }

    @Test
    fun equalPairsIsADraw() {
        val state = NumberMatchGameState(
            cells = engine.newGame().cells,
            phase = GamePhase.GAME_OVER,
            currentSide = Side.CPU,
            playerPairs = 9,
            cpuPairs = 9,
            playerScore = 108,
            cpuScore = 99,
            pairsRemaining = 0
        )
        assertEquals(GameOutcome.DRAW, state.outcome)
        assertFalse(state.isPerfectMatch)
    }

    @Test
    fun capturingAllEighteenPairsIsPerfect() {
        var state = engine.newGame()
        while (state.phase != GamePhase.GAME_OVER) {
            val cells = state.availableCells
            val first = cells.first()
            val second = cells.drop(1).first {
                com.example.numbermatchboard.game.logic.isValidPair(first.value, it.value)
            }
            state = play(state, first.id, second.id).state
        }
        assertEquals(18, state.playerPairs)
        assertEquals(0, state.cpuPairs)
        assertTrue(state.isPerfectMatch)
        assertEquals(GameOutcome.PLAYER_WIN, state.outcome)
    }

    @Test
    fun matchCandidatesListsIdenticalAndComplementNumbers() {
        val state = engine.initialState(cellsOf(4, 6, 4, 2, 7, 8))
        assertEquals(setOf(1, 2), engine.matchCandidates(state, 0))
    }

    @Test
    fun newGameProducesFreshRandomBoard() {
        val a = engine.newGame()
        val b = engine.newGame()
        assertEquals(36, b.cells.size)
        assertEquals(18, b.pairsRemaining)
        assertEquals(GamePhase.PLAYER_TURN, b.phase)
        assertEquals(0, b.playerPairs + b.cpuPairs)
        assertNotEquals(a.cells.map { it.value }, b.cells.map { it.value })
    }
}
