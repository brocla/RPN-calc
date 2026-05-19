package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.EntryState
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class DisplayFormatter : IDisplayFormatter {

    override fun format(state: CalculatorState): String {
        state.error?.let { return it }  // errors: no sign slot

        return when (val es = state.entryState) {
            is EntryState.Idle     -> formatValue(state.stack.x, state.displaySettings.mode)
            is EntryState.Standard -> formatStandard(es)
            is EntryState.Exponent -> formatExponent(es)
        }
    }

    // ── Entry state formatting ────────────────────────────────────────────────

    private fun formatStandard(es: EntryState.Standard): String {
        val sign = if (es.isNegative) "-" else " "
        val intDisplay = es.digits.ifEmpty { "0" }
        val content = if (es.hasDecimal) "$intDisplay.${es.fracDigits}" else intDisplay
        return "$sign$content"
    }

    /**
     * Formats exponent-entry state into a fixed-width positional string.
     *
     * Output structure (12 or 13 chars):
     *   sign(1) + mantissaContent + padding + expSign(1) + expStr(2)
     *
     * No 'E' character. No '+' for positive exponent.
     * expStr encoding: 0 digits → "  ", 1 digit → "${d} ", 2 digits → d
     */
    private fun formatExponent(es: EntryState.Exponent): String {
        val sign = if (es.mantissaIsNegative) "-" else " "
        val intPart = es.mantissaIntPart.ifEmpty { "1" }
        val mantissaContent = if (es.mantissaHasDecimal || es.mantissaFracPart.isNotEmpty()) {
            "$intPart.${es.mantissaFracPart}"
        } else {
            intPart
        }
        val sigDigitCount = intPart.length + es.mantissaFracPart.length
        val paddingSpaces = maxOf(0, 8 - sigDigitCount)
        val expSign = if (es.exponentIsNegative) '-' else ' '
        val expStr = when (es.exponentDigits.length) {
            0    -> "00"
            1    -> "0${es.exponentDigits[0]}"
            else -> es.exponentDigits
        }
        return "$sign$mantissaContent${" ".repeat(paddingSpaces)}$expSign$expStr"
    }

    // ── Idle state formatting ─────────────────────────────────────────────────

    private fun formatValue(value: Double, mode: DisplayMode): String {
        val v = value + 0.0  // collapse -0.0 → 0.0 so negative sign never appears before zero
        return when (mode) {
            is DisplayMode.Fix -> formatFix(v, mode.decimalPlaces)
            is DisplayMode.Sci -> formatSci(v, mode.decimalPlaces)
            is DisplayMode.Eng -> formatEng(v, mode.decimalPlaces)
            is DisplayMode.All -> formatAll(v)
        }
    }

    private fun formatFix(v: Double, dp: Int): String {
        val sign = if (v < 0) "-" else " "

        if (v == 0.0) {
            val content = if (dp == 0) "0." else "0.${"0".repeat(dp)}"
            return "$sign$content"
        }

        val intPartLen = "%.0f".format(abs(v)).length
        if (intPartLen > 10) return formatSci(v, dp)

        // Cap dp so total display fits in 10 digit positions
        val maxDp = (10 - intPartLen).coerceAtLeast(0)
        val effectiveDp = minOf(dp, maxDp)

        val formatted = "%.${effectiveDp}f".format(abs(v))

        // Strict fixed with SCI fallback: if result shows no significant digit, fall back
        val isAllZeros = formatted.replace(".", "").all { it == '0' }
        if (isAllZeros) return formatSci(v, dp)

        // Always show decimal point
        val withDot = if (formatted.contains('.')) formatted else "$formatted."
        return "$sign$withDot"
    }

    /**
     * Formats in scientific notation. Always produces exactly 13 chars for non-error output.
     *
     * Structure: sign(1) + sigStr(cappedDp+2) + padding(7-cappedDp) + expSign(1) + expStr(2)
     * No 'e' character. No '+' for positive exponent.
     * cappedDp = min(dp, 7): at most 7 frac digits in the significand (8 sig digits total).
     */
    private fun formatSci(v: Double, dp: Int): String {
        val sign = if (v < 0) "-" else " "
        val cappedDp = minOf(dp, 7)

        if (v == 0.0) {
            val sigStr = if (cappedDp == 0) "0." else "0.${"0".repeat(cappedDp)}"
            return "$sign$sigStr${" ".repeat(7 - cappedDp)} 00"
        }

        // Use %e at the desired precision; extract mantissa string and exponent together.
        val raw = "%.${cappedDp}e".format(abs(v))
        val eIdx = raw.indexOf('e')
        val rawMantissa = raw.substring(0, eIdx)
        val expPart = raw.substring(eIdx + 1)
        val expIsNegative = expPart.startsWith('-')
        val absExp = expPart.removePrefix("+").removePrefix("-")
            .trimStart('0').ifEmpty { "0" }.toInt()
        if (absExp > 99) return if (expIsNegative) "Underflow" else "Overflow"

        // Append "." for dp=0 (always show decimal point, per display spec)
        val sigStr = if (cappedDp == 0) "$rawMantissa." else rawMantissa
        val paddingSpaces = 7 - cappedDp
        val expSign = if (expIsNegative) '-' else ' '
        val expStr = absExp.toString().padStart(2, '0')
        return "$sign$sigStr${" ".repeat(paddingSpaces)}$expSign$expStr"
    }

    /**
     * Formats in engineering notation. Always produces exactly 13 chars for non-error output.
     *
     * Structure: sign(1) + sigStr + padding + expSign(1) + expStr(2) = 13
     * The significand occupies exactly 8 positions (int digits 1–3 + dot + frac digits).
     * No 'e' character. No '+' for positive exponent.
     */
    private fun formatEng(v: Double, dp: Int): String {
        val sign = if (v < 0) "-" else " "

        if (v == 0.0) {
            val cappedDp = minOf(dp, 7)
            val sigStr = if (cappedDp == 0) "0." else "0.${"0".repeat(cappedDp)}"
            return "$sign$sigStr${" ".repeat(7 - cappedDp)} 00"
        }

        val absV = abs(v)
        val exp = floor(log10(absV)).toInt()
        val engExp = floor(exp / 3.0).toInt() * 3
        if (abs(engExp) > 99) return if (engExp < 0) "Underflow" else "Overflow"

        val mantissa = absV / 10.0.pow(engExp.toDouble())
        // Use log10 for digit-count estimate — avoids rounding distortion from "%.0f".
        val estimatedIntDigits = (floor(log10(mantissa)).toInt() + 1).coerceIn(1, 3)
        val maxFrac = (8 - estimatedIntDigits).coerceAtLeast(0)
        val cappedDp = minOf(dp, maxFrac)

        var mantissaStr = "%.${cappedDp}f".format(mantissa)
        var finalEngExp = engExp

        // If rounding pushed the mantissa past the tier boundary (e.g. 999.95 dp=1 → "1000.0"),
        // step up one ENG tier and reformat. One retry is sufficient — the new mantissa is ~1.xxx.
        if (mantissaStr.substringBefore('.').length > 3) {
            finalEngExp += 3
            if (abs(finalEngExp) > 99) return if (finalEngExp < 0) "Underflow" else "Overflow"
            val newMantissa = absV / 10.0.pow(finalEngExp.toDouble())
            val newCappedDp = minOf(dp, 7)  // 1 int digit after stepping up, so maxFrac = 7
            mantissaStr = "%.${newCappedDp}f".format(newMantissa)
        }

        val actualIntDigits = mantissaStr.substringBefore('.').length
        val actualFracDigits = mantissaStr.substringAfter('.', "").length
        val sigStr = if (!mantissaStr.contains('.')) "$mantissaStr." else mantissaStr
        val paddingSpaces = 8 - actualIntDigits - actualFracDigits
        val expSign = if (finalEngExp < 0) '-' else ' '
        val expStr = abs(finalEngExp).toString().padStart(2, '0')
        return "$sign$sigStr${" ".repeat(paddingSpaces)}$expSign$expStr"
    }

    private fun formatAll(v: Double): String {
        val sign = if (v < 0) "-" else " "

        if (v == 0.0) return "${sign}0"

        val sig = "%.10g".format(v)
        if (!sig.contains('e') && !sig.contains('E')) {
            val raw = sig.trimStart('-')
            val trimmed = if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
            return "$sign$trimmed"
        }

        // Value doesn't fit in fixed notation: delegate to SCI with max precision
        return formatSci(v, 7)
    }
}
