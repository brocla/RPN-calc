package com.brocla.rpn_calc.ui.calculator

/**
 * Describes which (if any) animation the display should play on a state update.
 *
 * None      — instant update; used for digit entry and unary ops
 * Enter     — value "rises": new content slides in from bottom, old exits to top
 * BinaryOp  — stack collapses: new content slides in from top, old exits to bottom
 *             (used for +, −, ×, ÷, yˣ, nCr, nPr, R↓)
 * Swap      — X↔Y: for the X display, old X exits upward and new X (old Y) enters from top;
 *             for the Y display, old Y exits downward and new Y (old X) enters from bottom
 */
enum class AnimationType { None, Enter, BinaryOp, Swap }
