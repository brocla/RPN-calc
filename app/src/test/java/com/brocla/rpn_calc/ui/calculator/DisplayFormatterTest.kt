package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.EntryState
import org.junit.Test
import kotlin.test.assertEquals

class DisplayFormatterTest {

    // Tests operate on raw strings; use FIX mode and Idle state as the neutral baseline.
    // SCI/ENG and exponent-entry exclusion is tested via the mode/entryState parameters below.
    private fun fmt(s: String, mode: DisplayMode = DisplayMode.Fix(2), entryState: EntryState = EntryState.Idle) =
        insertThousandsCommas(s, mode, entryState)

    // --- Values below 1000: no comma inserted ---
    // All inputs now start with a sign-slot character (' ' or '-').

    @Test fun noComma_twoDigits()       = assertEquals(" 42",      fmt(" 42"))
    @Test fun noComma_threeDigits()     = assertEquals(" 999",     fmt(" 999"))
    @Test fun noComma_withDecimal()     = assertEquals(" 42.5",    fmt(" 42.5"))
    @Test fun noComma_zero()            = assertEquals(" 0",       fmt(" 0"))
    @Test fun noComma_zeroDecimal()     = assertEquals(" 0.0",     fmt(" 0.0"))
    @Test fun noComma_decimalOnly()     = assertEquals(" 0.001",   fmt(" 0.001"))

    // --- Integer parts with 4+ digits get commas ---

    @Test fun comma_fourDigits()        = assertEquals(" 1,000",         fmt(" 1000"))
    @Test fun comma_sevenDigits()       = assertEquals(" 1,234,567",     fmt(" 1234567"))
    @Test fun comma_tenDigits()         = assertEquals(" 1,234,567,890", fmt(" 1234567890"))
    @Test fun comma_exactlyOneGroup()   = assertEquals(" 1,000,000",     fmt(" 1000000"))

    // --- With decimal portion ---

    @Test fun comma_withDecimal()       = assertEquals(" 1,234,567.89", fmt(" 1234567.89"))
    @Test fun comma_fourDigitsDecimal() = assertEquals(" 1,234.5678",   fmt(" 1234.5678"))

    // --- Negative numbers ---

    @Test fun comma_negative()          = assertEquals("-1,234.5",   fmt("-1234.5"))
    @Test fun noComma_negativeSmall()   = assertEquals("-42.0",      fmt("-42.0"))
    @Test fun comma_negativeLarge()     = assertEquals("-1,000,000", fmt("-1000000"))

    // --- Exponent-entry strings must NOT be grouped ---

    @Test fun noComma_exponentEntry_negExp_fullMantissa() =
        assertEquals("-12345678-88", fmt("-12345678-88", entryState = EntryState.Exponent("12345678", "", false, true, "88", false)))

    @Test fun noComma_exponentEntry_negExp_positiveMantissa() =
        assertEquals(" 12345678-88", fmt(" 12345678-88", entryState = EntryState.Exponent("12345678", "", false, false, "88", false)))

    // --- SCI / ENG strings must NOT be grouped ---

    @Test fun noComma_sciPositive()     = assertEquals(" 1.234     07",  fmt(" 1.234     07",  mode = DisplayMode.Sci(3)))
    @Test fun noComma_sciNegative()     = assertEquals("-3.50     -12",  fmt("-3.50     -12",  mode = DisplayMode.Sci(2)))
    @Test fun noComma_sciLargeDp()      = assertEquals(" 1.0000000 00",  fmt(" 1.0000000 00",  mode = DisplayMode.Sci(7)))
    @Test fun noComma_engFormat()       = assertEquals(" 12.35     03",  fmt(" 12.35     03",  mode = DisplayMode.Eng(2)))
}
