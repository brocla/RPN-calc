package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

private val ShiftedLabelHeight = 24.dp

@Composable
fun CalcKey(
    def: KeyDef,
    shiftActive: Boolean,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
    primaryTopPadding: Dp = 0.dp,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectiveEvent = if (shiftActive && def.shiftedEvent != CalcKeyEvent.NoOp) {
        def.shiftedEvent
    } else {
        def.event
    }

    val bgColor = if (isPressed) CalcColors.KeyPressed else def.keyColor
    val shape = RoundedCornerShape(4.dp)

    Box(
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
        // Primary label — drawn first (behind), offset controlled per row via primaryTopPadding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = primaryTopPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (def.customLabel != null) {
                def.customLabel.invoke(def.labelColor, def.primaryLabelSize)
            } else if (def.primaryLabel.isNotEmpty()) {
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

        // Shifted label — drawn last (on top); no fixed height so descenders aren't clipped
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.TopCenter)
                .padding(top = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
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
        }
    }
}
