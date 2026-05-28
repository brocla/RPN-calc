package com.brocla.rpn_calc.di

import com.brocla.rpn_calc.voice.VoiceInputController
import com.brocla.rpn_calc.voice.VoskVoiceInputController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class VoiceControllerModule {

    @Binds
    @ActivityScoped
    abstract fun bindVoiceInputController(
        impl: VoskVoiceInputController,
    ): VoiceInputController
}
