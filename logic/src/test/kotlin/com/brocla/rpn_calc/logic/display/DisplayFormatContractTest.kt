package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Display output contract tests.
 *
 * These tests drive the expected output format for DisplayFormatter:
 *   - 10 digit positions are usable in entry mode
 *   - SCI/ENG idle output contains no 'e'/'E' character and no '+' character
 *   - Exponent entry output contains no 'E'/'e' and no '+' character
 *   - Sign character occupies string index 0 (display position 0)
 *   - Digits fill string indices 1, 2, 3, … left-to-right
 *
 * Tests that are expected to fail before the formatter is redesigned are
 * marked with a comment: [RED until fix].
 */
class DisplayFormatContractTest {

    private val fmt = DisplayFormatter()

    private val engine = CalculatorEngine(
        entryStateMachine = EntryStateMachine(),
        mathOperations = MathOperations(),
        displayFormatter = fmt
    )

    private fun state(mode: DisplayMode = DisplayMode.Fix(2)) =
        CalculatorState(displaySettings = DisplaySettings(mode))

    private fun idleState(x: Double, mode: DisplayMode) =
        CalculatorState(stack = Stack(x = x), displaySettings = DisplaySettings(mode))

    // ─── SCI idle: no 'e'/'E' and no '+' ────────────────────────────────────
    // Notes on Display.md specifies no 'e' character and no '+' for positive
    // exponents.  The current formatSci outputs e.g. " 1.23e+04".

    // [RED until fix] — formatSci currently emits 'e' and '+'
    @Test fun sciIdle_noE() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(2)))
        assertFalse(result.contains('e', ignoreCase = true),
            "SCI idle must not contain 'e' or 'E': \"$result\"")
    }

    @Test fun sciIdle_noPlus() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(2)))
        assertFalse(result.contains('+'), "SCI idle must not contain '+': \"$result\"")
    }

    // ─── ENG idle: no 'e'/'E' and no '+' ────────────────────────────────────

    // [RED until fix] — formatEng currently emits 'e' and '+'
    @Test fun engIdle_noE() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Eng(2)))
        assertFalse(result.contains('e', ignoreCase = true),
            "ENG idle must not contain 'e' or 'E': \"$result\"")
    }

    @Test fun engIdle_noPlus() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Eng(2)))
        assertFalse(result.contains('+'), "ENG idle must not contain '+': \"$result\"")
    }

    // ─── No 'E'/'e' or '+' in exponent entry ─────────────────────────────────
    // Mode: SCI 4.  Input sequence for both cases: 1 2 3 . 4 EEX 5 6 [CHS]
    // The display mode is SCI 4 but the formatter uses formatExponent during
    // entry — the mode setting does not change which format method is called.

    // [RED until fix] — current formatExponent emits an 'E' character
    @Test fun exponentEntry_positive_noE() {
        val result = buildExponentEntry(negative = false)
        assertFalse(result.contains('E'), "Must not contain 'E': \"$result\"")
        assertFalse(result.contains('e'), "Must not contain 'e': \"$result\"")
    }

    @Test fun exponentEntry_positive_noPlus() {
        val result = buildExponentEntry(negative = false)
        assertFalse(result.contains('+'), "Must not contain '+': \"$result\"")
    }

    // [RED until fix] — current formatExponent emits an 'E' character
    @Test fun exponentEntry_negativeExp_noE() {
        val result = buildExponentEntry(negative = true)
        assertFalse(result.contains('E'), "Must not contain 'E': \"$result\"")
        assertFalse(result.contains('e'), "Must not contain 'e': \"$result\"")
    }

    @Test fun exponentEntry_negativeExp_noPlus() {
        val result = buildExponentEntry(negative = true)
        assertFalse(result.contains('+'), "Must not contain '+': \"$result\"")
    }

    // ─── FIX 0: decimal point always shown ──────────────────────────────────────
    // Notes on Display.md: the decimal point is always present, even in FIX 0.
    // Current formatFix uses "%.0f" which never emits a decimal point.

    // [RED until fix] — formatFix dp=0 currently omits the decimal point
    @Test fun fix0_integerValue_hasDecimalPoint() {
        val result = fmt.format(idleState(3.0, DisplayMode.Fix(0)))
        assertTrue(result.contains('.'),
            "FIX 0 must always show decimal point; got: \"$result\"")
    }

    // [RED until fix]
    @Test fun fix0_roundedValue_hasDecimalPoint() {
        val result = fmt.format(idleState(3.14, DisplayMode.Fix(0)))
        assertTrue(result.contains('.'),
            "FIX 0 must show decimal point even after rounding; got: \"$result\"")
    }

    // [RED until fix]
    @Test fun fix0_zero_hasDecimalPoint() {
        val result = fmt.format(idleState(0.0, DisplayMode.Fix(0)))
        assertTrue(result.contains('.'),
            "FIX 0 zero must show decimal point; got: \"$result\"")
    }

    /** Presses 1 2 3 . 4 EEX 5 6 [CHS] and returns the display string. */
    private fun buildExponentEntry(negative: Boolean): String {
        var s = state(DisplayMode.Sci(4))
        listOf(1,2,3).forEach { s = engine.pressDigit(s, it) }
        s = engine.pressDecimal(s)
        s = engine.pressDigit(s, 4)
        s = engine.pressEex(s)
        s = engine.pressDigit(s, 5)
        s = engine.pressDigit(s, 6)
        if (negative) s = engine.pressChs(s)
        return engine.getDisplay(s)
    }

}
