package com.example.numbermatchboard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.numbermatchboard.data.SettingsRepository
import com.example.numbermatchboard.data.SharedPreferencesStore
import com.example.numbermatchboard.data.StatisticsRepository
import com.example.numbermatchboard.navigation.NumberMatchNavHost
import com.example.numbermatchboard.ui.NumberMatchViewModel
import com.example.numbermatchboard.ui.theme.NumberMatchBoardTheme
import com.example.numbermatchboard.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NumberMatchViewModel by viewModels {
        NumberMatchViewModel.Factory(
            settingsRepository = SettingsRepository(
                SharedPreferencesStore.create(applicationContext, SettingsRepository.PREFS_NAME)
            ),
            statisticsRepository = StatisticsRepository(
                SharedPreferencesStore.create(applicationContext, StatisticsRepository.PREFS_NAME)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = isAppInDarkTheme(settings.themeMode)

            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            NumberMatchBoardTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                NumberMatchNavHost(viewModel)
            }
        }
    }
}
