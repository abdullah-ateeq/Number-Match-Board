package com.example.numbermatchboard.game

import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.Coordinate
import com.example.numbermatchboard.game.model.NumberCell

/** Cells laid out row by row on a 6-wide board: index 0 = A1, index 9 = B4. */
fun cellsOf(vararg values: Int): List<NumberCell> =
    values.mapIndexed { index, value ->
        NumberCell(index, Coordinate(index / 6, index % 6), value, CellState.ACTIVE)
    }

/** A full 6×6 board where A1 (id 0) and B4 (id 9) hold the given values. */
fun boardWithA1B4(a1: Int, b4: Int): List<NumberCell> {
    val values = IntArray(36) { 0 }
    values[0] = a1
    values[9] = b4
    // Fill the rest with identical pairs so the board stays completable.
    val filler = listOf(1, 1, 2, 2, 3, 3, 9, 9, 8, 8, 7, 7, 1, 1, 2, 2, 3, 3, 9, 9, 8, 8, 7, 7, 1, 1, 2, 2, 3, 3, 9, 9, 8, 8)
    var next = 0
    for (i in values.indices) if (i != 0 && i != 9) values[i] = filler[next++]
    return cellsOf(*values)
}

const val A1 = 0
const val B4 = 9
