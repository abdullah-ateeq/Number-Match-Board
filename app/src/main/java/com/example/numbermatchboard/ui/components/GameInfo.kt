package com.example.numbermatchboard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.numbermatchboard.game.model.GamePhase
import com.example.numbermatchboard.game.model.MatchEvent
import com.example.numbermatchboard.game.model.PairType
import com.example.numbermatchboard.game.model.PendingPair
import com.example.numbermatchboard.game.model.Side
import com.example.numbermatchboard.ui.theme.NumberStyle
import com.example.numbermatchboard.util.GameAccessibility

@Composable
fun Scoreboard(
    playerPairs: Int,
    playerScore: Int,
    cpuPairs: Int,
    cpuScore: Int,
    pairsRemaining: Int,
    currentSide: Side,
    gameOver: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SideScore(
            label = "PLAYER",
            pairs = playerPairs,
            score = playerScore,
            side = Side.PLAYER,
            isActive = !gameOver && currentSide == Side.PLAYER,
            animationsEnabled = animationsEnabled,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = GameAccessibility.pairsRemaining(pairsRemaining)
                }
        ) {
            Text(
                "PAIRS LEFT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { }
            )
            Text(
                "$pairsRemaining",
                style = NumberStyle,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        SideScore(
            label = "CPU",
            pairs = cpuPairs,
            score = cpuScore,
            side = Side.CPU,
            isActive = !gameOver && currentSide == Side.CPU,
            animationsEnabled = animationsEnabled,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun SideScore(
    label: String,
    pairs: Int,
    score: Int,
    side: Side,
    isActive: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (side == Side.PLAYER) colors.primary else colors.secondary
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = if (animationsEnabled) tween(450) else snap(),
        label = "score"
    )
    val shape = if (side == Side.PLAYER) RoundedCornerShape(18.dp) else CutCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (isActive) accent.copy(alpha = 0.12f) else colors.surfaceContainer,
        border = if (isActive) BorderStroke(2.dp, accent) else BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $pairs pairs, $score points" + if (isActive) ", playing" else ""
        }
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clearAndSetSemantics { },
            horizontalAlignment = if (side == Side.PLAYER) Alignment.Start else Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (side == Side.PLAYER) TurnDot(accent, isActive)
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                if (side == Side.CPU) TurnDot(accent, isActive)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$pairs", style = NumberStyle, fontSize = 26.sp, color = colors.onSurface)
                Text(
                    if (pairs == 1) " PAIR" else " PAIRS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Text(
                "$animatedScore pts",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TurnDot(color: Color, active: Boolean) {
    Box(
        Modifier
            .size(8.dp)
            .background(if (active) color else color.copy(alpha = 0.25f), CircleShape)
    )
}

@Composable
fun RuleCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Valid pair: same number, or two numbers that add up to 10"
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "VALID PAIR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SAME NUMBER", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "  OR  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("SUM = 10", style = MaterialTheme.typography.labelLarge)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                RuleExample("7 + 7")
                RuleExample("4 + 6 = 10")
            }
        }
    }
}

@Composable
private fun RuleExample(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp).size(14.dp)
        )
    }
}

@Composable
fun TurnIndicator(
    phase: GamePhase,
    currentSide: Side,
    selectedCount: Int,
    cpuThinking: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (currentSide == Side.PLAYER) colors.primary else colors.secondary
    val title = when {
        phase == GamePhase.GAME_OVER -> "GAME OVER"
        currentSide == Side.PLAYER -> "YOUR TURN"
        else -> "CPU TURN"
    }
    val subtitle = when {
        phase == GamePhase.GAME_OVER -> "Board captured"
        phase == GamePhase.RESOLVING -> "Checking pair…"
        currentSide == Side.CPU -> if (cpuThinking) "Choosing a pair..." else "Selecting…"
        selectedCount == 0 -> "Select a number"
        else -> "Select a matching number"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = GameAccessibility.turnDescription(phase, currentSide) + ". $subtitle"
        }
    ) {
        Row(
            Modifier
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurnDot(accent, phase != GamePhase.GAME_OVER)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
            Spacer(Modifier.width(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

private data class EventLine(val title: String, val detail: String, val success: Boolean?)

@Composable
fun LastEventLine(
    pending: PendingPair?,
    lastEvent: MatchEvent?,
    modifier: Modifier = Modifier
) {
    val line = when {
        pending != null -> EventLine(
            title = pendingPairLabel(pending),
            detail = "${pending.firstValue} + ${pending.secondValue}",
            success = pending.isMatch
        )
        lastEvent != null -> {
            val who = if (lastEvent.side == Side.CPU) "CPU " else ""
            val title = when (lastEvent.pairType) {
                PairType.IDENTICAL -> "${who}MATCHED"
                PairType.SUM_TO_TEN -> "${who}PAIR CAPTURED"
                null -> if (lastEvent.side == Side.CPU) "CPU MISSED" else "NOT A MATCH"
            }
            val points = if (lastEvent.isMatch) "  ·  +${lastEvent.points}" else ""
            EventLine(title, "${lastEvent.firstValue} + ${lastEvent.secondValue}$points", lastEvent.isMatch)
        }
        else -> EventLine("READY", "Find identical numbers or a sum of 10", null)
    }
    AnimatedContent(
        targetState = line,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "lastEvent",
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) { event ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (event.success) {
                true -> Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                false -> Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                null -> Unit
            }
            Spacer(Modifier.width(6.dp))
            Text(event.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(10.dp))
            Text(
                event.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
