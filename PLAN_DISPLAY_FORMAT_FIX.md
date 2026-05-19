# Plan: Fix Display Output Format (Issues 1–5)

## What went wrong

Three root causes, not five separate issues:

**A. Formatter output not redesigned** — `formatSci`, `formatEng`, and `formatExponent` were
modified but never redesigned. They still emit `e+`/`e-` notation carried over from the original
code. Notes on Display.md specifies a fixed-width positional output with no `e` character and
no `+` for positive exponents.

**B. `insertThousandsCommas` SCI/ENG detection broken** — it currently skips strings containing
`'e'`. After the redesign, SCI/ENG strings have no `'e'`. The detection must be updated.

**C. `DisplayPanel` is right-aligned** — `TextAlign.End` causes numbers to stack against the
right edge. Notes say "numbers are loaded into the positions from left to right." This is also
the cause of issues 4 and 5 (right-aligned text, sign not visible at position 0) and most
likely issue 1 (leading-space in an 11-char string clips off the visible area).


## Target output format

The 12-position display model (positions 0–11):

```
| 0 | 1–8     | 9   | 10–11 |
| ± | sig      | ±exp| exp   |   ← SCI / ENG / Exponent-entry
| ± | digits 1–10             |   ← Standard / Fix / All (pos 11 unused)
```

Rules:
- Position 0 is always `' '` or `'-'` (already correct).
- Decimal point and commas are zero-advance-width; they do not occupy a position.
- SCI / ENG: significand fills positions 1–8; unused positions are filled with spaces.
  Position 9 is `' '` for positive exponent, `'-'` for negative. Positions 10–11 are the
  two-digit exponent, zero-padded. No `'e'` character. No `'+'`.
- Standard / Fix / All: digits fill left-to-right from position 1; position 11 is unused.

### SCI string length

`sign(1) + significand_digits(N+1) + "."(1 char, 0-width) + padding(7-N spaces) + expSign(1) + expDigits(2) = 13 chars`

Always 13 chars for non-error SCI output (N capped at 7).

### ENG string length

`sign(1) + intPart(1–3) + "."(1, 0-width) + fracPart(cappedN) + padding(8-intLen-cappedN spaces) + expSign(1) + expDigits(2)`

Also 13 chars (total significand positions always = 8).

### Exponent-entry string length

Same structure: 12 chars if no decimal typed, 13 chars if decimal typed.
(With left-alignment this is fine — the display just shows fewer characters.)


## Phases

### Phase Q — Redesign formatter output (logic module)

**Q1. Rewrite `formatSci(v, dp)`**
- Compute exp from `"%.0e".format(abs(v))` (existing probe approach is fine).
- Overflow/Underflow guard: unchanged.
- cappedDp = min(dp, 7).
- Compute significand = abs(v) / 10^exp.
- sigStr: `"%.${cappedDp}f".format(significand)` + (if cappedDp == 0 then "." else "").
  (Always include decimal, per Notes example `3.       08`.)
- paddingSpaces = 7 - cappedDp.
- expSign = if exp < 0 then '-' else ' '.
- expStr = abs(exp).toString().padStart(2, '0').
- Return `"$sign$sigStr${" ".repeat(paddingSpaces)}$expSign$expStr"`.

**Q2. Rewrite `formatEng(v, dp)`**
- Same overflow guard.
- Compute engExp, mantissa, mantissaIntDigits (1–3): unchanged.
- cappedDp = min(dp, 8 - mantissaIntDigits).
- sigStr: `"%.${cappedDp}f".format(mantissa)` + (if cappedDp == 0 then "." else "").
- paddingSpaces = 8 - mantissaIntDigits - cappedDp.
- expSign = if engExp < 0 then '-' else ' '.
- expStr = abs(engExp).toString().padStart(2, '0').
- Return `"$sign$sigStr${" ".repeat(paddingSpaces)}$expSign$expStr"`.

**Q3. Rewrite `formatExponent(es)` (entry mode)**
- sign, intPart, mantissaContent: unchanged.
- sigDigitCount = (if intPart is empty then 1 else intPart.length) + es.mantissaFracPart.length.
- paddingSpaces = max(0, 8 - sigDigitCount).
- expSign = if es.exponentIsNegative then '-' else ' '.
- expStr = when exponentDigits.length:
    0 → "  "
    1 → "${exponentDigits[0]} "
    2 → exponentDigits
