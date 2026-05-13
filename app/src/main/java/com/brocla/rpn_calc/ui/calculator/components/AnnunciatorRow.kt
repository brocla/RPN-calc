package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.ui.theme.AnnunciatorTextStyle
import com.brocla.rpn_calc.ui.theme.CalcColors

@Composable
fun AnnunciatorRow(
    state: CalculatorState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Display mode group
        AnnunciatorLabel("FIX", state.displaySettings.mode is DisplayMode.Fix)
        AnnunciatorLabel("SCI", state.displaySettings.mode is DisplayMode.Sci)
        AnnunciatorLabel("ENG", state.displaySettings.mode is DisplayMode.Eng)
        AnnunciatorLabel("ALL", state.displaySettings.mode is DisplayMode.All)

        Spacer(modifier = Modifier.weight(1f))

        // Angle mode
        AnnunciatorLabel("DEG", state.angleMode == AngleMode.DEG)
        AnnunciatorLabel("RAD", state.angleMode == AngleMode.RAD)

        Spacer(modifier = Modifier.weight(1f))

        // Shift latch
        AnnunciatorLabel(
            label = "f",
            active = state.shiftActive,
            activeColor = CalcColors.KeyShift,
        )
    }
}

@Composable
private fun AnnunciatorLabel(
    label: String,
    active: Boolean,
    activeColor: Color = CalcColors.AnnunciatorOn,
) {
    Text(
        text = label,
        style = AnnunciatorTextStyle,
        color = if (active) activeColor else CalcColors.AnnunciatorOff,
        modifier = Modifier.padding(horizontal = 3.dp),
    )
}
