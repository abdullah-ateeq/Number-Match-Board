package com.example.numbermatchboard.game.logic

import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.Coordinate
import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.PairType
import kotlin.math.abs
import kotlin.random.Random

/** One intended pair produced by the generator. Players never see these links. */
data class PairUnit(
    val first: Int,
    val second: Int
) {
    val type: PairType
        get() = requireNotNull(classifyPair(first, second)) { "Invalid pair unit $first + $second" }
}

data class GeneratedBoard(
    val cells: List<NumberCell>,
    val units: List<PairUnit>,
    /** Which intended unit each cell came from. Every unit index appears exactly twice. */
    val unitIndexByCellId: Map<Int, Int>
) {
    val identicalUnits: Int
        get() = units.count { it.type == PairType.IDENTICAL }

    val sumToTenUnits: Int
        get() = units.count { it.type == PairType.SUM_TO_TEN }
}

class NumberBoardGenerator(
    private val random: Random = Random.Default
) {

    fun generate(): GeneratedBoard {
        val units = generatePairUnits()
        val tokens = units.flatMapIndexed { index, unit ->
            listOf(index to unit.first, index to unit.second)
        }

        var bestLayout = tokens.shuffled(random)
        var bestScore = adjacentPartnerCount(bestLayout)
        var attempt = 1
        while (bestScore > 0 && attempt < MAX_LAYOUT_ATTEMPTS) {
            val candidate = tokens.shuffled(random)
            val score = adjacentPartnerCount(candidate)
            if (score < bestScore) {
                bestLayout = candidate
                bestScore = score
            }
            attempt++
        }

        val cells = bestLayout.mapIndexed { position, (_, value) ->
            NumberCell(
                id = position,
                coordinate = Coordinate(position / COLUMNS, position % COLUMNS),
                value = value,
                state = CellState.ACTIVE
            )
        }
        val unitIndexByCellId = bestLayout.mapIndexed { position, (unitIndex, _) -> position to unitIndex }.toMap()
        return GeneratedBoard(cells, units, unitIndexByCellId)
    }

    /** Roughly half identical, half sum-to-10 units, with each template capped to keep variety. */
    fun generatePairUnits(): List<PairUnit> {
        val identicalCount = random.nextInt(MIN_IDENTICAL_UNITS, MAX_IDENTICAL_UNITS + 1)
        val sumToTenCount = PAIR_UNITS - identicalCount
        val identical = pickWithCap(IDENTICAL_TEMPLATES, identicalCount, MAX_IDENTICAL_REPEATS)
        val sumToTen = pickWithCap(SUM_TO_TEN_TEMPLATES, sumToTenCount, MAX_SUM_TO_TEN_REPEATS)
        return (identical + sumToTen).shuffled(random)
    }

    private fun pickWithCap(templates: List<PairUnit>, count: Int, cap: Int): List<PairUnit> {
        val uses = IntArray(templates.size)
        return List(count) {
            val allowed = templates.indices.filter { uses[it] < cap }
            val index = allowed.random(random)
            uses[index]++
            if (random.nextBoolean()) templates[index] else templates[index].let { PairUnit(it.second, it.first) }
        }
    }

    /** Number of intended partners that ended up orthogonally next to each other. */
    private fun adjacentPartnerCount(layout: List<Pair<Int, Int>>): Int {
        val positionsByUnit = layout.withIndex().groupBy({ it.value.first }, { it.index })
        return positionsByUnit.values.count { positions ->
            val (a, b) = positions
            val rowDistance = abs(a / COLUMNS - b / COLUMNS)
            val columnDistance = abs(a % COLUMNS - b % COLUMNS)
            rowDistance + columnDistance == 1
        }
    }

    companion object {
        const val ROWS = 6
        const val COLUMNS = 6
        const val CELL_COUNT = ROWS * COLUMNS
        const val PAIR_UNITS = CELL_COUNT / 2
        const val MIN_IDENTICAL_UNITS = 7
        const val MAX_IDENTICAL_UNITS = 11
        private const val MAX_IDENTICAL_REPEATS = 2
        private const val MAX_SUM_TO_TEN_REPEATS = 4
        private const val MAX_LAYOUT_ATTEMPTS = 60

        /** 1+1 … 9+9. 5+5 lives here because it is classified as IDENTICAL. */
        val IDENTICAL_TEMPLATES: List<PairUnit> = (1..9).map { PairUnit(it, it) }

        val SUM_TO_TEN_TEMPLATES: List<PairUnit> = listOf(
            PairUnit(1, 9),
            PairUnit(2, 8),
            PairUnit(3, 7),
            PairUnit(4, 6)
        )

        /** Returns every rule the board breaks; an empty list means the board is valid. */
        fun validate(board: GeneratedBoard): List<String> {
            val problems = mutableListOf<String>()
            val cells = board.cells
            if (cells.size != CELL_COUNT) problems += "Expected $CELL_COUNT cells, found ${cells.size}"
            if (cells.any { it.value !in 1..9 }) problems += "Cell value outside 1–9"
            if (cells.map { it.id }.toSet().size != cells.size) problems += "Duplicate cell ids"
            if (cells.map { it.coordinate }.toSet().size != cells.size) problems += "Duplicate coordinates"
            if (cells.any { it.coordinate.row !in 0 until ROWS || it.coordinate.column !in 0 until COLUMNS }) {
                problems += "Coordinate outside the board"
            }
            if (board.units.size != PAIR_UNITS) problems += "Expected $PAIR_UNITS pair units, found ${board.units.size}"
            if (board.units.any { classifyPair(it.first, it.second) == null }) problems += "Pair unit is not a valid pair"
            if (board.identicalUnits !in MIN_IDENTICAL_UNITS..MAX_IDENTICAL_UNITS) {
                problems += "Unbalanced pair composition (${board.identicalUnits} identical)"
            }

            val unitCounts = board.unitIndexByCellId.values.groupingBy { it }.eachCount()
            if (board.unitIndexByCellId.keys != cells.map { it.id }.toSet() ||
                unitCounts.size != board.units.size ||
                unitCounts.values.any { it != 2 }
            ) {
                problems += "Every cell must belong to exactly one intended pair"
            } else {
                val valuesById = cells.associate { it.id to it.value }
                board.units.forEachIndexed { index, unit ->
                    val unitValues = board.unitIndexByCellId.filterValues { it == index }.keys.map { valuesById.getValue(it) }
                    if (unitValues.sorted() != listOf(unit.first, unit.second).sorted()) {
                        problems += "Cells do not match pair unit $index"
                    }
                }
            }
            if (cells.any { it.state != CellState.ACTIVE }) problems += "New board cells must be active"
            if (!isCompletable(cells.map { it.value })) problems += "Board cannot be completed"
            return problems
        }
    }
}
