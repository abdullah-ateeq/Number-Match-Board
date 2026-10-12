package com.example.numbermatchboard.game.ai

import com.example.numbermatchboard.game.logic.calculatePairScore
import com.example.numbermatchboard.game.logic.classifyPair
import com.example.numbermatchboard.game.logic.findInvalidPairs
import com.example.numbermatchboard.game.logic.matchClass
import com.example.numbermatchboard.game.model.Difficulty
import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.NumberMatchGameState
import com.example.numbermatchboard.game.model.PairType
import kotlin.math.abs
import kotlin.random.Random
import com.example.numbermatchboard.game.logic.findValidPairs as findValidPairsRule

/**
 * Chooses the CPU's pair. Everything on the board is visible, so the CPU sees exactly
 * what the player sees. No UI code.
 */
class NumberMatchCpuStrategy(
    private val random: Random = Random.Default
) {

    fun findValidPairs(activeCells: List<NumberCell>): List<Pair<NumberCell, NumberCell>> =
        findValidPairsRule(activeCells.filter { it.isAvailable })

    fun choosePair(state: NumberMatchGameState, difficulty: Difficulty): Pair<NumberCell, NumberCell>? {
        val available = state.availableCells
        if (available.size < 2) return null
        val valid = findValidPairs(available)

        if (difficulty.mistakeRate > 0 && random.nextDouble() < difficulty.mistakeRate) {
            val invalid = findInvalidPairs(available)
            if (invalid.isNotEmpty()) return invalid.random(random)
        }
        if (valid.isEmpty()) return null

        return when (difficulty) {
            Difficulty.EASY -> valid.random(random)
            Difficulty.NORMAL ->
                if (random.nextDouble() < NORMAL_RANDOM_CHOICE_RATE) valid.random(random) else chooseSimple(valid)
            Difficulty.HARD -> chooseStrong(valid, available)
        }
    }

    /** Normal: the most "obvious" match — identical numbers first, then the closest cells. */
    fun chooseSimple(valid: List<Pair<NumberCell, NumberCell>>): Pair<NumberCell, NumberCell> {
        val ranked = valid.groupBy { pair ->
            val identicalRank = if (classifyPair(pair.first.value, pair.second.value) == PairType.IDENTICAL) 0 else 1
            identicalRank * 100 + distance(pair)
        }
        return ranked.getValue(ranked.keys.min()).random(random)
    }

    /** Hard: highest evaluation, ties broken randomly. */
    fun chooseStrong(
        valid: List<Pair<NumberCell, NumberCell>>,
        available: List<NumberCell>
    ): Pair<NumberCell, NumberCell> {
        val classCounts = available.groupingBy { matchClass(it.value) }.eachCount()
        val scored = valid.map { it to evaluatePair(it, classCounts) }
        val best = scored.maxOf { it.second }
        return scored.filter { it.second >= best - 1e-9 }.random(random).first
    }

    /**
     * Simple heuristic, no game theory:
     *  - pair type: identical pairs score higher
     *  - future options: remaining valid pairs after the capture (taking from a small
     *    group keeps more combinations open)
     *  - number frequency: rare numbers are cleared before they get stranded behind others
     *  - board position: a slight preference for nearby cells
     */
    fun evaluatePair(
        pair: Pair<NumberCell, NumberCell>,
        classCounts: Map<Int, Int>
    ): Double {
        val type = classifyPair(pair.first.value, pair.second.value) ?: return Double.NEGATIVE_INFINITY
        val pairClass = matchClass(pair.first.value)
        val futurePairs = classCounts.entries.sumOf { (cls, count) ->
            val remaining = if (cls == pairClass) count - 2 else count
            remaining * (remaining - 1) / 2
        }
        val classSize = classCounts[pairClass] ?: 2
        return calculatePairScore(type) * 1.0 +
            futurePairs * 0.05 +
            (1.0 / classSize) +
            (10 - distance(pair)) * 0.01
    }

    private fun distance(pair: Pair<NumberCell, NumberCell>): Int =
        abs(pair.first.coordinate.row - pair.second.coordinate.row) +
            abs(pair.first.coordinate.column - pair.second.coordinate.column)

    companion object {
        const val NORMAL_RANDOM_CHOICE_RATE = 0.2
    }
}
