package com.brocla.rpn_calc.ui.calculator

sealed interface PendingOp {
    data object None   : PendingOp
    data object Sto    : PendingOp
    data object Rcl    : PendingOp
    data object FixArg : PendingOp
    data object SciArg : PendingOp
    data object EngArg : PendingOp
}
