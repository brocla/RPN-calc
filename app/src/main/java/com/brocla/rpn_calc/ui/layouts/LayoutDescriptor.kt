package com.brocla.rpn_calc.ui.layouts

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brocla.rpn_calc.ui.calculator.components.KeyDef

enum class LayoutOrientation { Landscape, Portrait }

sealed interface KeySlot {
    data class Key(val keyDef: KeyDef, val weight: Float = 1f) : KeySlot
    data class Spacer(val weight: Float) : KeySlot
}

data class KeyRow(
    val slots: List<KeySlot>,
    val primaryTopPadding: Dp = 0.dp,
)

data class LayoutDescriptor(
    val name: String,
    val orientation: LayoutOrientation,
    val rows: List<KeyRow>,
)
