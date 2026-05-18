package com.brocla.rpn_calc.ui.calculator

/**
 * Inserts zero-width comma separators (U+002C) into the integer part of a plain
 * numeric string produced by :logic's DisplayFormatter.  The comma glyph in the
 * modified DSEG7 font has advance-width 0, so it does not shift digit positions.
 *
 * Rules:
 *  - Error strings (no sign-slot prefix) are returned unchanged.
 *  - SCI / ENG / exponent-entry strings (which contain internal spaces after position 0)
 *    are returned unchanged.
 *  - Position 0 is always the sign-slot character (' ' or '-'); it is preserved
 *    unchanged and is never included in the digit-grouping logic.
 *  - Only the integer portion (left of '.') is grouped.
 *  - Integer parts with ≤ 3 digits need no comma.
 */
fun insertThousandsCommas(plain: String): String {
    if (plain.isEmpty()) return plain
    if (plain[0] !in " -") return plain          // error strings have no sign slot
    if (plain.drop(1).contains(' ')) return plain // SCI/ENG/exponent-entry have internal spaces
    if (plain.drop(1).contains('-')) return plain // exponent-entry with negative exponent, no spaces

    val signChar = plain[0]           // always ' ' or '-'
    val rest = plain.substring(1)

    val dotIndex = rest.indexOf('.')
    val intPart  = if (dotIndex >= 0) rest.substring(0, dotIndex) else rest
    val decPart  = if (dotIndex >= 0) rest.substring(dotIndex)    else ""

    if (intPart.length <= 3) return plain

    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return signChar + grouped + decPart
}
