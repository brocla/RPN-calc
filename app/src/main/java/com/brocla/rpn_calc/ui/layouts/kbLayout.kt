package com.brocla.rpn_calc.ui.layouts

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import com.brocla.rpn_calc.ui.calculator.components.KeyDef
import com.brocla.rpn_calc.ui.theme.CalcColors
import com.brocla.rpn_calc.ui.calculator.components.RadicalLabel
import androidx.compose.ui.text.font.FontWeight

// ---------------------------------------------------------------------------
// Portrait layout — built by hand. kb

// ---------------------------------------------------------------------------

// Row 1: mathy stuff
private val portRow1 = KeyRow(listOf(

    KeySlot.Key(KeyDef("1/x", "yˣ",   CalcKeyEvent.Reciprocal, CalcKeyEvent.Power)),
    KeySlot.Key(KeyDef(
        primaryLabel = "√x",
        shiftedLabel = "x²",
        event        = CalcKeyEvent.Sqrt,
        shiftedEvent = CalcKeyEvent.Square,
        customLabel  = { color, fontSize ->
            RadicalLabel(fontSize = fontSize, color = color, fontWeight = FontWeight.Medium)
        },
    )),
    KeySlot.Key(KeyDef("LOG", "10ˣ",  CalcKeyEvent.Log, CalcKeyEvent.Pow10)),
    KeySlot.Key(KeyDef("LN", "eˣ",    CalcKeyEvent.Ln,  CalcKeyEvent.Exp)),
    KeySlot.Key(KeyDef("%",    "Δ%",     CalcKeyEvent.Percent,   CalcKeyEvent.PercentChange)),
), 
primaryTopPadding = 24.dp   // rows with shifted labels
)

// Row 2: fact, comb/perm, deg/rad, trig funcs
private val portRow2 = KeyRow(listOf(
    KeySlot.Key(KeyDef("n!",  "nPr",   CalcKeyEvent.Factorial,  CalcKeyEvent.NPr)),
    KeySlot.Key(KeyDef("D/R",  "nCr",   CalcKeyEvent.DegRad,  CalcKeyEvent.NCr)),
    KeySlot.Key(KeyDef("SIN", "SIN⁻¹", CalcKeyEvent.Sin,        CalcKeyEvent.ArcSin)),
    KeySlot.Key(KeyDef("COS", "COS⁻¹", CalcKeyEvent.Cos,        CalcKeyEvent.ArcCos)),
    KeySlot.Key(KeyDef("TAN", "TAN⁻¹", CalcKeyEvent.Tan,        CalcKeyEvent.ArcTan)),
), 
primaryTopPadding = 24.dp,   // rows with shifted labels
)

// Row 3: shift, exchange roll down, sto, rcl
private val portRow3 = KeyRow(listOf(

    KeySlot.Key(KeyDef(
        primaryLabel = "",
        event        = CalcKeyEvent.Shift,
        keyColor     = CalcColors.KeyShift,
        labelColor   = CalcColors.LabelShiftKey,
    )),
    KeySlot.Key(KeyDef("x↔y", "ALL", CalcKeyEvent.Swap, CalcKeyEvent.AllMode)),
    KeySlot.Key(KeyDef("R↓",  "FIX",    CalcKeyEvent.RollDown, CalcKeyEvent.FixArg)),
    KeySlot.Key(KeyDef("STO", "SCI", CalcKeyEvent.Sto, CalcKeyEvent.SciArg)),
    KeySlot.Key(KeyDef("RCL", "ENG", CalcKeyEvent.Rcl, CalcKeyEvent.EngArg)),
), 
primaryTopPadding = 24.dp,   // rows with shifted labels
)

// Row 4: enter, chs, eex backspace, clx
private val portRow4 = KeyRow(listOf(
    KeySlot.Key(KeyDef("ENTER", "", CalcKeyEvent.Enter,  keyColor = CalcColors.KeyArith), weight = 2f),  
    KeySlot.Key(KeyDef("CHS",   "", CalcKeyEvent.Chs)),
    KeySlot.Key(KeyDef("EEX",   "", CalcKeyEvent.Eex,         CalcKeyEvent.OpenLayoutPicker)),
    KeySlot.Key(KeyDef("⌫",    "", CalcKeyEvent.Backspace,   CalcKeyEvent.Clx)),  // ←
))

// Row 5: - 7 8 9 
private val portRow5 = KeyRow(listOf(
    KeySlot.Key(KeyDef("−",  "", CalcKeyEvent.Subtract, keyColor = CalcColors.KeyArith)),
    KeySlot.Key(KeyDef("7",  "", CalcKeyEvent.Digit(7))),
    KeySlot.Key(KeyDef("8",  "", CalcKeyEvent.Digit(8))),
    KeySlot.Key(KeyDef("9",  "", CalcKeyEvent.Digit(9))),
))

// Row 6: + 3 4 5 
private val portRow6 = KeyRow(listOf(
    KeySlot.Key(KeyDef("+",  "", CalcKeyEvent.Add,      keyColor = CalcColors.KeyArith)),
    KeySlot.Key(KeyDef("4",  "", CalcKeyEvent.Digit(4))),
    KeySlot.Key(KeyDef("5",  "", CalcKeyEvent.Digit(5))),
    KeySlot.Key(KeyDef("6",  "", CalcKeyEvent.Digit(6))),
))

// Row 7: x 1 2 3
private val portRow7 = KeyRow(listOf(
    KeySlot.Key(KeyDef("×",  "",    CalcKeyEvent.Multiply, keyColor = CalcColors.KeyArith)),
    KeySlot.Key(KeyDef("1",  "", CalcKeyEvent.Digit(1))),
    KeySlot.Key(KeyDef("2",  "", CalcKeyEvent.Digit(2))),
    KeySlot.Key(KeyDef("3",  "", CalcKeyEvent.Digit(3))),
))

// Row 8: div 0 . pi
private val portRow8 = KeyRow(listOf(
    KeySlot.Key(KeyDef("÷",   "",       CalcKeyEvent.Divide, keyColor = CalcColors.KeyArith)),
    KeySlot.Key(KeyDef("0",    "",      CalcKeyEvent.Digit(0))),
    KeySlot.Key(KeyDef(".",    "",      CalcKeyEvent.Decimal)),
    KeySlot.Key(KeyDef("π",   "",       CalcKeyEvent.Pi)),

))

val PortraitLayout = LayoutDescriptor(
    name        = "kbPortrait",
    orientation = LayoutOrientation.Portrait,
    rows        = listOf(portRow1, portRow2, portRow3, portRow4, portRow5, portRow6, portRow7, portRow8),
)
