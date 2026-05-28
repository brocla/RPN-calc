package com.brocla.rpn_calc

import android.app.Application
import com.brocla.rpn_calc.voice.VoiceModelLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RpnCalcApplication : Application() {

    @Inject lateinit var voiceModelLoader: VoiceModelLoader

    override fun onCreate() {
        super.onCreate()
        voiceModelLoader.load()
    }
}
