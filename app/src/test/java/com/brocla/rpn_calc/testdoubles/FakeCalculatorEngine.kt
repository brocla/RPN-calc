package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.logic.display.DisplayResult
import com.brocla.rpn_calc.logic.engine.ICalculatorEngine
import com.brocla.rpn_calc.logic.model.CalculatorState

/**
 * Fake ICalculatorEngine for ViewModel isolation tests.
 *
 * All methods return [state] unchanged by default (transparent pass-through).
 * Configure [displayResult] to control what getDisplayResult/getDisplay returns.
 * Call-recording fields capture register/dp arguments for pendingOp tests.
 */
class FakeCalculatorEngine : ICalculatorEngine {

    /** Controls what getDisplayResult returns. Default: normal text display. */
    var displayResult: DisplayResult = DisplayResult.Text(" 0.")

    /** Records the register passed to the last pressSto call. */
    var lastStoRegister: Int? = null

    /** Records the register passed to the last pressRcl call. */
    var lastRclRegister: Int? = null

    /** Records the dp passed to the last pressFixMode call. */
    var lastFixDp: Int? = null

    /** Records the dp passed to the last pressSciMode call. */
    var lastSciDp: Int? = null

    /** Records the dp passed to the last pressEngMode call. */
    var lastEngDp: Int? = null

    // ---- Digit entry ----
    override fun pressDigit(state: CalculatorState, digit: Int) = state
    override fun pressDecimal(state: CalculatorState) = state
    override fun pressChs(state: CalculatorState) = state
    override fun pressEex(state: CalculatorState) = state
    override fun pressBackspace(state: CalculatorState) = state

    // ---- Stack operations ----
    override fun pressEnter(state: CalculatorState) = state
    override fun pressCLX(state: CalculatorState) = state
    override fun pressRollDown(state: CalculatorState) = state
    override fun pressSwap(state: CalculatorState) = state
    override fun pressLastX(state: CalculatorState) = state

    // ---- Memory ----
    override fun pressSto(state: CalculatorState, register: Int) =
        state.also { lastStoRegister = register }

    override fun pressRcl(state: CalculatorState, register: Int) =
        state.also { lastRclRegister = register }

    // ---- Arithmetic ----
    override fun pressAdd(state: CalculatorState) = state
    override fun pressSubtract(state: CalculatorState) = state
    override fun pressMultiply(state: CalculatorState) = state
    override fun pressDivide(state: CalculatorState) = state

    // ---- Powers and logarithms ----
    override fun pressReciprocal(state: CalculatorState) = state
    override fun pressSqrt(state: CalculatorState) = state
    override fun pressSquare(state: CalculatorState) = state
    override fun pressPow10(state: CalculatorState) = state
    override fun pressLog(state: CalculatorState) = state
    override fun pressExp(state: CalculatorState) = state
    override fun pressLn(state: CalculatorState) = state
    override fun pressPower(state: CalculatorState) = state

    // ---- Trigonometry ----
    override fun pressSin(state: CalculatorState) = state
    override fun pressCos(state: CalculatorState) = state
    override fun pressTan(state: CalculatorState) = state
    override fun pressArcsin(state: CalculatorState) = state
    override fun pressArccos(state: CalculatorState) = state
    override fun pressArctan(state: CalculatorState) = state

    // ---- Percentage ----
    override fun pressPercent(state: CalculatorState) = state
    override fun pressPercentChange(state: CalculatorState) = state

    // ---- Combinatorics ----
    override fun pressFactorial(state: CalculatorState) = state
    override fun pressCombinations(state: CalculatorState) = state
    override fun pressPermutations(state: CalculatorState) = state

    // ---- Polar / Rectangular ----
    override fun pressToPolar(state: CalculatorState) = state
    override fun pressToRectangular(state: CalculatorState) = state

    // ---- Constants ----
    override fun pressPi(state: CalculatorState) = state

    // ---- Shift and display mode ----
    override fun pressShift(state: CalculatorState) = state.copy(shiftActive = true)

    override fun pressFixMode(state: CalculatorState, decimalPlaces: Int) =
        state.also { lastFixDp = decimalPlaces }

    override fun pressSciMode(state: CalculatorState, decimalPlaces: Int) =
        state.also { lastSciDp = decimalPlaces }

    override fun pressEngMode(state: CalculatorState, decimalPlaces: Int) =
        state.also { lastEngDp = decimalPlaces }

    override fun pressAllMode(state: CalculatorState) = state
    override fun pressDegRad(state: CalculatorState) = state

    // ---- Display ----
    override fun getDisplayResult(state: CalculatorState) = displayResult

    override fun getDisplay(state: CalculatorState) = when (val r = displayResult) {
        is DisplayResult.Text       -> r.string
        is DisplayResult.RangeError -> r.label
    }
}
