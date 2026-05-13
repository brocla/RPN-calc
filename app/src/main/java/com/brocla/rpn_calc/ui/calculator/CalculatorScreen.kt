package com.brocla.rpn_calc.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.ui.calculator.components.DisplayPanel
import com.brocla.rpn_calc.ui.calculator.components.KeyGrid
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.theme.CalcTheme

@Composable
fun CalculatorScreen(
    uiState: CalculatorUiState,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcColors.Body)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        DisplayPanel(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        KeyGrid(
            shiftActive = uiState.calcState.shiftActive,
            onKey = onKey,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.72f),
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
            uiState = CalculatorUiState(displayString = "3.1416"),
            onKey = {},
        )
    }
}
