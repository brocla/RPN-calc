package com.brocla.rpn_calc.logic.engine

import com.brocla.rpn_calc.logic.display.DisplayResult
import com.brocla.rpn_calc.logic.model.CalculatorState

interface ICalculatorEngine {
    // ---- Digit entry ----
    fun pressDigit(state: CalculatorState, digit: Int): CalculatorState
    fun pressDecimal(state: CalculatorState): CalculatorState
    fun pressChs(state: CalculatorState): CalculatorState
    fun pressEex(state: CalculatorState): CalculatorState
    fun pressBackspace(state: CalculatorState): CalculatorState

    // ---- Stack operations ----
    fun pressEnter(state: CalculatorState): CalculatorState
    fun pressCLX(state: CalculatorState): CalculatorState
    fun pressRollDown(state: CalculatorState): CalculatorState
    fun pressSwap(state: CalculatorState): CalculatorState
    fun pressLastX(state: CalculatorState): CalculatorState

    // ---- Memory ----
    fun pressSto(state: CalculatorState, register: Int): CalculatorState
    fun pressRcl(state: CalculatorState, register: Int): CalculatorState

    // ---- Arithmetic ----
    fun pressAdd(state: CalculatorState): CalculatorState
    fun pressSubtract(state: CalculatorState): CalculatorState
    fun pressMultiply(state: CalculatorState): CalculatorState
    fun pressDivide(state: CalculatorState): CalculatorState

    // ---- Powers and logarithms ----
    fun pressReciprocal(state: CalculatorState): CalculatorState
    fun pressSqrt(state: CalculatorState): CalculatorState
    fun pressSquare(state: CalculatorState): CalculatorState
    fun pressPow10(state: CalculatorState): CalculatorState
    fun pressLog(state: CalculatorState): CalculatorState
    fun pressExp(state: CalculatorState): CalculatorState
    fun pressLn(state: CalculatorState): CalculatorState
    fun pressPower(state: CalculatorState): CalculatorState

    // ---- Trigonometry ----
    fun pressSin(state: CalculatorState): CalculatorState
    fun pressCos(state: CalculatorState): CalculatorState
    fun pressTan(state: CalculatorState): CalculatorState
    fun pressArcsin(state: CalculatorState): CalculatorState
    fun pressArccos(state: CalculatorState): CalculatorState
    fun pressArctan(state: CalculatorState): CalculatorState

    // ---- Percentage ----
    fun pressPercent(state: CalculatorState): CalculatorState
    fun pressPercentChange(state: CalculatorState): CalculatorState

    // ---- Combinatorics ----
    fun pressFactorial(state: CalculatorState): CalculatorState
    fun pressCombinations(state: CalculatorState): CalculatorState
    fun pressPermutations(state: CalculatorState): CalculatorState

    // ---- Polar / Rectangular ----
    fun pressToPolar(state: CalculatorState): CalculatorState
    fun pressToRectangular(state: CalculatorState): CalculatorState

    // ---- Constants ----
    fun pressPi(state: CalculatorState): CalculatorState

    // ---- Shift and display mode ----
    fun pressShift(state: CalculatorState): CalculatorState
    fun pressFixMode(state: CalculatorState, decimalPlaces: Int): CalculatorState
    fun pressSciMode(state: CalculatorState, decimalPlaces: Int): CalculatorState
    fun pressEngMode(state: CalculatorState, decimalPlaces: Int): CalculatorState
    fun pressAllMode(state: CalculatorState): CalculatorState
    fun pressDegRad(state: CalculatorState): CalculatorState

    // ---- Display ----
    fun getDisplayResult(state: CalculatorState): DisplayResult
    fun getDisplay(state: CalculatorState): String
}
