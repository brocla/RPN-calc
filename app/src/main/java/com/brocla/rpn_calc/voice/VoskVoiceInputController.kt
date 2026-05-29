package com.brocla.rpn_calc.voice

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import javax.inject.Inject

/** Sets / restores the audio mode around a recording session. */
fun interface AudioModeController {
    fun setMode(mode: Int)
}

class VoskVoiceInputController @Inject constructor(
    private val modelLoader: VoiceModelProvider,
    private val audioMode: AudioModeController,
) : VoiceInputController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state          = MutableStateFlow<VoiceState>(VoiceState.Idle)
    private val _interimText    = MutableStateFlow("")
    private val _finalUtterance = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override val state:          StateFlow<VoiceState> = _state
    override val interimText:    StateFlow<String>     = _interimText
    override val finalUtterance: SharedFlow<String>    = _finalUtterance

    private var speechService: SpeechService? = null

    // ── Public API ────────────────────────────────────────────────────────────

    override fun startListening() {
        val model = modelLoader.model.value ?: return   // guard: FAB should prevent this
        try {
            audioMode.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION)
            val recognizer = Recognizer(model, SAMPLE_RATE, GRAMMAR)
            recognizer.setMaxAlternatives(1)
            speechService = SpeechService(recognizer, SAMPLE_RATE).also {
                it.startListening(recognitionListener)
            }
            _state.value = VoiceState.Listening
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Vosk", e)
            audioMode.setMode(android.media.AudioManager.MODE_NORMAL)
            _state.value = VoiceState.Error(VoiceError.AudioHardware)
        }
    }

    override fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        audioMode.setMode(android.media.AudioManager.MODE_NORMAL)
        _state.value       = VoiceState.Idle
        _interimText.value = ""
    }

    override fun destroy() = stopListening()

    // ── RecognitionListener ───────────────────────────────────────────────────

    internal val recognitionListener = object : RecognitionListener {

        override fun onPartialResult(hypothesis: String) {
            val partial = parseText(hypothesis, "partial")
            if (partial.isNotEmpty()) _interimText.value = partial
        }

        override fun onResult(hypothesis: String) {
            val text = extractBestResult(hypothesis)
            if (text.isNotEmpty() && text != "[unk]") {
                _interimText.value = "▶ $text"
                scope.launch { _finalUtterance.emit(text) }
            }
        }

        override fun onFinalResult(hypothesis: String) = onResult(hypothesis)

        override fun onError(e: Exception) {
            Log.e(TAG, "Vosk error", e)
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
            audioMode.setMode(android.media.AudioManager.MODE_NORMAL)
            _state.value = VoiceState.Error(VoiceError.Unknown)
        }

        override fun onTimeout() {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseText(hypothesis: String, key: String): String =
        runCatching { JSONObject(hypothesis).optString(key) }.getOrDefault("")

    private fun extractBestResult(hypothesis: String): String {
        val json = runCatching { JSONObject(hypothesis) }.getOrNull() ?: return ""
        val alternatives = json.optJSONArray("alternatives")
        if (alternatives != null && alternatives.length() > 0) {
            return alternatives.getJSONObject(0).optString("text")
        }
        return json.optString("text")
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG         = "VoskVoiceController"
        private const val SAMPLE_RATE = 16000.0f

        val GRAMMAR = """
            ["zero","one","two","to","too","three","four","five","six","seven","eight","nine","oh",
             "ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen",
             "seventeen","eighteen","nineteen",
             "twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety",
             "hundred","thousand","million","billion",
             "enter","push",
             "plus","add",
             "minus","subtract",
             "times","multiply",
             "divide","divided by",
             "point","decimal","dot",
             "clear","clear x","backspace","back space","delete",
             "negate","change sign",
             "roll","roll down",
             "swap","exchange",
             "store","recall",
             "last","last x",
             "root","square root","square","squared",
             "reciprocal","inverse",
             "power","raise","raised","anti","anti log",
             "log","logarithm","natural log","ln",
             "exponential","exponent",
             "pi","factorial","percent","percent change",
             "choose","permutations",
             "sine","sin","sign","cosine","cos","tangent","tan",
             "arcsin","arc sin","arccos","arc cos","arctan","arc tan",
             "all","fix","sci","scientific","eng","engineering",
             "angle",
             "copy","paste",
             "help",
             "[unk]"]
        """.trimIndent()
    }
}
