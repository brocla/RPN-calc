package com.brocla.rpn_calc.ui.calculator

/**
 * Inserts zero-width comma separators (U+002C) into the integer part of a plain
 * numeric string produced by :logic's DisplayFormatter.  The comma glyph in the
 * modified DSEG7 font has advance-width 0, so it does not shift digit positions.
 *
 * Rules:
 *  - Strings containing 'e'/'E' (SCI / ENG format) are returned unchanged.
 *  - Only the integer portion (left of '.') is grouped.
 *  - Negative sign is preserved before the first group.
 *  - Integer parts with ≤ 3 digits need no comma.
 */
fun insertThousandsCommas(plain: String): String {
    if (plain.contains('e', ignoreCase = true)) return plain

    val dotIndex = plain.indexOf('.')
    val intPart  = if (dotIndex >= 0) plain.substring(0, dotIndex) else plain
    val decPart  = if (dotIndex >= 0) plain.substring(dotIndex)    else ""

    val negative = intPart.startsWith('-')
    val digits   = if (negative) intPart.drop(1) else intPart

    if (digits.length <= 3) return plain

    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return (if (negative) "-" else "") + grouped + decPart
}
