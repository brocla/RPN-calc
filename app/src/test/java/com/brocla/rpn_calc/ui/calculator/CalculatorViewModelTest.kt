package com.brocla.rpn_calc.ui.calculator

import androidx.lifecycle.SavedStateHandle
import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.DisplayMode
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorViewModelTest {

    private lateinit var vm: CalculatorViewModel

    @Before
    fun setUp() {
        val engine = CalculatorEngine(EntryStateMachine(), MathOperations(), DisplayFormatter())
        vm = CalculatorViewModel(engine, SavedStateHandle())
    }

    private fun key(event: CalcKeyEvent) = vm.onKey(event)
    private val state get() = vm.uiState.value.calcState
    private val pending get() = vm.uiState.value.pendingOp
    private val display get() = vm.uiState.value.displayString

    // -----------------------------------------------------------------------
    // PendingOp — STO
    // -----------------------------------------------------------------------

    @Test
    fun sto_armsPendingOp() {
        key(CalcKeyEvent.Sto)
        assertEquals(PendingOp.Sto, pending)
    }

    @Test
    fun sto_digitStores() {
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Digit(3))
        assertEquals(PendingOp.None, pending)
        assertEquals(5.0, state.memory[3])
    }

    @Test
    fun sto_nonDigitCancels() {
        // Enter two numbers, then STO + non-digit
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(4))
        key(CalcKeyEvent.Sto)
        assertEquals(PendingOp.Sto, pending)
        key(CalcKeyEvent.Add)      // cancels STO and executes Add
        assertEquals(PendingOp.None, pending)
        assertEquals(7.0, state.stack.x)
    }

    @Test
    fun sto_cancelDoesNotSwallowKey() {
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Add)      // cancels STO then Add executes: 5+3=8
        assertEquals(8.0, state.stack.x)
    }

    // -----------------------------------------------------------------------
    // PendingOp — RCL
    // -----------------------------------------------------------------------

    @Test
    fun rcl_digitRecalls() {
        // First store a value
        key(CalcKeyEvent.Digit(9))
        key(CalcKeyEvent.Digit(9))
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Digit(2))
        // Now recall it
        key(CalcKeyEvent.Clx)
        key(CalcKeyEvent.Rcl)
        key(CalcKeyEvent.Digit(2))
        assertEquals(PendingOp.None, pending)
        assertEquals(99.0, state.stack.x)
    }

    @Test
    fun rcl_nonDigitCancels() {
        key(CalcKeyEvent.Rcl)
        key(CalcKeyEvent.Add)   // cancels RCL, Add runs normally
        assertEquals(PendingOp.None, pending)
    }

    // -----------------------------------------------------------------------
    // PendingOp — FIX / SCI / ENG
    // -----------------------------------------------------------------------

    @Test
    fun fix_digitSetsMode() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.FixArg)
        key(CalcKeyEvent.Digit(4))
        assertEquals(PendingOp.None, pending)
        val mode = state.displaySettings.mode
        assertTrue(mode is DisplayMode.Fix)
        assertEquals(4, mode.decimalPlaces)
    }

    @Test
    fun fix_nonDigitCancels() {
        val modeBefore = state.displaySettings.mode
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.FixArg)
        key(CalcKeyEvent.Add)   // cancels FixArg, Add runs normally
        assertEquals(PendingOp.None, pending)
        assertEquals(modeBefore, state.displaySettings.mode)
    }

    @Test
    fun sci_digitSetsMode() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.SciArg)
        key(CalcKeyEvent.Digit(2))
        val mode = state.displaySettings.mode
        assertTrue(mode is DisplayMode.Sci)
        assertEquals(2, mode.decimalPlaces)
    }

    @Test
    fun eng_digitSetsMode() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.EngArg)
        key(CalcKeyEvent.Digit(3))
        val mode = state.displaySettings.mode
        assertTrue(mode is DisplayMode.Eng)
        assertEquals(3, mode.decimalPlaces)
    }

    // -----------------------------------------------------------------------
    // Shift latch
    // -----------------------------------------------------------------------

    @Test
    fun shift_activatesLatch() {
        key(CalcKeyEvent.Shift)
        assertTrue(state.shiftActive)
    }

    @Test
    fun shift_againKeepsLatchActive() {
        // HP spec §11.1: second SHIFT does not toggle off
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.Shift)
        assertTrue(state.shiftActive)
    }

    @Test
    fun shift_clearedAfterNonShiftKey() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.Add)
        assertTrue(!state.shiftActive)
    }

    @Test
    fun shift_clearedAfterShiftedFunction() {
        key(CalcKeyEvent.Digit(4))
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.Square)  // shifted √x → x²
        assertTrue(!state.shiftActive)
        assertEquals(16.0, state.stack.x)
    }

    // -----------------------------------------------------------------------
    // Error handling
    // -----------------------------------------------------------------------

    @Test
    fun error_divByZeroShowsError() {
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Reciprocal)
        assertTrue(state.error != null)
    }

    @Test
    fun error_anyKeyClearsError() {
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Reciprocal)
        assertTrue(state.error != null)
        key(CalcKeyEvent.Add)
        assertNull(state.error)
    }

    @Test
    fun error_firstKeyAfterErrorDoesNotExecute() {
        // Set up: 5 on stack, force error
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Reciprocal)     // error: 1/0
        assertTrue(state.error != null)
        val xBeforeClear = 0.0           // engine sets X to 0.0 on error
        key(CalcKeyEvent.Add)            // first key: clears error only
        assertNull(state.error)
        // Add did NOT execute: X is still 0.0, Y is still 5.0
        assertEquals(xBeforeClear, state.stack.x)
        // Now Add executes: 5 + 0 = 5
        key(CalcKeyEvent.Add)
        assertEquals(5.0, state.stack.x)
    }

    // -----------------------------------------------------------------------
    // Display string
    // -----------------------------------------------------------------------

    @Test
    fun displayString_hasCommasForLargeValues() {
        key(CalcKeyEvent.Digit(1))
        repeat(6) { key(CalcKeyEvent.Digit(0)) }   // enters 1000000
        key(CalcKeyEvent.Enter)
        assertTrue(display.contains(","), "Expected commas in '$display'")
    }

    @Test
    fun displayString_noCommasForSciFormat() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.SciArg)
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Digit(1))
        repeat(6) { key(CalcKeyEvent.Digit(0)) }
        key(CalcKeyEvent.Enter)
        assertTrue(!display.contains(","), "SCI display should not contain commas: '$display'")
    }

    // -----------------------------------------------------------------------
    // Regression: decimal key (issue 2)
    // Bug: formatMantissa appended the decimal at the END of the digit string
    // instead of inserting it after the first digit (to match parseMantissa).
    // Typing 3 · 1 4 produced "314." on the display instead of "3.14".
    // -----------------------------------------------------------------------

    @Test
    fun decimal_digitsAfterDecimalAppearRightOfDecimalPoint() {
        // Regression: decimal position is where the key was pressed, not always after first digit.
        // Old bug: formatMantissa packed all digits into one string and always placed the decimal
        // after the first digit, so "3" "1" "." "4" showed "3.14" (correct accident) but
        // "3" "1" "." "4" typed as integer-then-decimal showed wrong position.
        // The real scenario: type decimal AFTER multiple digits.
        key(CalcKeyEvent.Digit(3))
        assertEquals("3", display)

        key(CalcKeyEvent.Digit(1))
        assertEquals("31", display)

        key(CalcKeyEvent.Decimal)
        assertEquals("31.", display)

        key(CalcKeyEvent.Digit(4))
        // Before the fix: display was "3.14" (decimal forced to after first digit)
        assertEquals("31.4", display)
    }

    @Test
    fun decimal_digitsBeforeDecimalAccumulateNormally() {
        // Decimal pressed first, then digits — straightforward case.
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(1))
        key(CalcKeyEvent.Digit(4))
        assertEquals("3.14", display)
    }

    @Test
    fun decimal_enterPushesCorrectValue() {
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Digit(1))
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(4))
        key(CalcKeyEvent.Enter)
        assertEquals(31.4, state.stack.x, 1e-10)
    }

    // -----------------------------------------------------------------------
    // State persistence
    // -----------------------------------------------------------------------

    @Test
    fun persistence_stateRoundTrips() {
        // Set up a non-default state
        key(CalcKeyEvent.Digit(7))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.SciArg)
        key(CalcKeyEvent.Digit(2))
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.DegRad)   // switch to RAD

        val savedHandle = vm.uiState.value.let {
            // Grab the saved state handle by reconstructing with same handle
            SavedStateHandle(mapOf("calculator_state" to
                kotlinx.serialization.json.Json.encodeToString(
                    com.brocla.rpn_calc.logic.model.CalculatorState.serializer(),
                    it.calcState
                )
            ))
        }

        val engine2 = CalculatorEngine(EntryStateMachine(), MathOperations(), DisplayFormatter())
        val vm2 = CalculatorViewModel(engine2, savedHandle)

        assertEquals(state.stack.x, vm2.uiState.value.calcState.stack.x)
        assertEquals(state.displaySettings.mode, vm2.uiState.value.calcState.displaySettings.mode)
        assertEquals(state.angleMode, vm2.uiState.value.calcState.angleMode)
    }
}
