package com.brocla.rpn_calc.logic.engine

import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.testdoubles.FakeEntryStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Isolation tests for CalculatorEngine using a fake IEntryStateMachine.
 *
 * These tests verify the engine's delegation contract — which operations
 * route to the entry state machine, which handle Idle state themselves,
 * and which commit entry before operating — independently of
 * EntryStateMachine's internal correctness.
 *
 * See CalculatorEngineTest for integration-style tests using real dependencies.
 */
class CalculatorEngineIsolationTest {

    private val fakeEsm = FakeEntryStateMachine()
    private val engine = CalculatorEngine(fakeEsm, MathOperations(), DisplayFormatter())

    private val idle = CalculatorState()
    private val inEntry = idle.copy(entryState = EntryState.Standard("4"))

    // -------------------------------------------------------------------------
    // Entry delegation — engine routes to ESM
    // -------------------------------------------------------------------------

    @Test fun pressDigit_delegates_to_esm() {
        engine.pressDigit(idle, 7)
        assertEquals(7, fakeEsm.lastDigitPressed)
    }

    @Test fun pressDigit_passes_correct_digit() {
        engine.pressDigit(idle, 3)
        assertEquals(3, fakeEsm.lastDigitPressed)
    }

    @Test fun pressDecimal_delegates_to_esm() {
        engine.pressDecimal(idle)
        assertTrue(fakeEsm.pressDecimalCalled)
    }

    @Test fun pressEex_delegates_to_esm() {
        engine.pressEex(idle)
        assertTrue(fakeEsm.pressEexCalled)
    }

    @Test fun pressChs_in_entry_state_delegates_to_esm() {
        engine.pressChs(inEntry)
        assertTrue(fakeEsm.pressChsCalled)
    }

    @Test fun pressBackspace_in_entry_state_delegates_to_esm() {
        engine.pressBackspace(inEntry)
        assertTrue(fakeEsm.pressBackspaceCalled)
    }

    // -------------------------------------------------------------------------
    // Idle-state overrides — engine handles directly, does NOT call ESM
    // -------------------------------------------------------------------------

    @Test fun pressChs_in_idle_state_does_not_delegate_to_esm() {
        engine.pressChs(idle)
        assertFalse(fakeEsm.pressChsCalled,
            "engine must handle CHS in Idle itself (negate X); must not call ESM")
    }

    @Test fun pressChs_in_idle_state_negates_x() {
        val state = idle.copy(stack = idle.stack.withX(5.0))
        val result = engine.pressChs(state)
        assertEquals(-5.0, result.stack.x)
    }

    @Test fun pressBackspace_in_idle_state_does_not_delegate_to_esm() {
        engine.pressBackspace(idle)
        assertFalse(fakeEsm.pressBackspaceCalled,
            "engine must handle Backspace in Idle itself (CLX); must not call ESM")
    }

    @Test fun pressBackspace_in_idle_state_clears_x_to_zero() {
        val state = idle.copy(stack = idle.stack.withX(9.0))
        val result = engine.pressBackspace(state)
        assertEquals(0.0, result.stack.x)
    }

    // -------------------------------------------------------------------------
    // completeEntry called before operations
    // -------------------------------------------------------------------------

    @Test fun pressEnter_calls_completeEntry() {
        engine.pressEnter(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressAdd_calls_completeEntry() {
        engine.pressAdd(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressSubtract_calls_completeEntry() {
        engine.pressSubtract(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressMultiply_calls_completeEntry() {
        engine.pressMultiply(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressDivide_calls_completeEntry() {
        engine.pressDivide(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressSqrt_calls_completeEntry() {
        engine.pressSqrt(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressSto_calls_completeEntry() {
        engine.pressSto(inEntry, register = 0)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressRcl_calls_completeEntry() {
        engine.pressRcl(inEntry, register = 0)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressRollDown_calls_completeEntry() {
        engine.pressRollDown(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressSwap_calls_completeEntry() {
        engine.pressSwap(inEntry)
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    // -------------------------------------------------------------------------
    // completeEntry NOT called for pure entry operations
    // -------------------------------------------------------------------------

    @Test fun pressDigit_does_not_call_completeEntry() {
        engine.pressDigit(inEntry, 5)
        assertEquals(0, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressDecimal_does_not_call_completeEntry() {
        engine.pressDecimal(inEntry)
        assertEquals(0, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressEex_does_not_call_completeEntry() {
        engine.pressEex(inEntry)
        assertEquals(0, fakeEsm.completeEntryCallCount)
    }

    @Test fun pressBackspace_does_not_call_completeEntry() {
        engine.pressBackspace(inEntry)
        assertEquals(0, fakeEsm.completeEntryCallCount)
    }

    // -------------------------------------------------------------------------
    // Error clearing — engine clears error before delegating
    // -------------------------------------------------------------------------

    @Test fun pressDigit_clears_error_before_delegating() {
        val errorState = idle.copy(error = "Overflow")
        val result = engine.pressDigit(errorState, 1)
        assertNull(result.error)
    }

    @Test fun pressAdd_clears_error_before_operating() {
        val errorState = idle.copy(error = "Overflow")
        val result = engine.pressAdd(errorState)
        assertNull(result.error)
    }
}
