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

        // Check if integer part overflows 10 digits
        val intPartLen = "%.0f".format(abs(v)).length
        if (intPartLen > 10) return formatSci(v, minOf(dp, 6))

        // Format at requested dp
        val formatted = "%.${dp}f".format(v)

        // If the result loses precision (e.g. 0.001 shown as "0.00"), expand dp to show value
        // but cap total digit width at 10 (integer digits + decimal digits)
        val absFormatted = formatted.removePrefix("-")
        val intPart = absFormatted.substringBefore(".")
        val intLen = intPart.length  // e.g. "0" → 1

        val isLossy = v != 0.0 && "%.${dp}f".format(v).replace("-", "").replace(".", "").all { it == '0' }
        if (isLossy) {
            // Find minimum dp to display a non-zero value
            val neededDp = if (abs(v) > 0) {
                (-floor(log10(abs(v)))).toInt()
            } else dp
            val maxDp = 10 - intLen
            return if (neededDp > maxDp) formatSci(v, minOf(dp, 6))
            else "%.${neededDp}f".format(v)
        }

        return formatted
    }

    private fun formatSci(v: Double, dp: Int): String {
        if (v == 0.0) return if (dp == 0) "0e+00" else "0.${"0".repeat(dp)}e+00"
        val formatted = "%.${dp}e".format(v)
        return normalizeExp(formatted)
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
        if (v == 0.0) return if (dp == 0) "0e+00" else "0.${"0".repeat(dp)}e+00"

        val absV = abs(v)
        val sign = if (v < 0) "-" else ""

        val exp = floor(log10(absV)).toInt()
        val engExp = floor(exp / 3.0).toInt() * 3
        val mantissa = absV / 10.0.pow(engExp.toDouble())

        val mantissaStr = "%.${dp}f".format(mantissa)
        val expSign = if (engExp < 0) "-" else "+"
        val expStr = abs(engExp).toString().padStart(2, '0')
        return "$sign${mantissaStr}e$expSign$expStr"
    }

    private fun formatAll(v: Double): String {
        if (v == 0.0) return "0"
        // 10 significant digits, no trailing zeros
        val sig = "%.10g".format(v)
        return if (!sig.contains('e') && !sig.contains('E')) {
            sig.trimEnd('0').trimEnd('.')
        } else {
            normalizeExp(sig)
        }
    }
}
