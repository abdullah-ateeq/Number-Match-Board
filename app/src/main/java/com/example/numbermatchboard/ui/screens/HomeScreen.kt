package com.example.numbermatchboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.numbermatchboard.game.model.Difficulty
import com.example.numbermatchboard.ui.NumberMatchViewModel
import com.example.numbermatchboard.ui.components.HowToPlaySheet
import com.example.numbermatchboard.ui.components.PairMark
import com.example.numbermatchboard.ui.components.StatValue

@Composable
fun HomeScreen(
    viewModel: NumberMatchViewModel,
    onPlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    var showHowToPlay by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PairMark()
            Spacer(Modifier.height(18.dp))
            Text(
                "NUMBER MATCH",
                style = MaterialTheme.typography.displayMedium,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "BOARD",
                style = MaterialTheme.typography.displayMedium,
                color = colors.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Match the numbers. Capture the pairs.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("PLAY", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatValue("BEST PAIRS", "${stats.bestPairs}")
                StatValue("BEST SCORE", "${stats.bestScore}")
                StatValue("WIN STREAK", "${stats.currentStreak}", valueColor = colors.primary)
            }
            Spacer(Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("DIFFICULTY", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { difficulty ->
                            FilterChip(
                                selected = settings.difficulty == difficulty,
                                onClick = { viewModel.updateSettings { it.copy(difficulty = difficulty) } },
                                label = { Text(difficulty.label.uppercase()) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatValue("GAMES", "${stats.gamesPlayed}", valueSize = 20)
                    StatValue("WINS", "${stats.wins}", valueSize = 20)
                    StatValue("BEST PAIRS", "${stats.bestPairs}", valueSize = 20)
                    StatValue("BEST STREAK", "${stats.bestStreak}", valueSize = 20)
                }
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatValue("IDENTICAL", "${stats.totalIdenticalMatches}", valueSize = 16)
                    StatValue("SUM TO 10", "${stats.totalSumToTenMatches}", valueSize = 16)
                    StatValue("LOSSES", "${stats.losses}", valueSize = 16)
                    StatValue("DRAWS", "${stats.draws}", valueSize = 16)
                }
            }
            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = { showHowToPlay = true },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("How to Play")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Settings")
                }
                OutlinedButton(onClick = onOpenAbout, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("About")
                }
            }
        }
    }

    if (showHowToPlay) {
        HowToPlaySheet(onDismiss = { showHowToPlay = false })
    }
}
