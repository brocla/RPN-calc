package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.EntryState

/**
 * Inserts zero-width comma separators (U+002C) into the integer part of a plain
 * numeric string produced by :logic's DisplayFormatter.  The comma glyph in the
 * modified DSEG7 font has advance-width 0, so it does not shift digit positions.
 *
 * Commas are only applicable in FIX and ALL modes — SCI and ENG use a positional
 * exponent format where the integer part is never wide enough to need grouping, and
 * exponent entry always uses the positional format regardless of mode.
 *
 * Rules:
 *  - Error strings (no sign-slot prefix) are returned unchanged.
 *  - SCI and ENG modes are returned unchanged.
 *  - Exponent entry state is returned unchanged.
 *  - Position 0 is always the sign-slot character (' ' or '-'); it is preserved
 *    unchanged and is never included in the digit-grouping logic.
 *  - Only the integer portion (left of '.') is grouped.
 *  - Integer parts with ≤ 3 digits need no comma.
 */
fun insertThousandsCommas(plain: String, mode: DisplayMode, entryState: EntryState): String {
    if (plain.isEmpty()) return plain
    if (plain[0] !in " -") return plain                      // error string, no sign slot
    if (mode !is DisplayMode.Fix && mode !is DisplayMode.All) return plain  // SCI/ENG never get commas
    if (entryState is EntryState.Exponent) return plain      // positional exponent entry, no commas

    val signChar = plain[0]
    val rest = plain.substring(1)

    val dotIndex = rest.indexOf('.')
    val intPart  = if (dotIndex >= 0) rest.substring(0, dotIndex) else rest
    val decPart  = if (dotIndex >= 0) rest.substring(dotIndex)    else ""

    if (intPart.length <= 3) return plain

    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return signChar + grouped + decPart
}
