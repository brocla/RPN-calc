package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.ui.calculator.CalculatorUiState
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.theme.CalcTheme
import com.brocla.rpn_calc.ui.theme.DisplayTextStyle

@Composable
fun DisplayPanel(
    uiState: CalculatorUiState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(3.dp, CalcColors.DisplayBezel, shape)
            .background(CalcColors.DisplayBg, shape)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 0.dp),
    ) {
        Text(
            text = uiState.displayString,
            style = DisplayTextStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .wrapContentHeight(Alignment.Bottom),
        )
        AnnunciatorRow(state = uiState.calcState)
    }
}

@Preview(widthDp = 800, heightDp = 100, showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun DisplayPanelPreview() {
    CalcTheme {
        DisplayPanel(
            uiState = CalculatorUiState(displayString = "3.1416"),
        )
    }
}
