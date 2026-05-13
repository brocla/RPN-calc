package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.theme.CalcColors

@Stable
class KeyDef(
    val primaryLabel: String,
    val shiftedLabel: String = "",
    val event: CalcKeyEvent,
    val shiftedEvent: CalcKeyEvent = CalcKeyEvent.NoOp,
    val keyColor: Color = CalcColors.KeyTop,
    val labelColor: Color = CalcColors.LabelPrimary,
    val primaryLabelSize: TextUnit = 26.sp,
    val primaryLineHeight: TextUnit = TextUnit.Unspecified,
    val customLabel: (@Composable (color: Color, fontSize: TextUnit) -> Unit)? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyDef) return false
        return primaryLabel == other.primaryLabel &&
            shiftedLabel == other.shiftedLabel &&
            event == other.event &&
            shiftedEvent == other.shiftedEvent &&
            keyColor == other.keyColor &&
            labelColor == other.labelColor &&
            primaryLabelSize == other.primaryLabelSize &&
            primaryLineHeight == other.primaryLineHeight
    }

    override fun hashCode(): Int {
        var result = primaryLabel.hashCode()
        result = 31 * result + shiftedLabel.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + shiftedEvent.hashCode()
        result = 31 * result + keyColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + primaryLabelSize.hashCode()
        result = 31 * result + primaryLineHeight.hashCode()
        return result
    }
}
