package com.brocla.rpn_calc.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.EntryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val engine: CalculatorEngine,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onKey(event: CalcKeyEvent) {
        val current = _uiState.value
        val cs = current.calcState

        // Any key press clears the error — does NOT also execute the key; no animation
        if (cs.error != null) {
            val cleared = cs.copy(error = null, shiftActive = false)
            saveAndEmit(current.copy(
                calcState      = cleared,
                displayString  = buildDisplay(cleared),
                yDisplayString = buildYDisplay(cleared),
                animationType  = AnimationType.None,
            ))
            return
        }

        // A digit key while a two-step op is pending resolves the op; no animation
        if (current.pendingOp != PendingOp.None && event is CalcKeyEvent.Digit) {
            val d = event.d
            val newCs = when (current.pendingOp) {
                PendingOp.Sto    -> engine.pressSto(cs, d)
                PendingOp.Rcl    -> engine.pressRcl(cs, d)
                PendingOp.FixArg -> engine.pressFixMode(cs, d)
                PendingOp.SciArg -> engine.pressSciMode(cs, d)
                PendingOp.EngArg -> engine.pressEngMode(cs, d)
                PendingOp.None   -> cs
            }
            saveAndEmit(current.copy(
                calcState      = newCs.copy(shiftActive = false),
                pendingOp      = PendingOp.None,
                displayString  = buildDisplay(newCs),
                yDisplayString = buildYDisplay(newCs),
                animationType  = AnimationType.None,
            ))
            return
        }

        // Any non-digit key while pending cancels the op, then executes normally
        if (current.pendingOp != PendingOp.None) {
            val cancelled = current.copy(pendingOp = PendingOp.None)
            saveAndEmit(dispatch(cancelled, event))
            return
        }

        saveAndEmit(dispatch(current, event))
    }

    private fun dispatch(ui: CalculatorUiState, event: CalcKeyEvent): CalculatorUiState {
        val cs = ui.calcState
        var newPendingOp: PendingOp = PendingOp.None

        val newCs = when (event) {
            is CalcKeyEvent.Digit      -> engine.pressDigit(cs, event.d)
            CalcKeyEvent.Decimal       -> engine.pressDecimal(cs)
            CalcKeyEvent.Enter         -> engine.pressEnter(cs)
            CalcKeyEvent.Chs           -> engine.pressChs(cs)
            CalcKeyEvent.Eex           -> engine.pressEex(cs)
            CalcKeyEvent.Backspace     -> engine.pressBackspace(cs)
            CalcKeyEvent.Clx           -> engine.pressCLX(cs)
            CalcKeyEvent.RollDown      -> engine.pressRollDown(cs)
            CalcKeyEvent.Swap          -> engine.pressSwap(cs)
            CalcKeyEvent.Sto           -> { newPendingOp = PendingOp.Sto;    cs }
            CalcKeyEvent.Rcl           -> { newPendingOp = PendingOp.Rcl;    cs }
            CalcKeyEvent.Add           -> engine.pressAdd(cs)
            CalcKeyEvent.Subtract      -> engine.pressSubtract(cs)
            CalcKeyEvent.Multiply      -> engine.pressMultiply(cs)
            CalcKeyEvent.Divide        -> engine.pressDivide(cs)
            CalcKeyEvent.Reciprocal    -> engine.pressReciprocal(cs)
            CalcKeyEvent.Sqrt          -> engine.pressSqrt(cs)
            CalcKeyEvent.Square        -> engine.pressSquare(cs)
            CalcKeyEvent.Pow10         -> engine.pressPow10(cs)
            CalcKeyEvent.Log           -> engine.pressLog(cs)
            CalcKeyEvent.Exp           -> engine.pressExp(cs)
            CalcKeyEvent.Ln            -> engine.pressLn(cs)
            CalcKeyEvent.Power         -> engine.pressPower(cs)
            CalcKeyEvent.Pi            -> engine.pressPi(cs)
            CalcKeyEvent.Percent       -> engine.pressPercent(cs)
            CalcKeyEvent.PercentChange -> engine.pressPercentChange(cs)
            CalcKeyEvent.Sin           -> engine.pressSin(cs)
            CalcKeyEvent.Cos           -> engine.pressCos(cs)
            CalcKeyEvent.Tan           -> engine.pressTan(cs)
            CalcKeyEvent.ArcSin        -> engine.pressArcsin(cs)
            CalcKeyEvent.ArcCos        -> engine.pressArccos(cs)
            CalcKeyEvent.ArcTan        -> engine.pressArctan(cs)
            CalcKeyEvent.NCr           -> engine.pressCombinations(cs)
            CalcKeyEvent.NPr           -> engine.pressPermutations(cs)
            CalcKeyEvent.Factorial     -> engine.pressFactorial(cs)
            CalcKeyEvent.ToPolar       -> engine.pressToPolar(cs)
            CalcKeyEvent.ToRect        -> engine.pressToRectangular(cs)
            CalcKeyEvent.LastX         -> engine.pressLastX(cs)
            CalcKeyEvent.FixArg        -> { newPendingOp = PendingOp.FixArg; cs }
            CalcKeyEvent.SciArg        -> { newPendingOp = PendingOp.SciArg; cs }
            CalcKeyEvent.EngArg        -> { newPendingOp = PendingOp.EngArg; cs }
            CalcKeyEvent.AllMode       -> engine.pressAllMode(cs)
            CalcKeyEvent.DegRad        -> engine.pressDegRad(cs)
            CalcKeyEvent.Shift         -> engine.pressShift(cs)
            CalcKeyEvent.NoOp          -> cs
            CalcKeyEvent.OpenLayoutPicker -> cs
        }

        val finalCs = if (event is CalcKeyEvent.Shift) newCs else newCs.copy(shiftActive = false)

        val animationType = when (event) {
            CalcKeyEvent.Enter                                              -> AnimationType.Enter
            CalcKeyEvent.Add, CalcKeyEvent.Subtract,
            CalcKeyEvent.Multiply, CalcKeyEvent.Divide,
            CalcKeyEvent.Power, CalcKeyEvent.NCr, CalcKeyEvent.NPr,
            CalcKeyEvent.RollDown                                           -> AnimationType.BinaryOp
            CalcKeyEvent.Swap                                               -> AnimationType.Swap
            else                                                            -> AnimationType.None
        }

        return ui.copy(
            calcState      = finalCs,
            pendingOp      = newPendingOp,
            displayString  = buildDisplay(finalCs),
            yDisplayString = buildYDisplay(finalCs),
            animSeq        = if (animationType != AnimationType.None) ui.animSeq + 1 else ui.animSeq,
            animationType  = animationType,
        )
    }

    private fun buildDisplay(cs: CalculatorState): String =
        insertThousandsCommas(engine.getDisplay(cs))

    private fun buildYDisplay(cs: CalculatorState): String =
        insertThousandsCommas(engine.getDisplay(
            cs.copy(
                stack      = cs.stack.copy(x = cs.stack.y),
                entryState = EntryState.Idle,
                error      = null,
            )
        ))

    private fun loadState(): CalculatorUiState {
        val stored = savedStateHandle.get<String>(STATE_KEY)
        if (stored != null) {
            try {
                val calcState = json.decodeFromString<CalculatorState>(stored)
                return CalculatorUiState(
                    calcState      = calcState,
                    displayString  = buildDisplay(calcState),
                    yDisplayString = buildYDisplay(calcState),
                )
            } catch (_: Exception) { /* fall through to default */ }
        }
        val default = CalculatorState()
        return CalculatorUiState(
            displayString  = buildDisplay(default),
            yDisplayString = buildYDisplay(default),
        )
    }

    private fun saveAndEmit(newUi: CalculatorUiState) {
        savedStateHandle[STATE_KEY] = json.encodeToString(newUi.calcState)
        _uiState.value = newUi
    }

    companion object {
        private const val STATE_KEY = "calculator_state"
    }
}
