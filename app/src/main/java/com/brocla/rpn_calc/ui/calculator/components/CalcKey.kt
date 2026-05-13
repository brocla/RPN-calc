package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.theme.Helvetica
import com.brocla.rpn_calc.ui.theme.mixedFontLabel

@Composable
fun CalcKey(
    def: KeyDef,
    shiftActive: Boolean,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // When shift is active: use the shifted event if available, otherwise primary
    val effectiveEvent = if (shiftActive && def.shiftedEvent != CalcKeyEvent.NoOp) {
        def.shiftedEvent
    } else {
        def.event
    }

    val bgColor = if (isPressed) CalcColors.KeyPressed else def.keyColor
    val shape = RoundedCornerShape(4.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(2.dp)
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onKey(effectiveEvent)
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
        ) {
            // Shifted label — amber, small, top of key
            if (def.shiftedLabel.isNotEmpty()) {
                Text(
                    text = mixedFontLabel(def.shiftedLabel, timesScale = 1.15f),
                    fontFamily = Helvetica,
                    color = if (shiftActive) CalcColors.LabelShifted.copy(alpha = 1f)
                            else CalcColors.LabelShifted.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Primary label — white, larger, bottom of key
            if (def.primaryLabel.isNotEmpty()) {
                Text(
                    text = mixedFontLabel(def.primaryLabel, timesScale = 1.15f),
                    fontFamily = Helvetica,
                    color = def.labelColor,
                    fontSize = def.primaryLabelSize,
                    lineHeight = def.primaryLineHeight,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
