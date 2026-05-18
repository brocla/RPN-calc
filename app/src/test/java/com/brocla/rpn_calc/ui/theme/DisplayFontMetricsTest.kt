package com.brocla.rpn_calc.ui.theme

import org.junit.Test
import java.awt.Font
import java.awt.font.FontRenderContext
import java.io.File
import kotlin.test.assertEquals

/**
 * Regression test for DSEG7Classic-BoldItalic font advance widths.
 *
 * The display layout model depends on these exact advance widths:
 *   - Digits 0–9, minus '-', space ' ' : 816 font units (one digit-position wide)
 *   - Decimal point '.'               : 0   (zero-width — does not shift digit positions)
 *   - Comma ','                        : 0   (zero-width — does not shift digit positions)
 *
 * The font's unitsPerEm = 1000. Deriving the AWT font at 1000pt makes
 * advanceX in pixels equal to advance in font units, allowing direct comparison.
 *
 * Checks both the master file in artwork/ and the deployed copy in app/src/main/res/font/.
 * If the two files diverge (e.g. artwork/ edited but not copied to res/font/), both
 * failures are reported.
 */
class DisplayFontMetricsTest {

    // Gradle unit-test working directory is the module root (app/).
    private val deployedFont = File("src/main/res/font/dseg7classic_bolditalic.ttf")
    private val masterFont   = File("../artwork/DSEG7Classic-BoldItalic.ttf")

    private val frc = FontRenderContext(null, false, false)

    /** Returns advance width in font units (UPM=1000, so 1000pt → units == pixels). */
    private fun advanceOf(font: Font, ch: Char): Int {
        val gv = font.createGlyphVector(frc, ch.toString())
        return gv.getGlyphMetrics(0).advanceX.toInt()
    }

    private fun loadFont(file: File): Font =
        Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(1000f)

    private fun assertWidths(label: String, font: Font) {
        // Digits
        for (d in '0'..'9') {
            assertEquals(816, advanceOf(font, d),
                "$label: digit '$d' must have advance width 816")
        }
        // Sign-slot characters
        assertEquals(816, advanceOf(font, '-'),
            "$label: minus '-' must have advance width 816")
        assertEquals(816, advanceOf(font, ' '),
            "$label: space ' ' must have advance width 816 (sign slot must equal digit width)")
        // Zero-width separators
        assertEquals(0, advanceOf(font, '.'),
            "$label: decimal point '.' must have advance width 0")
        assertEquals(0, advanceOf(font, ','),
            "$label: comma ',' must have advance width 0")
    }

    @Test fun deployedFont_characterWidths() =
        assertWidths("deployed font (res/font/)", loadFont(deployedFont))

    @Test fun masterFont_characterWidths() =
        assertWidths("master font (artwork/)", loadFont(masterFont))

    @Test fun deployedFont_matchesMaster() {
        val deployed = loadFont(deployedFont)
        val master   = loadFont(masterFont)
        val chars = ('0'..'9').toList() + listOf('-', ' ', '.', ',')
        for (ch in chars) {
            assertEquals(advanceOf(master, ch), advanceOf(deployed, ch),
                "Character '$ch': deployed font advance differs from master font")
        }
    }
}
