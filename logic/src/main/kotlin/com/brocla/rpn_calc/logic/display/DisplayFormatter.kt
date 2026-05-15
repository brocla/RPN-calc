package com.brocla.rpn_calc.logic.display

import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.EntryState
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class DisplayFormatter {

    fun format(state: CalculatorState): String {
        state.error?.let { return it }

        return when (val es = state.entryState) {
            is EntryState.Idle -> formatValue(state.stack.x, state.displaySettings.mode)
            is EntryState.Mantissa -> formatMantissa(es)
            is EntryState.Exponent -> formatExponent(es)
        }
    }

    private fun formatMantissa(es: EntryState.Mantissa): String {
        val sign = if (es.isNegative) "-" else ""
        val intDisplay = es.digits.ifEmpty { "0" }
        val str = if (es.hasDecimal) "$intDisplay.${es.fracDigits}" else intDisplay
        return "$sign$str"
    }

    private fun formatExponent(es: EntryState.Exponent): String {
        val mantissaSign = if (es.mantissaIsNegative) "-" else ""
        val intPart = es.mantissaIntPart.ifEmpty { "1" }
        val mantissaStr = if (es.mantissaHasDecimal || es.mantissaFracPart.isNotEmpty()) {
            "$intPart.${es.mantissaFracPart}"
        } else {
            intPart
        }
        val expSign = if (es.exponentIsNegative) "-" else ""
        val expDigits = es.exponentDigits.padStart(2, '0')
        return "$mantissaSign$mantissaStr E$expSign$expDigits"
    }

    private fun formatValue(value: Double, mode: DisplayMode): String {
        val v = if (value == 0.0) 0.0 else value  // collapse -0.0
        return when (mode) {
            is DisplayMode.Fix -> formatFix(v, mode.decimalPlaces)
            is DisplayMode.Sci -> formatSci(v, mode.decimalPlaces)
            is DisplayMode.Eng -> formatEng(v, mode.decimalPlaces)
            is DisplayMode.All -> formatAll(v)
        }
    }

    private fun formatFix(v: Double, dp: Int): String {
        if (v == 0.0) return if (dp == 0) "0" else "0.${"0".repeat(dp)}"

        val signWidth = if (v < 0) 1 else 0
        val intPartLen = "%.0f".format(abs(v)).length

        // Check if integer part overflows the display (accounting for sign)
        if (signWidth + intPartLen > 10) return formatSci(v, dp)

        // Cap dp so the full display fits in 10 chars
        val maxDp = 10 - signWidth - intPartLen
        val effectiveDp = minOf(dp, maxOf(0, maxDp))

        // Format at the effective dp
        val formatted = "%.${effectiveDp}f".format(v)

        // If the result loses precision (e.g. 0.001 shown as "0.00"), expand dp to show value
        val isLossy = v != 0.0 &&
            formatted.replace("-", "").replace(".", "").all { it == '0' }
        if (isLossy) {
            val neededDp = if (abs(v) > 0) (-floor(log10(abs(v)))).toInt() else effectiveDp
            return if (neededDp > maxDp) formatSci(v, dp)
            else "%.${neededDp}f".format(v)
        }

        return formatted
    }

    /** Width of the exponent suffix, e.g. "e+47" = 4, "e+308" = 5. */
    private fun expSuffixWidth(absExp: Int): Int = 2 + maxOf(2, absExp.toString().length)

    private fun formatSci(v: Double, dp: Int): String {
        val signWidth = if (v < 0) 1 else 0
        if (v == 0.0) {
            val maxDp = 9 - signWidth - 4  // exponent is always "00", suffix = 4
            val cappedDp = minOf(dp, maxOf(0, maxDp))
            return if (cappedDp == 0) "0e+00" else "0.${"0".repeat(cappedDp)}e+00"
        }
        // Probe at dp=0 to read the actual exponent without fragile log10 arithmetic
        val probe = normalizeExp("%.0e".format(v))
        val absExp = probe.substringAfterLast('e')
            .removePrefix("+").removePrefix("-")
            .trimStart('0').ifEmpty { "0" }.toInt()
        val expIsNegative = probe.substringAfterLast('e').startsWith('-')
        if (absExp > 99) return if (expIsNegative) "Underflow" else "Overflow"
        val suffix = expSuffixWidth(absExp)
        val maxDp = 9 - signWidth - suffix
        val cappedDp = minOf(dp, maxOf(0, maxDp))
        return normalizeExp("%.${cappedDp}e".format(v))
    }

    private fun normalizeExp(s: String): String {
        val eIdx = s.indexOfFirst { it == 'e' || it == 'E' }
        if (eIdx < 0) return s
        val mantissa = s.substring(0, eIdx)
        val expPart = s.substring(eIdx + 1)
        val expSign = if (expPart.startsWith('-')) "-" else "+"
        val expNum = expPart.removePrefix("+").removePrefix("-").trimStart('0').ifEmpty { "0" }.padStart(2, '0')
        return "${mantissa}e${expSign}${expNum}"
    }

    private fun formatEng(v: Double, dp: Int): String {
        val signWidth = if (v < 0) 1 else 0
        if (v == 0.0) {
            val maxDp = 10 - signWidth - 1 - 4  // mantissaIntDigits=1, suffix=4 for "e+00"
            val cappedDp = minOf(dp, maxOf(0, maxDp))
            return if (cappedDp == 0) "0e+00" else "0.${"0".repeat(cappedDp)}e+00"
        }

        val absV = abs(v)
        val sign = if (v < 0) "-" else ""

        val exp = floor(log10(absV)).toInt()
        val engExp = floor(exp / 3.0).toInt() * 3
        if (abs(engExp) > 99) return if (engExp < 0) "Underflow" else "Overflow"
        val mantissa = absV / 10.0.pow(engExp.toDouble())

        // Probe mantissa integer digit count without fragile rounding assumptions
        val mantissaIntDigits = "%.0f".format(mantissa).length
        val suffix = expSuffixWidth(abs(engExp))
        val maxDp = 10 - signWidth - mantissaIntDigits - suffix
        val cappedDp = minOf(dp, maxOf(0, maxDp))

        val mantissaStr = "%.${cappedDp}f".format(mantissa)
        val expSign = if (engExp < 0) "-" else "+"
        val expStr = abs(engExp).toString().padStart(2, '0')
        return "$sign${mantissaStr}e$expSign$expStr"
    }

    private fun formatAll(v: Double): String {
        if (v == 0.0) return "0"
        val sig = "%.10g".format(v)
        if (!sig.contains('e') && !sig.contains('E')) {
            // Only trim trailing zeros from the fractional part; never from integer digits
            return if (sig.contains('.')) sig.trimEnd('0').trimEnd('.') else sig
        }
        // Scientific notation: normalize then strip trailing zeros from mantissa
        val normalized = normalizeExp(sig)
        val eIdx = normalized.indexOf('e')
        val trimmed = normalized.substring(0, eIdx).trimEnd('0').trimEnd('.') +
                      normalized.substring(eIdx)
        if (trimmed.count { it != '.' && it != ',' } <= 10) return trimmed

        // Still too wide (dense value, no trailing zeros): tighten dp budget
        val signWidth = if (v < 0) 1 else 0
        val absExp = normalized.substring(eIdx + 1)
            .removePrefix("+").removePrefix("-")
            .trimStart('0').ifEmpty { "0" }.toInt()
        val maxDp = 9 - signWidth - expSuffixWidth(absExp)
        val sigFigs = 1 + maxOf(0, maxDp)
        val reduced = normalizeExp("%.${sigFigs}g".format(v))
        val eIdx2 = reduced.indexOf('e')
        return reduced.substring(0, eIdx2).trimEnd('0').trimEnd('.') +
               reduced.substring(eIdx2)
    }
}
