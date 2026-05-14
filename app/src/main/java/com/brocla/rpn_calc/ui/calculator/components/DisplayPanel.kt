package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.ui.calculator.AnimationType
import com.brocla.rpn_calc.ui.calculator.CalculatorUiState
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.theme.CalcTheme
import com.brocla.rpn_calc.ui.theme.DisplayTextStyle

private data class XAnimState(
    val display: String,
    val animSeq: Int,
    val animationType: AnimationType,
)

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
        AnnunciatorRow(state = uiState.calcState)
        AnimatedContent(
            targetState = XAnimState(uiState.displayString, uiState.animSeq, uiState.animationType),
            transitionSpec = {
                when (targetState.animationType) {
                    // Digit typing / unary ops: instant swap, no visual animation
                    AnimationType.None ->
                        EnterTransition.None togetherWith ExitTransition.None
                    // ENTER: value "rises" — new X slides in from bottom, old exits to top
                    AnimationType.Enter ->
                        (slideInVertically { it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut())
                    // Binary ops / R↓: stack collapses — Y drops into X
                    // New X enters from top, old X exits to bottom
                    AnimationType.BinaryOp ->
                        (slideInVertically { -it } + fadeIn()) togetherWith
                        (slideOutVertically { it } + fadeOut())
                    // Swap: old X exits upward (going to Y), new X (old Y) enters from top
                    AnimationType.Swap ->
                        (slideInVertically { -it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut())
                }
            },
            label = "xRegister",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { state ->
            Text(
                text = state.display,
                style = DisplayTextStyle,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom),
            )
        }
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
