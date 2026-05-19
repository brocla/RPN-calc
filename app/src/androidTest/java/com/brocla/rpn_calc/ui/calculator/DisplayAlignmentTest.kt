package com.brocla.rpn_calc.ui.calculator

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brocla.rpn_calc.ui.calculator.components.DisplayPanel
import com.brocla.rpn_calc.ui.theme.CalcTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for DisplayPanel character positions.
 *
 * String index 0 is NOT the same as display position 0.
 * The formatter always puts the sign character at string index 0, but with
 * TextAlign.End the rendered text is right-aligned — so the sign character
 * appears at the RIGHT side of the display, not at display position 0 (left edge).
 *
 * Similarly, digits typed left-to-right appear on the RIGHT side of the display,
 * and a sign-slot space in an 11-character string is pushed off the left edge
 * (display position 0 is blank, causing the apparent "blank display" bug).
 *
 * All tests are RED until DisplayPanel is changed to TextAlign.Start.
 *
 * getBoundingBox(offset).left gives the x-coordinate of a character within
 * the text layout.  With TextAlign.Start the sign (index 0) is at x ≈ 0.
 * With TextAlign.End it is shifted far to the right.
 */
@RunWith(AndroidJUnit4::class)
class DisplayAlignmentTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun layout(displayStr: String): TextLayoutResult {
        composeTestRule.setContent {
            CalcTheme {
                DisplayPanel(uiState = CalculatorUiState(displayString = displayStr))
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule
            .onNodeWithTag("x_register", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.first()
    }

    // ── TextAlign assertion (direct check) ───────────────────────────────────

    /**
     * The x_register Text must use TextAlign.Start.
     * RED: current DisplayPanel uses TextAlign.End.
     */
    @Test fun xRegister_textAlign_isStart() {
        val textAlign = layout("-123.00").layoutInput.style.textAlign
        assertEquals("x_register must use TextAlign.Start", TextAlign.Start, textAlign)
    }

    // ── Sign at display position 0 ───────────────────────────────────────────
    // Display position 0 is the left edge of the display (x = 0).
    // getBoundingBox(0) gives the rect of the character at string index 0 (the sign).
    // With TextAlign.Start the sign is at x ≈ 0.
    // With TextAlign.End  the sign is at x >> 0 (pushed to the right).

    /**
     * FIX 2, input 1 2 3 CHS → display "-123" — sign '-' must be at display position 0.
     * RED: with TextAlign.End, '-' is rendered near the right edge of the display.
     */
    @Test fun signAtDisplayPosition0_negative() {
        val left = layout("-123").getBoundingBox(0).left
        assertTrue(
            "Sign '-' must be at display position 0 (x<2px); found x=$left. " +
            "DisplayPanel is using right-alignment instead of left-alignment.",
            left < 2f
        )
    }

    /**
     * Positive numbers have a space in the sign slot (position 0).
     * With TextAlign.Start the space is at x ≈ 0 (position 0).
     * With TextAlign.End  the space is pushed far to the right, so display
     * position 0 appears blank.
     */
    @Test fun signSlotAtDisplayPosition0_positive() {
        val left = layout(" 123").getBoundingBox(0).left
        assertTrue(
            "Sign slot ' ' must be at display position 0 (x<2px); found x=$left. " +
            "With TextAlign.End the sign slot is pushed right, leaving position 0 blank.",
            left < 2f
        )
    }

    /**
     * Exponent entry with negative mantissa — sign '-' must be at display position 0.
     * RED: with TextAlign.End '-' is not at display position 0.
     */
    @Test fun signAtDisplayPosition0_exponentEntry_negative() {
        val left = layout("-123 E99").getBoundingBox(0).left
        assertTrue(
            "Sign '-' must be at display position 0 (x<2px); found x=$left.",
            left < 2f
        )
    }

    // ── Left-to-right digit construction ─────────────────────────────────────
    // Input: 1 2 3 .  →  display string " 123."
    // String layout: index 0 = ' ' (sign), 1 = '1', 2 = '2', 3 = '3', 4 = '.'
    //
    // In display coordinates (TextAlign.Start):
    //   position 0 → sign slot   (x ≈ 0)
    //   position 1 → digit '1'   (x ≈ 1 char-width from left)
    //
    // With TextAlign.End all x-values are shifted to the right half of the display —
    // the sign slot is not at display position 0 and '1' is not near the left edge.

    /**
     * After typing 1 2 3 . the sign slot must be at display position 0 (x ≈ 0).
     * RED: with TextAlign.End, sign slot is near the right of the display.
     */
    @Test fun leftConstruction_signSlotAtDisplayPosition0() {
        val left = layout(" 123.").getBoundingBox(0).left
        assertTrue(
            "Sign slot must be at display position 0 (x<2px) so digits build left-to-right; " +
            "found x=$left.",
            left < 2f
        )
    }

    /**
     * Digit '1' (string index 1) must be in the LEFT half of the display,
     * confirming that numbers build left-to-right from position 1.
     * RED: with TextAlign.End, '1' is near the right edge.
     */
    @Test fun leftConstruction_firstDigitNearLeftEdge() {
        val result = layout(" 123.")
        val layoutWidth = result.size.width.toFloat()
        val digit1Left = result.getBoundingBox(1).left
        assertTrue(
            "Digit '1' (string index 1) must be in the left half of the display " +
            "(x=$digit1Left < width/2=${layoutWidth / 2f}). " +
            "With TextAlign.End it is near the right edge.",
            digit1Left < layoutWidth / 2f
        )
    }

    // ── 10-digit entry — sign slot must not be clipped off the left ──────────
    // The formatter produces " 1234567890" (11 chars: space + 10 digits).
    // With TextAlign.End the text is right-aligned. If the display is sized for
    // 10 characters, the leading space is pushed off the left edge (x < 0),
    // and display position 0 appears blank — the "blank display" bug.

    /**
     * After entering 10 integer digits the sign slot must be at display position 0.
     * RED: with TextAlign.End the 11-char string is right-aligned and the sign slot
     * is either off-screen (x < 0) or far from the left edge.
     */
    @Test fun tenDigits_signSlotAtDisplayPosition0() {
        val left = layout(" 1234567890").getBoundingBox(0).left
        assertTrue(
            "Sign slot must be at display position 0 (x<2px); found x=$left. " +
            "With TextAlign.End the 11-char string overflows and the sign slot " +
            "is pushed off the left edge of the display.",
            left < 2f
        )
    }

    /**
     * The sign slot of a 10-digit entry must not be clipped off-screen (x >= 0).
     * RED: with TextAlign.End and a right-aligned overflow, the sign slot has x < 0.
     */
    @Test fun tenDigits_signSlotNotClipped() {
        val left = layout(" 1234567890").getBoundingBox(0).left
        assertTrue(
            "Sign slot must not be clipped off the left edge (x>=0); found x=$left.",
            left >= 0f
        )
    }

    // ── Sign-slot space is one full digit-width (816 units in DSEG7) ─────────
    // The space at string index 0 must push the first digit to display position 1.
    // getBoundingBox(0).width gives the advance-width of the space character.

    /**
     * FIX 2 positive: " 3.14" — sign-slot ' ' at index 0 must be one digit-wide
     * so '3' (index 1) sits at display position 1, not position 0.
     */
    @Test fun positive_fix_signSlotIsOneDigitWide() {
        val result = layout(" 3.14")
        // index 0=' ', 1='3', 2='.', 3='1', 4='4'
        val slotWidth = result.getBoundingBox(0).width
        val firstDigitWidth = result.getBoundingBox(1).width
        assertTrue(
            "Sign-slot ' ' must have the same advance-width as a digit " +
            "(slotWidth=$slotWidth, digitWidth=$firstDigitWidth).",
            slotWidth >= firstDigitWidth * 0.9f
        )
    }

    /**
     * Entry mode positive: " 123" — sign-slot ' ' at index 0, first digit '1' at index 1.
     * The first digit must start at x ≈ one digit-width from the left edge.
     */
    @Test fun positive_entry_firstDigitAtPosition1() {
        val result = layout(" 123")
        val slotWidth = result.getBoundingBox(0).width
        val firstDigitLeft = result.getBoundingBox(1).left
        assertTrue(
            "First digit '1' (index 1) must start at x ≈ slotWidth=$slotWidth; " +
            "found firstDigitLeft=$firstDigitLeft.",
            firstDigitLeft >= slotWidth * 0.9f
        )
    }

    // ── SCI/ENG: padding spaces must be digit-wide so exponent sits at positions 10-11 ──
    // In " 1.23      04" indices 5-10 are spaces/expSign (6 chars).
    // With space = one digit-width, exponent '0' at index 11 should be
    // ~6 digit-widths to the right of the last mantissa digit.

    /**
     * " 1.23      04" — exponent '0' at string index 11 must be well separated
     * from the mantissa's last digit ('3' at index 4).
     */
    @Test fun sci_exponentSeparatedFromMantissa() {
        val result = layout(" 1.23      04")
        // index 0=' ', 1='1', 2='.', 3='2', 4='3', 5..10=spaces, 11='0', 12='4'
        val digitWidth = result.getBoundingBox(1).width   // width of '1'
        val mantissaRight = result.getBoundingBox(4).right  // right edge of '3'
        val expLeft = result.getBoundingBox(11).left        // left edge of exponent '0'
        val gap = expLeft - mantissaRight
        assertTrue(
            "Exponent start (x=$expLeft) must be ≥ 5 digit-widths right of mantissa end " +
            "(x=$mantissaRight); gap=$gap, threshold=${5 * digitWidth}.",
            gap >= 5 * digitWidth * 0.9f
        )
    }

}
