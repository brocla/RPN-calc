package com.brocla.rpn_calc.testdoubles

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake DataStore<Preferences> for CalcStateRepository isolation tests.
 *
 * Backed by a MutableStateFlow so that the repository's [calcState] flow
 * reflects writes made through [updateData] / the [edit] extension function.
 */
class FakeDataStore : DataStore<Preferences> {

    private val _data = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val result = transform(_data.value)
        _data.value = result
        return result
    }
}
