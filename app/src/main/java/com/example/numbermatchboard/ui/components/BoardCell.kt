package com.example.numbermatchboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.numbermatchboard.game.model.CellState
import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.Side
import com.example.numbermatchboard.ui.theme.NumberStyle
import com.example.numbermatchboard.util.GameAccessibility

enum class PairHighlight { NONE, MATCH, MISMATCH }

private val ActiveShape = RoundedCornerShape(24)
private val PlayerCapturedShape = RoundedCornerShape(50)
private val CpuCapturedShape = CutCornerShape(26)

/** Ownership is shown by color, shape (round vs cut corners) and a P / C badge. */
fun cellShape(state: CellState): Shape = when (state) {
    CellState.PLAYER_CAPTURED -> PlayerCapturedShape
    CellState.CPU_CAPTURED -> CpuCapturedShape
    else -> ActiveShape
}

@Composable
fun BoardCell(
    cell: NumberCell,
    size: Dp,
    isHint: Boolean,
    highlight: PairHighlight,
    selectingSide: Side,
    animationsEnabled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isSelected = cell.state == CellState.SELECTED
    val owner = cell.capturedBy
    val sideAccent = if (selectingSide == Side.PLAYER) colors.primary else colors.secondary

    val selectScale by animateFloatAsState(
        targetValue = if (isSelected) 1.07f else 1f,
        animationSpec = if (animationsEnabled) spring(dampingRatio = 0.55f, stiffness = 500f) else snap(),
        label = "selectScale"
    )
    val numberAlpha by animateFloatAsState(
        targetValue = if (owner != null) 0.3f else 1f,
        animationSpec = if (animationsEnabled) tween(320) else snap(),
        label = "numberAlpha"
    )
    val containerColor by animateColorAsState(
        targetValue = when (owner) {
            Side.PLAYER -> colors.primaryContainer
            Side.CPU -> colors.secondaryContainer
            null -> if (isSelected) colors.surfaceContainerHighest else colors.surfaceContainerLow
        },
        animationSpec = if (animationsEnabled) tween(320) else snap(),
        label = "cellColor"
    )
    val pulse = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(highlight, animationsEnabled) {
        when (highlight) {
            PairHighlight.MATCH -> if (animationsEnabled) {
                repeat(2) {
                    pulse.animateTo(1.13f, tween(120))
                    pulse.animateTo(1f, tween(120))
                }
            }
            PairHighlight.MISMATCH -> if (animationsEnabled) {
                for (offset in listOf(-7f, 7f, -5f, 5f, -2f, 0f)) shake.animateTo(offset, tween(50))
            }
            PairHighlight.NONE -> {
                pulse.snapTo(1f)
                shake.snapTo(0f)
            }
        }
    }

    val border = when {
        highlight == PairHighlight.MISMATCH -> BorderStroke(3.dp, colors.error)
        isSelected -> BorderStroke(3.dp, sideAccent)
        isHint -> BorderStroke(2.dp, colors.primary.copy(alpha = 0.55f))
        owner == Side.PLAYER -> BorderStroke(1.5.dp, colors.primary.copy(alpha = 0.7f))
        owner == Side.CPU -> BorderStroke(1.5.dp, colors.secondary.copy(alpha = 0.7f))
        else -> BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.7f))
    }
    val description = GameAccessibility.cellDescription(cell, isHint)
    val density = LocalDensity.current
    val numberSize = with(density) { (size * 0.46f).toSp() }
    val badgeSize = with(density) { (size * 0.2f).toSp() }

    Surface(
        onClick = onClick,
        enabled = enabled && cell.isAvailable,
        shape = cellShape(cell.state),
        color = containerColor,
        border = border,
        shadowElevation = when {
            isSelected -> 6.dp
            owner != null -> 0.dp
            else -> 1.5.dp
        },
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val scale = selectScale * pulse.value
                scaleX = scale
                scaleY = scale
                translationX = shake.value * density.density
            }
            .semantics {
                contentDescription = description
                selected = isSelected
                role = Role.Button
            }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = cell.value.toString(),
                style = NumberStyle,
                fontSize = numberSize,
                color = when (owner) {
                    Side.PLAYER -> colors.onPrimaryContainer
                    Side.CPU -> colors.onSecondaryContainer
                    null -> colors.onSurface
                },
                modifier = Modifier
                    .alpha(numberAlpha)
                    .clearAndSetSemantics { }
            )

            val badge = when {
                owner == Side.PLAYER -> "✓P"
                owner == Side.CPU -> "C"
                highlight == PairHighlight.MATCH -> "✓"
                highlight == PairHighlight.MISMATCH -> "≠"
                else -> null
            }
            if (badge != null) {
                Text(
                    text = badge,
                    fontSize = badgeSize,
                    fontWeight = FontWeight.Black,
                    color = when {
                        owner == Side.PLAYER -> colors.primary
                        owner == Side.CPU -> colors.secondary
                        highlight == PairHighlight.MISMATCH -> colors.error
                        else -> sideAccent
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = size * 0.06f, end = size * 0.1f)
                        .clearAndSetSemantics { }
                )
            }
            if (isHint && cell.state == CellState.ACTIVE) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = size * 0.1f)
                        .size(size * 0.09f)
                        .background(colors.primary.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
    }
}
