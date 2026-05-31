package com.brocla.rpn_calc.voice

import kotlinx.coroutines.flow.StateFlow
import org.vosk.Model

interface VoiceModelProvider {
    val model: StateFlow<Model?>
}
