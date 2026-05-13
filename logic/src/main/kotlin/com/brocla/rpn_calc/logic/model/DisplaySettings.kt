package com.brocla.rpn_calc.logic.model

import kotlinx.serialization.Serializable

@Serializable
data class DisplaySettings(val mode: DisplayMode = DisplayMode.Fix(4))
