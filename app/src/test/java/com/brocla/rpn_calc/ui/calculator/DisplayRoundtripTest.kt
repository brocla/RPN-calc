package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.display.DisplayFormatter
import com.brocla.rpn_calc.logic.engine.CalculatorEngine
import com.brocla.rpn_calc.logic.entry.EntryStateMachine
import com.brocla.rpn_calc.logic.math.MathOperations
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.Stack
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD display correctness tests using the exact string that Copy puts on the
 * clipboard: insertThousandsCommas(engine.getDisplay(state)).
 *
 * Test 1 – width + value roundtrip across every display mode:
 *   (a) Width: count digit positions (excluding sign slot and dot/comma).
 *       For FIX/ALL strings: sign slot is position 0; positions 1–10 hold digits.
 *       For SCI/ENG strings (13 chars): always fits by design — skip width check.
 *   (b) Value: parse the display string back to a Double and assert it equals
 *       the original value (mathematical equality: 100 == 100.0 == 1.00e+02).
 *
 * Test 2 – exact Sci format for value 1 with dp = 0..9 (capped at 7 dp).
 */
class DisplayRoundtripTest {

    private val engine = CalculatorEngine(EntryStateMachine(), MathOperations(), DisplayFormatter())

    /** The string Copy puts on the clipboard for a given value and display mode. */
    private fun display(value: Double, mode: DisplayMode): String {
        val state = CalculatorState(
            stack           = Stack(x = value),
            displaySettings = DisplaySettings(mode),
        )
        return insertThousandsCommas(engine.getDisplay(state))
    }

    /**
     * Characters that count toward the physical display width, excluding sign slot.
     * For FIX/ALL strings: count digit/space chars in positions 1+ (no dot, no comma).
     * SCI/ENG strings are always 13 chars and always fit — return 0 to skip check.
     */
    private fun displayWidth(s: String): Int {
        if (s.length == 13) return 0  // SCI/ENG: always fits by design
        return s.drop(1).count { it != '.' && it != ',' }
    }

    /**
     * Parse a display string back to Double.
     * SCI/ENG (13-char) format: reconstruct from sign + mantissa + expSign + expDigits.
     * FIX/ALL format: strip commas and spaces then parse directly.
     */
    private fun parseDisplay(s: String): Double {
        val plain = s.replace(",", "")
        if (plain.length == 13) {
            // sign(1) + sigStr + padding + expSign(1) + expStr(2) = 13
            val negSign = plain[0] == '-'
            val mantissa = plain.substring(1, 10).trimEnd()
            val expIsNeg = plain[10] == '-'
            val expStr = plain.substring(11, 13).trim().ifEmpty { "0" }
            val sci = "${if (negSign) "-" else ""}${mantissa}e${if (expIsNeg) "-" else "+"}${expStr}"
            return sci.toDouble()
        }
        return plain.replace(" ", "").toDouble()
    }

    // All 31 display modes: Fix 0-9, Sci 0-9, Eng 0-9, All
    private val allModes: List<DisplayMode> =
        (0..9).map { DisplayMode.Fix(it) } +
        (0..9).map { DisplayMode.Sci(it) } +
        (0..9).map { DisplayMode.Eng(it) } +
        listOf(DisplayMode.All)

    // -------------------------------------------------------------------------
    // Test 1 – width + value roundtrip for the five seed values
    // -------------------------------------------------------------------------

    private fun assertDisplayCorrect(value: Double, label: String) {
        println("\n=== value=$label ===")
        println("  %-30s  %-26s  %5s  %-22s  %s".format(
            "mode", "display", "width", "parsed", "result"
        ))
        println("  " + "-".repeat(100))

        // Log all modes first so the full table is visible even when an assertion fails
        for (mode in allModes) {
            val d = display(value, mode)
            val width = displayWidth(d)
            val parsed = runCatching { parseDisplay(d) }.getOrElse { Double.NaN }
            val widthOk = width <= 10
            val valueOk = parsed == value
            val tag = when {
                !widthOk && !valueOk -> "FAIL (width+value)"
                !widthOk             -> "FAIL (width=$width)"
                !valueOk             -> "FAIL (value: parsed=$parsed)"
                else                 -> "PASS"
            }
            println("  %-30s  %-26s  %5d  %-22s  %s".format(mode, "\"$d\"", width, parsed, tag))
        }

        // Now assert
        for (mode in allModes) {
            val d = display(value, mode)

            val width = displayWidth(d)
            assertTrue(width <= 10,
                "WIDTH > 10: value=$label, mode=$mode, display=\"$d\", width=$width")

            val parsed = parseDisplay(d)
            assertEquals(value, parsed,
                "VALUE MISMATCH: value=$label, mode=$mode, display=\"$d\", parsed=$parsed")
        }
    }

