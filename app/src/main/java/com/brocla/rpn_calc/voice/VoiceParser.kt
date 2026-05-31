package com.brocla.rpn_calc.voice

import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent

interface VoiceParser {
    fun parse(utterance: String): List<CalcKeyEvent>
}
