package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisplayFormatterTest {

    val fmt = DisplayFormatter()

    private fun state(x: Double, mode: DisplayMode) =
        CalculatorState(stack = Stack(x = x), displaySettings = DisplaySettings(mode))

    // ---- FIX mode ----

    @Test fun fix2_normal() = assertEquals(" 3.14", fmt.format(state(3.14159, DisplayMode.Fix(2))))
    @Test fun fix2_rounds() = assertEquals(" 3.15", fmt.format(state(3.145, DisplayMode.Fix(2))))
    @Test fun fix0_rounds() = assertEquals(" 4.", fmt.format(state(3.7, DisplayMode.Fix(0))))
    @Test fun fix2_zero() = assertEquals(" 0.00", fmt.format(state(0.0, DisplayMode.Fix(2))))
    @Test fun fix2_negative() = assertEquals("-3.14", fmt.format(state(-3.14, DisplayMode.Fix(2))))
    @Test fun fix9_tenDigits() = assertEquals(" 1.234567890", fmt.format(state(1.23456789, DisplayMode.Fix(9))))

    @Test fun fix2_largeOverflow() {
        val result = fmt.format(state(1.23e15, DisplayMode.Fix(2)))
        assertEquals(13, result.length, "Expected SCI fallback (13-char format), got: $result")
    }

    // fix2_smallNoOverflow deleted — replaced by fix2_noSignificantDigit_fallsBack in spec

    @Test fun fix2_tooSmallOverflow() {
        val result = fmt.format(state(1.23e-15, DisplayMode.Fix(2)))
        assertEquals(13, result.length, "Expected SCI fallback (13-char format), got: $result")
    }

    // ---- SCI mode ----
    // Format: sign(1) + sigStr(cappedDp+2) + padding(7-cappedDp) + expSign(1) + expStr(2) = 13 chars
    // No 'e' character. No '+' for positive exponent. Decimal always shown.

    @Test fun sci2_normal() = assertEquals(" 1.23      04", fmt.format(state(12345.0, DisplayMode.Sci(2))))
    @Test fun sci0_normal() = assertEquals(" 1.        04", fmt.format(state(12345.0, DisplayMode.Sci(0))))
    @Test fun sci2_negative() = assertEquals("-1.23      04", fmt.format(state(-12345.0, DisplayMode.Sci(2))))
    @Test fun sci2_small() = assertEquals(" 1.23     -03", fmt.format(state(0.00123, DisplayMode.Sci(2))))
    @Test fun sci2_exactlyOne() = assertEquals(" 1.00      00", fmt.format(state(1.0, DisplayMode.Sci(2))))

    // ---- ENG mode ----
    // Same 13-char structure. Significand occupies 8 positions (int digits + frac digits).

    @Test fun eng2_thousands() = assertEquals(" 12.35     03", fmt.format(state(12345.0, DisplayMode.Eng(2))))
    @Test fun eng2_millions() = assertEquals(" 1.23      06", fmt.format(state(1234567.0, DisplayMode.Eng(2))))
    @Test fun eng2_units() = assertEquals(" 1.23      00", fmt.format(state(1.23, DisplayMode.Eng(2))))
    @Test fun eng2_small() = assertEquals(" 1.23     -03", fmt.format(state(0.00123, DisplayMode.Eng(2))))

    // ---- ALL mode ----

    @Test fun all_noTrailingZeros() = assertEquals(" 3.14", fmt.format(state(3.14, DisplayMode.All)))
    @Test fun all_integer() = assertEquals(" 100", fmt.format(state(100.0, DisplayMode.All)))
    @Test fun all_pi() = assertEquals(" 3.141592654", fmt.format(state(Math.PI, DisplayMode.All)))

    // ---- Special cases ----

    @Test fun negativeZero_fix2() = assertEquals(" 0.00", fmt.format(state(-0.0, DisplayMode.Fix(2))))

    @Test fun error_state() {
        val s = CalculatorState(error = "Error")
        assertEquals("Error", fmt.format(s))
    }

    // ---- Entry state formatting ----

    private fun entryState(es: EntryState) =
        CalculatorState(entryState = es, displaySettings = DisplaySettings(DisplayMode.Fix(4)))

    @Test fun entry_digits() = assertEquals(" 123", fmt.format(entryState(EntryState.Standard("123"))))
    @Test fun entry_decimal() = assertEquals(" 1.2", fmt.format(entryState(EntryState.Standard("1", fracDigits = "2", hasDecimal = true))))
    @Test fun entry_decimal_oneDigit() = assertEquals(" 1.", fmt.format(entryState(EntryState.Standard("1", hasDecimal = true))))
    @Test fun entry_decimal_multiDigit() = assertEquals(" 3.14", fmt.format(entryState(EntryState.Standard("3", fracDigits = "14", hasDecimal = true))))
    @Test fun entry_negative() = assertEquals("-5", fmt.format(entryState(EntryState.Standard("5", isNegative = true))))
    @Test fun entry_empty() = assertEquals(" 0", fmt.format(entryState(EntryState.Standard(""))))

    @Test fun entry_exponent() = assertEquals(
        " 1.23      04",
        fmt.format(entryState(EntryState.Exponent("1", "23", true, false, "04", false)))
    )

    @Test fun entry_negExp() = assertEquals(
        " 1       -05",
        fmt.format(entryState(EntryState.Exponent("1", "", false, false, "5", true)))
    )
}
