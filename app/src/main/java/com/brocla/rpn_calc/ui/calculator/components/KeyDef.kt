package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.theme.CalcColors

data class KeyDef(
    val primaryLabel: String,
    val shiftedLabel: String = "",
    val event: CalcKeyEvent,
    val shiftedEvent: CalcKeyEvent = CalcKeyEvent.NoOp,
    val keyColor: Color = CalcColors.KeyTop,
    val labelColor: Color = CalcColors.LabelPrimary,
    val primaryLabelSize: TextUnit = 26.sp,
    val primaryLineHeight: TextUnit = TextUnit.Unspecified,
)
