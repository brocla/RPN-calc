package com.brocla.rpn_calc.ui.calculator

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.brocla.rpn_calc.data.CalcStateRepository
import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.EntryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var vm: CalculatorViewModel

    private fun testRepository(): CalcStateRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("test.preferences_pb") },
        )
        return CalcStateRepository(dataStore, EntryStateMachine())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val engine = CalculatorEngine(EntryStateMachine(), MathOperations(), DisplayFormatter())
        vm = CalculatorViewModel(engine, testRepository(), ClipboardParserImpl())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(4))
        key(CalcKeyEvent.Sto)
        assertEquals(PendingOp.Sto, pending)
        key(CalcKeyEvent.Add)
        assertEquals(PendingOp.None, pending)
        assertEquals(7.0, state.stack.x)
    }

    @Test
    fun sto_cancelDoesNotSwallowKey() {
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Add)
        assertEquals(8.0, state.stack.x)
    }

    // -----------------------------------------------------------------------
    // PendingOp — RCL
    // -----------------------------------------------------------------------

    @Test
    fun rcl_digitRecalls() {
        key(CalcKeyEvent.Digit(9))
        key(CalcKeyEvent.Digit(9))
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Digit(2))
        key(CalcKeyEvent.Clx)
        key(CalcKeyEvent.Rcl)
        key(CalcKeyEvent.Digit(2))
        assertEquals(PendingOp.None, pending)
        assertEquals(99.0, state.stack.x)
    }

    @Test
    fun rcl_nonDigitCancels() {
        key(CalcKeyEvent.Rcl)
        key(CalcKeyEvent.Add)
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
        key(CalcKeyEvent.Add)
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
        key(CalcKeyEvent.Square)
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
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Reciprocal)
        assertTrue(state.error != null)
        key(CalcKeyEvent.Add)          // clears error — does NOT execute Add
        assertNull(state.error)
        // Pre-error state is restored: user had "0" being typed (entry not yet committed)
        assertTrue(state.entryState is EntryState.Mantissa,
            "Expected entry state restored to Mantissa, got ${state.entryState}")
        key(CalcKeyEvent.Add)          // now executes: commits 0, then Y=5 + X=0 = 5
        assertEquals(5.0, state.stack.x)
    }

    // -----------------------------------------------------------------------
    // Display string
    // -----------------------------------------------------------------------

    @Test
    fun displayString_hasCommasForLargeValues() {
        key(CalcKeyEvent.Digit(1))
        repeat(6) { key(CalcKeyEvent.Digit(0)) }
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
    // Regression: decimal key
    // -----------------------------------------------------------------------

    @Test
    fun decimal_digitsAfterDecimalAppearRightOfDecimalPoint() {
        key(CalcKeyEvent.Digit(3))
        assertEquals("3", display)
        key(CalcKeyEvent.Digit(1))
        assertEquals("31", display)
        key(CalcKeyEvent.Decimal)
        assertEquals("31.", display)
        key(CalcKeyEvent.Digit(4))
        assertEquals("31.4", display)
    }

    @Test
    fun decimal_digitsBeforeDecimalAccumulateNormally() {
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
    // Regression: chained subtraction with decimal operands
    // -----------------------------------------------------------------------

    @Test
    fun subtract_chained_10_enter_dotFive_minus_dotFive_minus_equals_9() {
        key(CalcKeyEvent.Digit(1))
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Subtract)
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Subtract)
        assertEquals(9.0, state.stack.x, 1e-10)
    }

    @Test
    fun subtract_chained_10_enter_0point5_minus_0point5_minus_equals_9() {
        key(CalcKeyEvent.Digit(1))
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Subtract)
        key(CalcKeyEvent.Digit(0))
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Subtract)
        assertEquals(9.0, state.stack.x, 1e-10)
    }

    @Test
    fun eex_fromIdle_afterArithmetic_liftsStack() {
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Add)
        key(CalcKeyEvent.Eex)
        key(CalcKeyEvent.Digit(2))
        key(CalcKeyEvent.Subtract)
        assertEquals(-92.0, state.stack.x, 1e-10)
    }

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    @Test
    fun reset_clearsStack() {
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        key(CalcKeyEvent.Digit(3))
        vm.reset()
        assertEquals(0.0, state.stack.x)
        assertEquals(0.0, state.stack.y)
        assertEquals(0.0, state.stack.z)
        assertEquals(0.0, state.stack.t)
    }

    @Test
    fun reset_clearsMemory() {
        key(CalcKeyEvent.Digit(7))
        key(CalcKeyEvent.Sto)
        key(CalcKeyEvent.Digit(2))
        vm.reset()
        assertEquals(0.0, state.memory[2])
    }

    @Test
    fun reset_clearsDisplayMode() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.FixArg)
        key(CalcKeyEvent.Digit(4))
        vm.reset()
        assertTrue(state.displaySettings.mode is DisplayMode.All)
    }

    @Test
    fun reset_clearsAngleMode() {
        key(CalcKeyEvent.Shift)
        key(CalcKeyEvent.DegRad)
        vm.reset()
        assertEquals(com.brocla.rpn_calc.logic.model.AngleMode.DEG, state.angleMode)
    }

    // -----------------------------------------------------------------------
    // Paste
    // -----------------------------------------------------------------------

    @Test
    fun paste_liftsStackAndPlacesValue() {
        key(CalcKeyEvent.Digit(5))
        key(CalcKeyEvent.Enter)
        vm.pasteFromClipboard("3.0")
        assertEquals(3.0, state.stack.x, 1e-10)
        assertEquals(5.0, state.stack.y, 1e-10)
    }

    @Test
    fun paste_fromIdleLiftsStack() {
        vm.pasteFromClipboard("7.0")
        assertEquals(7.0, state.stack.x, 1e-10)
    }

    @Test
    fun paste_invalidShowsError() {
        vm.pasteFromClipboard("abc")
        assertNotNull(state.error)
    }

    @Test
    fun paste_commitsPartialEntry() {
        key(CalcKeyEvent.Digit(3))
        key(CalcKeyEvent.Decimal)
        key(CalcKeyEvent.Digit(1))
        vm.pasteFromClipboard("9.0")
        assertEquals(9.0, state.stack.x, 1e-10)
        assertEquals(3.1, state.stack.y, 1e-10)
    }
}
