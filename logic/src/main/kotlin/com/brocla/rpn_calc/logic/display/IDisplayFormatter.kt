package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.model.CalculatorState

interface IDisplayFormatter {
    fun format(state: CalculatorState): String
}
