package com.example.numbermatchboard.game

import com.example.numbermatchboard.game.logic.NumberBoardGenerator
import com.example.numbermatchboard.game.logic.NumberMatchGameEngine
import com.example.numbermatchboard.game.logic.findValidPairs
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.PairType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class NumberBoardGeneratorTest {

    @Test
    fun generatesThirtySixCellsWithValuesOneToNine() {
        val board = NumberBoardGenerator(Random(1)).generate()
        assertEquals(36, board.cells.size)
        assertTrue(board.cells.all { it.value in 1..9 })
    }

    @Test
    fun generatesEighteenValidPairUnits() {
        val board = NumberBoardGenerator(Random(2)).generate()
        assertEquals(18, board.units.size)
        assertEquals(18, board.identicalUnits + board.sumToTenUnits)
        assertTrue(board.units.none { it.first == 5 && it.second == 5 && it.type != PairType.IDENTICAL })
    }

    @Test
    fun thousandRandomBoardsAreValid() {
        val generator = NumberBoardGenerator(Random(42))
        repeat(1000) { index ->
            val board = generator.generate()
            val problems = NumberBoardGenerator.validate(board)
            assertTrue("Board $index: $problems", problems.isEmpty())
            assertEquals(36, board.cells.map { it.id }.toSet().size)
            assertEquals(36, board.cells.map { it.coordinate }.toSet().size)
        }
    }

    @Test
    fun pairCompositionIsBalanced() {
        val generator = NumberBoardGenerator(Random(7))
        var identical = 0
        var total = 0
        repeat(1000) {
            val board = generator.generate()
            // No board is close to 90% one type.
            assertTrue(board.identicalUnits in 7..11)
            identical += board.identicalUnits
            total += board.units.size
        }
        val ratio = identical.toDouble() / total
        assertTrue("identical ratio $ratio", ratio in 0.45..0.55)
    }

    @Test
    fun intendedPartnersAreRarelyAdjacent() {
        val generator = NumberBoardGenerator(Random(9))
        var adjacent = 0
        repeat(500) {
            val board = generator.generate()
            val byUnit = board.unitIndexByCellId.entries.groupBy({ it.value }, { it.key })
            adjacent += byUnit.values.count { ids ->
                val (a, b) = ids.map { id -> board.cells.first { it.id == id }.coordinate }
                abs(a.row - b.row) + abs(a.column - b.column) == 1
            }
        }
        assertTrue("adjacent partners $adjacent", adjacent < 25)
    }

    @Test
    fun everyRandomBoardCanBeCompletedWithAnyValidChoices() {
        val random = Random(11)
        val engine = NumberMatchGameEngine(NumberBoardGenerator(random))
        repeat(1000) {
            var state = engine.newGame()
            while (state.phase != GamePhase.GAME_OVER) {
                val pairs = findValidPairs(state.availableCells)
                assertTrue("Board got stuck", pairs.isNotEmpty())
                val (first, second) = pairs.random(random)
                val resolution = engine.playPair(state, first.id, second.id)
                assertNotNull(resolution)
                state = resolution!!.state
            }
            assertEquals(0, state.pairsRemaining)
            assertEquals(18, state.playerPairs)
        }
    }
}
