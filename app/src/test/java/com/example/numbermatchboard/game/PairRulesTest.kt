package com.example.numbermatchboard.game

import com.example.numbermatchboard.game.logic.calculatePairScore
import com.example.numbermatchboard.game.logic.classifyPair
import com.example.numbermatchboard.game.logic.findValidPairs
import com.example.numbermatchboard.game.logic.isCompletable
import com.example.numbermatchboard.game.logic.isValidPair
import com.example.numbermatchboard.game.model.PairType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairRulesTest {

    @Test
    fun identicalPairsAreValid() {
        for (n in 1..9) {
            assertEquals(PairType.IDENTICAL, classifyPair(n, n))
            assertTrue(isValidPair(n, n))
        }
    }

    @Test
    fun sumToTenPairsAreValid() {
        listOf(1 to 9, 2 to 8, 3 to 7, 4 to 6, 9 to 1, 8 to 2, 7 to 3, 6 to 4).forEach { (a, b) ->
            assertEquals(PairType.SUM_TO_TEN, classifyPair(a, b))
            assertTrue(isValidPair(a, b))
        }
    }

    @Test
    fun fivePlusFiveIsClassifiedAsIdenticalOnly() {
        assertEquals(PairType.IDENTICAL, classifyPair(5, 5))
        assertEquals(12, calculatePairScore(classifyPair(5, 5)!!))
    }

    @Test
    fun invalidPairsAreRejected() {
        listOf(1 to 2, 2 to 4, 3 to 5, 7 to 8, 8 to 9, 2 to 5, 4 to 7).forEach { (a, b) ->
            assertNull(classifyPair(a, b))
            assertFalse(isValidPair(a, b))
        }
    }

    @Test
    fun sixPlusSixIsIdenticalEvenThoughSumIsNotTen() {
        // The brief lists 6 + 6 among invalid examples, but the core rule (identical numbers)
        // makes it valid; the identical rule wins.
        assertEquals(PairType.IDENTICAL, classifyPair(6, 6))
    }

    @Test
    fun scoreCalculation() {
        assertEquals(12, calculatePairScore(PairType.IDENTICAL))
        assertEquals(11, calculatePairScore(PairType.SUM_TO_TEN))
    }

    @Test
    fun findValidPairsUsesTheWholeNumberPool() {
        val cells = cellsOf(2, 2, 8, 8, 7, 3, 4, 6)
        val pairs = findValidPairs(cells)
        // {2,2,8,8}: 6 combinations, 7+3, 4+6.
        assertEquals(8, pairs.size)
        assertTrue(pairs.none { it.first.id == it.second.id })
        assertTrue(pairs.all { isValidPair(it.first.value, it.second.value) })
    }

    @Test
    fun completableOnlyWhenEveryMatchClassIsEven() {
        assertTrue(isCompletable(listOf(2, 8, 2, 2, 5, 5)))
        assertFalse(isCompletable(listOf(2, 3)))
        assertFalse(isCompletable(listOf(2, 8, 8, 3, 7, 7)))
    }
}
