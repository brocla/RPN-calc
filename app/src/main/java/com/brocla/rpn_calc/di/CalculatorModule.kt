package com.brocla.rpn_calc.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.brocla.rpn_calc.data.CalcStateRepository
import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.ui.calculator.ClipboardParser
import com.brocla.rpn_calc.ui.calculator.ClipboardParserImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("calc_state") }

    @Provides @Singleton
    fun provideCalcStateRepository(
        dataStore: DataStore<Preferences>,
        entryStateMachine: EntryStateMachine,
    ): CalcStateRepository = CalcStateRepository(dataStore, entryStateMachine)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CalculatorBindingsModule {
    @Binds @Singleton
    abstract fun bindClipboardParser(impl: ClipboardParserImpl): ClipboardParser
}
