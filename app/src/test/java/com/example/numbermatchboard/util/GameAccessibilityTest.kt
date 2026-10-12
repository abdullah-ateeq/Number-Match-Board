package com.example.numbermatchboard.util

import com.example.numbermatchboard.game.cellsOf
import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.Side
import org.junit.Assert.assertEquals
import org.junit.Test

class GameAccessibilityTest {

    private val cell = cellsOf(4, 1, 6).first()

    @Test
    fun cellDescriptionsReflectState() {
        assertEquals("Number 4 at A1", GameAccessibility.cellDescription(cell))
        assertEquals("Number 4 at A1, possible match", GameAccessibility.cellDescription(cell, isHint = true))
        assertEquals(
            "Selected number 4 at A1",
            GameAccessibility.cellDescription(cell.copy(state = CellState.SELECTED))
        )
        assertEquals(
            "Player captured number 4 at A1",
            GameAccessibility.cellDescription(cell.copy(state = CellState.PLAYER_CAPTURED))
        )
        assertEquals(
            "CPU captured number 6 at A3",
            GameAccessibility.cellDescription(cellsOf(4, 1, 6)[2].copy(state = CellState.CPU_CAPTURED))
        )
    }

    @Test
    fun turnAndPairsDescriptions() {
        assertEquals("Your turn", GameAccessibility.turnDescription(GamePhase.PLAYER_TURN, Side.PLAYER))
        assertEquals("CPU turn", GameAccessibility.turnDescription(GamePhase.CPU_TURN, Side.CPU))
        assertEquals("9 pairs remaining", GameAccessibility.pairsRemaining(9))
        assertEquals("1 pair remaining", GameAccessibility.pairsRemaining(1))
    }
}
