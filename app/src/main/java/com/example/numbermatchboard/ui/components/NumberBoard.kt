package com.example.numbermatchboard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.numbermatchboard.game.logic.NumberBoardGenerator
import com.example.numbermatchboard.game.model.Coordinate
import com.example.numbermatchboard.game.model.NumberCell
import com.example.numbermatchboard.game.model.PairType
import com.example.numbermatchboard.game.model.PendingPair
import com.example.numbermatchboard.game.model.Side

private const val ROWS = NumberBoardGenerator.ROWS
private const val COLUMNS = NumberBoardGenerator.COLUMNS

/** Feedback label shown on the connecting line: "IDENTICAL", "SUM 10" or "11 ≠ 10". */
fun pendingPairLabel(pending: PendingPair): String = when (pending.pairType) {
    PairType.IDENTICAL -> "IDENTICAL"
    PairType.SUM_TO_TEN -> "SUM 10"
    null -> "${pending.sum} ≠ 10"
}

@Composable
fun NumberBoard(
    cells: List<NumberCell>,
    currentSide: Side,
    hintCellIds: Set<Int>,
    pendingPair: PendingPair?,
    showCoordinates: Boolean,
    animationsEnabled: Boolean,
    inputEnabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxCellSize: Dp = 76.dp
) {
    val byCoordinate = remember(cells) { cells.associateBy { it.coordinate } }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val labelSpace = if (showCoordinates) 18.dp else 0.dp
        val gap = if (min(maxWidth, maxHeight) < 340.dp) 5.dp else 7.dp
        val available = min(maxWidth, maxHeight) - labelSpace
        val cellSize = ((available - gap * (COLUMNS - 1)) / COLUMNS).coerceIn(30.dp, maxCellSize)
        val boardSize = cellSize * COLUMNS + gap * (COLUMNS - 1)

        Column {
            if (showCoordinates) {
                Row(Modifier.padding(start = labelSpace).clearAndSetSemantics { }) {
                    repeat(COLUMNS) { column ->
                        Box(Modifier.width(cellSize).height(labelSpace), contentAlignment = Alignment.Center) {
                            AxisLabel("${column + 1}")
                        }
                        if (column < COLUMNS - 1) Spacer(Modifier.width(gap))
                    }
                }
            }
            Row {
                if (showCoordinates) {
                    Column(Modifier.clearAndSetSemantics { }) {
                        repeat(ROWS) { row ->
                            Box(Modifier.width(labelSpace).height(cellSize), contentAlignment = Alignment.Center) {
                                AxisLabel("${'A' + row}")
                            }
                            if (row < ROWS - 1) Spacer(Modifier.height(gap))
                        }
                    }
                }
                Box(Modifier.size(boardSize)) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(ROWS) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                repeat(COLUMNS) { column ->
                                    val cell = byCoordinate[Coordinate(row, column)]
                                    if (cell == null) {
                                        Spacer(Modifier.size(cellSize))
                                    } else {
                                        val inPending = pendingPair != null &&
                                            (cell.id == pendingPair.firstCellId || cell.id == pendingPair.secondCellId)
                                        val highlight = when {
                                            !inPending -> PairHighlight.NONE
                                            pendingPair.isMatch -> PairHighlight.MATCH
                                            else -> PairHighlight.MISMATCH
                                        }
                                        BoardCell(
                                            cell = cell,
                                            size = cellSize,
                                            isHint = cell.id in hintCellIds,
                                            highlight = highlight,
                                            selectingSide = currentSide,
                                            animationsEnabled = animationsEnabled,
                                            enabled = inputEnabled,
                                            onClick = { onCellClick(cell.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (pendingPair != null) {
                        val first = cells.firstOrNull { it.id == pendingPair.firstCellId }
                        val second = cells.firstOrNull { it.id == pendingPair.secondCellId }
                        if (first != null && second != null) {
                            PairConnector(
                                from = first.coordinate,
                                to = second.coordinate,
                                pending = pendingPair,
                                cellSize = cellSize,
                                gap = gap,
                                boardSize = boardSize,
                                animationsEnabled = animationsEnabled
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

@Composable
private fun PairConnector(
    from: Coordinate,
    to: Coordinate,
    pending: PendingPair,
    cellSize: Dp,
    gap: Dp,
    boardSize: Dp,
    animationsEnabled: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val progress = remember(pending) { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(pending) {
        if (animationsEnabled) progress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    val lineColor = when {
        !pending.isMatch -> colors.error
        pending.side == Side.PLAYER -> colors.primary
        else -> colors.secondary
    }
    val density = LocalDensity.current
    val step = with(density) { (cellSize + gap).toPx() }
    val half = with(density) { cellSize.toPx() / 2f }
    val start = Offset(from.column * step + half, from.row * step + half)
    val end = Offset(to.column * step + half, to.row * step + half)

    Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        val current = start + (end - start) * progress.value
        drawLine(
            color = lineColor.copy(alpha = if (pending.isMatch) 0.75f else 0.5f),
            start = start,
            end = current,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (pending.isMatch) null else PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }

    val boardPx = with(density) { boardSize.toPx() }
    val mid = (start + end) / 2f
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (pending.isMatch) lineColor else colors.errorContainer,
        contentColor = if (pending.isMatch) {
            if (pending.side == Side.PLAYER) colors.onPrimary else colors.onSecondary
        } else {
            colors.onErrorContainer
        },
        shadowElevation = 4.dp,
        modifier = Modifier
            .clearAndSetSemantics { }
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                val x = (mid.x - placeable.width / 2f).coerceIn(0f, (boardPx - placeable.width).coerceAtLeast(0f))
                val y = (mid.y - placeable.height / 2f).coerceIn(0f, (boardPx - placeable.height).coerceAtLeast(0f))
                layout(placeable.width, placeable.height) {
                    placeable.place(x.toInt(), y.toInt())
                }
            }
    ) {
        Text(
            text = pendingPairLabel(pending),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
