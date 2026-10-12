package com.example.numbermatchboard.data

import com.example.numbermatchboard.game.model.Difficulty
import com.example.numbermatchboard.game.model.GameOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RepositoryTest {

    @Test
    fun settingsDefaults() {
        val settings = SettingsRepository(InMemoryKeyValueStore()).settings.value
        assertEquals(GameSettings(), settings)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(Difficulty.NORMAL, settings.difficulty)
        assertEquals(true, settings.showMatchHints)
    }

    @Test
    fun settingsPersistAcrossRepositoryInstances() {
        val store = InMemoryKeyValueStore()
        SettingsRepository(store).update {
            it.copy(themeMode = ThemeMode.DARK, difficulty = Difficulty.HARD, showMatchHints = false, sound = false)
        }
        val reloaded = SettingsRepository(store).settings.value
        assertEquals(ThemeMode.DARK, reloaded.themeMode)
        assertEquals(Difficulty.HARD, reloaded.difficulty)
        assertFalse(reloaded.showMatchHints)
        assertFalse(reloaded.sound)
    }

    @Test
    fun resetSettingsRestoresDefaults() {
        val store = InMemoryKeyValueStore()
        val repo = SettingsRepository(store)
        repo.update { it.copy(animations = false) }
        repo.reset()
        assertEquals(GameSettings(), repo.settings.value)
        assertEquals(GameSettings(), SettingsRepository(store).settings.value)
    }

    private fun game(outcome: GameOutcome, pairs: Int, score: Int, identical: Int = 0, sum: Int = 0) =
        FinishedGame(outcome, pairs, score, identical, sum)

    @Test
    fun statisticsRecordResults() {
        val repo = StatisticsRepository(InMemoryKeyValueStore())
        repo.record(game(GameOutcome.PLAYER_WIN, 10, 116, identical = 6, sum = 4))
        repo.record(game(GameOutcome.CPU_WIN, 7, 80, identical = 3, sum = 4))
        repo.record(game(GameOutcome.DRAW, 9, 104))
        val stats = repo.statistics.value
        assertEquals(3, stats.gamesPlayed)
        assertEquals(1, stats.wins)
        assertEquals(1, stats.losses)
        assertEquals(1, stats.draws)
        assertEquals(9, stats.totalIdenticalMatches)
        assertEquals(8, stats.totalSumToTenMatches)
    }

    @Test
    fun bestScoreAndBestPairsKeepMaximum() {
        val repo = StatisticsRepository(InMemoryKeyValueStore())
        repo.record(game(GameOutcome.PLAYER_WIN, 12, 140))
        repo.record(game(GameOutcome.PLAYER_WIN, 10, 150))
        assertEquals(12, repo.statistics.value.bestPairs)
        assertEquals(150, repo.statistics.value.bestScore)
    }

    @Test
    fun winStreakCountsConsecutiveWinsAndResets() {
        val repo = StatisticsRepository(InMemoryKeyValueStore())
        repeat(3) { repo.record(game(GameOutcome.PLAYER_WIN, 10, 110)) }
        assertEquals(3, repo.statistics.value.currentStreak)
        repo.record(game(GameOutcome.CPU_WIN, 5, 55))
        assertEquals(0, repo.statistics.value.currentStreak)
        repo.record(game(GameOutcome.PLAYER_WIN, 10, 110))
        assertEquals(1, repo.statistics.value.currentStreak)
        assertEquals(3, repo.statistics.value.bestStreak)
    }

    @Test
    fun statisticsPersistAndReset() {
        val store = InMemoryKeyValueStore()
        StatisticsRepository(store).record(game(GameOutcome.PLAYER_WIN, 10, 110))
        val reloaded = StatisticsRepository(store)
        assertEquals(1, reloaded.statistics.value.wins)
        reloaded.reset()
        assertEquals(GameStatistics(), reloaded.statistics.value)
        assertEquals(GameStatistics(), StatisticsRepository(store).statistics.value)
    }
}
