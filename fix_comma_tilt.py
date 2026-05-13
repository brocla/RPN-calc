"""
Fix comma glyph tail tilt in dseg7classic_bolditalic.ttf.
Sets contour 1 (the tail) to +20-degree italic shear coordinates,
which produces a -20-degree visual tilt for a tail hanging below the baseline.

Shear: x' = x + y * tan(20°) = x + y * 0.36397
Pre-shear vertices (upright):
  top-right  (-15,  10)  → (-11,  10)
  top-left   (-75,  10)  → (-71,  10)
  left-mid   (-70, -30)  → (-81, -30)
  left-bot   (-55, -90)  → (-88, -90)
  right-bot  (-30, -90)  → (-63, -90)
  right-mid  (-20, -30)  → (-31, -30)

Perimeter is traversed top-right → top-left → left-mid → left-bot
→ right-bot → right-mid (closed), giving a non-self-intersecting outline.
"""

import shutil
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.pens.recordingPen import RecordingPen
from fontTools.pens.ttGlyphPen import TTGlyphPen

FONT_PATH = Path(
    r"c:\Users\micro\source\repos\RPN_calc\app\src\main\res\font\dseg7classic_bolditalic.ttf"
)
GLYPH_NAME = "comma"

TAIL_COORDS = [
    (-11,  10),   # top-right
    (-71,  10),   # top-left
    (-81, -30),   # left-mid
    (-88, -90),   # left-bottom
    (-63, -90),   # right-bottom
    (-31, -30),   # right-mid
]


def split_into_contours(ops):
    """Group recorded pen operations by contour (split on endPath/closePath)."""
    contours, current = [], []
    for op, args in ops:
        current.append((op, args))
        if op in ("endPath", "closePath"):
            contours.append(current)
            current = []
    return contours


def replay_contour(pen, ops):
    for op, args in ops:
        if op == "moveTo":
            pen.moveTo(*args)
        elif op == "lineTo":
            pen.lineTo(*args)
        elif op == "qCurveTo":
            pen.qCurveTo(*args)
        elif op == "curveTo":
            pen.curveTo(*args)
        elif op == "closePath":
            pen.closePath()
        elif op == "endPath":
            pen.endPath()


# --- backup ---
backup_path = FONT_PATH.with_suffix(".ttf.bak")
shutil.copy2(FONT_PATH, backup_path)
print(f"Backup saved to {backup_path}")

# --- read current glyph ---
font = TTFont(FONT_PATH)
rp = RecordingPen()
font.getGlyphSet()[GLYPH_NAME].draw(rp)

contours = split_into_contours(rp.value)
print(f"Contours found: {len(contours)}")
for i, c in enumerate(contours):
    print(f"  contour {i}: {[op for op, _ in c]}")

# --- rebuild: keep dot (contour 0), replace tail (contour 1) ---
pen = TTGlyphPen(None)

replay_contour(pen, contours[0])   # dot unchanged

pen.moveTo(TAIL_COORDS[0])
for pt in TAIL_COORDS[1:]:
    pen.lineTo(pt)
pen.closePath()                     # closed contour, not endPath

font["glyf"][GLYPH_NAME] = pen.glyph()
font.save(FONT_PATH)

print("\nFont saved. New tail vertices:")
for pt in TAIL_COORDS:
    print(f"  {pt}")
