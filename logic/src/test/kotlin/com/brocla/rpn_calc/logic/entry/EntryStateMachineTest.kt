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

    private fun mantissa(digits: String, hasDecimal: Boolean = false, isNegative: Boolean = false) =
        EntryState.Mantissa(digits, hasDecimal, isNegative)

    private fun exponent(
        mDigits: String, mDec: Boolean = false, mNeg: Boolean = false,
        eDigits: String = "", eNeg: Boolean = false
    ) = EntryState.Exponent(mDigits, mDec, mNeg, eDigits, eNeg)

    // ---- pressDigit from Idle ----

    @Test fun digit_fromIdle_noLift() {
        val s = esm.pressDigit(idle, 1)
        assertTrue(s.entryState is EntryState.Mantissa)
        assertEquals("1", (s.entryState as EntryState.Mantissa).digits)
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
        assertEquals("1234567890", (s.entryState as EntryState.Mantissa).digits)
    }

    @Test fun digit_suppressLeadingZero() {
        val s = esm.pressDigit(idle.copy(entryState = mantissa("0")), 0)
        assertEquals("0", (s.entryState as EntryState.Mantissa).digits)
    }

    @Test fun digit_appendsNormally() {
        val s = esm.pressDigit(idle.copy(entryState = mantissa("12")), 3)
        assertEquals("123", (s.entryState as EntryState.Mantissa).digits)
    }

    // ---- pressDecimal ----

    @Test fun decimal_fromIdle() {
        val s = esm.pressDecimal(idle)
        val es = s.entryState as EntryState.Mantissa
        assertTrue(es.hasDecimal)
    }

    @Test fun decimal_fromMantissa() {
        val s = esm.pressDecimal(idle.copy(entryState = mantissa("3")))
        assertTrue((s.entryState as EntryState.Mantissa).hasDecimal)
    }

    @Test fun decimal_twice_noop() {
        val base = idle.copy(entryState = mantissa("3", hasDecimal = true))
        val s = esm.pressDecimal(base)
        assertEquals(base.entryState, s.entryState)
    }

    // ---- pressChs ----

    @Test fun chs_mantissa_positive() {
        val s = esm.pressChs(idle.copy(entryState = mantissa("3", isNegative = false)))
        assertTrue((s.entryState as EntryState.Mantissa).isNegative)
    }

    @Test fun chs_mantissa_negative() {
        val s = esm.pressChs(idle.copy(entryState = mantissa("3", isNegative = true)))
        assertFalse((s.entryState as EntryState.Mantissa).isNegative)
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
        val s = esm.pressEex(idle.copy(entryState = mantissa("12", false, false)))
        val es = s.entryState as EntryState.Exponent
        assertEquals("12", es.mantissaDigits)
        assertFalse(es.mantissaHasDecimal)
        assertFalse(es.mantissaIsNegative)
        assertEquals("", es.exponentDigits)
        assertFalse(es.exponentIsNegative)
    }

    @Test fun eex_fromIdle() {
        val s = esm.pressEex(idle)
        val es = s.entryState as EntryState.Exponent
        assertEquals("1", es.mantissaDigits)
        assertEquals("", es.exponentDigits)
    }

    // ---- pressBackspace ----

    @Test fun backspace_removesLastDigit() {
        val s = esm.pressBackspace(idle.copy(entryState = mantissa("123")))
        assertEquals("12", (s.entryState as EntryState.Mantissa).digits)
    }

    @Test fun backspace_removesDecimal() {
        val s = esm.pressBackspace(idle.copy(entryState = mantissa("3", hasDecimal = true)))
        val es = s.entryState as EntryState.Mantissa
        assertFalse(es.hasDecimal)
        assertEquals("3", es.digits)
    }

    @Test fun backspace_emptyMantissa_stays() {
        val s = esm.pressBackspace(idle.copy(entryState = mantissa("")))
        assertEquals("", (s.entryState as EntryState.Mantissa).digits)
    }

    @Test fun backspace_exponentDigit() {
        val s = esm.pressBackspace(idle.copy(entryState = exponent("12", eDigits = "04")))
        assertEquals("0", (s.entryState as EntryState.Exponent).exponentDigits)
    }

    @Test fun backspace_exponentEmpty_revertsMantissa() {
        val s = esm.pressBackspace(idle.copy(entryState = exponent("12", eDigits = "")))
        val es = s.entryState as EntryState.Mantissa
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
        val s = esm.completeEntry(idle.copy(entryState = mantissa("314", hasDecimal = true)))
        // "314" with decimal → "3.14"
        assertEquals(3.14, s.stack.x, 1e-10)
    }

    @Test fun complete_negative() {
        val s = esm.completeEntry(idle.copy(entryState = mantissa("5", isNegative = true)))
        assertEquals(-5.0, s.stack.x)
    }

    @Test fun complete_withExponent() {
        val s = esm.completeEntry(idle.copy(entryState = exponent("123", true, false, "02", false)))
        assertEquals(1.23e2, s.stack.x, 1e-10)
    }

    @Test fun complete_negativeExponent() {
        // mantissaDigits="10", hasDecimal=true → 1.0; exp=-03 → 1.0e-3
        val s = esm.completeEntry(idle.copy(entryState = exponent("10", true, false, "03", true)))
        assertEquals(1.0e-3, s.stack.x, 1e-15)
    }
}
