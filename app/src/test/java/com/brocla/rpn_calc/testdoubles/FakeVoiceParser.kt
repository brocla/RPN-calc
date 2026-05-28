package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.voice.VoiceParser

class FakeVoiceParser(
    private val events: List<CalcKeyEvent> = emptyList()
) : VoiceParser {
    var lastUtterance: String? = null

    override fun parse(utterance: String): List<CalcKeyEvent> {
        lastUtterance = utterance
        return events
    }
}
