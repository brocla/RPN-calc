package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.logic.entry.IEntryStateMachine
import com.brocla.rpn_calc.logic.model.CalculatorState

/**
 * Fake IEntryStateMachine for CalculatorEngine isolation tests.
 *
 * All methods return [state] unchanged by default (transparent pass-through).
 * Call-recording fields let tests verify which methods the engine delegates to
 * and how many times completeEntry is called.
 */
class FakeEntryStateMachine : IEntryStateMachine {

    /** Number of times completeEntry was called. */
    var completeEntryCallCount = 0

    /** The digit passed to the last pressDigit call, or null if never called. */
    var lastDigitPressed: Int? = null

    var pressDecimalCalled = false
    var pressChsCalled = false
    var pressEexCalled = false
    var pressBackspaceCalled = false

    override fun pressDigit(state: CalculatorState, digit: Int) =
        state.also { lastDigitPressed = digit }

    override fun pressDecimal(state: CalculatorState) =
        state.also { pressDecimalCalled = true }

    override fun pressChs(state: CalculatorState) =
        state.also { pressChsCalled = true }

    override fun pressEex(state: CalculatorState) =
        state.also { pressEexCalled = true }

    override fun pressBackspace(state: CalculatorState) =
        state.also { pressBackspaceCalled = true }

    override fun completeEntry(state: CalculatorState) =
        state.also { completeEntryCallCount++ }

    override fun currentDisplayValue(state: CalculatorState) = state.stack.x
}
