package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.theme.CalcColors

// ---------------------------------------------------------------------------
// Key definitions — match the layout grid in PLAN_PHASE_B.md
// Columns are 1-indexed in comments to match the plan doc.
// ---------------------------------------------------------------------------

//
// Row 1  (cols 1–10)
// shifted: x²  LN  LOG  →P  →R  ALL  FIX  SCI  ENG  —
// primary: √x  eˣ  10ˣ  yˣ  1/x  CHS   7    8    9   ÷
//
private val keyRow1 = listOf(
    KeyDef("√x",  "x²",   CalcKeyEvent.Sqrt,       CalcKeyEvent.Square),
    KeyDef("eˣ",  "LN",   CalcKeyEvent.Exp,        CalcKeyEvent.Ln),
    KeyDef("10ˣ", "LOG",  CalcKeyEvent.Pow10,      CalcKeyEvent.Log),
    KeyDef("yˣ",  "→P",   CalcKeyEvent.Power,      CalcKeyEvent.ToPolar),
    KeyDef("1/x", "→R",   CalcKeyEvent.Reciprocal, CalcKeyEvent.ToRect),
    KeyDef("CHS", "ALL",  CalcKeyEvent.Chs,        CalcKeyEvent.AllMode),
    KeyDef("7",   "FIX",  CalcKeyEvent.Digit(7),   CalcKeyEvent.FixArg),
    KeyDef("8",   "SCI",  CalcKeyEvent.Digit(8),   CalcKeyEvent.SciArg),
    KeyDef("9",   "ENG",  CalcKeyEvent.Digit(9),   CalcKeyEvent.EngArg),
    KeyDef("÷",   "",     CalcKeyEvent.Divide,     keyColor = CalcColors.KeyArith),
)

//
// Row 2  (cols 1–10)
// shifted: D/R  nPr  SIN⁻¹  COS⁻¹  TAN⁻¹  —  —  —  —  —
// primary: n!  nCr   SIN    COS    TAN   EEX  4  5  6  ×
//
private val keyRow2 = listOf(
    KeyDef("n!",  "D/R",   CalcKeyEvent.Factorial,  CalcKeyEvent.DegRad),
    KeyDef("nCr", "nPr",   CalcKeyEvent.NCr,        CalcKeyEvent.NPr),
    KeyDef("SIN", "SIN⁻¹", CalcKeyEvent.Sin,        CalcKeyEvent.ArcSin),
    KeyDef("COS", "COS⁻¹", CalcKeyEvent.Cos,        CalcKeyEvent.ArcCos),
    KeyDef("TAN", "TAN⁻¹", CalcKeyEvent.Tan,        CalcKeyEvent.ArcTan),
    KeyDef("EEX", "",      CalcKeyEvent.Eex),
    KeyDef("4",   "",      CalcKeyEvent.Digit(4)),
    KeyDef("5",   "",      CalcKeyEvent.Digit(5)),
    KeyDef("6",   "",      CalcKeyEvent.Digit(6)),
    KeyDef("×",   "",      CalcKeyEvent.Multiply,   keyColor = CalcColors.KeyArith),
)

//
// Row 3 left  (cols 1–5)
// shifted: Δ%  —  LstX  —  —
// primary:  %  R↓  X↔Y  ←  CLX
//
private val keyRow3Left = listOf(
    KeyDef("%",   "Δ%",   CalcKeyEvent.Percent,   CalcKeyEvent.PercentChange),
    KeyDef("R↓",  "",     CalcKeyEvent.RollDown),
    KeyDef("x↔y", "LAST x", CalcKeyEvent.Swap,      CalcKeyEvent.LastX),
    KeyDef("←",   "",     CalcKeyEvent.Backspace),
    KeyDef("CLx", "",     CalcKeyEvent.Clx),
)

// ENTER — double-height, col 6; letters stacked vertically
private val enterKey = KeyDef(
    primaryLabel = "E\nN\nT\nE\nR",
    event = CalcKeyEvent.Enter,
    primaryLabelSize = 16.sp,
    primaryLineHeight = 16.sp,
)

//
// Row 3 right  (cols 7–10)
// primary: 1  2  3  −
//
private val keyRow3Right = listOf(
    KeyDef("1", "", CalcKeyEvent.Digit(1)),
    KeyDef("2", "", CalcKeyEvent.Digit(2)),
    KeyDef("3", "", CalcKeyEvent.Digit(3)),
    KeyDef("−", "", CalcKeyEvent.Subtract, keyColor = CalcColors.KeyArith),
)

//
// Row 4 left  (cols 1–5)
// primary: ON  SHIFT  —  STO  RCL
//
private val keyRow4Left = listOf(
    KeyDef("ON",    "", CalcKeyEvent.NoOp),
    KeyDef("SHIFT", "", CalcKeyEvent.Shift,
        keyColor   = CalcColors.KeyShift,
        labelColor = CalcColors.LabelShiftKey),
    KeyDef("",      "", CalcKeyEvent.NoOp),   // blank col 3
    KeyDef("STO",   "", CalcKeyEvent.Sto),
    KeyDef("RCL",   "", CalcKeyEvent.Rcl),
)

//
// Row 4 right  (cols 7–10)
// primary: 0  .  π  +
//
private val keyRow4Right = listOf(
    KeyDef("0", "", CalcKeyEvent.Digit(0)),
    KeyDef(".", "", CalcKeyEvent.Decimal),
    KeyDef("π", "", CalcKeyEvent.Pi),
    KeyDef("+", "", CalcKeyEvent.Add, keyColor = CalcColors.KeyArith),
)

// ---------------------------------------------------------------------------
// KeyGrid composable
// ---------------------------------------------------------------------------

@Composable
fun KeyGrid(
    shiftActive: Boolean,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        // Rows 1 and 2 — simple full-width rows
        KeyRow(keyRow1, shiftActive, onKey, Modifier.weight(1f))
        KeyRow(keyRow2, shiftActive, onKey, Modifier.weight(1f))

        // Rows 3+4 combined with double-height ENTER in col 6
        Row(modifier = Modifier.weight(2f).fillMaxWidth()) {
            // Left 5 columns
            Column(modifier = Modifier.weight(5f).fillMaxHeight()) {
                KeyRow(keyRow3Left, shiftActive, onKey, Modifier.weight(1f))
                KeyRow(keyRow4Left, shiftActive, onKey, Modifier.weight(1f))
            }
            // ENTER — spans full height of the bottom half
            CalcKey(
                def = enterKey,
                shiftActive = shiftActive,
                onKey = onKey,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            // Right 4 columns
            Column(modifier = Modifier.weight(4f).fillMaxHeight()) {
                KeyRow(keyRow3Right, shiftActive, onKey, Modifier.weight(1f))
                KeyRow(keyRow4Right, shiftActive, onKey, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<KeyDef>,
    shiftActive: Boolean,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        keys.forEach { def ->
            CalcKey(
                def = def,
                shiftActive = shiftActive,
                onKey = onKey,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}
