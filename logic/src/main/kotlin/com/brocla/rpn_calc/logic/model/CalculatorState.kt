package com.brocla.rpn_calc.logic.model

import kotlinx.serialization.Serializable

@Serializable
data class CalculatorState(
    val stack: Stack = Stack(),
    val entryState: EntryState = EntryState.Idle,
    val displaySettings: DisplaySettings = DisplaySettings(),
    val angleMode: AngleMode = AngleMode.DEG,
    val memory: List<Double> = List(10) { 0.0 },
    val lastX: Double = 0.0,
    val stackLiftEnabled: Boolean = false,
    val error: String? = null,
    val shiftActive: Boolean = false
)
