package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.voice.VoiceError
import com.brocla.rpn_calc.voice.VoiceInputController
import com.brocla.rpn_calc.voice.VoiceState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVoiceInputController : VoiceInputController {

    private val _state          = MutableStateFlow<VoiceState>(VoiceState.Idle)
    private val _interimText    = MutableStateFlow("")
    private val _finalUtterance = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override val state:          StateFlow<VoiceState> = _state
    override val interimText:    StateFlow<String>     = _interimText
    override val finalUtterance: SharedFlow<String>    = _finalUtterance

    var startCallCount   = 0
    var stopCallCount    = 0
    var destroyCallCount = 0

    override fun startListening() { startCallCount++   }   // does NOT set state
    override fun stopListening()  { stopCallCount++    }   // does NOT set state
    override fun destroy()        { destroyCallCount++ }

    // Tests call these to drive state explicitly
    fun emitListening()                      { _state.value = VoiceState.Listening }
    fun emitIdle()                           { _state.value = VoiceState.Idle }
    fun emitError(error: VoiceError)         { _state.value = VoiceState.Error(error) }
    suspend fun emitUtterance(text: String)  = _finalUtterance.emit(text)
    fun emitInterim(text: String)            { _interimText.value = text }
}
