package com.brocla.rpn_calc.logic.display.spec

import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisplayFormatterSpec {

    val fmt = DisplayFormatter()

    private fun state(x: Double, mode: DisplayMode) =
        CalculatorState(stack = Stack(x = x), displaySettings = DisplaySettings(mode))

    private fun entryState(es: EntryState) =
        CalculatorState(entryState = es, displaySettings = DisplaySettings(DisplayMode.Fix(4)))

    // ── O1: Sign-slot prefix — Standard entry ─────────────────────────────────

    @Test fun formatStandard_positive_hasLeadingSpace() =
        assertEquals(" 123", fmt.format(entryState(EntryState.Standard("123"))))

    @Test fun formatStandard_negative_hasLeadingMinus() =
        assertEquals("-5", fmt.format(entryState(EntryState.Standard("5", isNegative = true))))

    @Test fun formatStandard_empty_showsZeroWithSpace() =
        assertEquals(" 0", fmt.format(entryState(EntryState.Standard(""))))

    @Test fun formatStandard_withDecimal_noFrac() =
        assertEquals(" 3.", fmt.format(entryState(EntryState.Standard("3", hasDecimal = true))))

    @Test fun formatStandard_withDecimalAndFrac() =
        assertEquals(" 1.2", fmt.format(entryState(EntryState.Standard("1", fracDigits = "2", hasDecimal = true))))

    // ── O2: Sign-slot prefix — Exponent entry ────────────────────────────────

    // Exponent-entry format: sign(1) + mantissaContent + padding(to fill 8 sig positions) + expSign(1) + expStr(2)
    // No 'E' character. expStr: 0 digits → "  ", 1 digit → "d ", 2 digits → "dd"

    @Test fun formatExponent_noExpDigits_showsTrailingE() =
        assertEquals(" 1        00", fmt.format(entryState(EntryState.Exponent("1", "", false, false, "", false))))

    @Test fun formatExponent_oneExpDigit_padded() =
        assertEquals(" 1        04", fmt.format(entryState(EntryState.Exponent("1", "", false, false, "4", false))))

    @Test fun formatExponent_twoExpDigits() =
        assertEquals(" 1.23      04", fmt.format(entryState(EntryState.Exponent("1", "23", true, false, "04", false))))

    @Test fun formatExponent_negativeExp() =
        assertEquals(" 1       -05", fmt.format(entryState(EntryState.Exponent("1", "", false, false, "5", true))))

    @Test fun formatExponent_negativeMantissa() =
        assertEquals("-1        05", fmt.format(entryState(EntryState.Exponent("1", "", false, true, "5", false))))

    // ── O3: Sign-slot prefix — FIX idle ──────────────────────────────────────

    @Test fun fix2_positive_hasLeadingSpace() =
        assertEquals(" 3.14", fmt.format(state(3.14159, DisplayMode.Fix(2))))

    @Test fun fix2_negative_hasLeadingMinus() =
        assertEquals("-3.14", fmt.format(state(-3.14, DisplayMode.Fix(2))))

    @Test fun fix2_zero_hasLeadingSpace() =
        assertEquals(" 0.00", fmt.format(state(0.0, DisplayMode.Fix(2))))

    @Test fun fix2_negativeZero_treatedAsPositive() =
        assertEquals(" 0.00", fmt.format(state(-0.0, DisplayMode.Fix(2))))

    @Test fun fix2_noSignificantDigit_fallsBackToSci() {
        val result = fmt.format(state(0.001, DisplayMode.Fix(2)))
        assertEquals(13, result.length, "Expected SCI fallback (13-char format) for 0.001 Fix(2), got: $result")
    }

    @Test fun fix2_elevenDigitInteger_fallsBackToSci() {
        val result = fmt.format(state(12345678901.0, DisplayMode.Fix(2)))
        assertEquals(13, result.length, "Expected SCI fallback (13-char format) for 11-digit int, got: $result")
    }

    @Test fun fix0_tenDigitInteger_fits() =
        assertEquals(" 1234567890.", fmt.format(state(1234567890.0, DisplayMode.Fix(0))))

    @Test fun fix4_tenDigitInteger_noFracRoom() =
        assertEquals(" 1234567890.", fmt.format(state(1234567890.0, DisplayMode.Fix(4))))

    // ── O4: Sign-slot prefix — SCI idle ──────────────────────────────────────
    // Format: sign(1) + sigStr(cappedDp+2) + padding(7-cappedDp) + expSign(1) + expStr(2) = 13 chars
    // No 'e' character. No '+' for positive exponent. Decimal always shown.

    @Test fun sci2_positive_hasLeadingSpace() =
        assertEquals(" 1.23      04", fmt.format(state(12345.0, DisplayMode.Sci(2))))

    @Test fun sci2_negative_hasLeadingMinus() =
        assertEquals("-1.23      04", fmt.format(state(-12345.0, DisplayMode.Sci(2))))

    @Test fun sci2_zero_hasLeadingSpace() =
        assertEquals(" 0.00      00", fmt.format(state(0.0, DisplayMode.Sci(2))))

    @Test fun sci2_negativeZero_noSign() =
        assertEquals(" 0.00      00", fmt.format(state(-0.0, DisplayMode.Sci(2))))

    @Test fun sci7_nCapAt7() {
        val result = fmt.format(state(1.23456789, DisplayMode.Sci(9)))
        // N capped to 7: 1 integer + 7 frac = 8 sig digits max
        // In 13-char format, significand occupies result[1..9]; expSign at [10]; expDigits at [11..12]
        assertEquals(13, result.length, "Expected 13-char SCI format, got: $result")
        assertTrue(result.startsWith(" "), "Expected leading space, got: $result")
        val mantissa = result.substring(1, 10).trimEnd()
        val fracLen = if (mantissa.contains('.')) mantissa.substringAfter('.').length else 0
        assertTrue(fracLen <= 7, "Expected frac digits ≤ 7, got $fracLen in $result")
    }

    @Test fun sci4_padWithTrailingZeros() =
        assertEquals(" 2.3400    00", fmt.format(state(2.34, DisplayMode.Sci(4))))

    // ── O5: Sign-slot prefix — ENG idle ──────────────────────────────────────
    // Same 13-char structure. Significand occupies 8 positions (int digits + dot + frac digits).

    @Test fun eng2_positive_hasLeadingSpace() =
        assertEquals(" 12.35     03", fmt.format(state(12345.0, DisplayMode.Eng(2))))

    @Test fun eng2_zero_hasLeadingSpace() =
        assertEquals(" 0.00      00", fmt.format(state(0.0, DisplayMode.Eng(2))))

    @Test fun eng2_negativeZero_noSign() =
        assertEquals(" 0.00      00", fmt.format(state(-0.0, DisplayMode.Eng(2))))

    @Test fun eng6_threeLeadDigits_capsAt5FracDigits() {
        // 123.xxx — 3 int digits, budget = 8-3 = 5 frac max, not 6
        // In 13-char format, significand occupies result[1..9]; expSign at [10]; expDigits at [11..12]
        val result = fmt.format(state(123.456789, DisplayMode.Eng(6)))
        assertEquals(13, result.length, "Expected 13-char ENG format, got: $result")
        assertTrue(result.startsWith(" "), "Expected leading space")
        val mantissa = result.substring(1, 10).trimEnd()
        val fracLen = if (mantissa.contains('.')) mantissa.substringAfter('.').length else 0
        assertTrue(fracLen <= 5, "Expected frac ≤ 5 (budget 8-3), got $fracLen in $result")
    }

    // ── O6: Sign-slot prefix — ALL idle ──────────────────────────────────────

    @Test fun all_positive_hasLeadingSpace() =
        assertEquals(" 3.14", fmt.format(state(3.14, DisplayMode.All)))

    @Test fun all_integer_hasLeadingSpace() =
        assertEquals(" 100", fmt.format(state(100.0, DisplayMode.All)))

    @Test fun all_zero_hasLeadingSpace() =
        assertEquals(" 0", fmt.format(state(0.0, DisplayMode.All)))

    @Test fun all_negativeZero_noSign() =
        assertEquals(" 0", fmt.format(state(-0.0, DisplayMode.All)))

    @Test fun all_exactInteger_noDecimal() =
        assertEquals(" 5", fmt.format(state(5.0, DisplayMode.All)))

    @Test fun all_elevenDigitInt_switchesToSci() {
        val result = fmt.format(state(12345678901.0, DisplayMode.All))
        assertEquals(13, result.length, "Expected SCI fallback (13-char format) for 11-digit int, got: $result")
    }

    @Test fun all_tinyValue_switchesToSci() {
        val result = fmt.format(state(0.0000001, DisplayMode.All))
        assertEquals(13, result.length, "Expected SCI fallback (13-char format) for tiny value, got: $result")
    }

    // ── O7: Error strings have no sign slot ──────────────────────────────────

    @Test fun error_noSignSlot() {
        val s = CalculatorState(error = "Error")
        assertEquals("Error", fmt.format(s))
    }
}
