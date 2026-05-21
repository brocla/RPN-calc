package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.data.ICalcStateRepository
import com.brocla.rpn_calc.logic.model.CalculatorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake ICalcStateRepository for ViewModel isolation tests.
 *
 * Provide [initial] to simulate a previously persisted state being loaded on startup.
 * [savedStates] records every state passed to save(), in order.
 * [clearCallCount] counts how many times clear() was called.
 */
class FakeCalcStateRepository(
    initial: CalculatorState? = null,
) : ICalcStateRepository {

    private val _flow = MutableStateFlow(initial)
    override val calcState: Flow<CalculatorState?> = _flow

    val savedStates = mutableListOf<CalculatorState>()
    var clearCallCount = 0

    override suspend fun save(state: CalculatorState) {
        savedStates += state
    }

    override suspend fun clear() {
        clearCallCount++
    }
}
