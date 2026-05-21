package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.display.DisplayResult
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.Stack
import com.brocla.rpn_calc.testdoubles.FakeCalcStateRepository
import com.brocla.rpn_calc.testdoubles.FakeCalculatorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Isolation tests for CalculatorViewModel using fake dependencies.
 *
 * These tests verify the ViewModel's own logic — pending op state machine,
 * error clearing, finalizeState promotion, animation types, repository
 * interaction, and init loading — independently of CalculatorEngine and
 * CalcStateRepository correctness.
 *
 * See CalculatorViewModelTest for integration-style tests using real dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelIsolationTest {

    private lateinit var fakeEngine: FakeCalculatorEngine
    private lateinit var fakeRepo: FakeCalcStateRepository
    private lateinit var vm: CalculatorViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fakeEngine = FakeCalculatorEngine()
        fakeRepo = FakeCalcStateRepository()
        vm = CalculatorViewModel(fakeEngine, fakeRepo, ClipboardParserImpl())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun key(event: CalcKeyEvent) = vm.onKey(event)
    private val ui get() = vm.uiState.value
    private val cs get() = vm.uiState.value.calcState
    private val pending get() = vm.uiState.value.pendingOp

    // -------------------------------------------------------------------------
    // PendingOp state machine
    // -------------------------------------------------------------------------

    @Test fun sto_key_registers_pendingOp_and_does_not_call_engine() {
        key(CalcKeyEvent.Sto)
        assertEquals(PendingOp.Sto, pending)
        assertNull(fakeEngine.lastStoRegister, "engine.pressSto must not be called until digit follows")
    }

    @Test fun sto_then_digit_calls_engine_pressSto_with_correct_register() {
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Digit(3))
        assertEquals(3, fakeEngine.lastStoRegister)
        assertEquals(PendingOp.None, pending, "pendingOp must be cleared after resolution")
    }

    @Test fun rcl_then_digit_calls_engine_pressRcl_with_correct_register() {
        key(CalcKeyEvent.Rcl)
        key(CalcKeyEvent.Digit(7))
        assertEquals(7, fakeEngine.lastRclRegister)
        assertEquals(PendingOp.None, pending)
    }

    @Test fun fix_then_digit_calls_engine_pressFixMode_with_correct_dp() {
        key(CalcKeyEvent.FixArg)
        key(CalcKeyEvent.Digit(4))
        assertEquals(4, fakeEngine.lastFixDp)
        assertEquals(PendingOp.None, pending)
    }

    @Test fun sci_then_digit_calls_engine_pressSciMode_with_correct_dp() {
        key(CalcKeyEvent.SciArg)
        key(CalcKeyEvent.Digit(2))
        assertEquals(2, fakeEngine.lastSciDp)
        assertEquals(PendingOp.None, pending)
    }

    @Test fun eng_then_digit_calls_engine_pressEngMode_with_correct_dp() {
        key(CalcKeyEvent.EngArg)
        key(CalcKeyEvent.Digit(6))
        assertEquals(6, fakeEngine.lastEngDp)
        assertEquals(PendingOp.None, pending)
    }

    @Test fun pendingOp_cancelled_by_non_digit_key() {
        key(CalcKeyEvent.Sto)
        assertEquals(PendingOp.Sto, pending)
        key(CalcKeyEvent.Add)   // non-digit cancels the op
        assertEquals(PendingOp.None, pending)
        assertNull(fakeEngine.lastStoRegister, "engine.pressSto must not be called when op is cancelled")
    }

    // -------------------------------------------------------------------------
    // Error clearing
    // -------------------------------------------------------------------------

    @Test fun any_key_when_error_present_clears_error() {
        // Put the VM into error state by forcing finalizeState to see a RangeError
        fakeEngine.displayResult = DisplayResult.RangeError.Overflow
        key(CalcKeyEvent.Add)
        assertNotNull(cs.error, "error must be set before test proceeds")

        fakeEngine.displayResult = DisplayResult.Text(" 0.")
        key(CalcKeyEvent.Digit(1))  // any key clears the error
        assertNull(cs.error)
    }

    @Test fun error_clearing_key_is_not_also_executed() {
        // Set engine to produce an error
        fakeEngine.displayResult = DisplayResult.RangeError.Overflow
        key(CalcKeyEvent.Add)
        assertNotNull(cs.error)

        // Clear the error — the clearing key (Sto) must NOT register a pendingOp
        fakeEngine.displayResult = DisplayResult.Text(" 0.")
        key(CalcKeyEvent.Sto)
        assertNull(cs.error, "error must be cleared")
        assertEquals(PendingOp.None, pending, "Sto must not have been processed as a key, only as error-clear")
    }

    @Test fun savedState_is_set_when_error_first_occurs() {
        val stateBeforeError = cs
        fakeEngine.displayResult = DisplayResult.RangeError.Overflow
        key(CalcKeyEvent.Add)
        assertNotNull(ui.savedState, "savedState must capture the pre-error state")
        assertEquals(stateBeforeError, ui.savedState)
    }

    @Test fun savedState_is_null_after_error_is_cleared() {
        fakeEngine.displayResult = DisplayResult.RangeError.Overflow
        key(CalcKeyEvent.Add)
        fakeEngine.displayResult = DisplayResult.Text(" 0.")
        key(CalcKeyEvent.Digit(1))
        assertNull(ui.savedState, "savedState must be cleared after error is dismissed")
    }

    // -------------------------------------------------------------------------
    // finalizeState — RangeError promotion
    // -------------------------------------------------------------------------

    @Test fun rangeError_overflow_is_promoted_to_cs_error() {
        fakeEngine.displayResult = DisplayResult.RangeError.Overflow
        key(CalcKeyEvent.Add)
        assertEquals("Overflow", cs.error)
    }

    @Test fun rangeError_underflow_is_promoted_to_cs_error() {
        fakeEngine.displayResult = DisplayResult.RangeError.Underflow
        key(CalcKeyEvent.Add)
        assertEquals("Underflow", cs.error)
    }

    @Test fun normal_display_result_does_not_set_error() {
        fakeEngine.displayResult = DisplayResult.Text(" 1.23456  00")
        key(CalcKeyEvent.Add)
        assertNull(cs.error)
    }

    // -------------------------------------------------------------------------
    // Repository interaction
    // -------------------------------------------------------------------------

    @Test fun every_key_press_triggers_repository_save() {
        key(CalcKeyEvent.Digit(1))
        key(CalcKeyEvent.Digit(2))
        key(CalcKeyEvent.Add)
        assertEquals(3, fakeRepo.savedStates.size,
            "repository.save must be called once per key press")
    }

    @Test fun reset_calls_repository_clear() {
        vm.reset()
        assertEquals(1, fakeRepo.clearCallCount)
    }

    @Test fun init_loads_saved_state_from_repository() {
        val savedStack = Stack(x = 42.0, y = 7.0, z = 0.0, t = 0.0)
        val savedState = CalculatorState(stack = savedStack)
        val repoWithState = FakeCalcStateRepository(initial = savedState)

        Dispatchers.setMain(UnconfinedTestDispatcher())
        val vmWithSavedState = CalculatorViewModel(fakeEngine, repoWithState, ClipboardParserImpl())

        assertEquals(42.0, vmWithSavedState.uiState.value.calcState.stack.x,
            "ViewModel must load X from repository on init")
        assertEquals(7.0, vmWithSavedState.uiState.value.calcState.stack.y,
            "ViewModel must load Y from repository on init")
    }

    @Test fun init_with_no_saved_state_uses_default() {
        // fakeRepo has initial = null (default setUp)
        assertEquals(0.0, cs.stack.x, "default stack X must be 0.0 when no saved state")
    }

    // -------------------------------------------------------------------------
    // Animation types
    // -------------------------------------------------------------------------

    @Test fun enter_key_produces_enter_animation() {
        key(CalcKeyEvent.Enter)
        assertEquals(AnimationType.Enter, ui.animationType)
    }

    @Test fun binary_op_keys_produce_binaryOp_animation() {
        key(CalcKeyEvent.Add)
        assertEquals(AnimationType.BinaryOp, ui.animationType)
        key(CalcKeyEvent.Subtract)
        assertEquals(AnimationType.BinaryOp, ui.animationType)
        key(CalcKeyEvent.Multiply)
        assertEquals(AnimationType.BinaryOp, ui.animationType)
        key(CalcKeyEvent.Divide)
        assertEquals(AnimationType.BinaryOp, ui.animationType)
    }

    @Test fun swap_key_produces_swap_animation() {
        key(CalcKeyEvent.Swap)
        assertEquals(AnimationType.Swap, ui.animationType)
    }

    @Test fun digit_key_produces_no_animation() {
        key(CalcKeyEvent.Digit(5))
        assertEquals(AnimationType.None, ui.animationType)
    }

    // -------------------------------------------------------------------------
    // Shift latch
    // -------------------------------------------------------------------------

    @Test fun shift_key_activates_shiftActive() {
        key(CalcKeyEvent.Shift)
        assertTrue(cs.shiftActive)
    }

    @Test fun non_shift_key_after_shift_clears_shiftActive() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.Add)
        assertTrue(!cs.shiftActive)
    }
}
