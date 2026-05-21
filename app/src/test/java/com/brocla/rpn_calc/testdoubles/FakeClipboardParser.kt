package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.ui.calculator.ClipboardParser

/**
 * Fake ClipboardParser for CalculatorViewModel clipboard tests.
 *
 * Configure [result] to control what parse() returns.
 * [lastParsed] records the raw string passed to the last parse() call.
 */
class FakeClipboardParser(
    var result: ClipboardParser.Result = ClipboardParser.Result.Invalid,
) : ClipboardParser {

    /** The raw string passed to the last parse() call, or null if never called. */
    var lastParsed: String? = null

    override fun parse(raw: String): ClipboardParser.Result {
        lastParsed = raw
        return result
    }
}
