package com.brocla.rpn_calc.voice

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class VoiceError {
    AudioHardware, InsufficientPermissions, NoSpeechDetected,
    RecognizerBusy, NetworkUnavailable, Unknown,
}

sealed class VoiceState {
    data object Idle      : VoiceState()
    data object Listening : VoiceState()
    data class  Error(val error: VoiceError) : VoiceState()
}

interface VoiceInputController {
    val state:          StateFlow<VoiceState>
    val interimText:    StateFlow<String>   // interim results for overlay
    val finalUtterance: SharedFlow<String>  // one emission per complete utterance

    fun startListening()
    fun stopListening()
    fun destroy()                           // called from Activity.onDestroy()
}
