package com.brocla.rpn_calc.logic.engine

import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.EntryState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorEngineTest {

    val engine = CalculatorEngine(EntryStateMachine(), MathOperations(), DisplayFormatter())
    fun s() = CalculatorState()

    // Helper: type a sequence of digits as a number
    private fun typeNumber(state: CalculatorState, vararg digits: Int): CalculatorState {
        var s = state
        for (d in digits) s = engine.pressDigit(s, d)
        return s
    }

    // ---- Basic arithmetic sequences ----

    @Test fun add_twoNumbers() {
        var s = typeNumber(s(), 2)
        s = engine.pressEnter(s)
        s = typeNumber(s, 3)
        s = engine.pressAdd(s)
        assertEquals(5.0, s.stack.x)
    }

    @Test fun subtract_twoNumbers() {
        var s = typeNumber(s(), 5)
        s = engine.pressEnter(s)
        s = typeNumber(s, 3)
        s = engine.pressSubtract(s)
        assertEquals(2.0, s.stack.x)
    }

    @Test fun multiply_twoNumbers() {
        var s = typeNumber(s(), 3)
        s = engine.pressEnter(s)
        s = typeNumber(s, 4)
        s = engine.pressMultiply(s)
        assertEquals(12.0, s.stack.x)
    }

    @Test fun divide_twoNumbers() {
        var s = typeNumber(s(), 6)
        s = engine.pressEnter(s)
        s = typeNumber(s, 2)
        s = engine.pressDivide(s)
        assertEquals(3.0, s.stack.x)
    }

    @Test fun divide_byZero_error() {
        var s = typeNumber(s(), 6)
        s = engine.pressEnter(s)
        s = typeNumber(s, 0)
        s = engine.pressDivide(s)
        assertNotNull(s.error)
        assertEquals(0.0, s.stack.x)
    }

    // ---- Stack lift behavior ----

    @Test fun stackLift_disabledAfterEnter() {
        // After ENTER, liftEnabled=false, so next digit does NOT push a new stack level
        var s = engine.pressEnter(s())  // X=0, Y=0, liftEnabled=false
        s = engine.pressDigit(s, 5)     // entry buffer: "5", stack.y stays 0
        assertEquals(0.0, s.stack.y)   // not lifted (ENTER disabled lift)
        // Commit entry and verify X
        s = engine.pressEnter(s)
        assertEquals(5.0, s.stack.x)
    }

    @Test fun stackLift_enabledAfterOp() {
        var s = typeNumber(s(), 2)
        s = engine.pressEnter(s)
        s = typeNumber(s, 3)
        s = engine.pressAdd(s)    // result=5, liftEnabled=true
        s = engine.pressDigit(s, 5)
        assertEquals(5.0, s.stack.x)
        assertEquals(5.0, s.stack.y)  // was lifted
    }

    @Test fun enter_duplicatesX() {
        var s = engine.pressDigit(s(), 3)
        s = engine.pressEnter(s)
        assertEquals(3.0, s.stack.x)
        assertEquals(3.0, s.stack.y)
    }

    @Test fun enter_tripleEnter() {
        var s = engine.pressDigit(s(), 3)
        s = engine.pressEnter(s)
        s = engine.pressEnter(s)
        s = engine.pressEnter(s)
        assertEquals(3.0, s.stack.x)
        assertEquals(3.0, s.stack.y)
        assertEquals(3.0, s.stack.z)
        assertEquals(3.0, s.stack.t)
    }

    // ---- CLX ----

    @Test fun clx_clearsX() {
        var s = engine.pressDigit(s(), 5)
        s = engine.pressCLX(s)
        assertEquals(0.0, s.stack.x)
    }

    @Test fun clx_preservesYZT() {
        var s = typeNumber(s(), 5)
        s = engine.pressEnter(s)
        s = typeNumber(s, 3)
        s = engine.pressCLX(s)
        assertEquals(5.0, s.stack.y)
        assertEquals(0.0, s.stack.x)
    }

    @Test fun clx_disablesLift() {
        var s = engine.pressCLX(s())
        assertFalse(s.stackLiftEnabled)
        s = engine.pressDigit(s, 7)
        assertEquals(0.0, s.stack.y)  // not lifted
        // Commit entry and verify X
        s = engine.pressEnter(s)
        assertEquals(7.0, s.stack.x)
    }

    // ---- Last X ----

    @Test fun lastX_savedBeforeOp() {
        var s = typeNumber(s(), 3)
        s = engine.pressEnter(s)
        s = typeNumber(s, 5)
        s = engine.pressAdd(s)     // lastX = 5.0
        assertEquals(5.0, s.lastX)
        s = engine.pressLastX(s)
        assertEquals(5.0, s.stack.x)
    }

    @Test fun lastX_notSavedBySto() {
        var s = typeNumber(s(), 7)
        s = engine.pressEnter(s)
        s = engine.pressSto(s, 0)
        assertEquals(0.0, s.lastX)
    }

    @Test fun lastX_notSavedByChs() {
        var s = typeNumber(s(), 7)
        s = engine.pressChs(s)
        assertEquals(0.0, s.lastX)
    }

    // ---- Roll Down and Swap ----

    @Test fun rollDown_correct() {
        // Build stack: T=4, Z=3, Y=2, X=1 via four ENTER pushes
        var s = engine.pressDigit(s(), 1)
        s = engine.pressEnter(s)
        s = engine.pressDigit(s, 2)
        s = engine.pressEnter(s)
        s = engine.pressDigit(s, 3)
        s = engine.pressEnter(s)
        s = engine.pressDigit(s, 4)
        // Stack now: X=4, Y=3, Z=2, T=1 (wait let me think through this)
        // After "1 ENTER 2 ENTER 3 ENTER 4":
        // start: X=0
        // press 1: X=1 (entry)
        // ENTER: X=1, Y=1 (lift disabled)
        // press 2: X=2 (no lift since ENTER disabled it, overwrites X)
        // ENTER: X=2, Y=2, Z=1
        // press 3: overwrites X=3
        // ENTER: X=3, Y=3, Z=2, T=1
        // press 4: overwrites X=4
        // Stack: X=4, Y=3, Z=2, T=1
        s = engine.pressRollDown(s)
        // Roll down: X←Y←Z←T←X → X=3, Y=2, Z=1, T=4
        assertEquals(3.0, s.stack.x)
        assertEquals(2.0, s.stack.y)
        assertEquals(1.0, s.stack.z)
        assertEquals(4.0, s.stack.t)
    }

    @Test fun swap_correct() {
        var s = engine.pressDigit(s(), 1)
        s = engine.pressEnter(s)
        s = engine.pressDigit(s, 2)
        s = engine.pressSwap(s)
        assertEquals(1.0, s.stack.x)
        assertEquals(2.0, s.stack.y)
    }

    // ---- Memory ----

    @Test fun sto_rcl_roundtrip() {
        var s = typeNumber(s(), 7)
        s = engine.pressSto(s, 0)
        s = engine.pressCLX(s)
        s = engine.pressRcl(s, 0)
        assertEquals(7.0, s.stack.x)
    }

    @Test fun rcl_liftsStack() {
        var s = typeNumber(s(), 3)
        s = engine.pressEnter(s)
        s = engine.pressSto(s, 1)
        s = engine.pressRcl(s, 1)
        assertEquals(3.0, s.stack.y)
        assertEquals(3.0, s.stack.x)
    }

    @Test fun sto_noStackChange() {
        var s = typeNumber(s(), 5)
        s = engine.pressEnter(s)
        s = typeNumber(s, 3)
        s = engine.pressSto(s, 0)
        assertEquals(3.0, s.stack.x)
        assertEquals(5.0, s.stack.y)
    }

    // ---- Error recovery ----

    @Test fun error_anyKey_clears() {
        var s = typeNumber(s(), 0)
        s = engine.pressDivide(s)   // error: divide by zero
        assertNotNull(s.error)
        s = engine.pressDigit(s, 5)
        assertNull(s.error)
    }

    @Test fun error_xIsZero() {
        var s = typeNumber(s(), 6)
        s = engine.pressEnter(s)
        s = typeNumber(s, 0)
        s = engine.pressDivide(s)
        assertEquals(0.0, s.stack.x)
    }

    @Test fun error_yztPreserved() {
        var s = typeNumber(s(), 6)
        s = engine.pressEnter(s)
        s = typeNumber(s, 0)
        s = engine.pressDivide(s)
        assertEquals(6.0, s.stack.y)
    }

    // ---- CHS outside entry ----

    @Test fun chs_negatesX() {
        var s = engine.pressDigit(s(), 5)
        s = engine.pressEnter(s)
        s = engine.pressChs(s)
        assertEquals(-5.0, s.stack.x)
    }

    @Test fun chs_doesNotUpdateLastX() {
        var s = engine.pressDigit(s(), 5)
        s = engine.pressEnter(s)
        s = engine.pressChs(s)
        assertEquals(0.0, s.lastX)
    }

    // ---- Shift and display mode ----

    @Test fun shiftLatch_activates() {
        val s = engine.pressShift(s())
        assertTrue(s.shiftActive)
    }

    @Test fun shiftLatch_secondShift_staysActive() {
        var s = engine.pressShift(s())
        s = engine.pressShift(s)
        assertTrue(s.shiftActive)
    }

    @Test fun shiftLatch_clearedByShiftedKey() {
        var s = engine.pressShift(s())
        s = engine.pressFixMode(s, 2)
        assertFalse(s.shiftActive)
    }

    @Test fun fixMode_set() {
        var s = engine.pressShift(s())
        s = engine.pressFixMode(s, 2)
        assertEquals(DisplayMode.Fix(2), s.displaySettings.mode)
    }

    @Test fun sciMode_set() {
        var s = engine.pressShift(s())
        s = engine.pressSciMode(s, 3)
        assertEquals(DisplayMode.Sci(3), s.displaySettings.mode)
    }

    @Test fun degRad_toggle() {
        var s = engine.pressShift(s())
        s = engine.pressDegRad(s)
        assertEquals(AngleMode.RAD, s.angleMode)
    }

    @Test fun degRad_toggleTwice() {
        var s = engine.pressShift(s())
        s = engine.pressDegRad(s)
        s = engine.pressShift(s)
        s = engine.pressDegRad(s)
        assertEquals(AngleMode.DEG, s.angleMode)
    }

    // ---- Pi ----

    @Test fun pi_pushesValue() {
        val s = engine.pressPi(s())
        assertEquals(Math.PI, s.stack.x)
    }

    @Test fun pi_liftsStack() {
        var s = typeNumber(s(), 5)
        s = engine.pressEnter(s)
        s = engine.pressPi(s)
        assertEquals(5.0, s.stack.y)
        assertEquals(Math.PI, s.stack.x)
    }

    // ---- Unary math spot-checks ----

    @Test fun sqrt_happyPath() {
        var s = typeNumber(s(), 9)
        s = engine.pressEnter(s)
        s = engine.pressSqrt(s)
        assertEquals(3.0, s.stack.x)
    }

    @Test fun sqrt_negative_error() {
        var s = engine.pressChs(typeNumber(s(), 4).let { engine.pressEnter(it) })
        s = engine.pressSqrt(s)
        assertNotNull(s.error)
    }

    @Test fun square_happyPath() {
        var s = typeNumber(s(), 3)
        s = engine.pressEnter(s)
        s = engine.pressSquare(s)
        assertEquals(9.0, s.stack.x)
    }

    @Test fun log_happyPath() {
        var s = typeNumber(s(), 1, 0, 0)
        s = engine.pressEnter(s)
        s = engine.pressLog(s)
        assertEquals(2.0, s.stack.x, 1e-10)
    }

    @Test fun factorial_five() {
        var s = typeNumber(s(), 5)
        s = engine.pressEnter(s)
        s = engine.pressFactorial(s)
        assertEquals(120.0, s.stack.x)
    }

    // ---- A8 gap-fill: boundary values ----

    @Test fun fix0_boundary() {
        val fmt = DisplayFormatter()
        val s = CalculatorState(
            stack = com.brocla.rpn_calc.logic.model.Stack(x = 3.7),
            displaySettings = DisplaySettings(DisplayMode.Fix(0))
        )
        assertEquals("4", fmt.format(s))
    }

    @Test fun fix9_boundary() {
        val fmt = DisplayFormatter()
        val s = CalculatorState(
            stack = com.brocla.rpn_calc.logic.model.Stack(x = 1.23456789),
            displaySettings = DisplaySettings(DisplayMode.Fix(9))
        )
        assertEquals("1.234567890", fmt.format(s))
    }

    @Test fun sci6_boundary() {
        val fmt = DisplayFormatter()
        val s = CalculatorState(
            stack = com.brocla.rpn_calc.logic.model.Stack(x = 12345.0),
            displaySettings = DisplaySettings(DisplayMode.Sci(6))
        )
        assertEquals("1.234500e+04", fmt.format(s))
    }

    @Test fun eng0_boundary() {
        val fmt = DisplayFormatter()
        val s = CalculatorState(
            stack = com.brocla.rpn_calc.logic.model.Stack(x = 12345.0),
            displaySettings = DisplaySettings(DisplayMode.Eng(0))
        )
        assertEquals("12e+03", fmt.format(s))
    }

    // ---- A8 gap-fill: stack depth sequences / T-replication ----

    @Test fun tReplicates_afterFiveEntries() {
        // T should replicate when stack is full
        var s = typeNumber(s(), 1); s = engine.pressEnter(s)
        s = typeNumber(s, 2); s = engine.pressEnter(s)
        s = typeNumber(s, 3); s = engine.pressEnter(s)
        s = typeNumber(s, 4); s = engine.pressEnter(s)
        // Stack: X=4, Y=4, Z=3, T=2 — wait let me trace more carefully
        // After: 1 ENTER 2 ENTER 3 ENTER 4 ENTER
        // The 5th ENTER pushes T out
        s = typeNumber(s, 5)
        s = engine.pressEnter(s)  // T was 1, now lost
        // X=5, Y=5, Z=4, T=3 (1 was pushed off)
        s = engine.pressAdd(s)    // 5+5=10, Y=4, Z=3, T=3
        s = engine.pressAdd(s)    // 10+4=14, Y=3, Z=3, T=3
        s = engine.pressAdd(s)    // 14+3=17, Y=3, Z=3, T=3 (T replicates)
        s = engine.pressAdd(s)    // 17+3=20
        assertEquals(20.0, s.stack.x)
    }

    // ---- A8 gap-fill: entry edge cases ----

    @Test fun entry_decimalOnly_gives_zero() {
        var s = engine.pressDecimal(s())
        s = engine.pressEnter(s)
        assertEquals(0.0, s.stack.x)
    }

    @Test fun entry_eexFromIdle_gives_1() {
        var s = engine.pressEex(s())
        val es = s.entryState as EntryState.Exponent
        assertEquals("1", es.mantissaDigits)
    }

    @Test fun entry_eexAfterCommittedNumber() {
        var s = typeNumber(s(), 3)
        s = engine.pressEnter(s)   // commit 3, lift disabled
        s = engine.pressEex(s)     // EEX from Idle: starts Exponent("1",...)
        assertTrue(s.entryState is EntryState.Exponent)
    }

    // ---- A8 gap-fill: mode doesn't change stored values ----

    @Test fun modeSwitch_doesNotAffectStoredValue() {
        // Compute sin(π/2) in RAD, store, switch to DEG, RCL — value unchanged
        var s = s().copy(angleMode = AngleMode.RAD)
        s = typeNumber(s, 1); s = engine.pressDecimal(s)
        // type π/2 ≈ 1.5707963 via digits
        // Easier: just store a known value in memory
        s = s().copy(stack = com.brocla.rpn_calc.logic.model.Stack(x = 1.5707963))
        s = engine.pressSto(s, 0)
        // Switch to DEG
        s = s.copy(angleMode = AngleMode.DEG)
        s = engine.pressRcl(s, 0)
        assertEquals(1.5707963, s.stack.x, 1e-7)
    }

    // ---- A8 gap-fill: STO/RCL all 10 registers ----

    @Test fun sto_rcl_allTenRegisters() {
        var s = s()
        for (i in 0..9) {
            s = s.copy(stack = com.brocla.rpn_calc.logic.model.Stack(x = (i + 1).toDouble()))
            s = engine.pressSto(s, i)
        }
        for (i in 0..9) {
            s = engine.pressRcl(s, i)
            assertEquals((i + 1).toDouble(), s.stack.x)
        }
    }

    // ---- A8 gap-fill: shift latch + unshifted key deactivates latch ----

    @Test fun shiftLatch_unshiftedKey_deactivatesLatch() {
        var s = engine.pressShift(s())
        assertTrue(s.shiftActive)
        // pressAllMode is a shifted function — deactivates
        s = engine.pressAllMode(s)
        assertFalse(s.shiftActive)
    }

    // ---- A8 gap-fill: chained operations verify full stack ----

    @Test fun chainedOps_fullStack() {
        // Build: T=1, Z=2, Y=3, X=4 then add: result=7, T replicates
        var s = typeNumber(s(), 1); s = engine.pressEnter(s)
        s = typeNumber(s, 2); s = engine.pressEnter(s)
        s = typeNumber(s, 3); s = engine.pressEnter(s)
        s = typeNumber(s, 4)
        s = engine.pressAdd(s)  // 3+4=7, stack drops: X=7, Y=2, Z=1, T=1
        assertEquals(7.0, s.stack.x)
        assertEquals(2.0, s.stack.y)
        assertEquals(1.0, s.stack.z)
        assertEquals(1.0, s.stack.t)
    }
}
