package com.brocla.rpn_calc.ui.theme

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.R

val Dseg7 = FontFamily(Font(R.font.dseg7classic_bolditalic, FontWeight.Bold))
val Helvetica = FontFamily(Font(R.font.helvetica))
val TimesRomanItalic = FontFamily(Font(R.font.timesi))

/** Renders [label] with x/y characters in Times Roman Italic; all other characters use the
 *  caller's base font (set via [Text]'s fontFamily parameter). */
// Characters that should render in Times Roman Italic:
//   x, y  — plain lowercase
//   Y     — uppercase (e.g. x↔Y)
//   ˣ     — U+02E3 modifier letter small x (superscript, e.g. eˣ, 10ˣ, yˣ)
private val timesChars = setOf('x', 'y', 'Y', 'ˣ')

fun mixedFontLabel(label: String, timesScale: Float = 1f) = buildAnnotatedString {
    for (ch in label) {
        if (ch in timesChars) {
            withStyle(SpanStyle(fontFamily = TimesRomanItalic, fontSize = timesScale.em)) { append(ch) }
        } else {
            append(ch)
        }
    }
}

val DisplayTextStyle = TextStyle(
    fontFamily = Dseg7,
    fontSize = 44.sp,
    letterSpacing = 2.sp,
    color = CalcColors.DisplayText,
)

val AnnunciatorTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    letterSpacing = 1.sp,
)
