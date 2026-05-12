package com.brocla.rpn_calc.logic.math

import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.CalcResult
import kotlin.math.abs
import kotlin.math.pow

class MathOperations {

    internal val ERR_DIV_ZERO = "Error: divide by zero"
    internal val ERR_DOMAIN = "Error: domain"
    internal val ERR_OVERFLOW = "Error: overflow"
    internal val ERR_INPUT = "Error: invalid input"

    private fun toRadians(x: Double, mode: AngleMode): Double =
        if (mode == AngleMode.DEG) Math.toRadians(x) else x

    private fun fromRadians(x: Double, mode: AngleMode): Double =
        if (mode == AngleMode.DEG) Math.toDegrees(x) else x

    private fun safe(block: () -> Double): CalcResult {
        val r = block()
        return if (r.isInfinite() || r.isNaN()) CalcResult.Err(ERR_OVERFLOW) else CalcResult.Value(r)
    }

    // ---- Arithmetic ----

    fun add(y: Double, x: Double): CalcResult = safe { y + x }
    fun subtract(y: Double, x: Double): CalcResult = safe { y - x }
    fun multiply(y: Double, x: Double): CalcResult = safe { y * x }
    fun divide(y: Double, x: Double): CalcResult =
        if (x == 0.0) CalcResult.Err(ERR_DIV_ZERO) else safe { y / x }

    // ---- Powers and roots ----

    fun sqrt(x: Double): CalcResult =
        if (x < 0.0) CalcResult.Err(ERR_DOMAIN) else safe { kotlin.math.sqrt(x) }

    fun square(x: Double): CalcResult = safe { x * x }

    fun reciprocal(x: Double): CalcResult =
        if (x == 0.0) CalcResult.Err(ERR_DIV_ZERO) else safe { 1.0 / x }

    fun power(y: Double, x: Double): CalcResult = when {
        y == 0.0 && x == 0.0 -> CalcResult.Err(ERR_DOMAIN)
        y < 0.0 && x != kotlin.math.floor(x) -> CalcResult.Err(ERR_DOMAIN)
        y < 0.0 -> {
            val n = x.toLong()
            val result = abs(y).pow(x)
            if (result.isInfinite() || result.isNaN()) CalcResult.Err(ERR_OVERFLOW)
            else if (n % 2 != 0L) CalcResult.Value(-result) else CalcResult.Value(result)
        }
        else -> safe { y.pow(x) }
    }

    fun pow10(x: Double): CalcResult = safe {
        val r = 10.0.pow(x)
        if (r.isInfinite()) Double.POSITIVE_INFINITY else r
    }

    // ---- Logarithms ----

    fun log10(x: Double): CalcResult =
        if (x <= 0.0) CalcResult.Err(ERR_DOMAIN) else safe { kotlin.math.log10(x) }

    fun ln(x: Double): CalcResult =
        if (x <= 0.0) CalcResult.Err(ERR_DOMAIN) else safe { kotlin.math.ln(x) }

    fun exp(x: Double): CalcResult = safe {
        val r = kotlin.math.exp(x)
        if (r.isInfinite()) Double.POSITIVE_INFINITY else r
    }

    // ---- Trigonometry ----

    fun sin(x: Double, mode: AngleMode): CalcResult = safe { kotlin.math.sin(toRadians(x, mode)) }
    fun cos(x: Double, mode: AngleMode): CalcResult = safe { kotlin.math.cos(toRadians(x, mode)) }

    fun tan(x: Double, mode: AngleMode): CalcResult {
        // In DEG mode, check for exact multiples of 90 that are undefined
        if (mode == AngleMode.DEG) {
            val normalized = ((x % 180.0) + 180.0) % 180.0
            if (normalized == 90.0) return CalcResult.Err(ERR_DOMAIN)
        }
        val r = kotlin.math.tan(toRadians(x, mode))
        return if (r.isInfinite() || r.isNaN()) CalcResult.Err(ERR_DOMAIN) else CalcResult.Value(r)
    }

    fun arcsin(x: Double, mode: AngleMode): CalcResult =
        if (abs(x) > 1.0) CalcResult.Err(ERR_DOMAIN)
        else safe { fromRadians(kotlin.math.asin(x), mode) }

    fun arccos(x: Double, mode: AngleMode): CalcResult =
        if (abs(x) > 1.0) CalcResult.Err(ERR_DOMAIN)
        else safe { fromRadians(kotlin.math.acos(x), mode) }

    fun arctan(x: Double, mode: AngleMode): CalcResult =
        safe { fromRadians(kotlin.math.atan(x), mode) }

    // ---- Percentage ----

    fun percentOf(y: Double, x: Double): CalcResult = safe { y * x / 100.0 }

    fun percentChange(y: Double, x: Double): CalcResult =
        if (y == 0.0) CalcResult.Err(ERR_DIV_ZERO)
        else safe { (x - y) / y * 100.0 }

    // ---- Combinatorics ----

    fun factorial(x: Double): CalcResult {
        if (x < 0.0 || x != kotlin.math.floor(x)) return CalcResult.Err(ERR_INPUT)
        val n = x.toInt()
        var result = 1.0
        for (i in 2..n) {
            result *= i
            if (result.isInfinite()) return CalcResult.Err(ERR_OVERFLOW)
        }
        return CalcResult.Value(result)
    }

    fun combinations(y: Double, x: Double): CalcResult {
        if (y != kotlin.math.floor(y) || x != kotlin.math.floor(x)) return CalcResult.Err(ERR_INPUT)
        val n = y.toInt()
        val r = x.toInt()
        if (r < 0 || r > n) return CalcResult.Err(ERR_INPUT)
        // C(n,r) = n! / (r! * (n-r)!)  — compute iteratively to avoid overflow
        val k = minOf(r, n - r)
        var result = 1.0
        for (i in 0 until k) {
            result = result * (n - i) / (i + 1)
            if (result.isInfinite()) return CalcResult.Err(ERR_OVERFLOW)
        }
        return CalcResult.Value(kotlin.math.round(result).toDouble())
    }

    fun permutations(y: Double, x: Double): CalcResult {
        if (y != kotlin.math.floor(y) || x != kotlin.math.floor(x)) return CalcResult.Err(ERR_INPUT)
        val n = y.toInt()
        val r = x.toInt()
        if (r < 0 || r > n) return CalcResult.Err(ERR_INPUT)
        var result = 1.0
        for (i in 0 until r) {
            result *= (n - i)
            if (result.isInfinite()) return CalcResult.Err(ERR_OVERFLOW)
        }
        return CalcResult.Value(result)
    }

    // ---- Polar / Rectangular ----

    /** Returns Pair(newY=angle, newX=radius) */
    fun toPolar(y: Double, x: Double, mode: AngleMode): Pair<CalcResult, CalcResult> {
        val radius = kotlin.math.sqrt(x * x + y * y)
        val angle = fromRadians(kotlin.math.atan2(y, x), mode)
        return Pair(CalcResult.Value(angle), CalcResult.Value(radius))
    }

    /** Returns Pair(newY=y-coord, newX=x-coord) */
    fun toRectangular(y: Double, x: Double, mode: AngleMode): Pair<CalcResult, CalcResult> {
        val angle = toRadians(y, mode)
        val radius = x
        val xCoord = radius * kotlin.math.cos(angle)
        val yCoord = radius * kotlin.math.sin(angle)
        return Pair(CalcResult.Value(yCoord), CalcResult.Value(xCoord))
    }

    // ---- Constants ----

    fun pi(): CalcResult = CalcResult.Value(Math.PI)
}
