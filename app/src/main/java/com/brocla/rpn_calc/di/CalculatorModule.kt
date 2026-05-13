package com.brocla.rpn_calc.di

import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalculatorModule {

    @Provides @Singleton
    fun provideEntryStateMachine() = EntryStateMachine()

    @Provides @Singleton
    fun provideMathOperations() = MathOperations()

    @Provides @Singleton
    fun provideDisplayFormatter() = DisplayFormatter()

    @Provides @Singleton
    fun provideCalculatorEngine(
        esm: EntryStateMachine,
        math: MathOperations,
        fmt: DisplayFormatter,
    ) = CalculatorEngine(esm, math, fmt)
}
