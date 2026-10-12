package com.example.numbermatchboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.numbermatchboard.ui.theme.NumberStyle
import kotlinx.coroutines.launch

/** A small label + big number pair used on Home, Result and Settings. */
@Composable
fun StatValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueSize: Int = 24
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label $value" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(
            value,
            style = NumberStyle,
            fontSize = valueSize.sp,
            color = valueColor,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/** The brand mark: a small 4 + 6 pair joined by a line. */
@Composable
fun PairMark(modifier: Modifier = Modifier, tile: Dp = 34.dp) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarkTile("4", tile, colors.primaryContainer, colors.onPrimaryContainer)
        Surface(
            color = colors.primary,
            modifier = Modifier.size(width = tile * 0.45f, height = 3.dp)
        ) {}
        MarkTile("6", tile, colors.primaryContainer, colors.onPrimaryContainer)
    }
}

@Composable
private fun MarkTile(text: String, size: Dp, container: Color, content: Color) {
    Surface(
        shape = RoundedCornerShape(26),
        color = container,
        shadowElevation = 2.dp,
        modifier = Modifier.size(size)
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = NumberStyle, fontSize = (size.value * 0.5f).sp, color = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlaySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text("HOW TO PLAY", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            HowToStep("1", "SELECT", "Choose any number on the board.")
            HowToStep("2", "MATCH", "A pair is valid when the numbers are identical OR they add up to 10.\n7 + 7 ✓    4 + 6 = 10 ✓    4 + 7 ✗")
            HowToStep("3", "CAPTURE", "Valid pairs belong to you.")
            HowToStep("4", "CONTINUE", "A successful match gives you another turn. A miss passes the turn to the CPU.")
            HowToStep("5", "WIN", "Capture more pairs than the CPU.")
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("GOT IT")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HowToStep(number: String, title: String, body: String) {
    Row(Modifier.padding(bottom = 16.dp)) {
        Surface(
            shape = RoundedCornerShape(30),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(number, style = NumberStyle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
