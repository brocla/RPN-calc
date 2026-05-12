package com.brocla.rpn_calc.logic.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StackTest {

    // ---- A2: defaults ----

    @Test fun defaultStack_allZero() {
        val s = Stack()
        assertEquals(0.0, s.x)
        assertEquals(0.0, s.y)
        assertEquals(0.0, s.z)
        assertEquals(0.0, s.t)
    }

    @Test fun defaultCalculatorState_stackLiftDisabled() {
        assertFalse(CalculatorState().stackLiftEnabled)
    }

    @Test fun defaultCalculatorState_angleModeIsDeg() {
        assertEquals(AngleMode.DEG, CalculatorState().angleMode)
    }

    @Test fun defaultCalculatorState_displayIsFixFour() {
        val mode = CalculatorState().displaySettings.mode
        assertTrue(mode is DisplayMode.Fix)
        assertEquals(4, (mode as DisplayMode.Fix).decimalPlaces)
    }

    @Test fun defaultCalculatorState_memoryAllZero() {
        val mem = CalculatorState().memory
        assertEquals(10, mem.size)
        mem.forEach { assertEquals(0.0, it) }
    }

    @Test fun defaultCalculatorState_noError() {
        assertNull(CalculatorState().error)
    }

    @Test fun defaultCalculatorState_entryStateIsIdle() {
        assertEquals(EntryState.Idle, CalculatorState().entryState)
    }

    @Test fun defaultCalculatorState_shiftNotActive() {
        assertFalse(CalculatorState().shiftActive)
    }

    // ---- A3: lift() ----

    @Test fun lift_shiftsYZT() {
        val s = Stack(x = 1.0, y = 2.0, z = 3.0, t = 4.0).lift()
        assertEquals(1.0, s.y)
        assertEquals(2.0, s.z)
        assertEquals(3.0, s.t)
    }

    @Test fun lift_xUnchanged() {
        val s = Stack(x = 1.0, y = 2.0, z = 3.0, t = 4.0).lift()
        assertEquals(1.0, s.x)
    }

    @Test fun lift_oldTLost() {
        val s = Stack(x = 1.0, y = 2.0, z = 3.0, t = 4.0).lift()
        assertTrue(s.t != 4.0)
    }

    // ---- A3: applyBinaryResult() ----

    @Test fun binaryResult_xIsResult() {
        val s = Stack(x = 2.0, y = 3.0, z = 5.0, t = 7.0).applyBinaryResult(9.0)
        assertEquals(9.0, s.x)
    }

    @Test fun binaryResult_yGetsOldZ() {
        val s = Stack(x = 2.0, y = 3.0, z = 5.0, t = 7.0).applyBinaryResult(9.0)
        assertEquals(5.0, s.y)
    }

    @Test fun binaryResult_zGetsOldT() {
        val s = Stack(x = 2.0, y = 3.0, z = 5.0, t = 7.0).applyBinaryResult(9.0)
        assertEquals(7.0, s.z)
    }

    @Test fun binaryResult_tReplicates() {
        val s = Stack(x = 2.0, y = 3.0, z = 5.0, t = 7.0).applyBinaryResult(9.0)
        assertEquals(7.0, s.t)
    }

    // ---- A3: applyUnaryResult() ----

    @Test fun unaryResult_xIsResult() {
        val s = Stack(x = 2.0, y = 3.0, z = 5.0, t = 7.0).applyUnaryResult(9.0)
        assertEquals(9.0, s.x)
    }

    @Test fun unaryResult_yzTUnchanged() {
        val s = Stack(x = 2.0, y = 3.0, z = 5.0, t = 7.0).applyUnaryResult(9.0)
        assertEquals(3.0, s.y)
        assertEquals(5.0, s.z)
        assertEquals(7.0, s.t)
    }

    // ---- A3: rollDown() ----

    @Test fun rollDown_xGetsOldY() {
        assertEquals(2.0, Stack(1.0, 2.0, 3.0, 4.0).rollDown().x)
    }

    @Test fun rollDown_yGetsOldZ() {
        assertEquals(3.0, Stack(1.0, 2.0, 3.0, 4.0).rollDown().y)
    }

    @Test fun rollDown_zGetsOldT() {
        assertEquals(4.0, Stack(1.0, 2.0, 3.0, 4.0).rollDown().z)
    }

    @Test fun rollDown_tGetsOldX() {
        assertEquals(1.0, Stack(1.0, 2.0, 3.0, 4.0).rollDown().t)
    }

    // ---- A3: swap() ----

    @Test fun swap_xGetsOldY() {
        assertEquals(2.0, Stack(1.0, 2.0, 3.0, 4.0).swap().x)
    }

    @Test fun swap_yGetsOldX() {
        assertEquals(1.0, Stack(1.0, 2.0, 3.0, 4.0).swap().y)
    }

    @Test fun swap_ztUnchanged() {
        val s = Stack(1.0, 2.0, 3.0, 4.0).swap()
        assertEquals(3.0, s.z)
        assertEquals(4.0, s.t)
    }

    // ---- A3: withX() ----

    @Test fun withX_setsX() {
        assertEquals(9.0, Stack(1.0, 2.0, 3.0, 4.0).withX(9.0).x)
    }

    @Test fun withX_yztUnchanged() {
        val s = Stack(1.0, 2.0, 3.0, 4.0).withX(9.0)
        assertEquals(2.0, s.y)
        assertEquals(3.0, s.z)
        assertEquals(4.0, s.t)
    }
}
