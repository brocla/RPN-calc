package com.brocla.rpn_calc.voice

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.vosk.Model
import org.vosk.android.StorageService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceModelLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoiceModelProvider {
    private val _model = MutableStateFlow<Model?>(null)
    override val model: StateFlow<Model?> = _model

    fun load() {
        StorageService.unpack(
            context,
            "model-en-us",   // assets subfolder
            "model",         // external storage subfolder
            { model ->
                Log.i(TAG, "Vosk model loaded")
                _model.value = model
            },
            { e ->
                Log.e(TAG, "Failed to load Vosk model", e)
            },
        )
    }

    companion object {
        private const val TAG = "VoiceModelLoader"
    }
}
