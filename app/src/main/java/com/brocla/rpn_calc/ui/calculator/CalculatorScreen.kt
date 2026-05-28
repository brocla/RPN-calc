package com.brocla.rpn_calc.ui.calculator

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.ui.calculator.components.DisplayPanel
import com.brocla.rpn_calc.ui.calculator.components.PortraitDisplayPanel
import com.brocla.rpn_calc.ui.layouts.ClassicLandscapeLayout
import com.brocla.rpn_calc.ui.layouts.LayoutDescriptor
import com.brocla.rpn_calc.ui.layouts.LayoutOrientation
import com.brocla.rpn_calc.ui.layouts.LayoutRenderer
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.theme.CalcTheme
import com.brocla.rpn_calc.voice.VoiceState

private val TRANSCRIPT_FONT_SIZE = 18.sp
private val TRANSCRIPT_BAR_COLOR = Color(0xFF1A1A2E)

@Composable
fun CalculatorScreen(
    uiState: CalculatorUiState,
    activeLayout: LayoutDescriptor,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDisplayLongPress: () -> Unit = {},
    voiceDebugText: String = "",
    voiceState: VoiceState = VoiceState.Idle,
) {
    val isPortrait = activeLayout.orientation == LayoutOrientation.Portrait
    val displayWeight = if (isPortrait) 0.20f else 0.28f
    val gridWeight    = 1f - displayWeight

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcColors.Body)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (isPortrait) {
            PortraitDisplayPanel(
                uiState     = uiState,
                modifier    = Modifier.fillMaxWidth().weight(displayWeight),
                onLongPress = onDisplayLongPress,
            )
        } else {
            DisplayPanel(
                uiState     = uiState,
                modifier    = Modifier.fillMaxWidth().weight(displayWeight),
                onLongPress = onDisplayLongPress,
            )
        }
        if (voiceState is VoiceState.Listening) {
            VoiceTranscriptBar(text = voiceDebugText)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LayoutRenderer(
            layout      = activeLayout,
            shiftActive = uiState.calcState.shiftActive,
            onKey       = onKey,
            modifier    = Modifier
                .fillMaxWidth()
                .weight(gridWeight)
                .pointerInput(Unit) {
                    var totalDragY = 0f
                    var triggered  = false
                    val thresholdPx = 40.dp.toPx()
                    detectVerticalDragGestures(
                        onDragStart = { totalDragY = 0f; triggered = false },
                        onVerticalDrag = { _, dragAmount ->
                            totalDragY += dragAmount
                            if (!triggered && totalDragY < -thresholdPx) {
                                triggered = true
                                onKey(CalcKeyEvent.Enter)
                            }
                            if (!triggered && totalDragY > thresholdPx) {
                                triggered = true
                                onKey(CalcKeyEvent.ToggleMic)
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun VoiceTranscriptBar(text: String) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text) { scrollState.scrollTo(scrollState.maxValue) }

    val cursorAlpha by rememberInfiniteTransition(label = "cursor-blink")
        .animateFloat(
            initialValue  = 1f,
            targetValue   = 0f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label         = "cursor-alpha",
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(TRANSCRIPT_BAR_COLOR)
            .horizontalScroll(scrollState)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text       = text,
            color      = Color.Yellow,
            fontSize   = TRANSCRIPT_FONT_SIZE,
            fontFamily = FontFamily.Monospace,
            maxLines   = 1,
        )
        Text(
            text       = " ▎",
            color      = Color.Yellow,
            fontSize   = TRANSCRIPT_FONT_SIZE,
            fontFamily = FontFamily.Monospace,
            maxLines   = 1,
            modifier   = Modifier.graphicsLayer { alpha = cursorAlpha },
        )
    }
}

@Preview(
    name = "Calculator — Landscape",
    widthDp = 800,
    heightDp = 360,
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
)
@Composable
private fun CalculatorScreenPreview() {
    CalcTheme {
        CalculatorScreen(
            uiState      = CalculatorUiState(displayString = "3.1416"),
            activeLayout = ClassicLandscapeLayout,
            onKey        = {},
        )
    }
}
