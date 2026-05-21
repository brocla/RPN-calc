package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.logic.entry.IEntryStateMachine
import com.brocla.rpn_calc.logic.model.CalculatorState

/**
 * Fake IEntryStateMachine for CalcStateRepository isolation tests (app module).
 *
 * All methods return [state] unchanged by default (transparent pass-through).
 * [completeEntryCallCount] lets tests verify how many times completeEntry is called.
 *
 * Note: a separate FakeEntryStateMachine exists in the logic module's testdoubles for
 * CalculatorEngine isolation tests; test source sets are not shared between modules.
 */
class FakeEntryStateMachine : IEntryStateMachine {

    /** Number of times completeEntry was called. */
    var completeEntryCallCount = 0

    override fun pressDigit(state: CalculatorState, digit: Int) = state
    override fun pressDecimal(state: CalculatorState) = state
    override fun pressChs(state: CalculatorState) = state
    override fun pressEex(state: CalculatorState) = state
    override fun pressBackspace(state: CalculatorState) = state
    override fun completeEntry(state: CalculatorState) = state.also { completeEntryCallCount++ }
    override fun currentDisplayValue(state: CalculatorState) = state.stack.x
}
