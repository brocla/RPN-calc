package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.model.CalculatorState

data class CalculatorUiState(
    val calcState: CalculatorState = CalculatorState(),
    val pendingOp: PendingOp = PendingOp.None,
    val displayString: String = "0",
)
