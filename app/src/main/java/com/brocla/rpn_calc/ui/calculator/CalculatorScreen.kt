package com.brocla.rpn_calc.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun CalculatorScreen(
    uiState: CalculatorUiState,
    activeLayout: LayoutDescriptor,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
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
                uiState  = uiState,
                modifier = Modifier.fillMaxWidth().weight(displayWeight),
            )
        } else {
            DisplayPanel(
                uiState  = uiState,
                modifier = Modifier.fillMaxWidth().weight(displayWeight),
            )
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
                        },
                    )
                },
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
