package com.example.numbermatchboard.game.logic

import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.MatchEvent
import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.NumberMatchGameState
import com.example.numbermatchboard.game.model.PairType
import com.example.numbermatchboard.game.model.PendingPair
import com.example.numbermatchboard.game.model.Side

sealed interface SelectionResult {
    /** Tap had no effect (captured cell, wrong phase, unknown id). The turn is not consumed. */
    data object Ignored : SelectionResult

    /** The same cell was tapped twice. */
    data object SameCell : SelectionResult

    data class FirstSelected(val state: NumberMatchGameState) : SelectionResult

    /** Two cells chosen; the state is now [GamePhase.RESOLVING] until [NumberMatchGameEngine.resolvePendingPair]. */
    data class PairSelected(val state: NumberMatchGameState, val pending: PendingPair) : SelectionResult
}

data class Resolution(
    val state: NumberMatchGameState,
    val event: MatchEvent
)

/** Pure game rules. Holds no state of its own and knows nothing about Compose. */
class NumberMatchGameEngine(
    private val generator: NumberBoardGenerator = NumberBoardGenerator()
) {

    fun newGame(): NumberMatchGameState = initialState(generator.generate().cells)

    fun initialState(cells: List<NumberCell>, startingSide: Side = Side.PLAYER): NumberMatchGameState {
        val fresh = cells.map { it.copy(state = CellState.ACTIVE) }
        return NumberMatchGameState(
            cells = fresh,
            phase = turnPhase(startingSide),
            currentSide = startingSide,
            pairsRemaining = fresh.size / 2
        )
    }

    fun selectCell(state: NumberMatchGameState, cellId: Int): SelectionResult {
        if (state.phase != GamePhase.PLAYER_TURN && state.phase != GamePhase.CPU_TURN) return SelectionResult.Ignored
        val cell = state.cell(cellId) ?: return SelectionResult.Ignored
        if (cell.isCaptured) return SelectionResult.Ignored
        if (cellId in state.selectedCellIds) return SelectionResult.SameCell

        return when (state.selectedCellIds.size) {
            0 -> SelectionResult.FirstSelected(
                state.copy(
                    cells = state.cells.withState(setOf(cellId), CellState.SELECTED),
                    selectedCellIds = listOf(cellId)
                )
            )

            else -> {
                val first = state.cell(state.selectedCellIds.first()) ?: return SelectionResult.Ignored
                val pending = PendingPair(
                    side = state.currentSide,
                    firstCellId = first.id,
                    secondCellId = cell.id,
                    firstValue = first.value,
                    secondValue = cell.value,
                    pairType = classifyPair(first.value, cell.value)
                )
                SelectionResult.PairSelected(
                    state = state.copy(
                        cells = state.cells.withState(setOf(cellId), CellState.SELECTED),
                        selectedCellIds = listOf(first.id, cell.id),
                        phase = GamePhase.RESOLVING,
                        pendingPair = pending
                    ),
                    pending = pending
                )
            }
        }
    }

    /**
     * Captures the pending pair (same side keeps the turn) or releases it on a mismatch
     * (turn passes to the other side). Returns null if nothing is pending.
     */
    fun resolvePendingPair(state: NumberMatchGameState): Resolution? {
        if (state.phase != GamePhase.RESOLVING) return null
        val pending = state.pendingPair ?: return null
        val side = pending.side
        val pairIds = setOf(pending.firstCellId, pending.secondCellId)
        val pairType = pending.pairType

        val resolved = if (pairType != null) {
            capture(state, side, pairIds, pairType)
        } else {
            val nextSide = side.opponent
            state.copy(
                cells = state.cells.withState(pairIds, CellState.ACTIVE),
                phase = turnPhase(nextSide),
                currentSide = nextSide,
                lastPairType = null,
                lastMatch = false
            )
        }

        val event = MatchEvent(
            side = side,
            firstCellId = pending.firstCellId,
            secondCellId = pending.secondCellId,
            firstValue = pending.firstValue,
            secondValue = pending.secondValue,
            pairType = pairType,
            points = pairType?.let(::calculatePairScore) ?: 0
        )
        val finalState = resolved.copy(
            selectedCellIds = emptyList(),
            pendingPair = null,
            turnCount = state.turnCount + 1,
            lastEvent = event
        )
        return Resolution(finalState, event)
    }

    /** Convenience used by the CPU and by tests: select two cells and resolve them immediately. */
    fun playPair(state: NumberMatchGameState, firstId: Int, secondId: Int): Resolution? {
        val first = selectCell(state, firstId) as? SelectionResult.FirstSelected ?: return null
        val second = selectCell(first.state, secondId) as? SelectionResult.PairSelected ?: return null
        return resolvePendingPair(second.state)
    }

    fun clearSelection(state: NumberMatchGameState): NumberMatchGameState {
        if (state.phase == GamePhase.RESOLVING) return state
        return state.copy(
            cells = state.cells.withState(state.selectedCellIds.toSet(), CellState.ACTIVE),
            selectedCellIds = emptyList()
        )
    }

    /** Ids of available cells that would form a valid pair with [cellId]. */
    fun matchCandidates(state: NumberMatchGameState, cellId: Int): Set<Int> {
        val selected = state.cell(cellId) ?: return emptySet()
        return state.cells
            .filter { it.id != cellId && it.isAvailable && isValidPair(selected.value, it.value) }
            .map { it.id }
            .toSet()
    }

    private fun capture(
        state: NumberMatchGameState,
        side: Side,
        pairIds: Set<Int>,
        pairType: PairType
    ): NumberMatchGameState {
        val capturedState = if (side == Side.PLAYER) CellState.PLAYER_CAPTURED else CellState.CPU_CAPTURED
        val cells = state.cells.withState(pairIds, capturedState)
        val remaining = cells.count { it.isAvailable } / 2
        val points = calculatePairScore(pairType)
        val identical = if (pairType == PairType.IDENTICAL) 1 else 0
        val sumToTen = 1 - identical
        val stuck = remaining > 0 && findValidPairs(cells.filter { it.isAvailable }).isEmpty()

        val scored = if (side == Side.PLAYER) {
            state.copy(
                playerPairs = state.playerPairs + 1,
                playerScore = state.playerScore + points,
                playerIdenticalPairs = state.playerIdenticalPairs + identical,
                playerSumToTenPairs = state.playerSumToTenPairs + sumToTen
            )
        } else {
            state.copy(
                cpuPairs = state.cpuPairs + 1,
                cpuScore = state.cpuScore + points,
                cpuIdenticalPairs = state.cpuIdenticalPairs + identical,
                cpuSumToTenPairs = state.cpuSumToTenPairs + sumToTen
            )
        }
        return scored.copy(
            cells = cells,
            pairsRemaining = remaining,
            // Generated boards can always be finished; "stuck" only guards hand-built states.
            phase = if (remaining == 0 || stuck) GamePhase.GAME_OVER else turnPhase(side),
            currentSide = side,
            lastPairType = pairType,
            lastMatch = true,
            lastCapturedBy = side
        )
    }

    private fun List<NumberCell>.withState(ids: Set<Int>, newState: CellState): List<NumberCell> =
        map { if (it.id in ids) it.copy(state = newState) else it }

    companion object {
        fun turnPhase(side: Side): GamePhase =
            if (side == Side.PLAYER) GamePhase.PLAYER_TURN else GamePhase.CPU_TURN
    }
}
