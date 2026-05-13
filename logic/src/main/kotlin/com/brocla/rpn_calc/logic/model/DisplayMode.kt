package com.brocla.rpn_calc.logic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DisplayMode {
    @Serializable @SerialName("fix")
    data class Fix(val decimalPlaces: Int) : DisplayMode()

    @Serializable @SerialName("sci")
    data class Sci(val decimalPlaces: Int) : DisplayMode()

    @Serializable @SerialName("eng")
    data class Eng(val decimalPlaces: Int) : DisplayMode()

    @Serializable @SerialName("all")
    object All : DisplayMode()
}
