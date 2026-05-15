package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD boundary tests for DisplayFormatter.
 *
 * (1) Fix / All: fallback to Sci at 11 integer digits.
 *     The display has 10 digit positions.  A value whose integer part is exactly
 *     10 digits must stay in Fix/All; at 11 integer digits the formatter must fall
 *     back to Sci so the result still fits.
 *
 * (2) Sci / Eng: exponent range ±99.
 *     The exponent field on a 10-character display can hold at most 2 digits (±99).
 *     Exponent == ±99 must format normally.
 *     Exponent beyond ±99 (i.e. |exp| ≥ 100) must produce an out-of-range error.
 */
class DisplayBoundaryTest {

    private val fmt = DisplayFormatter()

    private fun state(x: Double, mode: DisplayMode) =
        CalculatorState(stack = Stack(x = x), displaySettings = DisplaySettings(mode))

    private fun format(x: Double, mode: DisplayMode) = fmt.format(state(x, mode))

    private fun displayWidth(s: String) = s.count { it != '.' && it != ',' }

    // -------------------------------------------------------------------------
    // (1) Fix / All: 10-digit vs 11-digit integer part
    // -------------------------------------------------------------------------

    // 9_999_999_999  → 10 integer digits — Fix should NOT fall back
    @Test fun fix_tenIntegerDigits_staysInFix() {
        val result = format(9_999_999_999.0, DisplayMode.Fix(2))
        println("fix_tenIntegerDigits: \"$result\"")
        assertTrue(!result.contains('e'), "Expected Fix format (no 'e'), got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // 10_000_000_000 → 11 integer digits — Fix must fall back to Sci
    @Test fun fix_elevenIntegerDigits_fallsBackToSci() {
        val result = format(10_000_000_000.0, DisplayMode.Fix(2))
        println("fix_elevenIntegerDigits: \"$result\"")
        assertTrue(result.contains('e'), "Expected Sci fallback (contains 'e'), got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // Negative: -9_999_999_999 → 10 integer digits + sign = 11 total; Fix must fall back
    @Test fun fix_negTenIntegerDigits_fallsBackToSci() {
        val result = format(-9_999_999_999.0, DisplayMode.Fix(2))
        println("fix_negTenIntegerDigits: \"$result\"")
        assertTrue(result.contains('e'), "Expected Sci fallback for negative 10-digit integer, got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // Negative: -999_999_999 → 9 integer digits + sign = 10 total; Fix should NOT fall back
    @Test fun fix_negNineIntegerDigits_staysInFix() {
        val result = format(-999_999_999.0, DisplayMode.Fix(0))
        println("fix_negNineIntegerDigits: \"$result\"")
        assertTrue(!result.contains('e'), "Expected Fix format (no 'e'), got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // All mode: 10-digit integer stays in fixed notation
    @Test fun all_tenIntegerDigits_staysFixed() {
        val value = 9_999_999_990.0
        val result = format(value, DisplayMode.All)
        println("all_tenIntegerDigits: \"$result\"")
        assertTrue(!result.contains('e'), "Expected fixed notation (no 'e'), got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
        val parsed = result.replace(",", "").toDouble()
        assertEquals(value, parsed, "Parsed value \"$result\" does not equal input $value")
    }

    // All mode: 11-digit integer falls back to Sci notation
    @Test fun all_elevenIntegerDigits_fallsBackToSci() {
        val result = format(10_000_000_000.0, DisplayMode.All)
        println("all_elevenIntegerDigits: \"$result\"")
        assertTrue(result.contains('e'), "Expected Sci fallback (contains 'e'), got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // -------------------------------------------------------------------------
    // (2) Sci: exponent boundary at ±99
    // -------------------------------------------------------------------------

    // 1e+99 — exactly at the positive limit; must format normally
    @Test fun sci_exp99_positive_works() {
        val result = format(1e99, DisplayMode.Sci(2))
        println("sci_exp99_positive: \"$result\"")
        assertTrue(!result.contains("rr") && !result.contains("ver"),
            "Expected valid display, got error: $result")
        assertTrue(result.contains("e+99"), "Expected exponent +99, got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // 1e-99 — exactly at the negative limit; must format normally
    @Test fun sci_exp99_negative_works() {
        val result = format(1e-99, DisplayMode.Sci(2))
        println("sci_exp99_negative: \"$result\"")
        assertTrue(!result.contains("rr") && !result.contains("ver"),
            "Expected valid display, got error: $result")
        assertTrue(result.contains("e-99"), "Expected exponent -99, got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // 1e+100 — one beyond positive limit; must return "Overflow"
    @Test fun sci_exp100_positive_outOfRange() {
        val result = format(1e100, DisplayMode.Sci(2))
        println("sci_exp100_positive: \"$result\"")
        assertEquals("Overflow", result)
    }

    // 1e-100 — one beyond negative limit; must return "Underflow"
    @Test fun sci_exp100_negative_outOfRange() {
        val result = format(1e-100, DisplayMode.Sci(2))
        println("sci_exp100_negative: \"$result\"")
        assertEquals("Underflow", result)
    }

    // -------------------------------------------------------------------------
    // (3) Eng: exponent boundary at ±99
    // Eng exponents are multiples of 3.
    // 1e+99 → engExp=99 (fits); 1e+102 → engExp=102 (3 digits, out of range).
    // 1e-99 → engExp=-99 (fits); 1e-100 → engExp=-102 (3 digits, out of range).
    // -------------------------------------------------------------------------

    // 1e+99 — positive limit; must format normally
    @Test fun eng_exp99_positive_works() {
        val result = format(1e99, DisplayMode.Eng(2))
        println("eng_exp99_positive: \"$result\"")
        assertTrue(!result.contains("rr") && !result.contains("ver"),
            "Expected valid display, got error: $result")
        assertTrue(result.contains("e+99"), "Expected exponent +99, got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // 1e-99 — negative limit; must format normally
    @Test fun eng_exp99_negative_works() {
        val result = format(1e-99, DisplayMode.Eng(2))
        println("eng_exp99_negative: \"$result\"")
        assertTrue(!result.contains("rr") && !result.contains("ver"),
            "Expected valid display, got error: $result")
        assertTrue(result.contains("e-99"), "Expected exponent -99, got: $result")
        assertTrue(displayWidth(result) <= 10, "Width exceeded 10: $result")
    }

    // 1e+102 — engExp becomes 102 (3 digits); must return "Overflow"
    @Test fun eng_exp102_positive_outOfRange() {
        val result = format(1e102, DisplayMode.Eng(2))
        println("eng_exp102_positive: \"$result\"")
        assertEquals("Overflow", result)
    }

    // 1e-100 — engExp becomes -102 (3 digits); must return "Underflow"
    @Test fun eng_exp100_negative_outOfRange() {
        val result = format(1e-100, DisplayMode.Eng(2))
        println("eng_exp100_negative: \"$result\"")
        assertEquals("Underflow", result)
    }
}
