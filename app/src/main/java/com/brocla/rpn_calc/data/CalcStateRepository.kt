package com.brocla.rpn_calc.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brocla.rpn_calc.logic.entry.IEntryStateMachine
import com.brocla.rpn_calc.logic.model.CalculatorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

object CalcPersistenceKeys {
    val CALC_STATE = stringPreferencesKey("calc_state")
}

class CalcStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val entryStateMachine: IEntryStateMachine,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val calcState: Flow<CalculatorState?> = dataStore.data.map { prefs ->
        prefs[CalcPersistenceKeys.CALC_STATE]?.let { stored ->
            try { json.decodeFromString<CalculatorState>(stored) } catch (_: Exception) { null }
        }
    }

    suspend fun save(state: CalculatorState) {
        val committed = entryStateMachine.completeEntry(state)
        dataStore.edit { it[CalcPersistenceKeys.CALC_STATE] = json.encodeToString(committed) }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(CalcPersistenceKeys.CALC_STATE) }
    }
}
