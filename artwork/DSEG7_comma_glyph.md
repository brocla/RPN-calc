# Adding the Comma Glyph to DSEG7Classic-BoldItalic

## Problem

The DSEG7Classic-BoldItalic font does not include a comma glyph (U+002C).
A comma is needed to display thousand-separators like `1,234,567` without
shifting the display width.

## Solution

A FontForge Python script (`add_comma_glyph.py`) adds the glyph
programmatically and writes it back to the TTF.  No SFD source exists for
DSEG7 — only TTFs ship in the release package — so the script operates
directly on the TTF.

Run with:

```
"c:/Program Files/FontForgeBuilds/bin/fontforge.exe" -script artwork/add_comma_glyph.py
```

A backup of the original TTF is preserved at `DSEG7Classic-BoldItalic.ttf.bak`.

## Glyph Design

### Dot

An exact copy of the period (U+002E) bezier circle, taken verbatim from the
`uEE0E` component in `DSEG14Classic-BoldItalic.sfd` (same designer, same
em=1000 coordinate system).  Centre (−44, 62), radius ≈ 62 units.

### Tail

A tapered hexagon sitting below the baseline (y: +10 down to −90), consistent
with the angular 7-segment aesthetic.

The font has `ItalicAngle: −5` (leans right by 5°).  The tail vertices were
sheared by:

```
x' = x + y × tan(5°)   ≈   x + y × 0.0875
```

This shifts the tail tip leftward as it descends, matching the italic lean of
the rest of the font rather than pointing straight down.

### Advance Width

Set to **0** — the same as the period — so the comma overlays the preceding
digit cell and does not advance the cursor position.  This is the intended
behaviour for punctuation in the DSEG font family, which simulates an LCD /
7-segment display where separators sit within the digit cell.
