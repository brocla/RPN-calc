package com.brocla.rpn_calc.logic.engine

import com.brocla.rpn_calc.logic.display.DisplayResult
import com.brocla.rpn_calc.logic.display.IDisplayFormatter
import com.brocla.rpn_calc.logic.entry.IEntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.CalcResult
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.EntryState

class CalculatorEngine(
    private val entryStateMachine: IEntryStateMachine,
    private val mathOperations: MathOperations,
    private val displayFormatter: IDisplayFormatter
) : ICalculatorEngine {
    // ---- Entry-level helpers ----

    private fun commitEntry(state: CalculatorState): CalculatorState =
        entryStateMachine.completeEntry(state)

    private fun clearErrorIfAny(state: CalculatorState): CalculatorState =
        if (state.error != null) state.copy(error = null, entryState = EntryState.Idle) else state

    private fun applyUnary(
        state: CalculatorState,
        op: (Double) -> CalcResult
    ): CalculatorState {
        val committed = commitEntry(state)
        val x = committed.stack.x
        val withLastX = committed.copy(lastX = x)
        return when (val result = op(x)) {
            is CalcResult.Value -> withLastX.copy(
                stack = withLastX.stack.applyUnaryResult(result.value),
                stackLiftEnabled = true
            )
            is CalcResult.Err -> withLastX.copy(
                stack = withLastX.stack.withX(0.0),
                error = result.message,
                stackLiftEnabled = true
            )
        }
    }

    private fun applyBinary(
        state: CalculatorState,
        op: (Double, Double) -> CalcResult
    ): CalculatorState {
        val committed = commitEntry(state)
        val x = committed.stack.x
        val y = committed.stack.y
        val withLastX = committed.copy(lastX = x)
        return when (val result = op(y, x)) {
            is CalcResult.Value -> withLastX.copy(
                stack = withLastX.stack.applyBinaryResult(result.value),
                stackLiftEnabled = true
            )
            is CalcResult.Err -> withLastX.copy(
                stack = withLastX.stack.withX(0.0),
                error = result.message,
                stackLiftEnabled = true
            )
        }
    }

    // ---- Digit entry ----

    override fun pressDigit(state: CalculatorState, digit: Int): CalculatorState {
        val s = clearErrorIfAny(state)
        return entryStateMachine.pressDigit(s, digit)
    }

    override fun pressDecimal(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return entryStateMachine.pressDecimal(s)
    }

    override fun pressChs(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return when (s.entryState) {
            is EntryState.Idle -> s.copy(stack = s.stack.withX(-s.stack.x))
            else -> entryStateMachine.pressChs(s)
        }
    }

    override fun pressEex(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return entryStateMachine.pressEex(s)
    }

    override fun pressBackspace(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        // In Idle state (no digit entry in progress) backspace acts as CLX: clear X to 0.
        return if (s.entryState is EntryState.Idle) {
            s.copy(stack = s.stack.withX(0.0), stackLiftEnabled = false)
        } else {
            entryStateMachine.pressBackspace(s)
        }
    }

    // ---- Stack operations ----

    override fun pressEnter(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        val committed = commitEntry(s)
        val x = committed.stack.x
        return committed.copy(
            stack = committed.stack.lift().copy(x = x),
            entryState = EntryState.Idle,
            stackLiftEnabled = false
        )
    }

    override fun pressCLX(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return s.copy(
            stack = s.stack.withX(0.0),
            entryState = EntryState.Idle,
            stackLiftEnabled = false
        )
    }

    override fun pressRollDown(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(stack = s.stack.rollDown(), stackLiftEnabled = true)
    }

    override fun pressSwap(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(stack = s.stack.swap(), stackLiftEnabled = true)
    }

    override fun pressLastX(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(
            stack = s.stack.lift().withX(s.lastX),
            stackLiftEnabled = true
        )
    }

    // ---- Memory ----

    override fun pressSto(state: CalculatorState, register: Int): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        val newMemory = s.memory.toMutableList().also { it[register] = s.stack.x }
        return s.copy(memory = newMemory)
    }

    override fun pressRcl(state: CalculatorState, register: Int): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        val value = s.memory[register]
        val newStack = s.stack.lift().withX(value)
        return s.copy(stack = newStack, stackLiftEnabled = true)
    }

    // ---- Arithmetic ----

    override fun pressAdd(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.add(y, x) }

    override fun pressSubtract(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.subtract(y, x) }

    override fun pressMultiply(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.multiply(y, x) }

    override fun pressDivide(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.divide(y, x) }

    // ---- Powers and logarithms ----

    override fun pressReciprocal(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.reciprocal(x) }

    override fun pressSqrt(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.sqrt(x) }

    override fun pressSquare(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.square(x) }

    override fun pressPow10(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.pow10(x) }

    override fun pressLog(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.log10(x) }

    override fun pressExp(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.exp(x) }

    override fun pressLn(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.ln(x) }

    override fun pressPower(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.power(y, x) }

    // ---- Trigonometry ----

    override fun pressSin(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return applyUnary(s) { x -> mathOperations.sin(x, s.angleMode) }
    }

    override fun pressCos(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return applyUnary(s) { x -> mathOperations.cos(x, s.angleMode) }
    }

    override fun pressTan(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return applyUnary(s) { x -> mathOperations.tan(x, s.angleMode) }
    }

    override fun pressArcsin(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return applyUnary(s) { x -> mathOperations.arcsin(x, s.angleMode) }
    }

    override fun pressArccos(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return applyUnary(s) { x -> mathOperations.arccos(x, s.angleMode) }
    }

    override fun pressArctan(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        return applyUnary(s) { x -> mathOperations.arctan(x, s.angleMode) }
    }

    // ---- Percentage ----

    override fun pressPercent(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        val result = mathOperations.percentOf(s.stack.y, s.stack.x)
        return when (result) {
            is CalcResult.Value -> s.copy(
                stack = s.stack.withX(result.value),
                stackLiftEnabled = true
            )
            is CalcResult.Err -> s.copy(error = result.message)
        }
    }

    override fun pressPercentChange(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        val result = mathOperations.percentChange(s.stack.y, s.stack.x)
        return when (result) {
            is CalcResult.Value -> s.copy(
                stack = s.stack.withX(result.value),
                stackLiftEnabled = true
            )
            is CalcResult.Err -> s.copy(error = result.message)
        }
    }

    // ---- Combinatorics ----

    override fun pressFactorial(state: CalculatorState): CalculatorState =
        applyUnary(clearErrorIfAny(state)) { x -> mathOperations.factorial(x) }

    override fun pressCombinations(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.combinations(y, x) }

    override fun pressPermutations(state: CalculatorState): CalculatorState =
        applyBinary(clearErrorIfAny(state)) { y, x -> mathOperations.permutations(y, x) }

    // ---- Polar / Rectangular ----

    override fun pressToPolar(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        val withLastX = s.copy(lastX = s.stack.x)
        val (newY, newX) = mathOperations.toPolar(s.stack.y, s.stack.x, s.angleMode)
        return if (newY is CalcResult.Value && newX is CalcResult.Value) {
            withLastX.copy(
                stack = s.stack.copy(x = newX.value, y = newY.value),
                stackLiftEnabled = true
            )
        } else {
            s.copy(error = "Error: conversion failed")
        }
    }

    override fun pressToRectangular(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        val withLastX = s.copy(lastX = s.stack.x)
        val (newY, newX) = mathOperations.toRectangular(s.stack.y, s.stack.x, s.angleMode)
        return if (newY is CalcResult.Value && newX is CalcResult.Value) {
            withLastX.copy(
                stack = s.stack.copy(x = newX.value, y = newY.value),
                stackLiftEnabled = true
            )
        } else {
            s.copy(error = "Error: conversion failed")
        }
    }

    // ---- Constants ----

    override fun pressPi(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(state)
        val committed = commitEntry(s)
        val newStack = committed.stack.lift().withX(kotlin.math.PI)
        return committed.copy(stack = newStack, stackLiftEnabled = true)
    }

    // ---- Shift and display mode ----

    override fun pressShift(state: CalculatorState): CalculatorState =
        state.copy(shiftActive = true)

    override fun pressFixMode(state: CalculatorState, decimalPlaces: Int): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(displaySettings = DisplaySettings(DisplayMode.Fix(decimalPlaces)), shiftActive = false)
    }

    override fun pressSciMode(state: CalculatorState, decimalPlaces: Int): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(displaySettings = DisplaySettings(DisplayMode.Sci(decimalPlaces)), shiftActive = false)
    }

    override fun pressEngMode(state: CalculatorState, decimalPlaces: Int): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(displaySettings = DisplaySettings(DisplayMode.Eng(decimalPlaces)), shiftActive = false)
    }

    override fun pressAllMode(state: CalculatorState): CalculatorState {
        val s = clearErrorIfAny(commitEntry(state))
        return s.copy(displaySettings = DisplaySettings(DisplayMode.All), shiftActive = false)
    }

    override fun pressDegRad(state: CalculatorState): CalculatorState {
        val newMode = if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        return state.copy(angleMode = newMode, shiftActive = false)
    }

    // ---- Display ----

    override fun getDisplayResult(state: CalculatorState): DisplayResult =
        displayFormatter.formatResult(state)

    override fun getDisplay(state: CalculatorState): String =
        when (val r = getDisplayResult(state)) {
            is DisplayResult.Text       -> r.string
            is DisplayResult.RangeError -> r.label
        }
}
