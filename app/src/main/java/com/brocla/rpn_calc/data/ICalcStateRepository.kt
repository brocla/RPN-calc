package com.brocla.rpn_calc.data

import com.brocla.rpn_calc.logic.model.CalculatorState
import kotlinx.coroutines.flow.Flow

interface ICalcStateRepository {
    val calcState: Flow<CalculatorState?>
    suspend fun save(state: CalculatorState)
    suspend fun clear()
}
