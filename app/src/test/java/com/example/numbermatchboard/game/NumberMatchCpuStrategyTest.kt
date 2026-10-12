package com.example.numbermatchboard.game

import com.example.numbermatchboard.game.ai.NumberMatchCpuStrategy
import com.example.numbermatchboard.game.logic.NumberBoardGenerator
import com.example.numbermatchboard.game.logic.NumberMatchGameEngine
import com.example.numbermatchboard.game.logic.classifyPair
import com.example.numbermatchboard.game.logic.isValidPair
import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.Difficulty
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.PairType
import com.example.numbermatchboard.game.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NumberMatchCpuStrategyTest {

    private val engine = NumberMatchGameEngine(NumberBoardGenerator(Random(5)))

    @Test
    fun findValidPairsSkipsCapturedCellsAndSameCell() {
        val cells = cellsOf(3, 7, 3, 5, 5, 9).mapIndexed { index, cell ->
            if (index == 1) cell.copy(state = CellState.PLAYER_CAPTURED) else cell
        }
        val pairs = NumberMatchCpuStrategy(Random(1)).findValidPairs(cells)
        assertEquals(2, pairs.size) // 3+3 and 5+5
        assertTrue(pairs.none { it.first.id == 1 || it.second.id == 1 })
        assertTrue(pairs.none { it.first.id == it.second.id })
    }

    @Test
    fun easyCpuIsMostlyValidButSometimesWrong() {
        val strategy = NumberMatchCpuStrategy(Random(10))
        val state = engine.newGame()
        var valid = 0
        repeat(1000) {
            val (a, b) = strategy.choosePair(state, Difficulty.EASY)!!
            if (isValidPair(a.value, b.value)) valid++
        }
        assertTrue("valid $valid", valid in 600..800)
    }

    @Test
    fun normalCpuUsuallyValidAndPrefersSimpleMatches() {
        val strategy = NumberMatchCpuStrategy(Random(11))
        val state = engine.newGame()
        var valid = 0
        var identical = 0
        repeat(1000) {
            val (a, b) = strategy.choosePair(state, Difficulty.NORMAL)!!
            if (isValidPair(a.value, b.value)) valid++
            if (classifyPair(a.value, b.value) == PairType.IDENTICAL) identical++
        }
        assertTrue("valid $valid", valid >= 880)
        assertTrue("identical $identical", identical >= 700)
    }

    @Test
    fun hardCpuAlwaysFindsValidPairs() {
        val random = Random(12)
        val strategy = NumberMatchCpuStrategy(random)
        repeat(300) {
            var state = engine.newGame()
            while (state.phase != GamePhase.GAME_OVER) {
                val pair = strategy.choosePair(state, Difficulty.HARD)
                assertNotNull(pair)
                val (a, b) = pair!!
                assertTrue(a.isAvailable && b.isAvailable && a.id != b.id)
                assertTrue(isValidPair(a.value, b.value))
                state = engine.playPair(state, a.id, b.id)!!.state
            }
        }
    }

    @Test
    fun hardCpuPrefersIdenticalAndPreservesOptions() {
        val strategy = NumberMatchCpuStrategy(Random(13))
        // 2,8 (sum) vs 4,4 (identical): identical scores higher.
        val state = engine.initialState(cellsOf(2, 8, 4, 4))
        repeat(50) {
            val (a, b) = strategy.choosePair(state, Difficulty.HARD)!!
            assertEquals(4, a.value)
            assertEquals(4, b.value)
        }
    }

    @Test
    fun cpuNeverPicksCapturedCells() {
        val strategy = NumberMatchCpuStrategy(Random(14))
        var state = engine.newGame()
        val captured = engine.playPair(state, state.cells[0].id,
            state.cells.drop(1).first { isValidPair(state.cells[0].value, it.value) }.id)!!.state
        state = captured.copy(phase = GamePhase.CPU_TURN, currentSide = Side.CPU)
        Difficulty.entries.forEach { difficulty ->
            repeat(200) {
                val (a, b) = strategy.choosePair(state, difficulty)!!
                assertTrue(a.isAvailable && b.isAvailable)
            }
        }
    }
}
