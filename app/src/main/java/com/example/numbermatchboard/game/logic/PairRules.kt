package com.example.numbermatchboard.game.logic

import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.PairType

const val TARGET_SUM = 10
const val BASE_PAIR_POINTS = 10

/**
 * Identical numbers are checked first, so 5 + 5 is always [PairType.IDENTICAL] and is
 * never counted twice.
 */
fun classifyPair(first: Int, second: Int): PairType? = when {
    first == second -> PairType.IDENTICAL
    first + second == TARGET_SUM -> PairType.SUM_TO_TEN
    else -> null
}

fun isValidPair(first: Int, second: Int): Boolean = classifyPair(first, second) != null

fun pairBonus(pairType: PairType): Int = when (pairType) {
    PairType.IDENTICAL -> 2
    PairType.SUM_TO_TEN -> 1
}

/** IDENTICAL = 12, SUM_TO_TEN = 11. */
fun calculatePairScore(pairType: PairType): Int = BASE_PAIR_POINTS + pairBonus(pairType)

/**
 * Numbers that can ever match each other share a class: {1,9}, {2,8}, {3,7}, {4,6}, {5}.
 * Any two numbers in the same class form a valid pair, and numbers from different classes
 * never do. This is what guarantees a board can always be finished (see [isCompletable]).
 */
fun matchClass(value: Int): Int = minOf(value, TARGET_SUM - value)

/** Every unordered pair of distinct cells that satisfies the matching rule. */
fun findValidPairs(activeCells: List<NumberCell>): List<Pair<NumberCell, NumberCell>> {
    val pairs = mutableListOf<Pair<NumberCell, NumberCell>>()
    for (i in activeCells.indices) {
        for (j in i + 1 until activeCells.size) {
            val first = activeCells[i]
            val second = activeCells[j]
            if (first.id != second.id && isValidPair(first.value, second.value)) {
                pairs += first to second
            }
        }
    }
    return pairs
}

fun findInvalidPairs(activeCells: List<NumberCell>): List<Pair<NumberCell, NumberCell>> {
    val pairs = mutableListOf<Pair<NumberCell, NumberCell>>()
    for (i in activeCells.indices) {
        for (j in i + 1 until activeCells.size) {
            val first = activeCells[i]
            val second = activeCells[j]
            if (first.id != second.id && !isValidPair(first.value, second.value)) {
                pairs += first to second
            }
        }
    }
    return pairs
}

/**
 * A set of numbers can be fully captured iff every match class holds an even count.
 * Capturing a valid pair always removes two numbers from one class, so this property is
 * preserved no matter which valid pairs the players choose.
 */
fun isCompletable(values: List<Int>): Boolean =
    values.groupingBy { matchClass(it) }.eachCount().values.all { it % 2 == 0 }
