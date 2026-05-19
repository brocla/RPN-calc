package com.brocla.rpn_calc.ui.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.calculator.components.CalcKey

@Composable
fun LayoutRenderer(
    layout: LayoutDescriptor,
    shiftActive: Boolean,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        layout.rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.slots.forEach { slot ->
                    when (slot) {
                        is KeySlot.Key ->
                            CalcKey(
                                def = slot.keyDef,
                                shiftActive = shiftActive,
                                onKey = onKey,
                                modifier = Modifier.weight(slot.weight).fillMaxHeight(),
                                primaryTopPadding = row.primaryTopPadding,
                                cornerRadius = slot.cornerRadius,
                                onLongPress = if (slot.longPressEvent != CalcKeyEvent.NoOp)
                                    { { onKey(slot.longPressEvent) } } else null,
                            )
                        is KeySlot.Spacer ->
                            Spacer(modifier = Modifier.weight(slot.weight))
                    }
                }
            }
        }
    }
}
