package com.brocla.rpn_calc.logic.display

sealed class DisplayResult {
    data class Text(val string: String) : DisplayResult()
    sealed class RangeError(val label: String) : DisplayResult() {
        object Overflow  : RangeError("Overflow")
        object Underflow : RangeError("Underflow")
    }
}
