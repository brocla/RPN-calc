package com.brocla.rpn_calc.logic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class EntryState {
    @Serializable @SerialName("idle")
    object Idle : EntryState()

    @Serializable @SerialName("mantissa")
    data class Mantissa(
        val digits: String,           // integer-part digits
        val fracDigits: String = "",  // fractional-part digits (only populated after decimal pressed)
        val hasDecimal: Boolean = false,
        val isNegative: Boolean = false
    ) : EntryState()

    @Serializable @SerialName("exponent")
    data class Exponent(
        val mantissaIntPart: String,   // integer-part digits of mantissa
        val mantissaFracPart: String,  // fractional-part digits of mantissa
        val mantissaHasDecimal: Boolean,
        val mantissaIsNegative: Boolean,
        val exponentDigits: String,
        val exponentIsNegative: Boolean
    ) : EntryState()
}
