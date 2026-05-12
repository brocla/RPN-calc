"""
FontForge script: add comma glyph (U+002C) to DSEG7Classic-BoldItalic.ttf

Coordinates derived from DSEG14Classic-BoldItalic.sfd (same designer, same
em=1000 coordinate system).  The comma is:
  - dot:  exact circle from uEE0E (the period component), centre (-44, 62)
  - tail: a tapered hexagon below the dot, adapted from uEE0B, repositioned
          to sit below the baseline for a classic comma silhouette

Advance width is 0 — same as the period — so the mark does not shift the
display position.

Run with:
  "c:/Program Files/FontForgeBuilds/bin/fontforge.exe" -script add_comma_glyph.py
"""

import fontforge
import shutil
import os

FONT_IN  = r"c:\Users\micro\source\repos\RPN_calc\app\src\main\res\font\DSEG7Classic-BoldItalic.ttf"
FONT_OUT = FONT_IN
BACKUP   = FONT_IN + ".bak"

# ── backup ───────────────────────────────────────────────────────────────────
if not os.path.exists(BACKUP):
    shutil.copy2(FONT_IN, BACKUP)
    print(f"Backup written to {BACKUP}")
else:
    print("Backup already exists — skipping.")

font = fontforge.open(FONT_IN)

# Sanity-check: confirm the period is where we expect it
period = font[0x2E]
bb = period.boundingBox()
print(f"Period bounding box (expected ~(-106, 0, 18, 124)): {bb}")

# ── build the dot contour ────────────────────────────────────────────────────
# Exact 16-arc cubic bezier circle from uEE0E in DSEG14Classic-BoldItalic.sfd
# Centre (-44, 62), radius ≈ 62 units.
def P(x, y, on=True):
    p = fontforge.point(x, y)
    p.on_curve = on
    return p

dot = fontforge.contour()
dot.is_quadratic = False

dot += P(  18,  62)          # right (on-curve start)
dot += P(  18,  53, False)
dot += P(  16,  45, False)
dot += P(  13,  38)
dot += P(  10,  31, False)
dot += P(   6,  24, False)
dot += P(   0,  18)
dot += P(  -6,  12, False)
dot += P( -13,   8, False)
dot += P( -20,   5)
dot += P( -27,   2, False)
dot += P( -35,   0, False)
dot += P( -44,   0)          # bottom
dot += P( -53,   0, False)
dot += P( -61,   2, False)
dot += P( -68,   5)
dot += P( -75,   8, False)
dot += P( -82,  12, False)
dot += P( -88,  18)
dot += P( -94,  24, False)
dot += P( -98,  31, False)
dot += P(-101,  38)
dot += P(-104,  45, False)
dot += P(-106,  53, False)
dot += P(-106,  62)          # left
dot += P(-106,  71, False)
dot += P(-104,  79, False)
dot += P(-101,  86)
dot += P( -98,  93, False)
dot += P( -94, 100, False)
dot += P( -88, 106)
dot += P( -82, 112, False)
dot += P( -75, 116, False)
dot += P( -68, 119)
dot += P( -61, 122, False)
dot += P( -53, 124, False)
dot += P( -44, 124)          # top
dot += P( -35, 124, False)
dot += P( -27, 122, False)
dot += P( -20, 119)
dot += P( -13, 116, False)
dot += P(  -6, 112, False)
dot += P(   0, 106)
dot += P(   6, 100, False)
dot += P(  10,  93, False)
dot += P(  13,  86)
dot += P(  16,  79, False)
dot += P(  18,  71, False)
# contour closes back to the start point (18, 62)
dot.closed = True

# ── build the tail contour ───────────────────────────────────────────────────
# Adapted from uEE0B in DSEG14 (comma tail component).
# Original coords: x -604..-497, y 144..418 (all positive, above baseline).
# Here it is translated and scaled to sit BELOW the dot (below y=0) so the
# overall shape reads as a conventional comma: round dot + descending tail.
#
# The font has ItalicAngle: -5 (leans right by 5°).  The italic shear is:
#   x' = x + y * tan(5°)   (≈ x + y * 0.0875)
# This shifts the tail tip leftward as it descends, matching the font slant.
#
# Pre-shear vertices:           Post-shear (ItalicAngle -5°):
#   (-15,  10) ─── (-75,  10)    (-14,  10) ─── (-74,  10)
#        \              /              \              /
#    (-20, -30)─(-70, -30)         (-23, -30)─(-73, -30)
#          \        /                    \        /
#     (-30, -90)─(-55, -90)          (-38, -90)─(-63, -90)

tail = fontforge.contour()
tail.is_quadratic = False
tail += P( -14,  10)
tail += P( -74,  10)
tail += P( -73, -30)
tail += P( -63, -90)
tail += P( -38, -90)
tail += P( -23, -30)
tail.closed = True

# ── assemble and assign ───────────────────────────────────────────────────────
layer = fontforge.layer()
layer += dot
layer += tail

if 0x2C in font:
    comma_g = font[0x2C]
    print("Overwriting existing comma glyph.")
else:
    comma_g = font.createChar(0x2C, "comma")
    print("Created new comma glyph at U+002C.")

comma_g.layers[1] = layer   # layer 1 = foreground
comma_g.width = 0            # zero advance — same as period

print(f"Comma bounds: {comma_g.boundingBox()}")
print(f"Comma width:  {comma_g.width}")

# ── generate ──────────────────────────────────────────────────────────────────
font.generate(FONT_OUT)
font.close()
print(f"\nFont saved → {FONT_OUT}")
