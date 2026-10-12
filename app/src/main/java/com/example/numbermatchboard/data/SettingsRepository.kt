package com.example.numbermatchboard.data

import com.example.numbermatchboard.game.model.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class GameSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val confirmRestart: Boolean = true,
    val showMatchHints: Boolean = true,
    val showCoordinates: Boolean = true
)

class SettingsRepository(private val store: KeyValueStore) {

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    fun update(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_settings.value)
        save(updated)
        _settings.value = updated
    }

    fun reset() {
        store.remove(ALL_KEYS)
        _settings.value = GameSettings()
    }

    private fun load(): GameSettings {
        val defaults = GameSettings()
        return GameSettings(
            themeMode = enumOrDefault(store.getString(KEY_THEME, defaults.themeMode.name), defaults.themeMode),
            dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            difficulty = enumOrDefault(store.getString(KEY_DIFFICULTY, defaults.difficulty.name), defaults.difficulty),
            sound = store.getBoolean(KEY_SOUND, defaults.sound),
            haptics = store.getBoolean(KEY_HAPTICS, defaults.haptics),
            animations = store.getBoolean(KEY_ANIMATIONS, defaults.animations),
            confirmRestart = store.getBoolean(KEY_CONFIRM_RESTART, defaults.confirmRestart),
            showMatchHints = store.getBoolean(KEY_MATCH_HINTS, defaults.showMatchHints),
            showCoordinates = store.getBoolean(KEY_COORDINATES, defaults.showCoordinates)
        )
    }

    private fun save(settings: GameSettings) {
        store.putString(KEY_THEME, settings.themeMode.name)
        store.putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
        store.putString(KEY_DIFFICULTY, settings.difficulty.name)
        store.putBoolean(KEY_SOUND, settings.sound)
        store.putBoolean(KEY_HAPTICS, settings.haptics)
        store.putBoolean(KEY_ANIMATIONS, settings.animations)
        store.putBoolean(KEY_CONFIRM_RESTART, settings.confirmRestart)
        store.putBoolean(KEY_MATCH_HINTS, settings.showMatchHints)
        store.putBoolean(KEY_COORDINATES, settings.showCoordinates)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        const val PREFS_NAME = "number_match_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_ANIMATIONS = "animations"
        private const val KEY_CONFIRM_RESTART = "confirm_restart"
        private const val KEY_MATCH_HINTS = "show_match_hints"
        private const val KEY_COORDINATES = "show_coordinates"
        private val ALL_KEYS = listOf(
            KEY_THEME, KEY_DYNAMIC_COLOR, KEY_DIFFICULTY, KEY_SOUND, KEY_HAPTICS,
            KEY_ANIMATIONS, KEY_CONFIRM_RESTART, KEY_MATCH_HINTS, KEY_COORDINATES
        )
    }
}
