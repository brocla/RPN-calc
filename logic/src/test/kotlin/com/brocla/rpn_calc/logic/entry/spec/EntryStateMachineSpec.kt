package com.brocla.rpn_calc.logic.entry.spec

import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryStateMachineSpec {

    val esm = EntryStateMachine()
    val idle = CalculatorState()

    private fun standard(
        digits: String,
        fracDigits: String = "",
        hasDecimal: Boolean = false,
        isNegative: Boolean = false
    ) = EntryState.Standard(digits, fracDigits, hasDecimal, isNegative)

    private fun exponent(
        mInt: String, mFrac: String = "", mDec: Boolean = false, mNeg: Boolean = false,
        eDigits: String = "", eNeg: Boolean = false
    ) = EntryState.Exponent(mInt, mFrac, mDec, mNeg, eDigits, eNeg)

    // ── N1: EEX guards ────────────────────────────────────────────────────────

    @Test fun eex_fromZeroMantissa_clearsAndUsesOne() {
        val s = esm.pressEex(idle.copy(entryState = standard("0")))
        val es = s.entryState as EntryState.Exponent
        assertEquals("1", es.mantissaIntPart)
        assertEquals("", es.exponentDigits)
    }

    @Test fun eex_fromEmptyMantissa_clearsAndUsesOne() {
        val s = esm.pressEex(idle.copy(entryState = standard("")))
        val es = s.entryState as EntryState.Exponent
        assertEquals("1", es.mantissaIntPart)
    }

    @Test fun eex_fromZeroWithAllZeroFrac_clearsAndUsesOne() {
        val s = esm.pressEex(idle.copy(entryState = standard("0", fracDigits = "000", hasDecimal = true)))
        val es = s.entryState as EntryState.Exponent
        assertEquals("1", es.mantissaIntPart)
    }

    @Test fun eex_nineDigitInteger_truncatesTo8() {
        val s = esm.pressEex(idle.copy(entryState = standard("123456789")))
        val es = s.entryState as EntryState.Exponent
        assertEquals("12345678", es.mantissaIntPart)
    }

    @Test fun eex_eightDigitInteger_isAllowed() {
        val s = esm.pressEex(idle.copy(entryState = standard("12345678")))
        assertTrue(s.entryState is EntryState.Exponent)
    }

    @Test fun eex_intPartOneDecimalFracPart_usesIntLengthOnly() {
        // Integer part length 1 — well under 8, EEX allowed regardless of frac digits
        val s = esm.pressEex(idle.copy(entryState = standard("3", fracDigits = "14", hasDecimal = true)))
        assertTrue(s.entryState is EntryState.Exponent)
    }

    // ── N2: CHS zero guard ────────────────────────────────────────────────────

    @Test fun chs_zeroDigits_isNoop() {
        val base = idle.copy(entryState = standard("0"))
        val s = esm.pressChs(base)
        assertFalse((s.entryState as EntryState.Standard).isNegative)
    }

    @Test fun chs_emptyDigits_isNoop() {
        val base = idle.copy(entryState = standard(""))
        val s = esm.pressChs(base)
        assertFalse((s.entryState as EntryState.Standard).isNegative)
    }

    @Test fun chs_allFracZeros_isNoop() {
        val base = idle.copy(entryState = standard("0", fracDigits = "000", hasDecimal = true))
        val s = esm.pressChs(base)
        assertFalse((s.entryState as EntryState.Standard).isNegative)
    }

    @Test fun chs_nonZero_toggles() {
        val s = esm.pressChs(idle.copy(entryState = standard("1")))
        assertTrue((s.entryState as EntryState.Standard).isNegative)
    }

    // ── N3: Backspace last-digit → Idle ───────────────────────────────────────

    @Test fun backspace_singleDigit_transitionsToIdle() {
        val s = esm.pressBackspace(idle.copy(entryState = standard("5")))
        assertEquals(EntryState.Idle, s.entryState)
        assertEquals(0.0, s.stack.x)
        assertFalse(s.stackLiftEnabled)
    }

    @Test fun backspace_emptyMantissa_transitionsToIdle() {
        val s = esm.pressBackspace(idle.copy(entryState = standard("")))
        assertEquals(EntryState.Idle, s.entryState)
        assertEquals(0.0, s.stack.x)
    }

    @Test fun backspace_multipleDigits_removesLast() {
        val s = esm.pressBackspace(idle.copy(entryState = standard("12")))
        assertEquals("1", (s.entryState as EntryState.Standard).digits)
    }

    // ── N4: Exponent backspace sign-clear-before-revert ───────────────────────

    @Test fun eExpBackspace_emptyDigits_signSet_clearSignFirst() {
        val base = idle.copy(entryState = exponent("1", eDigits = "", eNeg = true))
        val s = esm.pressBackspace(base)
        val es = s.entryState as EntryState.Exponent
        assertFalse(es.exponentIsNegative)
        assertEquals("", es.exponentDigits)
    }

    @Test fun eExpBackspace_emptyDigits_noSign_revertsMantissa() {
        val s = esm.pressBackspace(idle.copy(entryState = exponent("12", eDigits = "", eNeg = false)))
        assertEquals("12", (s.entryState as EntryState.Standard).digits)
    }

    // ── N5: Exponent 2-digit cap (verify existing) ────────────────────────────

    @Test fun eExpDigit_thirdDigit_isNoop() {
        val base = idle.copy(entryState = exponent("1", eDigits = "42"))
        val s = esm.pressDigit(base, 1)
        assertEquals("42", (s.entryState as EntryState.Exponent).exponentDigits)
    }
}
