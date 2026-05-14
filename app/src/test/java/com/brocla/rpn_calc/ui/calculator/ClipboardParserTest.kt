package com.brocla.rpn_calc.ui.calculator

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipboardParserTest {

    private val parser = ClipboardParserImpl()

    private fun assertSuccess(input: String, expected: Double) {
        val result = parser.parse(input)
        assertTrue(result is ClipboardParser.Result.Success, "Expected Success for input '$input' but got $result")
        assertEquals(expected, (result as ClipboardParser.Result.Success).value, 1e-10)
    }

    private fun assertInvalid(input: String) {
        val result = parser.parse(input)
        assertEquals(ClipboardParser.Result.Invalid, result, "Expected Invalid for input '$input' but got $result")
    }

    @Test fun parse_plainInteger()           = assertSuccess("12345",          12345.0)
    @Test fun parse_withCommas()             = assertSuccess("1,234,567",      1234567.0)
    @Test fun parse_negativeWithCommas()     = assertSuccess("-1,234.56",      -1234.56)
    @Test fun parse_scientificUpperE()       = assertSuccess("1.5E+04",        15000.0)
    @Test fun parse_scientificLowerE()       = assertSuccess("1.5e-3",         0.0015)
    @Test fun parse_displayString_allMode()  = assertSuccess("3.141592653",    3.141592653)
    @Test fun parse_displayString_fixMode()  = assertSuccess("3.14",           3.14)
    @Test fun parse_displayString_sciMode()  = assertSuccess("3.14e+02",       314.0)
    @Test fun parse_leadingTrailingSpaces()  = assertSuccess("  42  ",         42.0)
    @Test fun parse_embeddedCurrencySymbol() = assertSuccess("\$1,234.56",      1234.56)
    @Test fun parse_emptyString()            = assertInvalid("")
    @Test fun parse_lettersOnly()            = assertInvalid("abc")
    @Test fun parse_multipleDecimalPoints()  = assertInvalid("1.2.3")
    @Test fun parse_onlyCommas()             = assertInvalid(",,,,")
    @Test fun parse_onlySign()              = assertInvalid("-")
    @Test fun parse_validAfterHeavyStripping() = assertSuccess("USD 1,234.00 cr", 1234.0)
}
