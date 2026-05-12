package com.brocla.rpn_calc.logic.model

sealed class DisplayMode {
    data class Fix(val decimalPlaces: Int) : DisplayMode()
    data class Sci(val decimalPlaces: Int) : DisplayMode()
    data class Eng(val decimalPlaces: Int) : DisplayMode()
    object All : DisplayMode()
}
