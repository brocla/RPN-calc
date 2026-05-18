package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.ui.calculator.AnimationType
import com.brocla.rpn_calc.ui.calculator.CalculatorUiState
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.theme.CalcTheme
import com.brocla.rpn_calc.ui.theme.DisplayTextStyle

private data class RegAnimState(
    val display: String,
    val animSeq: Int,
    val animationType: AnimationType,
)

/** Transition for the X register (same as landscape DisplayPanel). */
private fun xTransition(animationType: AnimationType): ContentTransform = when (animationType) {
    AnimationType.None ->
        EnterTransition.None togetherWith ExitTransition.None
    AnimationType.Enter ->
        (slideInVertically { it } + fadeIn()) togetherWith
        (slideOutVertically { -it } + fadeOut())
    AnimationType.BinaryOp ->
        (slideInVertically { -it } + fadeIn()) togetherWith
        (slideOutVertically { it } + fadeOut())
    AnimationType.Swap ->
        (slideInVertically { -it } + fadeIn()) togetherWith
        (slideOutVertically { -it } + fadeOut())
}

/** Transition for the Y register — mirrors X for most cases, inverts for Swap. */
private fun yTransition(animationType: AnimationType): ContentTransform = when (animationType) {
    AnimationType.None ->
        EnterTransition.None togetherWith ExitTransition.None
    AnimationType.Enter ->
        (slideInVertically { it } + fadeIn()) togetherWith
        (slideOutVertically { -it } + fadeOut())
    AnimationType.BinaryOp ->
        (slideInVertically { -it } + fadeIn()) togetherWith
        (slideOutVertically { it } + fadeOut())
    AnimationType.Swap ->
        (slideInVertically { it } + fadeIn()) togetherWith
        (slideOutVertically { it } + fadeOut())
}

@Composable
fun PortraitDisplayPanel(
    uiState: CalculatorUiState,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(3.dp, CalcColors.DisplayBezel, shape)
            .background(CalcColors.DisplayBg, shape)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp)
            .pointerInput(onLongPress) {
                detectTapGestures(onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                })
            },
    ) {
        AnnunciatorRow(state = uiState.calcState)
        AnimatedContent(
            targetState = RegAnimState(uiState.yDisplayString, uiState.animSeq, uiState.animationType),
            transitionSpec = { yTransition(targetState.animationType) },
            label = "yRegister",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { state ->
            Text(
                text = state.display,
                style = DisplayTextStyle,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom),
            )
        }
        AnimatedContent(
            targetState = RegAnimState(uiState.displayString, uiState.animSeq, uiState.animationType),
            transitionSpec = { xTransition(targetState.animationType) },
            label = "xRegister",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { state ->
            Text(
                text = state.display,
                style = DisplayTextStyle,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom),
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 140, showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun PortraitDisplayPanelPreview() {
    CalcTheme {
        PortraitDisplayPanel(
            uiState = CalculatorUiState(displayString = "3.1416", yDisplayString = "2.7183"),
        )
    }
}
