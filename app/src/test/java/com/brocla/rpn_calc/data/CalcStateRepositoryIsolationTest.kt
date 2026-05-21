package com.brocla.rpn_calc.data

import com.brocla.rpn_calc.logic.entry.IEntryStateMachine
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.Stack
import com.brocla.rpn_calc.testdoubles.FakeDataStore
import com.brocla.rpn_calc.testdoubles.FakeEntryStateMachine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Isolation tests for CalcStateRepository using fake dependencies.
 *
 * These tests verify the repository's own logic — that save() calls
 * completeEntry before persisting, that the committed (not raw) state is
 * what gets stored, and that clear() and calcState work correctly —
 * independently of the real DataStore and EntryStateMachine implementations.
 */
class CalcStateRepositoryIsolationTest {

    private val fakeEsm = FakeEntryStateMachine()
    private val fakeDataStore = FakeDataStore()
    private val repo = CalcStateRepository(fakeDataStore, fakeEsm)

    // -------------------------------------------------------------------------
    // save() — completeEntry contract
    // -------------------------------------------------------------------------

    @Test fun save_calls_completeEntry_once() = runTest {
        repo.save(CalculatorState())
        assertEquals(1, fakeEsm.completeEntryCallCount)
    }

    @Test fun save_persists_the_committed_state_not_the_raw_state() = runTest {
        // An ESM whose completeEntry stamps lastX = 99.0, making the committed
        // state detectably different from the raw input.
        val stampingEsm = object : IEntryStateMachine by FakeEntryStateMachine() {
            override fun completeEntry(state: CalculatorState) = state.copy(lastX = 99.0)
        }
        val repo2 = CalcStateRepository(fakeDataStore, stampingEsm)

        repo2.save(CalculatorState())           // raw state has lastX = 0.0
        val stored = repo2.calcState.first()
        assertEquals(99.0, stored?.lastX,
            "repository must persist the committed state, not the raw state passed to save()")
    }

    // -------------------------------------------------------------------------
    // clear() — does not commit entry
    // -------------------------------------------------------------------------

    @Test fun clear_does_not_call_completeEntry() = runTest {
        repo.clear()
        assertEquals(0, fakeEsm.completeEntryCallCount)
    }

    // -------------------------------------------------------------------------
    // calcState — round-trip persistence
    // -------------------------------------------------------------------------

    @Test fun calcState_is_null_before_any_save() = runTest {
        assertNull(repo.calcState.first(),
            "calcState must be null when nothing has been persisted")
    }

    @Test fun calcState_returns_state_that_was_saved() = runTest {
        val state = CalculatorState(stack = Stack(x = 7.0, y = 3.0, z = 0.0, t = 0.0))
        repo.save(state)
        val loaded = repo.calcState.first()
        assertEquals(7.0, loaded?.stack?.x, "X register must survive save/load round-trip")
        assertEquals(3.0, loaded?.stack?.y, "Y register must survive save/load round-trip")
    }
}
