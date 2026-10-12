package com.example.numbermatchboard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.numbermatchboard.ui.NumberMatchViewModel
import com.example.numbermatchboard.ui.theme.NumberStyle
import kotlinx.coroutines.delay

/** Decorative board for the intro only — not gameplay state. */
private val SplashValues = listOf(
    7, 2, 9, 5, 3, 8,
    1, 4, 8, 2, 6, 3,
    9, 3, 7, 1, 5, 4,
    6, 8, 2, 9, 7, 1,
    5, 1, 4, 3, 2, 6,
    8, 7, 3, 6, 9, 5
)
private const val FOUR_INDEX = 7 // B2
private const val SIX_INDEX = 10 // B5
private const val SPLASH_MILLIS = 1400

private fun stage(progress: Float, start: Float, end: Float): Float =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)

@Composable
fun SplashScreen(viewModel: NumberMatchViewModel, onFinished: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (settings.animations) {
            progress.animateTo(1f, tween(SPLASH_MILLIS, easing = LinearEasing))
            delay(100)
        } else {
            progress.snapTo(1f)
            delay(350)
        }
        finish()
    }

    val p = progress.value
    val colors = MaterialTheme.colorScheme
    val tile = 34.dp
    val gap = 6.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { contentDescription = "Number Match Board" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.alpha(stage(p, 0f, 0.12f)).clearAndSetSemantics { }) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(6) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            repeat(6) { column ->
                                val index = row * 6 + column
                                val isPair = index == FOUR_INDEX || index == SIX_INDEX
                                val numberAlpha = stage(p, 0.1f + index * 0.008f, 0.16f + index * 0.008f)
                                val captured = isPair && p >= 0.72f
                                val pulse = if (isPair) 1f + 0.12f * stage(p, 0.45f, 0.55f) * (1f - stage(p, 0.6f, 0.7f)) else 1f
                                Surface(
                                    shape = if (captured) RoundedCornerShape(50) else RoundedCornerShape(24),
                                    color = when {
                                        captured -> colors.primaryContainer
                                        isPair && p >= 0.45f -> colors.surfaceContainerHighest
                                        else -> colors.surfaceContainerLow
                                    },
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .size(tile)
                                        .graphicsLayer {
                                            scaleX = pulse
                                            scaleY = pulse
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = SplashValues[index].toString(),
                                            style = NumberStyle,
                                            fontSize = 16.sp,
                                            color = if (captured) colors.onPrimaryContainer else colors.onSurface,
                                            modifier = Modifier.alpha(if (captured) 0.35f else numberAlpha)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val connect = stage(p, 0.45f, 0.62f)
                if (connect > 0f) {
                    Canvas(Modifier.matchParentSize()) {
                        val step = (tile + gap).toPx()
                        val half = tile.toPx() / 2f
                        val start = Offset((FOUR_INDEX % 6) * step + half, (FOUR_INDEX / 6) * step + half)
                        val end = Offset((SIX_INDEX % 6) * step + half, (SIX_INDEX / 6) * step + half)
                        drawLine(
                            color = colors.primary.copy(alpha = 0.8f),
                            start = Offset(start.x + half, start.y),
                            end = Offset(start.x + half + (end.x - start.x - 2 * half) * connect, end.y),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                val badge = stage(p, 0.58f, 0.68f)
                if (badge > 0f) {
                    Surface(
                        color = colors.primary,
                        contentColor = colors.onPrimary,
                        shape = CutCornerShape(6.dp),
                        modifier = Modifier
                            .offset(x = (tile + gap) * 2.5f + tile / 2 - 32.dp, y = tile + gap + tile / 2 - 36.dp)
                            .alpha(badge)
                    ) {
                        Text(
                            "SUM 10",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            val title = stage(p, 0.75f, 0.95f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(title)
                    .graphicsLayer { translationY = (1f - title) * 24.dp.toPx() }
                    .clearAndSetSemantics { }
            ) {
                Text("NUMBER", style = MaterialTheme.typography.displayMedium, color = colors.onBackground)
                Text("MATCH", style = MaterialTheme.typography.displayMedium, color = colors.primary)
                Text("BOARD", style = MaterialTheme.typography.displayMedium, color = colors.onBackground)
            }
        }
    }
}
