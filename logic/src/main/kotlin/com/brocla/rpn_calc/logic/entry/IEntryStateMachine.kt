package com.brocla.rpn_calc.logic.entry

import com.brocla.rpn_calc.logic.model.CalculatorState

interface IEntryStateMachine {
    fun pressDigit(state: CalculatorState, digit: Int): CalculatorState
    fun pressDecimal(state: CalculatorState): CalculatorState
    fun pressChs(state: CalculatorState): CalculatorState
    fun pressEex(state: CalculatorState): CalculatorState
    fun pressBackspace(state: CalculatorState): CalculatorState
    fun completeEntry(state: CalculatorState): CalculatorState
    fun currentDisplayValue(state: CalculatorState): Double
}
