package com.example.numbermatchboard.game.model

/** Zero-based board position. Rows are labelled A–F, columns 1–6 (e.g. "A1", "B4"). */
data class Coordinate(
    val row: Int,
    val column: Int
) {
    val label: String
        get() = "${'A' + row}${column + 1}"
}

enum class Side {
    PLAYER,
    CPU;

    val opponent: Side
        get() = if (this == PLAYER) CPU else PLAYER
}

enum class CellState {
    ACTIVE,
    SELECTED,
    PLAYER_CAPTURED,
    CPU_CAPTURED
}

/** 5 + 5 is always classified as [IDENTICAL]. */
enum class PairType {
    IDENTICAL,
    SUM_TO_TEN
}

enum class GamePhase {
    PLAYER_TURN,
    CPU_TURN,
    RESOLVING,
    GAME_OVER
}

enum class GameOutcome {
    PLAYER_WIN,
    CPU_WIN,
    DRAW
}

enum class Difficulty(
    val label: String,
    /** Chance that the CPU deliberately picks an invalid pair when one exists. */
    val mistakeRate: Double
) {
    EASY("Easy", 0.30),
    NORMAL("Normal", 0.08),
    HARD("Hard", 0.0)
}

data class NumberCell(
    val id: Int,
    val coordinate: Coordinate,
    val value: Int,
    val state: CellState
) {
    /** True while the cell is still on the board (not captured by either side). */
    val isAvailable: Boolean
        get() = state == CellState.ACTIVE || state == CellState.SELECTED

    val isCaptured: Boolean
        get() = !isAvailable

    val capturedBy: Side?
        get() = when (state) {
            CellState.PLAYER_CAPTURED -> Side.PLAYER
            CellState.CPU_CAPTURED -> Side.CPU
            else -> null
        }
}

/** Two selected cells waiting for the match animation to finish before they are resolved. */
data class PendingPair(
    val side: Side,
    val firstCellId: Int,
    val secondCellId: Int,
    val firstValue: Int,
    val secondValue: Int,
    val pairType: PairType?
) {
    val isMatch: Boolean
        get() = pairType != null

    val sum: Int
        get() = firstValue + secondValue
}

/** The outcome of the most recent resolved pair, used for the "last event" line. */
data class MatchEvent(
    val side: Side,
    val firstCellId: Int,
    val secondCellId: Int,
    val firstValue: Int,
    val secondValue: Int,
    val pairType: PairType?,
    val points: Int
) {
    val isMatch: Boolean
        get() = pairType != null

    val sum: Int
        get() = firstValue + secondValue
}

data class NumberMatchGameState(
    val cells: List<NumberCell>,
    val phase: GamePhase,
    val currentSide: Side,
    val selectedCellIds: List<Int> = emptyList(),
    val playerPairs: Int = 0,
    val cpuPairs: Int = 0,
    val playerScore: Int = 0,
    val cpuScore: Int = 0,
    val pairsRemaining: Int,
    val turnCount: Int = 0,
    val lastPairType: PairType? = null,
    val lastMatch: Boolean? = null,
    val lastCapturedBy: Side? = null,
    val playerIdenticalPairs: Int = 0,
    val playerSumToTenPairs: Int = 0,
    val cpuIdenticalPairs: Int = 0,
    val cpuSumToTenPairs: Int = 0,
    val pendingPair: PendingPair? = null,
    val lastEvent: MatchEvent? = null
) {
    val totalPairs: Int
        get() = cells.size / 2

    val availableCells: List<NumberCell>
        get() = cells.filter { it.isAvailable }

    fun cell(id: Int): NumberCell? = cells.firstOrNull { it.id == id }

    /** Winner by pair count only; score never decides the result. Null until the game is over. */
    val outcome: GameOutcome?
        get() = if (phase != GamePhase.GAME_OVER) {
            null
        } else when {
            playerPairs > cpuPairs -> GameOutcome.PLAYER_WIN
            cpuPairs > playerPairs -> GameOutcome.CPU_WIN
            else -> GameOutcome.DRAW
        }

    val isPerfectMatch: Boolean
        get() = phase == GamePhase.GAME_OVER && cpuPairs == 0 && playerPairs == totalPairs
}

/** Summary of a finished game, shown on the Result screen. */
data class GameSummary(
    val gameId: Int,
    val outcome: GameOutcome,
    val isPerfectMatch: Boolean,
    val playerPairs: Int,
    val cpuPairs: Int,
    val playerScore: Int,
    val cpuScore: Int,
    val playerIdenticalPairs: Int,
    val playerSumToTenPairs: Int,
    val difficulty: Difficulty,
    val bestPairs: Int,
    val isNewBestPairs: Boolean,
    val isNewBestScore: Boolean
)
