package com.brocla.rpn_calc.ui.layouts

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.calculator.components.KeyDef
import com.brocla.rpn_calc.ui.calculator.components.RadicalLabel
import com.brocla.rpn_calc.ui.theme.CalcColors

// ---------------------------------------------------------------------------
// Row 1  (cols 1–10)
// shifted: x²  LN  LOG  →P  →R  ALL  FIX  SCI  ENG  —
// primary: √x  eˣ  10ˣ  yˣ  1/x  CHS   7    8    9   ÷
// ---------------------------------------------------------------------------
private val row1 = KeyRow(listOf(
    KeySlot.Key(KeyDef(
        primaryLabel = "√x",
        shiftedLabel = "x²",
        event        = CalcKeyEvent.Sqrt,
        shiftedEvent = CalcKeyEvent.Square,
        customLabel  = { color, fontSize ->
            RadicalLabel(fontSize = fontSize, color = color, fontWeight = FontWeight.Medium)
        },
    )),
    KeySlot.Key(KeyDef("eˣ",  "LN",   CalcKeyEvent.Exp,        CalcKeyEvent.Ln)),
    KeySlot.Key(KeyDef("10ˣ", "LOG",  CalcKeyEvent.Pow10,      CalcKeyEvent.Log)),
    KeySlot.Key(KeyDef("yˣ",  "→P",   CalcKeyEvent.Power,      CalcKeyEvent.ToPolar)),
    KeySlot.Key(KeyDef("1/x", "→R",   CalcKeyEvent.Reciprocal, CalcKeyEvent.ToRect)),
    KeySlot.Key(KeyDef("CHS", "ALL",  CalcKeyEvent.Chs,        CalcKeyEvent.AllMode)),
    KeySlot.Key(KeyDef("7",   "FIX",  CalcKeyEvent.Digit(7),   CalcKeyEvent.FixArg)),
    KeySlot.Key(KeyDef("8",   "SCI",  CalcKeyEvent.Digit(8),   CalcKeyEvent.SciArg)),
    KeySlot.Key(KeyDef("9",   "ENG",  CalcKeyEvent.Digit(9),   CalcKeyEvent.EngArg)),
    KeySlot.Key(KeyDef("÷",   "",     CalcKeyEvent.Divide,     keyColor = CalcColors.KeyArith)),
))

// ---------------------------------------------------------------------------
// Row 2  (cols 1–10)
// shifted: D/R  nPr  SIN⁻¹  COS⁻¹  TAN⁻¹  —  —  —  —  —
// primary: n!  nCr   SIN    COS    TAN   EEX  4  5  6  ×
// ---------------------------------------------------------------------------
private val row2 = KeyRow(listOf(
    KeySlot.Key(KeyDef("n!",  "D/R",   CalcKeyEvent.Factorial,  CalcKeyEvent.DegRad)),
    KeySlot.Key(KeyDef("nCr", "nPr",   CalcKeyEvent.NCr,        CalcKeyEvent.NPr)),
    KeySlot.Key(KeyDef("SIN", "SIN⁻¹", CalcKeyEvent.Sin,        CalcKeyEvent.ArcSin)),
    KeySlot.Key(KeyDef("COS", "COS⁻¹", CalcKeyEvent.Cos,        CalcKeyEvent.ArcCos)),
    KeySlot.Key(KeyDef("TAN", "TAN⁻¹", CalcKeyEvent.Tan,        CalcKeyEvent.ArcTan)),
    KeySlot.Key(KeyDef("EEX", "",      CalcKeyEvent.Eex)),
    KeySlot.Key(KeyDef("4",   "",      CalcKeyEvent.Digit(4))),
    KeySlot.Key(KeyDef("5",   "",      CalcKeyEvent.Digit(5))),
    KeySlot.Key(KeyDef("6",   "",      CalcKeyEvent.Digit(6))),
    KeySlot.Key(KeyDef("×",   "",      CalcKeyEvent.Multiply,   keyColor = CalcColors.KeyArith)),
))

// ---------------------------------------------------------------------------
// Row 3  (cols 1–10)
// shifted: Δ%  —  LstX  —  —  —  —  —  —  —
// primary:  %  R↓  x↔y  ←  CLx  ENTER  1  2  3  −
// ---------------------------------------------------------------------------
private val row3 = KeyRow(listOf(
    KeySlot.Key(KeyDef("%",    "Δ%",     CalcKeyEvent.Percent,   CalcKeyEvent.PercentChange)),
    KeySlot.Key(KeyDef("R↓",  "",       CalcKeyEvent.RollDown)),
    KeySlot.Key(KeyDef("x↔y", "LAST x", CalcKeyEvent.Swap,      CalcKeyEvent.LastX)),
    KeySlot.Key(KeyDef("←",   "",       CalcKeyEvent.Backspace)),
    KeySlot.Key(KeyDef("CLx", "",       CalcKeyEvent.Clx)),
    KeySlot.Key(KeyDef("ENTER", "",     CalcKeyEvent.Enter,     primaryLabelSize = 18.sp)),
    KeySlot.Key(KeyDef("1",   "",       CalcKeyEvent.Digit(1))),
    KeySlot.Key(KeyDef("2",   "",       CalcKeyEvent.Digit(2))),
    KeySlot.Key(KeyDef("3",   "",       CalcKeyEvent.Digit(3))),
    KeySlot.Key(KeyDef("−",   "",       CalcKeyEvent.Subtract,  keyColor = CalcColors.KeyArith)),
))

// ---------------------------------------------------------------------------
// Row 4  (cols 1–10)
// primary: LAYOUT  SHIFT  —  STO  RCL  [spacer]  0  .  π  +
// ---------------------------------------------------------------------------
private val row4 = KeyRow(listOf(
    KeySlot.Key(KeyDef(
        primaryLabel = "LAYOUT",
        event        = CalcKeyEvent.OpenLayoutPicker,
        primaryLabelSize = 14.sp,
    )),
    KeySlot.Key(KeyDef(
        primaryLabel = "SHIFT",
        event        = CalcKeyEvent.Shift,
        keyColor     = CalcColors.KeyShift,
        labelColor   = CalcColors.LabelShiftKey,
    )),
    KeySlot.Spacer(1f),   // blank column 3
    KeySlot.Key(KeyDef("STO", "", CalcKeyEvent.Sto)),
    KeySlot.Key(KeyDef("RCL", "", CalcKeyEvent.Rcl)),
    KeySlot.Spacer(1f),   // blank column 6 (below ENTER)
    KeySlot.Key(KeyDef("0",   "", CalcKeyEvent.Digit(0))),
    KeySlot.Key(KeyDef(".",   "", CalcKeyEvent.Decimal)),
    KeySlot.Key(KeyDef("π",   "", CalcKeyEvent.Pi)),
    KeySlot.Key(KeyDef("+",   "", CalcKeyEvent.Add,      keyColor = CalcColors.KeyArith)),
))

val ClassicLandscapeLayout = LayoutDescriptor(
    name        = "Classic Landscape",
    orientation = LayoutOrientation.Landscape,
    rows        = listOf(row1, row2, row3, row4),
)
