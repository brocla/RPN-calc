package com.brocla.rpn_calc.logic.entry

import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryStateMachineTest {

    val esm = EntryStateMachine()
    val idle = CalculatorState()

    private fun mantissa(
        digits: String,
        fracDigits: String = "",
        hasDecimal: Boolean = false,
        isNegative: Boolean = false
    ) = EntryState.Standard(digits, fracDigits, hasDecimal, isNegative)

    private fun exponent(
        mIntPart: String, mFracPart: String = "", mDec: Boolean = false, mNeg: Boolean = false,
        eDigits: String = "", eNeg: Boolean = false
    ) = EntryState.Exponent(mIntPart, mFracPart, mDec, mNeg, eDigits, eNeg)

    // ---- pressDigit from Idle ----

    @Test fun digit_fromIdle_noLift() {
        val s = esm.pressDigit(idle, 1)
        assertTrue(s.entryState is EntryState.Standard)
        assertEquals("1", (s.entryState as EntryState.Standard).digits)
        assertEquals(0.0, s.stack.x)  // no lift
    }

    @Test fun digit_fromIdle_withLift() {
        val base = idle.copy(stackLiftEnabled = true, stack = Stack(x = 5.0))
        val s = esm.pressDigit(base, 1)
        assertEquals(5.0, s.stack.y)  // lifted
    }

    @Test fun digit_maxTenDigits() {
        val tenDigits = idle.copy(entryState = mantissa("1234567890"))
        val s = esm.pressDigit(tenDigits, 1)
        assertEquals("1234567890", (s.entryState as EntryState.Standard).digits)
    }

    @Test fun digit_suppressLeadingZero() {
        val s = esm.pressDigit(idle.copy(entryState = mantissa("0")), 0)
        assertEquals("0", (s.entryState as EntryState.Standard).digits)
    }

    @Test fun digit_appendsNormally() {
        val s = esm.pressDigit(idle.copy(entryState = mantissa("12")), 3)
        assertEquals("123", (s.entryState as EntryState.Standard).digits)
    }

    // ---- pressDecimal ----

    @Test fun decimal_fromIdle() {
        val s = esm.pressDecimal(idle)
        val es = s.entryState as EntryState.Standard
        assertTrue(es.hasDecimal)
    }

    @Test fun decimal_fromMantissa() {
        val s = esm.pressDecimal(idle.copy(entryState = mantissa("3")))
        assertTrue((s.entryState as EntryState.Standard).hasDecimal)
    }

    @Test fun decimal_twice_noop() {
        val base = idle.copy(entryState = mantissa("3", hasDecimal = true))
        val s = esm.pressDecimal(base)
        assertEquals(base.entryState, s.entryState)
    }


    // ---- pressChs ----

    @Test fun chs_mantissa_positive() {
        val s = esm.pressChs(idle.copy(entryState = mantissa("3", isNegative = false)))
        assertTrue((s.entryState as EntryState.Standard).isNegative)
    }

    @Test fun chs_mantissa_negative() {
        val s = esm.pressChs(idle.copy(entryState = mantissa("3", isNegative = true)))
        assertFalse((s.entryState as EntryState.Standard).isNegative)
    }

    @Test fun chs_exponent_positive() {
        val s = esm.pressChs(idle.copy(entryState = exponent("1", eNeg = false)))
        assertTrue((s.entryState as EntryState.Exponent).exponentIsNegative)
    }

    @Test fun chs_exponent_negative() {
        val s = esm.pressChs(idle.copy(entryState = exponent("1", eNeg = true)))
        assertFalse((s.entryState as EntryState.Exponent).exponentIsNegative)
    }

    // ---- pressEex ----

    @Test fun eex_fromMantissa() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("12")))
        val es = s.entryState as EntryState.Exponent
        assertEquals("12", es.mantissaIntPart)
        assertEquals("", es.mantissaFracPart)
        assertFalse(es.mantissaHasDecimal)
        assertFalse(es.mantissaIsNegative)
        assertEquals("", es.exponentDigits)
        assertFalse(es.exponentIsNegative)
    }

    // Pressing . then a digit then EEX: mantissa digits="" but fracDigits is non-empty.
    // mantissaIntPart must be "0", not "" (which formatExponent/parseExponent treat as "1").
    @Test fun eex_fromDecimalOnlyMantissa_intPartIsZero() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("", "2", hasDecimal = true)))
        val es = s.entryState as EntryState.Exponent
        assertEquals("0", es.mantissaIntPart,
            "mantissaIntPart must be \"0\" when decimal was entered before any integer digit")
    }

    @Test fun eex_fromDecimalOnlyMantissa_parsedValueIsCorrect() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("", "2", hasDecimal = true)))
        val value = esm.currentDisplayValue(s)
        assertEquals(0.2, value,
            "'. 2 EEX' must parse as 0.2, not 1.2")
    }

    @Test fun eex_fromIdle() {
        val s = esm.pressEex(idle)
        val es = s.entryState as EntryState.Exponent
        assertEquals("1", es.mantissaIntPart)
        assertEquals("", es.exponentDigits)
    }

    // ---- pressEex with 9/10 digit mantissa ----
    // The display has 8 significand positions (1–8). When EEX is pressed with more
    // than 8 significant digits, the mantissa must be truncated to 8 to leave room
    // for the exponent at positions 9–11.
    //
    // Case A — pure integer, 9 digits.
    @Test fun eex_nineIntegerDigits_transitionsToExponent() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("123456789")))
        assertTrue(s.entryState is EntryState.Exponent,
            "EEX with 9 integer digits must transition to Exponent; stayed Standard")
    }

    @Test fun eex_nineIntegerDigits_truncatesTo8() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("123456789")))
        val es = s.entryState as EntryState.Exponent
        assertEquals("12345678", es.mantissaIntPart,
            "9-digit integer mantissa must be truncated to 8 digits")
        assertEquals("", es.mantissaFracPart)
    }

    // Case B — pure integer, 10 digits.
    @Test fun eex_tenIntegerDigits_transitionsToExponent() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("1234567890")))
        assertTrue(s.entryState is EntryState.Exponent,
            "EEX with 10 integer digits must transition to Exponent; stayed Standard")
    }

    @Test fun eex_tenIntegerDigits_truncatesTo8() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("1234567890")))
        val es = s.entryState as EntryState.Exponent
        assertEquals("12345678", es.mantissaIntPart,
            "10-digit integer mantissa must be truncated to 8 digits")
        assertEquals("", es.mantissaFracPart)
    }

    // Case C — 7 integer + 2 frac = 9 total digits.
    @Test fun eex_sevenIntTwoFrac_truncatesFracTo1() {
        val s = esm.pressEex(idle.copy(entryState = mantissa("1234567", "89", hasDecimal = true)))
        val es = s.entryState as EntryState.Exponent
        assertEquals("1234567", es.mantissaIntPart)
        assertEquals("8", es.mantissaFracPart,
            "frac part must be truncated so total sig digits = 8; expected \"8\", got \"${es.mantissaFracPart}\"")
    }

    // ---- pressBackspace ----

    @Test fun backspace_removesLastDigit() {
        val s = esm.pressBackspace(idle.copy(entryState = mantissa("123")))
        assertEquals("12", (s.entryState as EntryState.Standard).digits)
    }

    @Test fun backspace_removesDecimal() {
        val s = esm.pressBackspace(idle.copy(entryState = mantissa("3", hasDecimal = true)))
        val es = s.entryState as EntryState.Standard
        assertFalse(es.hasDecimal)
        assertEquals("3", es.digits)
    }

    @Test fun backspace_emptyMantissa_transitionsToIdle() {
        val s = esm.pressBackspace(idle.copy(entryState = mantissa("")))
        assertEquals(EntryState.Idle, s.entryState)
        assertEquals(0.0, s.stack.x)
    }

    @Test fun backspace_exponentDigit() {
        val s = esm.pressBackspace(idle.copy(entryState = exponent("12", eDigits = "04")))
        assertEquals("0", (s.entryState as EntryState.Exponent).exponentDigits)
    }

    @Test fun backspace_exponentEmpty_revertsMantissa() {
        val s = esm.pressBackspace(idle.copy(entryState = exponent("12", eDigits = "")))
        val es = s.entryState as EntryState.Standard
        assertEquals("12", es.digits)
    }

    @Test fun backspace_idle_noop() {
        val s = esm.pressBackspace(idle)
        assertEquals(idle, s)
    }

    // ---- completeEntry ----

    @Test fun complete_idle_unchanged() {
        assertEquals(idle, esm.completeEntry(idle))
    }

    @Test fun complete_integer() {
        val s = esm.completeEntry(idle.copy(entryState = mantissa("42")))
        assertEquals(42.0, s.stack.x)
        assertEquals(EntryState.Idle, s.entryState)
        assertTrue(s.stackLiftEnabled)
    }

    @Test fun complete_decimal() {
        val s = esm.completeEntry(idle.copy(entryState = mantissa("3", "14", hasDecimal = true)))
        assertEquals(3.14, s.stack.x, 1e-10)
    }

    @Test fun complete_negative() {
        val s = esm.completeEntry(idle.copy(entryState = mantissa("5", isNegative = true)))
        assertEquals(-5.0, s.stack.x)
    }

    @Test fun complete_withExponent() {
        // mantissa 1.23 (intPart="1", fracPart="23") × 10^2 = 123.0
        val s = esm.completeEntry(idle.copy(entryState = exponent("1", "23", true, false, "02", false)))
        assertEquals(1.23e2, s.stack.x, 1e-10)
    }

    @Test fun complete_negativeExponent() {
        // mantissa 1.0 (intPart="1", fracPart="0") × 10^-3 = 0.001
        val s = esm.completeEntry(idle.copy(entryState = exponent("1", "0", true, false, "03", true)))
        assertEquals(1.0e-3, s.stack.x, 1e-15)
    }
}
