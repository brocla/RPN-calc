package com.brocla.rpn_calc.logic.model

sealed class CalcResult {
    data class Value(val value: Double) : CalcResult()
    data class Err(val message: String) : CalcResult()
}
