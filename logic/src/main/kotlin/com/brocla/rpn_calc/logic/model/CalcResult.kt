package com.brocla.rpn_calc.logic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CalcResult {
    @Serializable @SerialName("value")
    data class Value(val value: Double) : CalcResult()

    @Serializable @SerialName("err")
    data class Err(val message: String) : CalcResult()
}
