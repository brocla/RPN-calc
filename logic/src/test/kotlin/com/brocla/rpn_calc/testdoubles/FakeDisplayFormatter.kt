package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.logic.display.DisplayResult
import com.brocla.rpn_calc.logic.display.IDisplayFormatter
import com.brocla.rpn_calc.logic.model.CalculatorState

/**
 * Fake IDisplayFormatter for CalculatorEngine display-contract tests.
 *
 * Configure [result] to control what formatResult returns.
 * [lastState] records the state passed to the last formatResult call.
 */
class FakeDisplayFormatter : IDisplayFormatter {

    /** Controls what formatResult returns. Default: normal text display. */
    var result: DisplayResult = DisplayResult.Text(" 0.")

    /** The state passed to the last formatResult call, or null if never called. */
    var lastState: CalculatorState? = null

    override fun formatResult(state: CalculatorState): DisplayResult {
        lastState = state
        return result
    }
}
