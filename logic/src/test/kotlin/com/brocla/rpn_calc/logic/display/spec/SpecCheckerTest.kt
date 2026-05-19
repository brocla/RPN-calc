package com.brocla.rpn_calc.logic.display.spec

import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SpecCheckerTest — independent validation that DisplayFormatter conforms to
 * REQUIREMENTS_DISPLAY_REWRITE.md (2026-05-17) and TDD_PLAN_DISPLAY.md.
 *
 * This suite is written from the SPEC, not from the code.
 * Failing tests indicate spec violations — that is the purpose.
 */
class SpecCheckerTest {

    private val fmt = DisplayFormatter()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun idleState(x: Double, mode: DisplayMode) =
        CalculatorState(stack = Stack(x = x), displaySettings = DisplaySettings(mode))

    private fun entryState(es: EntryState, mode: DisplayMode = DisplayMode.Fix(4)) =
        CalculatorState(entryState = es, displaySettings = DisplaySettings(mode))

    // ═════════════════════════════════════════════════════════════════════════
    // §7.0 SIGN SLOT — Every non-error output must start with ' ' or '-'
    // ═════════════════════════════════════════════════════════════════════════

    // --- FIX idle sign slot ---

    @Test fun signSlot_fix_positive_firstCharIsSpace() {
        val result = fmt.format(idleState(3.14, DisplayMode.Fix(2)))
        assertTrue(result[0] == ' ' || result[0] == '-',
            "FIX positive: expected sign slot ' ' or '-' at index 0, got '${result[0]}' in \"$result\"")
        assertEquals(' ', result[0], "FIX positive should have space sign slot, got \"$result\"")
    }

    @Test fun signSlot_fix_negative_firstCharIsMinus() {
        val result = fmt.format(idleState(-3.14, DisplayMode.Fix(2)))
        assertEquals('-', result[0], "FIX negative should have minus sign slot, got \"$result\"")
    }

    @Test fun signSlot_fix_zero_firstCharIsSpace() {
        val result = fmt.format(idleState(0.0, DisplayMode.Fix(2)))
        assertEquals(' ', result[0], "FIX zero should have space sign slot, got \"$result\"")
    }

    // --- SCI idle sign slot ---

