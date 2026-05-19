package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.model.CalculatorState

interface IDisplayFormatter {
    fun formatResult(state: CalculatorState): DisplayResult
    fun format(state: CalculatorState): String = when (val r = formatResult(state)) {
        is DisplayResult.Text       -> r.string
        is DisplayResult.RangeError -> r.label
    }
}
