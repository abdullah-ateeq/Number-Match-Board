package com.example.numbermatchboard.util

import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.Side

object GameAccessibility {

    fun cellDescription(cell: NumberCell, isHint: Boolean = false): String {
        val position = cell.coordinate.label
        return when (cell.state) {
            CellState.ACTIVE ->
                "Number ${cell.value} at $position" + if (isHint) ", possible match" else ""
            CellState.SELECTED -> "Selected number ${cell.value} at $position"
            CellState.PLAYER_CAPTURED -> "Player captured number ${cell.value} at $position"
            CellState.CPU_CAPTURED -> "CPU captured number ${cell.value} at $position"
        }
    }

    fun turnDescription(phase: GamePhase, side: Side): String = when (phase) {
        GamePhase.GAME_OVER -> "Game over"
        else -> if (side == Side.PLAYER) "Your turn" else "CPU turn"
    }

    fun pairsRemaining(count: Int): String =
        if (count == 1) "1 pair remaining" else "$count pairs remaining"
}
