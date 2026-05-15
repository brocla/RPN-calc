package com.brocla.rpn_calc.ui.calculator

sealed interface CalcKeyEvent {
    data class Digit(val d: Int) : CalcKeyEvent
    data object Decimal        : CalcKeyEvent
    data object Enter          : CalcKeyEvent
    data object Chs            : CalcKeyEvent
    data object Eex            : CalcKeyEvent
    data object Backspace      : CalcKeyEvent
    data object Clx            : CalcKeyEvent
    data object RollDown       : CalcKeyEvent
    data object Swap           : CalcKeyEvent
    data object Sto            : CalcKeyEvent   // arms PendingOp.Sto
    data object Rcl            : CalcKeyEvent   // arms PendingOp.Rcl
    data object Add            : CalcKeyEvent
    data object Subtract       : CalcKeyEvent
    data object Multiply       : CalcKeyEvent
    data object Divide         : CalcKeyEvent
    data object Reciprocal     : CalcKeyEvent
    data object Sqrt           : CalcKeyEvent
    data object Square         : CalcKeyEvent   // shifted
    data object Pow10          : CalcKeyEvent
    data object Log            : CalcKeyEvent   // shifted
    data object Exp            : CalcKeyEvent
    data object Ln             : CalcKeyEvent   // shifted
    data object Power          : CalcKeyEvent
    data object Pi             : CalcKeyEvent
    data object Percent        : CalcKeyEvent
    data object PercentChange  : CalcKeyEvent   // shifted
    data object Sin            : CalcKeyEvent
    data object Cos            : CalcKeyEvent
    data object Tan            : CalcKeyEvent
    data object ArcSin         : CalcKeyEvent   // shifted
    data object ArcCos         : CalcKeyEvent   // shifted
    data object ArcTan         : CalcKeyEvent   // shifted
    data object NCr            : CalcKeyEvent
    data object NPr            : CalcKeyEvent   // shifted
    data object Factorial      : CalcKeyEvent
    data object ToPolar        : CalcKeyEvent   // shifted
    data object ToRect         : CalcKeyEvent   // shifted
    data object LastX          : CalcKeyEvent   // shifted
    data object FixArg         : CalcKeyEvent   // arms PendingOp.FixArg
    data object SciArg         : CalcKeyEvent   // arms PendingOp.SciArg
    data object EngArg         : CalcKeyEvent   // arms PendingOp.EngArg
    data object AllMode        : CalcKeyEvent   // shifted, immediate
    data object DegRad         : CalcKeyEvent   // shifted
    data object Shift          : CalcKeyEvent
    data object NoOp           : CalcKeyEvent   // blank keys
    data object OpenLayoutPicker : CalcKeyEvent // LAYOUT key — intercepted by CalculatorRoute
    data object ResetRequest   : CalcKeyEvent   // long-press backspace — intercepted by CalculatorRoute
    data object OpenConstants  : CalcKeyEvent   // CONST key — intercepted by CalculatorRoute
    data class PasteValue(val value: Double) : CalcKeyEvent
    data class PushConstant(val value: Double) : CalcKeyEvent  // dispatched after user selects a constant
}