    @Test fun signSlot_sci_positive_firstCharIsSpace() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(2)))
        assertEquals(' ', result[0], "SCI positive should have space sign slot, got \"$result\"")
    }

    @Test fun signSlot_sci_negative_firstCharIsMinus() {
        val result = fmt.format(idleState(-12345.0, DisplayMode.Sci(2)))
        assertEquals('-', result[0], "SCI negative should have minus sign slot, got \"$result\"")
    }

    @Test fun signSlot_sci_zero_firstCharIsSpace() {
        val result = fmt.format(idleState(0.0, DisplayMode.Sci(2)))
        assertEquals(' ', result[0], "SCI zero should have space sign slot, got \"$result\"")
    }

    // --- ENG idle sign slot ---

    @Test fun signSlot_eng_positive_firstCharIsSpace() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Eng(2)))
        assertEquals(' ', result[0], "ENG positive should have space sign slot, got \"$result\"")
    }

    @Test fun signSlot_eng_negative_firstCharIsMinus() {
        val result = fmt.format(idleState(-12345.0, DisplayMode.Eng(2)))
        assertEquals('-', result[0], "ENG negative should have minus sign slot, got \"$result\"")
    }

    @Test fun signSlot_eng_zero_firstCharIsSpace() {
        val result = fmt.format(idleState(0.0, DisplayMode.Eng(2)))
        assertEquals(' ', result[0], "ENG zero should have space sign slot, got \"$result\"")
    }

    // --- ALL idle sign slot ---

    @Test fun signSlot_all_positive_firstCharIsSpace() {
        val result = fmt.format(idleState(3.14, DisplayMode.All))
        assertEquals(' ', result[0], "ALL positive should have space sign slot, got \"$result\"")
    }

    @Test fun signSlot_all_negative_firstCharIsMinus() {
        val result = fmt.format(idleState(-3.14, DisplayMode.All))
        assertEquals('-', result[0], "ALL negative should have minus sign slot, got \"$result\"")
    }

    @Test fun signSlot_all_zero_firstCharIsSpace() {
        val result = fmt.format(idleState(0.0, DisplayMode.All))
        assertEquals(' ', result[0], "ALL zero should have space sign slot, got \"$result\"")
    }

    // --- Standard entry sign slot ---

    @Test fun signSlot_standardEntry_positive_firstCharIsSpace() {
        val result = fmt.format(entryState(EntryState.Standard("42")))
        assertEquals(' ', result[0], "Standard entry positive should have space sign slot, got \"$result\"")
    }

    @Test fun signSlot_standardEntry_negative_firstCharIsMinus() {
        val result = fmt.format(entryState(EntryState.Standard("42", isNegative = true)))
        assertEquals('-', result[0], "Standard entry negative should have minus sign slot, got \"$result\"")
    }

    @Test fun signSlot_standardEntry_empty_firstCharIsSpace() {
        val result = fmt.format(entryState(EntryState.Standard("")))
        assertEquals(' ', result[0], "Standard entry empty (shows 0) should have space sign slot, got \"$result\"")
    }

    // --- Exponent entry sign slot ---

    @Test fun signSlot_exponentEntry_positive_firstCharIsSpace() {
        val result = fmt.format(entryState(EntryState.Exponent("1", "", false, false, "04", false)))
        assertEquals(' ', result[0], "Exponent entry positive should have space sign slot, got \"$result\"")
    }

    @Test fun signSlot_exponentEntry_negative_firstCharIsMinus() {
        val result = fmt.format(entryState(EntryState.Exponent("1", "", false, true, "04", false)))
        assertEquals('-', result[0], "Exponent entry negative mantissa should have minus sign slot, got \"$result\"")
    }

    // --- Error: must NOT have sign slot ---

    @Test fun signSlot_error_noSignSlotPrepended() {
        val errorState = CalculatorState(error = "Error: divide by zero")
        val result = fmt.format(errorState)
        assertEquals("Error: divide by zero", result,
            "Error string must be returned as-is with no sign slot prepended")
    }

    @Test fun signSlot_errorSimple_noSignSlotPrepended() {
        val errorState = CalculatorState(error = "Overflow")
        val result = fmt.format(errorState)
        assertEquals("Overflow", result,
            "Overflow error must be returned as-is with no sign slot prepended")
    }

    @Test fun signSlot_errorUnderflow_noSignSlotPrepended() {
        val errorState = CalculatorState(error = "Underflow")
        val result = fmt.format(errorState)
        assertEquals("Underflow", result,
            "Underflow error must be returned as-is with no sign slot prepended")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.8 COLUMN POSITION ASSERTIONS for SCI and ENG
    //
    // The §7.8 digit budget table uses LOGICAL POSITIONS (digit slots), not
    // string character indices. The dot character is in the string but is
    // zero-advance-width, so it does not consume a logical position.
    //
    // String character layout for SCI/ENG:
    //   charIdx[0]      = sign slot (' ' or '-')
    //   charIdx[1]      = integer digit of significand
    //   charIdx[2]      = '.' (zero-width char, but present in string)
    //   charIdx[3..]    = fractional digits of significand (0-7 chars)
    //   charIdx[3+dp..] = padding spaces (7-dp spaces in SCI)
    //   charIdx[-3]     = exponent sign (' ' or '-')
    //   charIdx[-2..-1] = two exponent digit chars
    //
    // Total string length = 1 + 1 + 1(dot) + dp + (7-dp) + 1 + 2 = 13 chars
    //
    // For SCI: exponent sign is at charIdx[10], expDigits at charIdx[11-12]
    // For ENG: same total length 13, exponent sign at charIdx[10], expDigits at [11-12]
    //
    // The critical invariant per §7.8: total length = 13, and the last 3 chars
    // are always: expSign + expDigit1 + expDigit2
    // ═════════════════════════════════════════════════════════════════════════

    // --- SCI column positions ---

    @Test fun colPos_sci_totalLength13() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI output must be exactly 13 chars, got: \"$result\" (len=${result.length})")
    }

    @Test fun colPos_sci_lastThreeChars_expSignThenTwoDigits() {
        // The last 3 characters of any 13-char SCI output must be: expSign + digit + digit
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI output must be exactly 13 chars")
        assertTrue(result[10] == ' ' || result[10] == '-',
            "SCI: result[10] (expSign) must be ' ' or '-', got '${result[10]}' in \"$result\"")
        assertTrue(result[11].isDigit(),
            "SCI: result[11] must be first exponent digit, got '${result[11]}' in \"$result\"")
        assertTrue(result[12].isDigit(),
            "SCI: result[12] must be second exponent digit, got '${result[12]}' in \"$result\"")
    }

    @Test fun colPos_sci_negative_totalLength13() {
        val result = fmt.format(idleState(-12345.0, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI negative output must be exactly 13 chars, got: \"$result\"")
    }

    @Test fun colPos_sci_negative_lastThreeChars() {
        val result = fmt.format(idleState(-12345.0, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI negative output must be exactly 13 chars")
        assertTrue(result[10] == ' ' || result[10] == '-',
            "SCI negative: result[10] must be exponent sign, got '${result[10]}' in \"$result\"")
        assertTrue(result[11].isDigit() && result[12].isDigit(),
            "SCI negative: result[11-12] must be exponent digits in \"$result\"")
    }

    @Test fun colPos_sci_negativeExp_lastThreeChars() {
        // For negative exponent, expSign at [10] must be '-'
        val result = fmt.format(idleState(0.00123, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI neg-exp output must be exactly 13 chars, got: \"$result\"")
        assertEquals('-', result[10], "SCI negative exp: result[10] must be '-', got \"$result\"")
        assertTrue(result[11].isDigit() && result[12].isDigit(),
            "SCI negative exp: result[11-12] must be digits in \"$result\"")
    }

    @Test fun colPos_sci_zero_totalLength13() {
        val result = fmt.format(idleState(0.0, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI zero must be exactly 13 chars, got: \"$result\"")
    }

    @Test fun colPos_sci_zero_lastThreeChars() {
        val result = fmt.format(idleState(0.0, DisplayMode.Sci(2)))
        assertEquals(13, result.length, "SCI zero must be exactly 13 chars")
        assertTrue(result[10] == ' ' || result[10] == '-',
            "SCI zero: result[10] must be exponent sign, got '${result[10]}' in \"$result\"")
        assertTrue(result[11].isDigit() && result[12].isDigit(),
            "SCI zero: result[11-12] must be digits in \"$result\"")
    }

    @Test fun colPos_sci_dp7_totalLength13() {
        val result = fmt.format(idleState(1.23456789, DisplayMode.Sci(7)))
        assertEquals(13, result.length, "SCI dp=7 output must be exactly 13 chars, got: \"$result\"")
    }

    @Test fun colPos_sci_dp0_totalLength13() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(0)))
        assertEquals(13, result.length, "SCI dp=0 output must be exactly 13 chars, got: \"$result\"")
    }

    // --- ENG column positions ---

    @Test fun colPos_eng_totalLength13() {
        val result = fmt.format(idleState(12345.0, DisplayMode.Eng(2)))
        assertEquals(13, result.length, "ENG output must be exactly 13 chars, got: \"$result\"")
    }

    @Test fun colPos_eng_lastThreeChars_expSignThenTwoDigits() {
        // ENG total = 13 chars; last 3 = expSign + digit + digit
        val result = fmt.format(idleState(12345.0, DisplayMode.Eng(2)))
        assertEquals(13, result.length, "ENG output must be exactly 13 chars for position assertions")
        assertTrue(result[10] == ' ' || result[10] == '-',
            "ENG: result[10] (expSign) must be ' ' or '-', got '${result[10]}' in \"$result\"")
        assertTrue(result[11].isDigit(),
            "ENG: result[11] must be first exponent digit, got '${result[11]}' in \"$result\"")
        assertTrue(result[12].isDigit(),
            "ENG: result[12] must be second exponent digit, got '${result[12]}' in \"$result\"")
    }

    @Test fun colPos_eng_zero_totalLength13() {
        val result = fmt.format(idleState(0.0, DisplayMode.Eng(2)))
        assertEquals(13, result.length, "ENG zero output must be exactly 13 chars, got: \"$result\"")
    }

    @Test fun colPos_eng_threeLeadDigits_totalLength13() {
        // 3-integer-digit mantissa: 100.xxx
        val result = fmt.format(idleState(100.0, DisplayMode.Eng(2)))
        assertEquals(13, result.length, "ENG 3-int-digit output must be exactly 13 chars, got: \"$result\"")
    }

    @Test fun colPos_eng_threeLeadDigits_lastThreeChars() {
        val result = fmt.format(idleState(100.0, DisplayMode.Eng(2)))
        assertEquals(13, result.length, "ENG 3-int-digit output must be exactly 13 chars")
        assertTrue(result[10] == ' ' || result[10] == '-',
            "ENG 3-int-digit: result[10] must be exponent sign, got '${result[10]}' in \"$result\"")
        assertTrue(result[11].isDigit() && result[12].isDigit(),
            "ENG 3-int-digit: result[11-12] must be exponent digits in \"$result\"")
    }

    // KEY TEST: rounding boundary where 999.9 rounds up, potentially shifting exponent and breaking alignment
    // Bug: the code computes mantissa int digits BEFORE rounding, so 999.9 with dp=2
    // rounds to 1000.0 (next eng tier), but the code already committed to 3 int digits.
    // This causes the output to be 12 chars instead of 13.
    @Test fun colPos_eng_roundingBoundary_999p9_totalLength13() {
        // 999.9 with dp=2 in ENG: should format as 1-int-digit (1.00e+03) after rounding
        // This is the known rounding boundary alignment bug
        val result = fmt.format(idleState(999.9, DisplayMode.Eng(2)))
        assertEquals(13, result.length,
            "ENG rounding boundary 999.9: output must be exactly 13 chars, got: \"$result\" (len=${result.length})")
    }

    @Test fun colPos_eng_roundingBoundary_999p9_totalLength13_fixed() {
        // Regression: was producing 12 chars due to mantissaIntDigits computed from "%.0f"
        // which rounds 999.9 to "1000" (4 digits) before formatting with dp=2 (3 digits).
        val result = fmt.format(idleState(999.9, DisplayMode.Eng(2)))
        assertEquals(13, result.length,
            "ENG rounding boundary 999.9 dp=2: must be 13 chars, got: \"$result\" (len=${result.length})")
    }

    @Test fun colPos_eng_roundingBoundary_999p9_lastThreeChars() {
        val result = fmt.format(idleState(999.9, DisplayMode.Eng(2)))
        if (result.length == 13) {
            assertTrue(result[10] == ' ' || result[10] == '-',
                "ENG 999.9: result[10] must be exponent sign, got '${result[10]}' in \"$result\"")
            assertTrue(result[11].isDigit() && result[12].isDigit(),
                "ENG 999.9: result[11-12] must be digits in \"$result\"")
        } else {
            assertTrue(false, "ENG 999.9: output wrong length (${result.length} chars): \"$result\"")
        }
    }

    // Tier-boundary bug: rounding causes mantissa to cross from 3 integer digits into 4.
    // "%.1f".format(999.95) = "1000.0" — now a 4-digit integer, which is the wrong ENG tier.
    // Correct result: 1.000 × 10³, i.e. " 1.0       03" not " 1000.0    00".
    // This test exposes the known remaining issue — it is expected to FAIL.
    @Test fun colPos_eng_tierBoundaryRounding_999p95_dp1() {
        // 999.95 with dp=1 in ENG: mantissa = 999.95, estimatedIntDigits = 3
        // "%.1f".format(999.95) = "1000.0" — crosses into 4 int digits (wrong tier)
        // Correct: should re-compute engExp for the rounded value and show "1.0 × 10³"
        val result = fmt.format(idleState(999.95, DisplayMode.Eng(1)))
        assertEquals(13, result.length,
            "ENG tier boundary 999.95 dp=1: must be 13 chars, got: \"$result\" (len=${result.length})")
        // The integer part of the mantissa must not be 4 digits (that would be the wrong ENG tier)
        val mantissaIntPart = result.substring(1).substringBefore('.')
        assertTrue(mantissaIntPart.trimStart().length <= 3,
            "ENG tier boundary: mantissa must have ≤ 3 integer digits (correct tier), got: \"$result\"")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.6 KNOWN BUG — ENG position budget should be 8, not 10
    // maxFrac = 8 - mantissaIntDigits
    // For 3-int-digit mantissa (e.g. 100.0), maxFrac = 8-3 = 5
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun knownBug_eng_threeIntDigits_cappedDpIs5_notMore() {
        // 123.456789 with Eng(4): 3 int digits → maxFrac = 8-3 = 5, cappedDp = min(4,5) = 4
        // Wait — with dp=4 and maxFrac=5, cappedDp=4, giving "123.4568e+00" (12 + sign = 13)
        // Let's use dp=6: cappedDp should be 5 (not 6, not 7)
        val result = fmt.format(idleState(123.456789, DisplayMode.Eng(6)))
        // With budget=8 and 3 int digits: maxFrac = 5, so at most 5 frac digits in output
        // Exponent sign is at position 9, so total = 1(sign) + 3(int) + 1(dot) + 5(frac) + 0(pad) + 1(expSign) + 2(expDig) = 13
        assertEquals(13, result.length, "ENG 3-int dp=6 must be 13 chars, got: \"$result\"")
        val mantissaSection = result.substring(1, 10).trimEnd() // remove padding
        val fracPart = if (mantissaSection.contains('.')) mantissaSection.substringAfter('.') else ""
        assertTrue(fracPart.length <= 5,
            "ENG with 3 int digits: frac digits must be ≤ 5 (budget 8-3=5), got ${fracPart.length} in \"$result\"")
    }

    @Test fun knownBug_eng_threeIntDigits_dp4_exactBudget() {
        // 123.456789 with Eng(4): 3 int digits → maxFrac = 8-3 = 5, cappedDp = min(4,5) = 4
        val result = fmt.format(idleState(123.456789, DisplayMode.Eng(4)))
        assertEquals(13, result.length, "ENG 3-int dp=4 must be 13 chars, got: \"$result\"")
        // Should show 4 frac digits: "123.4568e+00" style
        val mantissaSection = result.substring(1, 9) // after sign, before expSign position
        val fracPart = if (mantissaSection.contains('.')) mantissaSection.substringAfter('.').trimEnd() else ""
        assertTrue(fracPart.length <= 5,
            "ENG 3-int dp=4: frac ≤ 5 (budget), got ${fracPart.length} in \"$result\"")
    }

    // The known-bad case: if ENG uses budget 10 instead of 8, a 3-int-digit mantissa would allow up to 7 frac digits
    // With dp=7 and 3 int digits: correct cap → 5 frac; wrong cap (budget 10) → 7 frac, making string too long
    @Test fun knownBug_eng_budget10_wouldProduceTooLong() {
        // 100.0 with Eng(7): 3 int digits → maxFrac should be 8-3=5, cappedDp=5
        // If bug exists (budget=10): maxFrac=10-3=7, cappedDp=7 → sigStr has 3+dot+7=11 chars
        //   → total = 1(sign)+11+0(pad)+1(expSign)+2(expDig) = 15 chars (wrong)
        // If correct (budget=8): maxFrac=5, cappedDp=5 → sigStr has 3+dot+5=9 chars
        //   → total = 1(sign)+9+0(pad)+1(expSign)+2(expDig) = 13 chars (correct)
        val result = fmt.format(idleState(100.0, DisplayMode.Eng(7)))
        assertEquals(13, result.length,
            "ENG budget must be 8 not 10: 100.0 Eng(7) must be 13 chars, got ${result.length}: \"$result\"")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.2 EXPONENT ENTRY — positional format, no 'E' character
    //
    // Layout: sign(1) + mantissaContent + padding(8-sigDigits spaces) + expSign(1) + expStr(2)
    // No 'E' or 'e' appears anywhere. When exponentDigits is empty, expStr = "00".
    // When exponentDigits has one digit d, expStr = "0d". Two digits → used directly.
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun exponentEntry_emptyExpDigits_showsPaddedZeros() {
        // §7.2: empty exponentDigits → expStr = "00"; positional format, no 'E'
        // sign(' ') + "1" + 7 spaces + expSign(' ') + "00" = 12 chars
        val result = fmt.format(entryState(EntryState.Exponent("1", "", false, false, "", false)))
        assertEquals(" 1        00", result,
            "Exponent entry with empty exponentDigits must be \" 1        00\", got: \"$result\"")
    }

    @Test fun exponentEntry_oneExpDigit_showsPaddedDigit() {
        // §7.2: one digit "4" → expStr = "04"; positional, no 'E'
        // sign(' ') + "1" + 7 spaces + expSign(' ') + "04" = 12 chars
        val result = fmt.format(entryState(EntryState.Exponent("1", "", false, false, "4", false)))
        assertEquals(" 1        04", result,
            "Exponent entry with one digit '4' must be \" 1        04\", got: \"$result\"")
    }

    @Test fun exponentEntry_twoExpDigits_mantissaFrac() {
        // §7.2: mantissa "1.23", expDigits "04"
        // sign(' ') + "1.23" + 5 spaces + expSign(' ') + "04" = 13 chars
        val result = fmt.format(entryState(EntryState.Exponent("1", "23", true, false, "04", false)))
        assertEquals(" 1.23      04", result,
            "Exponent entry with frac mantissa must be \" 1.23      04\", got: \"$result\"")
    }

    @Test fun exponentEntry_negativeExp_format() {
        // §7.2: negative mantissa, negative exponent, one exp digit "5"
        // sign('-') + "1" + 7 spaces + expSign('-') + "05" = 12 chars
        val result = fmt.format(entryState(EntryState.Exponent("1", "", false, true, "5", true)))
        assertEquals("-1       -05", result,
            "Exponent entry with neg mantissa and neg exp must be \"-1       -05\", got: \"$result\"")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.3 NEGATIVE ZERO — must display as positive zero in all modes
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun negZero_fix_noMinusSign() {
        val result = fmt.format(idleState(-0.0, DisplayMode.Fix(2)))
        assertFalse(result.startsWith("-"),
            "Negative zero in FIX must not have minus sign, got: \"$result\"")
        assertEquals(' ', result[0], "Negative zero in FIX must have space sign slot, got: \"$result\"")
    }

    @Test fun negZero_sci_noMinusSign() {
        // TDD B5 — negative-zero guard may be missing in SCI branch
        val result = fmt.format(idleState(-0.0, DisplayMode.Sci(2)))
        assertFalse(result.startsWith("-"),
            "Negative zero in SCI must not have minus sign, got: \"$result\"")
        assertEquals(' ', result[0], "Negative zero in SCI must have space sign slot, got: \"$result\"")
    }

    @Test fun negZero_sci_fullOutput() {
        // TDD B5: -0.0 in Sci(2) should give " 0.00      00"
        val result = fmt.format(idleState(-0.0, DisplayMode.Sci(2)))
        assertEquals(" 0.00      00", result,
            "Negative zero in SCI(2) must format as \" 0.00      00\", got: \"$result\"")
    }

    @Test fun negZero_eng_noMinusSign() {
        val result = fmt.format(idleState(-0.0, DisplayMode.Eng(2)))
        assertFalse(result.startsWith("-"),
            "Negative zero in ENG must not have minus sign, got: \"$result\"")
        assertEquals(' ', result[0], "Negative zero in ENG must have space sign slot, got: \"$result\"")
    }

    @Test fun negZero_all_noMinusSign() {
        // TDD B6 — negative-zero guard may be missing in ALL branch
        val result = fmt.format(idleState(-0.0, DisplayMode.All))
        assertFalse(result.startsWith("-"),
            "Negative zero in ALL must not have minus sign, got: \"$result\"")
        assertEquals(' ', result[0], "Negative zero in ALL must have space sign slot, got: \"$result\"")
    }

    @Test fun negZero_all_fullOutput() {
        // TDD B6: -0.0 in All should give " 0"
        val result = fmt.format(idleState(-0.0, DisplayMode.All))
        assertEquals(" 0", result,
            "Negative zero in ALL must format as \" 0\", got: \"$result\"")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.4 FIX MODE — strict fixed with SCI fallback
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun fix_noSignificantDigit_0p001_dp2_fallsBackToSci() {
        // TDD R7: v=0.001, dp=2 → "0.00" shows no sig digit → fall back to SCI
        // The old test fix2_smallNoOverflow asserting "0.001" was wrong per spec
        val result = fmt.format(idleState(0.001, DisplayMode.Fix(2)))
        assertEquals(13, result.length,
            "FIX fallback: 0.001 with dp=2 must fall back to SCI (13 chars), got: \"$result\"")
    }

    @Test fun fix_noSignificantDigit_0p001_dp2_containsExponent() {
        val result = fmt.format(idleState(0.001, DisplayMode.Fix(2)))
        // The result should be SCI format, which in this formatter has no 'e' char —
        // instead it has a numeric exponent in position 10-11
        // We verify it's 13 chars (SCI format) which means it fell back
        assertFalse(result == " 0.00",
            "FIX 0.001 dp=2: must NOT return \" 0.00\" (no sig digit); must fall back, got: \"$result\"")
    }

    @Test fun fix_elevenDigitInteger_fallsBackToSci() {
        // TDD A2: 12345678901.0 has 11-digit integer part → must fall back to SCI
        val result = fmt.format(idleState(12345678901.0, DisplayMode.Fix(2)))
        assertEquals(13, result.length,
            "FIX: 11-digit integer must fall back to SCI (13 chars), got: \"$result\"")
    }

    @Test fun fix_tenDigitInteger_doesNotFallBack() {
        // 10-digit integer should fit (positions 1-10)
        val result = fmt.format(idleState(1234567890.0, DisplayMode.Fix(2)))
        // Should NOT fall back: the integer fills positions 1-10, dp is capped to 0
        assertFalse(result.length == 13 && result[9].isDigit().not(),
            "FIX: 10-digit integer 1234567890 should fit without SCI fallback")
    }

    @Test fun fix_negative10DigitInteger_fitsWithSign() {
        // -1234567890.0: sign in slot 0, 10 digits in slots 1-10 — must fit without fallback
        val result = fmt.format(idleState(-1234567890.0, DisplayMode.Fix(2)))
        assertEquals('-', result[0], "Negative 10-digit int: sign slot must be '-'")
        // Should not fall back to SCI
        assertFalse(result.length == 13 && result[9] == ' ',
            "FIX: negative 10-digit integer should not fall back to SCI")
    }

    @Test fun fix2_normal_value() {
        assertEquals(" 3.14", fmt.format(idleState(3.14159, DisplayMode.Fix(2))))
    }

    @Test fun fix2_zero_value() {
        assertEquals(" 0.00", fmt.format(idleState(0.0, DisplayMode.Fix(2))))
    }

    @Test fun fix0_rounds_up() {
        // 3.7 with Fix(0) → "4" (with sign slot and dot for Fix(0))
        val result = fmt.format(idleState(3.7, DisplayMode.Fix(0)))
        assertTrue(result.startsWith(" 4") || result == " 4.",
            "Fix(0) 3.7 should round up to 4, got: \"$result\"")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.5 SCI MODE — total 13 chars, N capped at 7
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun sci_totalLength_alwaysExactly13() {
        val testCases = listOf(
            Pair(12345.0, 2),
            Pair(1.0, 2),
            Pair(0.00123, 2),
            Pair(-12345.0, 2),
            Pair(12345.0, 0),
            Pair(12345.0, 7)
        )
        for ((v, dp) in testCases) {
            val result = fmt.format(idleState(v, DisplayMode.Sci(dp)))
            assertEquals(13, result.length,
                "SCI($dp) for $v must be exactly 13 chars, got ${result.length}: \"$result\"")
        }
    }

    @Test fun sci_nCappedAt7_fracDigitsNoMoreThan7() {
        // TDD A3/S4: Sci(9) — N capped to 7, frac digits ≤ 7
        val result = fmt.format(idleState(1.23456789, DisplayMode.Sci(9)))
        assertEquals(13, result.length, "SCI(9) must produce 13-char output (N capped), got: \"$result\"")
        // Verify frac length ≤ 7: after sign+intDigit+dot, up to 7 frac chars, then padding, expSign, 2 expDig
        // In 13-char string: sign(1) + "1."(2) + frac(≤7) + padding + expSign(1) + expDig(2) = 13
        // So frac + padding = 13 - 1 - 2 - 1 - 2 = 7
        // If N=9 is capped to 7, frac=7, padding=0
        val mantissaSection = result.substring(1, 10).trimEnd()
        val fracPart = if (mantissaSection.contains('.')) mantissaSection.substringAfter('.') else ""
        assertTrue(fracPart.length <= 7,
            "SCI(9): frac digits must be ≤ 7 (N capped at 7), got ${fracPart.length} in \"$result\"")
    }

    @Test fun sci_trailingZeroPadding_sci4_x2p34() {
        // TDD A4/S5: Sci(4) for 2.34 → "2.3400e+00" style → " 2.3400    00" (13 chars)
        val result = fmt.format(idleState(2.34, DisplayMode.Sci(4)))
        assertEquals(13, result.length, "SCI(4) 2.34 must be 13 chars, got: \"$result\"")
        assertEquals(' ', result[0], "Sign slot must be space")
        // Should have "2.3400" in mantissa section (positions 1-7)
        val mantissaSection = result.substring(1, 8) // sign removed, 7 chars
        assertTrue(mantissaSection.startsWith("2.3400"),
            "SCI(4) 2.34: mantissa should be \"2.3400...\", got mantissa=\"$mantissaSection\" in \"$result\"")
    }

    @Test fun sci_zero_dp2() {
        // TDD S3/W2: x=0.0, Sci(2) → " 0.00      00"
        val result = fmt.format(idleState(0.0, DisplayMode.Sci(2)))
        assertEquals(" 0.00      00", result,
            "SCI(2) zero must be \" 0.00      00\", got: \"$result\"")
    }

    @Test fun sci_overflow_exponentGt99() {
        // TDD S7: 1e100 with SCI — exponent > 99, must return "Overflow"
        val result = fmt.format(idleState(1e100, DisplayMode.Sci(2)))
        assertEquals("Overflow", result,
            "SCI: exponent > 99 must return \"Overflow\", got: \"$result\"")
    }

    @Test fun sci_underflow_exponentLtNeg99() {
        // TDD S8: 1e-100 with SCI — exponent < -99, must return "Underflow"
        val result = fmt.format(idleState(1e-100, DisplayMode.Sci(2)))
        assertEquals("Underflow", result,
            "SCI: exponent < -99 must return \"Underflow\", got: \"$result\"")
    }

    @Test fun sci2_value_12345() {
        // " 1.23      04"
        val result = fmt.format(idleState(12345.0, DisplayMode.Sci(2)))
        assertEquals(" 1.23      04", result, "SCI(2) 12345.0 failed")
    }

    @Test fun sci2_value_exactlyOne() {
        // " 1.00      00"
        val result = fmt.format(idleState(1.0, DisplayMode.Sci(2)))
        assertEquals(" 1.00      00", result, "SCI(2) 1.0 failed")
    }

    @Test fun sci2_value_small() {
        // " 1.23     -03"
        val result = fmt.format(idleState(0.00123, DisplayMode.Sci(2)))
        assertEquals(" 1.23     -03", result, "SCI(2) 0.00123 failed")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.6 ENG MODE — total 13 chars, budget 8 for significand
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun eng_totalLength_alwaysExactly13() {
        val testCases = listOf(
            Pair(1.23, 2),       // 1 int digit
            Pair(12.3, 2),       // 2 int digits
            Pair(123.4, 2),      // 3 int digits
            Pair(1234.5, 2),     // 4 → becomes 1 int digit in e+03
            Pair(0.00123, 2),    // 1 int digit, negative exp
            Pair(-12345.0, 2)    // negative
        )
        for ((v, dp) in testCases) {
            val result = fmt.format(idleState(v, DisplayMode.Eng(dp)))
            assertEquals(13, result.length,
                "ENG($dp) for $v must be exactly 13 chars, got ${result.length}: \"$result\"")
        }
    }

    @Test fun eng_threeLeads_digitCapAt5Frac() {
        // TDD A8/T5: 123.456789 with Eng(4) — 3 int digits → budget 8-3=5 frac max, cappedDp=min(4,5)=4
        val result = fmt.format(idleState(123.456789, DisplayMode.Eng(4)))
        assertEquals(13, result.length, "ENG 3-int dp=4 must be 13 chars, got: \"$result\"")
        // Verify: " 123.4568  00" — 3 int + dot + 4 frac + 2 pad + expSign + 2 expDig = 13
        // Actually: 1(sign) + 3(int) + 1(dot) + 4(frac) + 1(pad) + 1(expSign) + 2(expDig) = 13
        val mantissaSection = result.substring(1, 9).trimEnd()
        val fracPart = if (mantissaSection.contains('.')) mantissaSection.substringAfter('.') else ""
        assertTrue(fracPart.length <= 5,
            "ENG 3-int dp=4: frac must be ≤ 5 (budget 8-3=5), got ${fracPart.length} in \"$result\"")
    }

    @Test fun eng_zero_dp4() {
        // TDD T6/W3: x=0.0, Eng(4) → " 0.0000    00"
        val result = fmt.format(idleState(0.0, DisplayMode.Eng(4)))
        assertEquals(" 0.0000    00", result,
            "ENG(4) zero must be \" 0.0000    00\", got: \"$result\"")
    }

    @Test fun eng_overflow_exponentGt99() {
        // TDD T7: 1e100 with ENG — exponent > 99, must return "Overflow"
        val result = fmt.format(idleState(100e100, DisplayMode.Eng(2)))
        assertEquals("Overflow", result,
            "ENG: exponent > 99 must return \"Overflow\", got: \"$result\"")
    }

    @Test fun eng_underflow_exponentLtNeg99() {
        // TDD T8: 1e-100 with ENG — exponent < -99, must return "Underflow"
        val result = fmt.format(idleState(1e-100, DisplayMode.Eng(2)))
        assertEquals("Underflow", result,
            "ENG: exponent < -99 must return \"Underflow\", got: \"$result\"")
    }

    @Test fun eng2_value_12345() {
        // " 12.35     03"
        assertEquals(" 12.35     03", fmt.format(idleState(12345.0, DisplayMode.Eng(2))))
    }

    @Test fun eng2_value_1p23() {
        // " 1.23      00"
        assertEquals(" 1.23      00", fmt.format(idleState(1.23, DisplayMode.Eng(2))))
    }

    @Test fun eng2_value_small() {
        // " 1.23     -03"
        assertEquals(" 1.23     -03", fmt.format(idleState(0.00123, DisplayMode.Eng(2))))
    }

    @Test fun eng4_threeLeads_value() {
        // TDD A7: 123.4 Eng(4) → "123.40e+00" style → " 123.4000  00" (13 chars)
        val result = fmt.format(idleState(123.4, DisplayMode.Eng(4)))
        assertEquals(13, result.length, "ENG(4) 123.4 must be 13 chars, got: \"$result\"")
        // 3 int digits + budget 8: maxFrac = 8-3 = 5, cappedDp = min(4,5) = 4
        // Result: " 123.4000  00" = 1+3+1+4+2+1+2 = 14? No...
        // Let me count: sign(1)+int(3)+dot(0 width but char)+frac(4)+pad(1)+expSign(1)+expDig(2)=13... including dot as 1 char = 14
        // Hmm, dot is a char in the string but zero-advance-width. String length counts chars.
        // So: 1+3+1(dot)+4+?+1+2 = 12+?, so padding=1, total=13. Correct.
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.7 ALL MODE
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun all_normal_value() {
        assertEquals(" 3.14", fmt.format(idleState(3.14, DisplayMode.All)))
    }

    @Test fun all_integer_100() {
        assertEquals(" 100", fmt.format(idleState(100.0, DisplayMode.All)))
    }

    @Test fun all_zero() {
        assertEquals(" 0", fmt.format(idleState(0.0, DisplayMode.All)))
    }

    @Test fun all_exactInteger5() {
        assertEquals(" 5", fmt.format(idleState(5.0, DisplayMode.All)))
    }

    @Test fun all_elevenDigitInt_fallsBackToSci() {
        // TDD U6: 12345678901.0 — integer part > 10 digits → fall back to SCI
        val result = fmt.format(idleState(12345678901.0, DisplayMode.All))
        assertEquals(13, result.length,
            "ALL: 11-digit integer must fall back to SCI (13 chars), got: \"$result\"")
    }

    @Test fun all_tinyValue_fallsBackToSci() {
        // TDD U7: 0.0000001 — too small for plain decimal → fall back to SCI
        val result = fmt.format(idleState(0.0000001, DisplayMode.All))
        assertEquals(13, result.length,
            "ALL: tiny value 0.0000001 must fall back to SCI (13 chars), got: \"$result\"")
    }

    @Test fun all_pi_tenSigDigits() {
        // TDD U8: Math.PI → " 3.141592654" (10 sig digits)
        assertEquals(" 3.141592654", fmt.format(idleState(Math.PI, DisplayMode.All)))
    }

    // ═════════════════════════════════════════════════════════════════════════
    // §7.1 STANDARD ENTRY FORMATTING
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun standardEntry_digitsOnly() {
        assertEquals(" 123", fmt.format(entryState(EntryState.Standard("123"))))
    }

    @Test fun standardEntry_empty_showsZero() {
        assertEquals(" 0", fmt.format(entryState(EntryState.Standard(""))))
    }

    @Test fun standardEntry_negative() {
        assertEquals("-5", fmt.format(entryState(EntryState.Standard("5", isNegative = true))))
    }

    @Test fun standardEntry_withDecimalNoFrac() {
        assertEquals(" 3.", fmt.format(entryState(EntryState.Standard("3", hasDecimal = true))))
    }

    @Test fun standardEntry_withDecimalAndFrac() {
        assertEquals(" 1.2", fmt.format(entryState(EntryState.Standard("1", fracDigits = "2", hasDecimal = true))))
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADDITIONAL CORRECTNESS — FIX mode
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun fix2_largeOverflow_fallsBackToSci() {
        val result = fmt.format(idleState(1.23e15, DisplayMode.Fix(2)))
        assertEquals(13, result.length, "FIX(2) 1.23e15 must fall back to SCI (13 chars), got: \"$result\"")
    }

    @Test fun fix2_tinyOverflow_fallsBackToSci() {
        val result = fmt.format(idleState(1.23e-15, DisplayMode.Fix(2)))
        assertEquals(13, result.length, "FIX(2) 1.23e-15 must fall back to SCI (13 chars), got: \"$result\"")
    }

    @Test fun fix2_negative_value() {
        assertEquals("-3.14", fmt.format(idleState(-3.14, DisplayMode.Fix(2))))
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EXHAUSTIVE SIGN SLOT — all modes, many values
    // ═════════════════════════════════════════════════════════════════════════

    @Test fun signSlotInvariant_allModesAndValues() {
        val modes = listOf(
            DisplayMode.Fix(2), DisplayMode.Fix(0),
            DisplayMode.Sci(2), DisplayMode.Sci(7),
            DisplayMode.Eng(2), DisplayMode.Eng(4),
            DisplayMode.All
        )
        val values = listOf(0.0, 1.0, -1.0, 3.14, -3.14, 1000.0, 0.001, 1.23e10, -0.0)
        for (mode in modes) {
            for (v in values) {
                val result = fmt.format(idleState(v, mode))
                // Skip error returns (Overflow/Underflow) — they have no sign slot
                if (result == "Overflow" || result == "Underflow") continue
                assertTrue(result[0] == ' ' || result[0] == '-',
                    "Sign slot invariant violated for mode=$mode v=$v: got '${result[0]}' in \"$result\"")
            }
        }
    }
}
