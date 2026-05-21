package com.brocla.rpn_calc.logic.engine

import com.brocla.rpn_calc.logic.display.DisplayResult
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.testdoubles.FakeDisplayFormatter
import com.brocla.rpn_calc.testdoubles.FakeEntryStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Isolation tests for CalculatorEngine's display/formatter contract.
 *
 * These tests verify that the engine correctly delegates display queries
 * to IDisplayFormatter and that getDisplay unwraps DisplayResult properly.
 * They do not test DisplayFormatter's internal formatting logic.
 *
 * See DisplayFormatterTest for the formatter's own behaviour.
 * See CalculatorEngineTest for integration-style tests using real dependencies.
 */
class CalculatorEngineDisplayTest {

    private val fakeFormatter = FakeDisplayFormatter()
    private val engine = CalculatorEngine(FakeEntryStateMachine(), MathOperations(), fakeFormatter)
    private val idle = CalculatorState()

    // -------------------------------------------------------------------------
    // getDisplayResult — delegation and state forwarding
    // -------------------------------------------------------------------------

    @Test fun getDisplayResult_returns_what_formatter_returns() {
        fakeFormatter.result = DisplayResult.Text(" 1.5  ")
        assertEquals(DisplayResult.Text(" 1.5  "), engine.getDisplayResult(idle))
    }

    @Test fun getDisplayResult_passes_state_to_formatter() {
        engine.getDisplayResult(idle)
        assertEquals(idle, fakeFormatter.lastState)
    }

    // -------------------------------------------------------------------------
    // getDisplay — DisplayResult unwrapping
    // -------------------------------------------------------------------------

    @Test fun getDisplay_returns_string_from_text_result() {
        fakeFormatter.result = DisplayResult.Text(" 3.14159")
        assertEquals(" 3.14159", engine.getDisplay(idle))
    }

    @Test fun getDisplay_returns_label_for_overflow() {
        fakeFormatter.result = DisplayResult.RangeError.Overflow
        assertEquals("Overflow", engine.getDisplay(idle))
    }

    @Test fun getDisplay_returns_label_for_underflow() {
        fakeFormatter.result = DisplayResult.RangeError.Underflow
        assertEquals("Underflow", engine.getDisplay(idle))
    }
}
