package com.brocla.rpn_calc.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brocla.rpn_calc.data.CalcStateRepository
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.EntryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val engine: CalculatorEngine,
    private val repository: CalcStateRepository,
    private val clipboardParser: ClipboardParser,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow(defaultUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.calcState.first()
            if (saved != null) {
                _uiState.value = CalculatorUiState(
                    calcState      = saved,
                    displayString  = buildDisplay(saved),
                    yDisplayString = buildYDisplay(saved),
                )
            }
            _isLoading.value = false
        }
    }

    fun onKey(event: CalcKeyEvent) {
        val current = _uiState.value
        val cs = current.calcState

        // Any key press clears the error — does NOT also execute the key; no animation.
        // Restore the pre-error state that was saved when the error was introduced.
        if (cs.error != null) {
            val restored = (current.savedState ?: cs).copy(error = null, shiftActive = false)
            saveAndEmit(current.copy(
                calcState      = restored,
                savedState     = null,
                displayString  = buildDisplay(restored),
                yDisplayString = buildYDisplay(restored),
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
            val resolvedCs = finalizeState(newCs.copy(shiftActive = false))
            saveAndEmit(current.copy(
                calcState      = resolvedCs,
                pendingOp      = PendingOp.None,
                displayString  = buildDisplay(resolvedCs),
                yDisplayString = buildYDisplay(resolvedCs),
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

    fun reset() {
        val default = CalculatorState()
        _uiState.value = CalculatorUiState(
            calcState      = default,
            displayString  = buildDisplay(default),
            yDisplayString = buildYDisplay(default),
        )
        viewModelScope.launch { repository.clear() }
    }

    fun pasteFromClipboard(raw: String) {
        when (val result = clipboardParser.parse(raw)) {
            is ClipboardParser.Result.Success ->
                onKey(CalcKeyEvent.PasteValue(result.value))
            ClipboardParser.Result.Invalid ->
                saveAndEmit(_uiState.value.copy(
                    calcState = _uiState.value.calcState.copy(error = "Paste: invalid input"),
                ))
        }
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
            CalcKeyEvent.ResetRequest  -> cs
            CalcKeyEvent.OpenConstants -> cs  // intercepted by CalculatorRoute before reaching here
            is CalcKeyEvent.PasteValue -> {
                val entered = engine.pressEnter(cs)
                entered.copy(
                    stack = entered.stack.copy(x = event.value),
                    stackLiftEnabled = true,
                )
            }
            is CalcKeyEvent.PushConstant -> {
                val committed = engine.pressEnter(cs)
                committed.copy(
                    stack = committed.stack.copy(
                        x = event.value,
                        y = committed.stack.x,
                        z = committed.stack.y,
                        t = committed.stack.z,
                    ),
                    entryState = EntryState.Idle,
                )
            }
        }

        val finalCs = finalizeState(
            if (event is CalcKeyEvent.Shift) newCs else newCs.copy(shiftActive = false)
        )

        val animationType = when (event) {
            CalcKeyEvent.Enter                                              -> AnimationType.Enter
            CalcKeyEvent.Add, CalcKeyEvent.Subtract,
            CalcKeyEvent.Multiply, CalcKeyEvent.Divide,
            CalcKeyEvent.Power, CalcKeyEvent.NCr, CalcKeyEvent.NPr,
            CalcKeyEvent.RollDown                                           -> AnimationType.BinaryOp
            CalcKeyEvent.Swap                                               -> AnimationType.Swap
            else                                                            -> AnimationType.None
        }

        // If this key introduced a new error, save the state from before the key press
        // so the error-clear path can fully restore it.
        val newSavedState = if (finalCs.error != null && ui.calcState.error == null) ui.calcState else null

        return ui.copy(
            calcState      = finalCs,
            pendingOp      = newPendingOp,
            savedState     = newSavedState,
            displayString  = buildDisplay(finalCs),
            yDisplayString = buildYDisplay(finalCs),
            animSeq        = if (animationType != AnimationType.None) ui.animSeq + 1 else ui.animSeq,
            animationType  = animationType,
        )
    }

    private fun buildDisplay(cs: CalculatorState): String =
        insertThousandsCommas(engine.getDisplay(cs))

    /**
     * If the display string is a range error ("Overflow"/"Underflow"), promote it
     * into state.error so the VM's key-tap error-clearing logic handles it.
     */
    private fun finalizeState(cs: CalculatorState): CalculatorState {
        if (cs.error != null) return cs
        val display = engine.getDisplay(cs)
        return if (display == "Overflow" || display == "Underflow") cs.copy(error = display)
        else cs
    }

    private fun buildYDisplay(cs: CalculatorState): String =
        insertThousandsCommas(engine.getDisplay(
            cs.copy(
                stack      = cs.stack.copy(x = cs.stack.y),
                entryState = EntryState.Idle,
                error      = null,
            )
        ))

    private fun defaultUiState(): CalculatorUiState {
        val default = CalculatorState()
        return CalculatorUiState(
            displayString  = buildDisplay(default),
            yDisplayString = buildYDisplay(default),
        )
    }

    private fun saveAndEmit(newUi: CalculatorUiState) {
        viewModelScope.launch { repository.save(newUi.calcState) }
        _uiState.value = newUi
    }
}
