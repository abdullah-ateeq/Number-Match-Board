package com.example.numbermatchboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.numbermatchboard.game.model.GameOutcome
import com.example.numbermatchboard.game.model.GameSummary
import com.example.numbermatchboard.ui.NumberMatchViewModel
import com.example.numbermatchboard.ui.components.StatValue
import com.example.numbermatchboard.ui.theme.NumberStyle

fun resultTitle(summary: GameSummary): String = when {
    summary.isPerfectMatch -> "PERFECT MATCH"
    summary.outcome == GameOutcome.PLAYER_WIN -> "YOU WIN"
    summary.outcome == GameOutcome.CPU_WIN -> "CPU WINS"
    else -> "DRAW"
}

fun resultMessage(summary: GameSummary): String = when {
    summary.isPerfectMatch -> "Perfect number match."
    summary.outcome == GameOutcome.PLAYER_WIN -> "You captured more pairs."
    summary.outcome == GameOutcome.CPU_WIN -> "The CPU captured more pairs."
    else -> "Both sides captured the same number."
}

@Composable
fun ResultScreen(
    viewModel: NumberMatchViewModel,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = ui.summary
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (summary == null) {
                Text("No finished game", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
            } else {
                val accent = when (summary.outcome) {
                    GameOutcome.PLAYER_WIN -> colors.primary
                    GameOutcome.CPU_WIN -> colors.secondary
                    GameOutcome.DRAW -> colors.tertiary
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        summary.difficulty.label.uppercase() + " CPU",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Text(
                    resultTitle(summary),
                    style = MaterialTheme.typography.displayMedium,
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    resultMessage(summary),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Pairs: you ${summary.playerPairs}, CPU ${summary.cpuPairs}"
                    }
                ) {
                    Text(
                        "${summary.playerPairs}",
                        style = NumberStyle,
                        fontSize = 64.sp,
                        color = colors.primary,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Text(
                        "  —  ",
                        style = NumberStyle,
                        fontSize = 40.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Text(
                        "${summary.cpuPairs}",
                        style = NumberStyle,
                        fontSize = 64.sp,
                        color = colors.secondary,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
                Text("PAIRS", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))

                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatValue("YOUR SCORE", "${summary.playerScore}", valueColor = colors.primary)
                        StatValue("CPU SCORE", "${summary.cpuScore}", valueColor = colors.secondary)
                    }
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatValue("PAIRS CAPTURED", "${summary.playerPairs}", valueSize = 20)
                        StatValue("IDENTICAL", "${summary.playerIdenticalPairs}", valueSize = 20)
                        StatValue("SUM TO 10", "${summary.playerSumToTenPairs}", valueSize = 20)
                    }
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatValue(
                            if (summary.isNewBestPairs) "NEW BEST PAIRS" else "BEST PAIRS",
                            "${summary.bestPairs}",
                            valueSize = 18
                        )
                        if (summary.isNewBestScore) {
                            StatValue("NEW BEST SCORE", "${summary.playerScore}", valueSize = 18)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("PLAY AGAIN", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("HOME")
            }
        }
    }
}