- Return `"$sign$mantissaContent${" ".repeat(paddingSpaces)}$expSign$expStr"`.
- No `'E'` character. No `'+'`.

**Q4. Update `formatAll`**
- When it falls back to SCI (`normalizeExp` / long strings), delegate to `formatSci(v, 7)`.
- Remove `normalizeExp` references inside `formatAll` (they produced `e+` notation).
- Keep the fast path (no SCI needed) unchanged — it returns a plain FIX-style string.

**Q5. Update `normalizeExp` / helpers**
- `normalizeExp` is used only by `formatAll`'s fast path (trimming `g`-format output) and
  `formatSci`'s overflow probe. After the redesign, `normalizeExp` may be used only for
  the overflow probe; refactor as needed.

**Q6. Update tests**
Files to update:
- `logic/src/test/.../display/DisplayFormatterTest.kt` — all SCI/ENG assertions use `e+`/`e-`;
  replace with the new fixed-width format. The `contains("e")` guards in the FIX fallback
  tests need updating too.
- `logic/src/test/.../display/DisplayBoundaryTest.kt` — `contains('e')` checks for SCI
  fallback in FIX mode need updating; `contains("e+99")` etc. need updating.
- `logic/src/test/.../display/spec/DisplayFormatterSpec.kt` — update SCI/ENG spec tests.

Boundary tests (`DisplayBoundaryTest`) check Fix→SCI fallback using `result.contains('e')`.
After the change, SCI output has no `'e'`. Update these to check whether the string is 13
chars (or check that a space appears at position 9 and two digits at positions 10–11).


### Phase R — Fix `insertThousandsCommas` (app module)

The current SCI/ENG skip guard `plain.contains('e', ignoreCase = true)` will miss the new
format. Replace with a structural check:

```kotlin
// SCI/ENG strings are always 13 chars; pos 9 is the exp sign (' ' or '-');
// pos 10-11 are the two-digit exponent.
private fun isSciEngFormat(plain: String): Boolean =
    plain.length == 13 &&
    plain[9] in " -" &&
    plain[10].isDigit() &&
    plain[11].isDigit()
```

Replace `if (plain.contains('e', ignoreCase = true)) return plain` with
`if (isSciEngFormat(plain)) return plain`.

Update `app/src/test/.../ui/calculator/DisplayFormatterTest.kt` — the SCI/ENG pass-through
tests currently rely on `e` in the string; update inputs to the new format.

Also update the `insertThousandsCommas` KDoc comment.


### Phase S — Fix `DisplayPanel` alignment (app module)

In `app/src/main/java/.../ui/calculator/components/DisplayPanel.kt` line 88:
- Change `textAlign = TextAlign.End` → `textAlign = TextAlign.Start`.

This fixes issues 4 and 5 (right-alignment, sign position) and very likely issue 1
(blank at 10th digit — a leading space in an 11-char string clips with right-alignment).


## Order of work

1. Phase Q (formatter redesign) — TDD: update/write failing tests first, then implement.
2. Phase S (DisplayPanel) — one-line change; verify with app.
3. Phase R (insertThousandsCommas) — update detection and its tests.

Phase S before Phase R because Phase R's tests depend on knowing what SCI/ENG strings look like.


## Files changed

| File | Change |
|------|--------|
| `logic/.../display/DisplayFormatter.kt` | Rewrite formatSci, formatEng, formatExponent, update formatAll |
| `logic/.../display/DisplayFormatterTest.kt` | Update SCI/ENG/All-fallback assertions |
| `logic/.../display/DisplayBoundaryTest.kt` | Update SCI-fallback detection in Fix tests |
| `logic/.../display/spec/DisplayFormatterSpec.kt` | Update SCI/ENG spec assertions |
| `app/.../ui/calculator/DisplayFormatter.kt` | Replace SCI/ENG guard in insertThousandsCommas |
| `app/.../ui/calculator/DisplayFormatterTest.kt` | Update SCI/ENG pass-through test inputs |
| `app/.../ui/calculator/components/DisplayPanel.kt` | TextAlign.End → TextAlign.Start |

No data structure changes required. No DI changes required.
