package com.brocla.rpn_calc.di

import com.brocla.rpn_calc.voice.VoiceInputController
import com.brocla.rpn_calc.voice.VoiceModelLoader
import com.brocla.rpn_calc.voice.VoiceModelProvider
import com.brocla.rpn_calc.voice.VoskVoiceInputController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModelModule {

    @Binds
    @Singleton
    abstract fun bindVoiceModelProvider(impl: VoiceModelLoader): VoiceModelProvider
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
