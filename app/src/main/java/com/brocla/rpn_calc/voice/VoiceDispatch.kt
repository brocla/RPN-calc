package com.brocla.rpn_calc.voice

import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import kotlinx.coroutines.flow.Flow

suspend fun collectAndDispatch(
    utterances: Flow<String>,
    parser:     VoiceParser,
    onKey:      (CalcKeyEvent) -> Unit,
) {
    utterances.collect { utterance ->
        parser.parse(utterance).forEach(onKey)
    }
}
