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
 *   (a) Width: count every character except '.' and ',' — must be ≤ 10.
 *       The physical display is 10 characters wide; the decimal point sits
 *       between digit positions and does not consume one.
 *   (b) Value: parse the display string back to a Double and assert it equals
 *       the original value (mathematical equality: 100 == 100.0 == 1.23e+02).
 *
 * Test 2 – exact Sci format for value 1 with dp = 0..9 (capped at 5 dp).
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

    /** Characters that count toward the 10-char display width. */
    private fun displayWidth(s: String): Int = s.count { it != '.' && it != ',' }

    /** Parse a display string back to Double (strips commas and spaces; handles e/E). */
    private fun parseDisplay(s: String): Double =
        s.replace(",", "").replace(" ", "").toDouble()

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

    private val sciFormatTable = listOf(
        0 to "1e+00",
        1 to "1.0e+00",
        2 to "1.00e+00",
        3 to "1.000e+00",
        4 to "1.0000e+00",
        5 to "1.00000e+00",
        6 to "1.00000e+00",
        7 to "1.00000e+00",
        8 to "1.00000e+00",
        9 to "1.00000e+00",
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
