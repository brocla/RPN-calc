package com.brocla.rpn_calc.di

import android.content.Context
import android.media.AudioManager
import com.brocla.rpn_calc.voice.AudioModeController
import com.brocla.rpn_calc.voice.VoiceInputController
import com.brocla.rpn_calc.voice.VoiceModelLoader
import com.brocla.rpn_calc.voice.VoiceModelProvider
import com.brocla.rpn_calc.voice.VoskVoiceInputController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModelModule {

    @Binds
    @Singleton
    abstract fun bindVoiceModelProvider(impl: VoiceModelLoader): VoiceModelProvider

    companion object {
        @Provides
        @Singleton
        fun provideAudioModeController(@ApplicationContext context: Context): AudioModeController {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return AudioModeController { mode -> am.mode = mode }
        }
    }
}

@Module
@InstallIn(ActivityComponent::class)
abstract class VoiceControllerModule {

    @Binds
    @ActivityScoped
    abstract fun bindVoiceInputController(
        impl: VoskVoiceInputController,
    ): VoiceInputController
}
