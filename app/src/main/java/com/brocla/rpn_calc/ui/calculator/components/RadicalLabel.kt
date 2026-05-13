package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.ui.theme.Helvetica
import com.brocla.rpn_calc.ui.theme.TimesRomanItalic

/**
 * Draws √x with a proper radical: checkmark stroke + vinculum over x + down-tick at end.
 *
 * Layout (all proportions relative to fontSize):
 *   - "x" is drawn in Times Italic at [fontSize]
 *   - The radical stroke rises from bottom-left, peaks above the vinculum, then descends
 *     to meet the vinculum at the left edge of "x"
 *   - The vinculum (horizontal bar) runs across the top of "x"
 *   - A short downward tick marks the right end of the vinculum
 */
@Composable
fun RadicalLabel(
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val fsp = fontSize.value          // font size in sp (float)
    val fpx = with(density) { fsp.sp.toPx() }  // font size in px

    // Geometry constants (all as fractions of fpx)
    val strokeWidth = fpx * 0.07f
    val tickDown    = fpx * 0.18f    // length of the down-tick at vinculum end
    val vinculumY   = fpx * 0.35f   // vinculum sits this far above the top of the text area
    val totalHeight = fpx * 1.35f   // total composable height
    val leftPad     = fpx * 0.45f   // horizontal space reserved for the radical stroke

    // Width of the "x" text, measured at runtime
    var xWidthPx by remember { mutableStateOf(fpx * 0.65f) }

    val xText = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = TimesRomanItalic, fontSize = fontSize)) {
            append("x")
        }
    }

    // Total symbol width = radical stroke width + x glyph width.
    // We render into a Box that is exactly this wide, then center it.
    val symbolWidthDp: Dp = with(density) { (leftPad + xWidthPx).toDp() }
    val symbolHeightDp: Dp = with(density) { totalHeight.toDp() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(symbolHeightDp)
                .then(if (xWidthPx > 0f) Modifier.width(symbolWidthDp) else Modifier.fillMaxWidth())
                .offset(x = (-8).dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            // Measure the x glyph width
            Text(
                text = xText,
                fontWeight = fontWeight,
                color = Color.Transparent,   // invisible — just for measurement
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { xWidthPx = it.size.width.toFloat() },
            )

            // Visible x label
            Text(
                text = xText,
                fontWeight = fontWeight,
                color = color,
                modifier = Modifier.align(Alignment.CenterEnd),
            )

            // Canvas draws the radical stroke + vinculum + tick
            Canvas(
                modifier = Modifier.matchParentSize(),
            ) {
                val w = size.width
                val h = size.height

                // Anchor points: x occupies the right xWidthPx of the canvas
                val vinculumLeft  = w - xWidthPx
                val vinculumRight = w + leftPad * 0.5f   // extend vinculum slightly beyond x glyph
                val vinculumTop   = vinculumY

            // Radical stroke: starts at bottom-left, rises to peak, then descends to
            // meet the vinculum at (vinculumLeft, vinculumTop)
            val startX   = vinculumLeft - leftPad
            val startY   = h * 0.65f          // bottom of the short entry stroke
            val peakX    = vinculumLeft - leftPad * 0.35f
            val peakY    = vinculumTop // - fpx * 0.18f   // slightly above vinculum
            val checkX   = vinculumLeft - leftPad * 0.62f
            val checkY   = h * 0.80f          // bottom of the check dip

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(checkX, checkY)         // short downward entry stroke
                lineTo(peakX,  peakY)          // rise to peak
                lineTo(vinculumLeft, vinculumTop)  // descend to vinculum join
                lineTo(vinculumRight, vinculumTop) // vinculum across top of x
                lineTo(vinculumRight, vinculumTop + tickDown)  // down-tick
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width     = strokeWidth,
                    cap       = StrokeCap.Round,
                    join      = StrokeJoin.Round,
                ),
            )
        }
        } // end inner Box
    }
}
