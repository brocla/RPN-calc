package com.brocla.rpn_calc.logic.model

sealed class EntryState {
    object Idle : EntryState()

    data class Mantissa(
        val digits: String,
        val hasDecimal: Boolean = false,
        val isNegative: Boolean = false
    ) : EntryState()

    data class Exponent(
        val mantissaDigits: String,
        val mantissaHasDecimal: Boolean,
        val mantissaIsNegative: Boolean,
        val exponentDigits: String,
        val exponentIsNegative: Boolean
    ) : EntryState()
}