    @Test fun display_100_allModes()         = assertDisplayCorrect(100.0,   "100")
    @Test fun display_100E45_allModes()      = assertDisplayCorrect(100e45,  "100E45")
    @Test fun display_100E_neg45_allModes()  = assertDisplayCorrect(100e-45, "100E-45")
    @Test fun display_neg100E45_allModes()   = assertDisplayCorrect(-100e45, "-100E45")
    @Test fun display_neg100E_neg45_allModes() = assertDisplayCorrect(-100e-45, "-100E-45")

    // -------------------------------------------------------------------------
    // Test 2 – Sci mode for value 1, dp = 0..9
    // -------------------------------------------------------------------------

    @Test fun sci_value1_sigDigitIs1_forAllDecimalPlaces() {
        println("\n=== Test 2a: Sci value=1, leading sig char must be \"1\" ===")
        for (dp in 0..9) {
            val d = display(1.0, DisplayMode.Sci(dp))
            val sigChar = d.filter { it !in setOf('.', ',', ' ') }.take(1)
            val pass = sigChar == "1"
            println("  ${if (pass) "PASS" else "FAIL"}  Sci($dp)  display=\"$d\"  leadingChar=\"$sigChar\"")
            assertEquals("1", sigChar, "Sci($dp): display=\"$d\"")
        }
    }

    // New positional format: sign(1) + sigStr(cappedDp+2) + padding(7-cappedDp) + expSign(1) + expStr(2) = 13
    // cappedDp = min(dp, 7). For value=1.0, exp=0, expSign=' ', expStr="00".
    private val sciFormatTable = listOf(
        0 to " 1.        00",   // cappedDp=0: sigStr="1." (2), padding=7, expSign=' ', expStr="00"
        1 to " 1.0       00",   // cappedDp=1: sigStr="1.0" (3), padding=6
        2 to " 1.00      00",   // cappedDp=2: sigStr="1.00" (4), padding=5
        3 to " 1.000     00",   // cappedDp=3: sigStr="1.000" (5), padding=4
        4 to " 1.0000    00",   // cappedDp=4: sigStr="1.0000" (6), padding=3
        5 to " 1.00000   00",   // cappedDp=5: sigStr="1.00000" (7), padding=2
        6 to " 1.000000  00",   // cappedDp=6: sigStr="1.000000" (8), padding=1
        7 to " 1.0000000 00",   // cappedDp=7: sigStr="1.0000000" (9), padding=0
        8 to " 1.0000000 00",   // dp=8 capped to 7
        9 to " 1.0000000 00",   // dp=9 capped to 7
    )

    @Test fun sci_value1_exactFormat_allDecimalPlaces() {
        println("\n=== Test 2b: Sci value=1, exact display format ===")
        println("  %-8s  %-20s  %-20s  %s".format("dp", "expected", "actual", "result"))
        println("  " + "-".repeat(65))

        for ((dp, expected) in sciFormatTable) {
            val actual = display(1.0, DisplayMode.Sci(dp))
            val pass = actual == expected
            println("  %-8s  %-20s  %-20s  %s".format(
                "Sci($dp)", "\"$expected\"", "\"$actual\"", if (pass) "PASS" else "FAIL"
            ))
        }
        println()

        for ((dp, expected) in sciFormatTable) {
            val actual = display(1.0, DisplayMode.Sci(dp))
            assertEquals(expected, actual, "Sci($dp)")
        }
    }
}
