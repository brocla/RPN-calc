package com.brocla.rpn_calc.ui.theme

import androidx.compose.ui.graphics.Color

object CalcColors {
    val Body          = Color(0xFF1C1C1E)  // calculator body
    val DisplayBezel  = Color(0xFFA8A8A8)  // silver frame
    val DisplayBg     = Color(0xFFB8C99A)  // warm LCD green
    val DisplayText   = Color(0xFF1A2410)  // dark segment ink
    val DisplayOff    = Color(0xFF9AAD80)  // "off" segment ghost
    val KeyTop        = Color(0xFF2A2A2C)  // standard key surface
    val KeyArith      = Color(0xFF323234)  // arithmetic keys (slightly lighter)
    val KeyShift      = Color(0xFFFBD806)  // yellow SHIFT key
    val LabelPrimary  = Color(0xFFEEEEEE)  // white key label
    val LabelShifted  = Color(0xFFD4A017)  // amber shifted label (above key)
    val LabelShiftKey = Color(0xFFFFFFFF)  // white label on orange SHIFT key
    val AnnunciatorOn = Color(0xFF1A2410)  // active annunciator
    val AnnunciatorOff= Color(0xFF8A9A70)  // inactive annunciator
    val KeyPressed    = Color(0xFF484848)  // highlight on touch down
}
