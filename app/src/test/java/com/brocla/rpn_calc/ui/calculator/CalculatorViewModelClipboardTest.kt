package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.testdoubles.FakeCalcStateRepository
import com.brocla.rpn_calc.testdoubles.FakeCalculatorEngine
import com.brocla.rpn_calc.testdoubles.FakeClipboardParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Isolation tests for CalculatorViewModel's pasteFromClipboard using a fake ClipboardParser.
 *
 * These tests verify the ViewModel's own paste logic — that the raw string is forwarded
 * to the parser, that a successful parse places the value on X, and that an invalid
 * parse sets the correct error — independently of ClipboardParserImpl's parsing rules.
 *
 * See ClipboardParserTest for the parser's own parsing behaviour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelClipboardTest {

    private lateinit var fakeParser: FakeClipboardParser
    private lateinit var fakeEngine: FakeCalculatorEngine
    private lateinit var fakeRepo: FakeCalcStateRepository
    private lateinit var vm: CalculatorViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fakeParser = FakeClipboardParser()
        fakeEngine = FakeCalculatorEngine()
        fakeRepo = FakeCalcStateRepository()
        vm = CalculatorViewModel(fakeEngine, fakeRepo, fakeParser)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private val cs get() = vm.uiState.value.calcState

    // -------------------------------------------------------------------------
    // Delegation — raw string forwarded to parser
    // -------------------------------------------------------------------------

    @Test fun pasteFromClipboard_passes_raw_string_to_parser() {
        vm.pasteFromClipboard("1,234.56")
        assertEquals("1,234.56", fakeParser.lastParsed)
    }

    // -------------------------------------------------------------------------
    // Success path — value lands on X, no error
    // -------------------------------------------------------------------------

    @Test fun pasteFromClipboard_on_success_places_parsed_value_on_x() {
        fakeParser.result = ClipboardParser.Result.Success(42.0)
        vm.pasteFromClipboard("42")
        assertEquals(42.0, cs.stack.x)
    }

    @Test fun pasteFromClipboard_on_success_does_not_set_error() {
        fakeParser.result = ClipboardParser.Result.Success(1.0)
        vm.pasteFromClipboard("1")
        assertNull(cs.error)
    }

    @Test fun pasteFromClipboard_on_success_triggers_repository_save() {
        val countBefore = fakeRepo.savedStates.size
        fakeParser.result = ClipboardParser.Result.Success(5.0)
        vm.pasteFromClipboard("5")
        assertEquals(countBefore + 1, fakeRepo.savedStates.size)
    }

    // -------------------------------------------------------------------------
    // Invalid path — error set, no value pushed
    // -------------------------------------------------------------------------

    @Test fun pasteFromClipboard_on_invalid_sets_paste_error_message() {
        fakeParser.result = ClipboardParser.Result.Invalid
        vm.pasteFromClipboard("garbage")
        assertEquals("Paste: invalid input", cs.error)
    }
}
