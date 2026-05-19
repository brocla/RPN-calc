package com.brocla.rpn_calc.logic.entry

import com.brocla.rpn_calc.logic.model.CalculatorState
import kotlin.math.pow
import com.brocla.rpn_calc.logic.model.EntryState

class EntryStateMachine : IEntryStateMachine {

    override fun pressDigit(state: CalculatorState, digit: Int): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> {
                val newStack = if (state.stackLiftEnabled) state.stack.lift() else state.stack
                state.copy(
                    stack = newStack,
                    entryState = EntryState.Standard(digit.toString()),
                    stackLiftEnabled = false
                )
            }
            is EntryState.Standard -> {
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

    override fun pressDecimal(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> {
                val newStack = if (state.stackLiftEnabled) state.stack.lift() else state.stack
                state.copy(
                    stack = newStack,
                    entryState = EntryState.Standard("", hasDecimal = true),
                    stackLiftEnabled = false
                )
            }
            is EntryState.Standard -> {
                if (es.hasDecimal) state
                else state.copy(entryState = es.copy(hasDecimal = true))
            }
            is EntryState.Exponent -> state  // no-op during exponent entry
        }
    }

    override fun pressChs(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Standard -> {
                val isZero = (es.digits.isEmpty() || es.digits == "0") &&
                    (es.fracDigits.isEmpty() || es.fracDigits.all { it == '0' })
                if (isZero) state
                else state.copy(entryState = es.copy(isNegative = !es.isNegative))
            }
            // Once in Exponent entry, CHS always toggles the exponent sign.
            // To change the mantissa sign, backspace out of Exponent mode first.
            is EntryState.Exponent -> state.copy(entryState = es.copy(exponentIsNegative = !es.exponentIsNegative))
            is EntryState.Idle -> state  // handled by CalculatorEngine
        }
    }

    override fun pressEex(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Standard -> {
                val isZero = (es.digits.isEmpty() || es.digits == "0") &&
                    (es.fracDigits.isEmpty() || es.fracDigits.all { it == '0' })
                if (isZero) {
                    // Treat as EEX from Idle: start fresh with mantissa "1"
                    state.copy(
                        entryState = EntryState.Exponent(
                            mantissaIntPart = "1",
                            mantissaFracPart = "",
                            mantissaHasDecimal = false,
                            mantissaIsNegative = false,
                            exponentDigits = "",
                            exponentIsNegative = false
                        )
                    )
                } else {
                    // Truncate mantissa to 8 significant digits to leave room for exponent
                    val maxFrac = (8 - es.digits.length).coerceAtLeast(0)
                    val truncatedFrac = es.fracDigits.take(maxFrac)
                    state.copy(
                        entryState = EntryState.Exponent(
                            mantissaIntPart = es.digits.ifEmpty { "0" }.take(8),
                            mantissaFracPart = truncatedFrac,
                            mantissaHasDecimal = es.hasDecimal,
                            mantissaIsNegative = es.isNegative,
                            exponentDigits = "",
                            exponentIsNegative = false
                        )
                    )
                }
            }
            is EntryState.Idle -> {
                val newStack = if (state.stackLiftEnabled) state.stack.lift() else state.stack
                state.copy(
                    stack = newStack,
                    stackLiftEnabled = false,
                    entryState = EntryState.Exponent(
                        mantissaIntPart = "1",
                        mantissaFracPart = "",
                        mantissaHasDecimal = false,
                        mantissaIsNegative = false,
                        exponentDigits = "",
                        exponentIsNegative = false
                    )
                )
            }
            is EntryState.Exponent -> state  // no-op
        }
    }

    override fun pressBackspace(state: CalculatorState): CalculatorState {
        return when (val es = state.entryState) {
            is EntryState.Idle -> state
            is EntryState.Standard -> when {
                es.hasDecimal && es.fracDigits.isNotEmpty() ->
                    state.copy(entryState = es.copy(fracDigits = es.fracDigits.dropLast(1)))
                es.hasDecimal ->
                    state.copy(entryState = es.copy(hasDecimal = false))
                else -> {
                    val newDigits = es.digits.dropLast(1)
                    if (newDigits.isEmpty()) {
                        state.copy(
                            stack = state.stack.withX(0.0),
                            entryState = EntryState.Idle,
                            stackLiftEnabled = false
                        )
                    } else {
                        state.copy(entryState = es.copy(digits = newDigits))
                    }
                }
            }
            is EntryState.Exponent -> when {
                es.exponentDigits.isNotEmpty() ->
                    state.copy(entryState = es.copy(exponentDigits = es.exponentDigits.dropLast(1)))
                es.exponentIsNegative ->
                    state.copy(entryState = es.copy(exponentIsNegative = false))
                else ->
                    state.copy(
                        entryState = EntryState.Standard(
                            digits = es.mantissaIntPart,
                            fracDigits = es.mantissaFracPart,
                            hasDecimal = es.mantissaHasDecimal,
                            isNegative = es.mantissaIsNegative
                        )
                    )
            }
        }
    }

    override fun completeEntry(state: CalculatorState): CalculatorState {
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

    override fun currentDisplayValue(state: CalculatorState): Double {
        return when (val es = state.entryState) {
            is EntryState.Idle -> state.stack.x
            is EntryState.Standard -> parseMantissa(es)
            is EntryState.Exponent -> parseExponent(es)
        }
    }

    private fun parseMantissa(es: EntryState.Standard): Double {
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
        return signedMantissa * 10.0.pow(expSigned.toDouble())
    }
}
