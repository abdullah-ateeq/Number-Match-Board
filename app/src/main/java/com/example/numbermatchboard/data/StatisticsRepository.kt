package com.example.numbermatchboard.data

import com.example.numbermatchboard.game.model.GameOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameStatistics(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val bestPairs: Int = 0,
    val bestScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalIdenticalMatches: Int = 0,
    val totalSumToTenMatches: Int = 0
)

/** The player's side of a finished game, as far as statistics are concerned. */
data class FinishedGame(
    val outcome: GameOutcome,
    val playerPairs: Int,
    val playerScore: Int,
    val identicalMatches: Int,
    val sumToTenMatches: Int
)

class StatisticsRepository(private val store: KeyValueStore) {

    private val _statistics = MutableStateFlow(load())
    val statistics: StateFlow<GameStatistics> = _statistics.asStateFlow()

    /** Records a finished game and returns the updated totals. */
    fun record(game: FinishedGame): GameStatistics {
        val current = _statistics.value
        val streak = if (game.outcome == GameOutcome.PLAYER_WIN) current.currentStreak + 1 else 0
        val updated = current.copy(
            gamesPlayed = current.gamesPlayed + 1,
            wins = current.wins + if (game.outcome == GameOutcome.PLAYER_WIN) 1 else 0,
            losses = current.losses + if (game.outcome == GameOutcome.CPU_WIN) 1 else 0,
            draws = current.draws + if (game.outcome == GameOutcome.DRAW) 1 else 0,
            bestPairs = maxOf(current.bestPairs, game.playerPairs),
            bestScore = maxOf(current.bestScore, game.playerScore),
            currentStreak = streak,
            bestStreak = maxOf(current.bestStreak, streak),
            totalIdenticalMatches = current.totalIdenticalMatches + game.identicalMatches,
            totalSumToTenMatches = current.totalSumToTenMatches + game.sumToTenMatches
        )
        save(updated)
        _statistics.value = updated
        return updated
    }

    fun reset() {
        store.remove(ALL_KEYS)
        _statistics.value = GameStatistics()
    }

    private fun load() = GameStatistics(
        gamesPlayed = store.getInt(KEY_GAMES, 0),
        wins = store.getInt(KEY_WINS, 0),
        losses = store.getInt(KEY_LOSSES, 0),
        draws = store.getInt(KEY_DRAWS, 0),
        bestPairs = store.getInt(KEY_BEST_PAIRS, 0),
        bestScore = store.getInt(KEY_BEST_SCORE, 0),
        currentStreak = store.getInt(KEY_STREAK, 0),
        bestStreak = store.getInt(KEY_BEST_STREAK, 0),
        totalIdenticalMatches = store.getInt(KEY_IDENTICAL, 0),
        totalSumToTenMatches = store.getInt(KEY_SUM_TEN, 0)
    )

    private fun save(stats: GameStatistics) {
        store.putInt(KEY_GAMES, stats.gamesPlayed)
        store.putInt(KEY_WINS, stats.wins)
        store.putInt(KEY_LOSSES, stats.losses)
        store.putInt(KEY_DRAWS, stats.draws)
        store.putInt(KEY_BEST_PAIRS, stats.bestPairs)
        store.putInt(KEY_BEST_SCORE, stats.bestScore)
        store.putInt(KEY_STREAK, stats.currentStreak)
        store.putInt(KEY_BEST_STREAK, stats.bestStreak)
        store.putInt(KEY_IDENTICAL, stats.totalIdenticalMatches)
        store.putInt(KEY_SUM_TEN, stats.totalSumToTenMatches)
    }

    companion object {
        const val PREFS_NAME = "number_match_statistics"
        private const val KEY_GAMES = "games_played"
        private const val KEY_WINS = "wins"
        private const val KEY_LOSSES = "losses"
        private const val KEY_DRAWS = "draws"
        private const val KEY_BEST_PAIRS = "best_pairs"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_IDENTICAL = "total_identical"
        private const val KEY_SUM_TEN = "total_sum_to_ten"
        private val ALL_KEYS = listOf(
            KEY_GAMES, KEY_WINS, KEY_LOSSES, KEY_DRAWS, KEY_BEST_PAIRS, KEY_BEST_SCORE,
            KEY_STREAK, KEY_BEST_STREAK, KEY_IDENTICAL, KEY_SUM_TEN
        )
    }
}
