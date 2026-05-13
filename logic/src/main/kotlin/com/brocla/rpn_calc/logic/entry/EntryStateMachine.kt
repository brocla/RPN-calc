package com.brocla.rpn_calc.logic.entry

import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.EntryState

class EntryStateMachine {

    fun pressDigit(state: CalculatorState, digit: Int): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> {
                val newStack = if (state.stackLiftEnabled) state.stack.lift() else state.stack
                state.copy(
                    stack = newStack,
                    entryState = EntryState.Mantissa(digit.toString()),
                    stackLiftEnabled = false
                )
            }
            is EntryState.Mantissa -> {
                val totalDigits = es.digits.length + es.fracDigits.length
                if (totalDigits >= 10) return state
                if (es.hasDecimal) {
                    // Append to fractional part
                    state.copy(entryState = es.copy(fracDigits = es.fracDigits + digit.toString()))
                } else {
                    // Append to integer part, suppressing leading zeros
                    val newDigits = when {
                        (es.digits == "0" || es.digits.isEmpty()) && digit == 0 -> es.digits
                        es.digits == "0" && digit != 0 -> digit.toString()
                        else -> es.digits + digit.toString()
                    }
                    state.copy(entryState = es.copy(digits = newDigits))
                }
            }
            is EntryState.Exponent -> {
                if (es.exponentDigits.length >= 2) return state
                state.copy(entryState = es.copy(exponentDigits = es.exponentDigits + digit.toString()))
            }
        }
    }

    fun pressDecimal(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> state.copy(
                entryState = EntryState.Mantissa("", hasDecimal = true)
            )
            is EntryState.Mantissa -> {
                if (es.hasDecimal) state
                else state.copy(entryState = es.copy(hasDecimal = true))
            }
            is EntryState.Exponent -> state  // no-op during exponent entry
        }
    }

    fun pressChs(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Mantissa -> state.copy(entryState = es.copy(isNegative = !es.isNegative))
            is EntryState.Exponent -> state.copy(entryState = es.copy(exponentIsNegative = !es.exponentIsNegative))
            is EntryState.Idle -> state  // handled by CalculatorEngine
        }
    }

    fun pressEex(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Mantissa -> state.copy(
                entryState = EntryState.Exponent(
                    mantissaIntPart = es.digits,
                    mantissaFracPart = es.fracDigits,
                    mantissaHasDecimal = es.hasDecimal,
                    mantissaIsNegative = es.isNegative,
                    exponentDigits = "",
                    exponentIsNegative = false
                )
            )
            is EntryState.Idle -> state.copy(
                entryState = EntryState.Exponent(
                    mantissaIntPart = "1",
                    mantissaFracPart = "",
                    mantissaHasDecimal = false,
                    mantissaIsNegative = false,
                    exponentDigits = "",
                    exponentIsNegative = false
                )
            )
            is EntryState.Exponent -> state  // no-op
        }
    }

    fun pressBackspace(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> state
            is EntryState.Mantissa -> when {
                es.hasDecimal && es.fracDigits.isNotEmpty() ->
                    state.copy(entryState = es.copy(fracDigits = es.fracDigits.dropLast(1)))
                es.hasDecimal ->
                    state.copy(entryState = es.copy(hasDecimal = false))
                es.digits.isEmpty() -> state
                else -> state.copy(entryState = es.copy(digits = es.digits.dropLast(1)))
            }
            is EntryState.Exponent -> {
                if (es.exponentDigits.isEmpty()) {
                    state.copy(
                        entryState = EntryState.Mantissa(
                            digits = es.mantissaIntPart,
                            fracDigits = es.mantissaFracPart,
                            hasDecimal = es.mantissaHasDecimal,
                            isNegative = es.mantissaIsNegative
                        )
                    )
                } else {
                    state.copy(entryState = es.copy(exponentDigits = es.exponentDigits.dropLast(1)))
                }
            }
        }
    }

    fun completeEntry(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> state
            else -> {
                val value = currentDisplayValue(state)
                state.copy(
                    stack = state.stack.withX(value),
                    entryState = EntryState.Idle,
                    stackLiftEnabled = true
                )
            }
        }
    }

    fun currentDisplayValue(state: CalculatorState): Double {
        return when (val es = state.entryState) {
            is EntryState.Idle -> state.stack.x
            is EntryState.Mantissa -> parseMantissa(es)
            is EntryState.Exponent -> parseExponent(es)
        }
    }

    private fun parseMantissa(es: EntryState.Mantissa): Double {
        val intPart = es.digits.ifEmpty { "0" }
        val str = if (es.hasDecimal) "$intPart.${es.fracDigits}" else intPart
        val value = str.toDoubleOrNull() ?: 0.0
        return if (es.isNegative) -value else value
    }

    private fun parseExponent(es: EntryState.Exponent): Double {
        val intPart = es.mantissaIntPart.ifEmpty { "1" }
        val str = if (es.mantissaHasDecimal || es.mantissaFracPart.isNotEmpty()) {
            "$intPart.${es.mantissaFracPart}"
        } else {
            intPart
        }
        val mantissa = str.toDoubleOrNull() ?: 1.0
        val signedMantissa = if (es.mantissaIsNegative) -mantissa else mantissa
        val expStr = es.exponentDigits.ifEmpty { "0" }
        val exp = expStr.toIntOrNull() ?: 0
        val expSigned = if (es.exponentIsNegative) -exp else exp
        return signedMantissa * Math.pow(10.0, expSigned.toDouble())
    }
}
