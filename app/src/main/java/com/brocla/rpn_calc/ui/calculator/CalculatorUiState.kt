package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.model.CalculatorState

data class CalculatorUiState(
    val calcState: CalculatorState = CalculatorState(),
    val pendingOp: PendingOp = PendingOp.None,
    val displayString: String = "0",
    val yDisplayString: String = "0",
    val animSeq: Int = 0,
    val animationType: AnimationType = AnimationType.None,
)
