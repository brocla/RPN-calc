package com.brocla.rpn_calc.di

import com.brocla.rpn_calc.voice.DigitSequenceParser
import com.brocla.rpn_calc.voice.DigitSequenceParserImpl
import com.brocla.rpn_calc.voice.VoiceParser
import com.brocla.rpn_calc.voice.VoiceParserImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceParserModule {

    @Binds
    @Singleton
    abstract fun bindVoiceParser(impl: VoiceParserImpl): VoiceParser

    @Binds
    @Singleton
    abstract fun bindDigitSequenceParser(impl: DigitSequenceParserImpl): DigitSequenceParser
}
