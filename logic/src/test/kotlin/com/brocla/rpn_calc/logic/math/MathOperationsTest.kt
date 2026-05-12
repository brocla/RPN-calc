package com.brocla.rpn_calc.logic.math

import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.CalcResult
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MathOperationsTest {

    val math = MathOperations()

    private fun assertNear(expected: Double, actual: CalcResult, tol: Double = 1e-10) {
        assertTrue(actual is CalcResult.Value, "Expected Value but got $actual")
        assertTrue(
            kotlin.math.abs((actual as CalcResult.Value).value - expected) <= tol,
            "Expected $expected but got ${actual.value}"
        )
    }

    private fun assertValue(expected: Double, actual: CalcResult) {
        assertTrue(actual is CalcResult.Value, "Expected Value but got $actual")
        assertEquals(expected, (actual as CalcResult.Value).value)
    }

    private fun assertErr(actual: CalcResult) {
        assertTrue(actual is CalcResult.Err, "Expected Err but got $actual")
    }

    // ---- Arithmetic ----

    @Test fun add_happyPath() = assertValue(5.0, math.add(3.0, 2.0))
    @Test fun subtract_happyPath() = assertValue(2.0, math.subtract(5.0, 3.0))
    @Test fun subtract_negativeResult() = assertValue(-2.0, math.subtract(3.0, 5.0))
    @Test fun multiply_happyPath() = assertValue(12.0, math.multiply(3.0, 4.0))
    @Test fun divide_happyPath() = assertValue(3.0, math.divide(6.0, 2.0))
    @Test fun divide_byZero() = assertErr(math.divide(6.0, 0.0))
    @Test fun divide_negativeByZero() = assertErr(math.divide(-6.0, 0.0))

    // ---- Powers and roots ----

    @Test fun sqrt_positive() = assertValue(3.0, math.sqrt(9.0))
    @Test fun sqrt_zero() = assertValue(0.0, math.sqrt(0.0))
    @Test fun sqrt_negative() = assertErr(math.sqrt(-1.0))
    @Test fun square_positive() = assertValue(9.0, math.square(3.0))
    @Test fun square_negative() = assertValue(9.0, math.square(-3.0))
    @Test fun reciprocal_nonzero() = assertValue(0.25, math.reciprocal(4.0))
    @Test fun reciprocal_zero() = assertErr(math.reciprocal(0.0))
    @Test fun power_normalCase() = assertValue(1024.0, math.power(2.0, 10.0))
    @Test fun power_negativeBaseIntegerExp() = assertValue(-8.0, math.power(-2.0, 3.0))
    @Test fun power_negativeBaseFractionalExp() = assertErr(math.power(-2.0, 0.5))
    @Test fun power_zeroToZero() = assertErr(math.power(0.0, 0.0))
    @Test fun pow10_normal() = assertValue(1000.0, math.pow10(3.0))
    @Test fun pow10_overflow() = assertErr(math.pow10(999.0))

    // ---- Logarithms ----

    @Test fun log10_positive() = assertNear(2.0, math.log10(100.0))
    @Test fun log10_zero() = assertErr(math.log10(0.0))
    @Test fun log10_negative() = assertErr(math.log10(-1.0))
    @Test fun ln_positive() = assertNear(1.0, math.ln(Math.E))
    @Test fun ln_zero() = assertErr(math.ln(0.0))
    @Test fun ln_negative() = assertErr(math.ln(-1.0))
    @Test fun exp_normal() = assertNear(Math.E, math.exp(1.0))
    @Test fun exp_overflow() = assertErr(math.exp(1000.0))

    // ---- Trig DEG ----

    @Test fun sin_zero_deg() = assertNear(0.0, math.sin(0.0, AngleMode.DEG))
    @Test fun sin_90_deg() = assertNear(1.0, math.sin(90.0, AngleMode.DEG))
    @Test fun sin_180_deg() = assertNear(0.0, math.sin(180.0, AngleMode.DEG))
    @Test fun cos_zero_deg() = assertNear(1.0, math.cos(0.0, AngleMode.DEG))
    @Test fun cos_90_deg() = assertNear(0.0, math.cos(90.0, AngleMode.DEG))
    @Test fun cos_180_deg() = assertNear(-1.0, math.cos(180.0, AngleMode.DEG))
    @Test fun tan_zero_deg() = assertNear(0.0, math.tan(0.0, AngleMode.DEG))
    @Test fun tan_45_deg() = assertNear(1.0, math.tan(45.0, AngleMode.DEG))
    @Test fun tan_90_deg() = assertErr(math.tan(90.0, AngleMode.DEG))
    @Test fun arcsin_zero_deg() = assertNear(0.0, math.arcsin(0.0, AngleMode.DEG))
    @Test fun arcsin_one_deg() = assertNear(90.0, math.arcsin(1.0, AngleMode.DEG))
    @Test fun arcsin_outOfRange() = assertErr(math.arcsin(2.0, AngleMode.DEG))
    @Test fun arccos_one_deg() = assertNear(0.0, math.arccos(1.0, AngleMode.DEG))
    @Test fun arccos_zero_deg() = assertNear(90.0, math.arccos(0.0, AngleMode.DEG))
    @Test fun arccos_outOfRange() = assertErr(math.arccos(-2.0, AngleMode.DEG))
    @Test fun arctan_one_deg() = assertNear(45.0, math.arctan(1.0, AngleMode.DEG))
    @Test fun arctan_large() = assertNear(90.0, math.arctan(1e15, AngleMode.DEG), tol = 0.001)

    // ---- Trig RAD ----

    @Test fun sin_halfPi_rad() = assertNear(1.0, math.sin(PI / 2, AngleMode.RAD))
    @Test fun cos_pi_rad() = assertNear(-1.0, math.cos(PI, AngleMode.RAD))
    @Test fun arcsin_one_rad() = assertNear(PI / 2, math.arcsin(1.0, AngleMode.RAD))

    // ---- Percentage ----

    @Test fun percent_of() = assertValue(30.0, math.percentOf(200.0, 15.0))
    @Test fun percent_of_zero_base() = assertValue(0.0, math.percentOf(0.0, 15.0))
    @Test fun percentChange_increase() = assertNear(50.0, math.percentChange(100.0, 150.0))
    @Test fun percentChange_decrease() = assertNear(-50.0, math.percentChange(200.0, 100.0))
    @Test fun percentChange_zeroBase() = assertErr(math.percentChange(0.0, 5.0))

    // ---- Combinatorics ----

    @Test fun factorial_zero() = assertValue(1.0, math.factorial(0.0))
    @Test fun factorial_five() = assertValue(120.0, math.factorial(5.0))
    @Test fun factorial_nonInteger() = assertErr(math.factorial(2.5))
    @Test fun factorial_negative() = assertErr(math.factorial(-1.0))
    @Test fun factorial_overflow() = assertErr(math.factorial(171.0))
    @Test fun combinations_5c2() = assertValue(10.0, math.combinations(5.0, 2.0))
    @Test fun combinations_5c0() = assertValue(1.0, math.combinations(5.0, 0.0))
    @Test fun combinations_5c5() = assertValue(1.0, math.combinations(5.0, 5.0))
    @Test fun combinations_nLessThanR() = assertErr(math.combinations(3.0, 5.0))
    @Test fun combinations_nonInteger() = assertErr(math.combinations(5.1, 2.0))
    @Test fun permutations_5p2() = assertValue(20.0, math.permutations(5.0, 2.0))
    @Test fun permutations_nLessThanR() = assertErr(math.permutations(3.0, 5.0))

    // ---- Polar / Rectangular ----

    @Test fun toPolar_xAxis() {
        val (newY, newX) = math.toPolar(0.0, 1.0, AngleMode.DEG)
        assertNear(0.0, newY)
        assertNear(1.0, newX)
    }

    @Test fun toPolar_yAxis() {
        val (newY, newX) = math.toPolar(1.0, 0.0, AngleMode.DEG)
        assertNear(90.0, newY)
        assertNear(1.0, newX)
    }

    @Test fun toPolar_rad() {
        val (newY, newX) = math.toPolar(1.0, 1.0, AngleMode.RAD)
        assertNear(PI / 4, newY)
        assertNear(sqrt(2.0), newX)
    }

    @Test fun toRect_zeroAngle() {
        val (newY, newX) = math.toRectangular(0.0, 1.0, AngleMode.DEG)
        assertNear(0.0, newY)
        assertNear(1.0, newX)
    }

    @Test fun toRect_90deg() {
        val (newY, newX) = math.toRectangular(90.0, 1.0, AngleMode.DEG)
        assertNear(1.0, newY)
        assertNear(0.0, newX)
    }

    // ---- Constants ----

    @Test fun pi_value() = assertValue(Math.PI, math.pi())
}
