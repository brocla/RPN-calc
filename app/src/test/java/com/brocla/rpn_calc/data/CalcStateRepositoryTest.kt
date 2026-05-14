package com.brocla.rpn_calc.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalcStateRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun testRepository(): CalcStateRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("test.preferences_pb") },
        )
        return CalcStateRepository(dataStore, EntryStateMachine())
    }

    @Test
    fun save_and_reload_preserves_stack() = runTest {
        val repo = testRepository()
        val state = CalculatorState(
            stack = Stack(x = 1.0, y = 2.0, z = 3.0, t = 0.0),
        )
        repo.save(state)
        val loaded = repo.calcState.first()
        assertEquals(1.0, loaded?.stack?.x)
        assertEquals(2.0, loaded?.stack?.y)
        assertEquals(3.0, loaded?.stack?.z)
    }

    @Test
    fun save_and_reload_preserves_memory() = runTest {
        val repo = testRepository()
        val memory = List(10) { if (it == 3) 42.0 else 0.0 }
        val state = CalculatorState(memory = memory)
        repo.save(state)
        val loaded = repo.calcState.first()
        assertEquals(42.0, loaded?.memory?.get(3))
    }

    @Test
    fun save_and_reload_preserves_display_mode() = runTest {
        val repo = testRepository()
        val state = CalculatorState(
            displaySettings = DisplaySettings(mode = DisplayMode.Fix(2))
        )
        repo.save(state)
        val loaded = repo.calcState.first()
        assertEquals(DisplayMode.Fix(2), loaded?.displaySettings?.mode)
    }

    @Test
    fun save_and_reload_preserves_angle_mode() = runTest {
        val repo = testRepository()
        val state = CalculatorState(angleMode = AngleMode.RAD)
        repo.save(state)
        val loaded = repo.calcState.first()
        assertEquals(AngleMode.RAD, loaded?.angleMode)
    }

    @Test
    fun save_commits_partial_entry() = runTest {
        val repo = testRepository()
        val esm = EntryStateMachine()
        var state = CalculatorState()
        state = esm.pressDigit(state, 3)
        state = esm.pressDecimal(state)
        state = esm.pressDigit(state, 1)
        // entryState is now Mantissa "3.1"
        repo.save(state)
        val loaded = repo.calcState.first()!!
        assertEquals(3.1, loaded.stack.x, 1e-10)
        assertEquals(EntryState.Idle, loaded.entryState)
    }

    @Test
    fun clear_resets_to_null() = runTest {
        val repo = testRepository()
        repo.save(CalculatorState(angleMode = AngleMode.RAD))
        repo.clear()
        val loaded = repo.calcState.first()
        assertNull(loaded)
    }
}
