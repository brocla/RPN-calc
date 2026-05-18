# TDD Test Plan — Display Rewrite
Source: Notes on Display.md
Generated: 2026-05-15

Legend:
- [EXISTS]   — test already exists in DisplayFormatterTest.kt or EntryStateMachineTest.kt
- [NEW-PASS] — new test; expected to pass against current code (no bug)
- [NEW-FAIL] — new test; expected to FAIL against current code (exposes a bug)

Test files:
- `logic/src/test/.../display/DisplayFormatterTest.kt`
- `logic/src/test/.../entry/EntryStateMachineTest.kt`

**Sign-slot note:** Per REQUIREMENTS_DISPLAY_REWRITE.md §7.0, every string returned by `DisplayFormatter.format()` starts with a sign-slot character: `" "` for non-negative, `"-"` for negative. Error strings have no sign slot. All expected values in this plan that show a formatted number must include this leading character in the actual test assertion. For brevity, the "Expected" column below omits it — prepend `" "` to all non-negative formatter outputs and `"-"` to all negative ones when writing the test code.

---

## A — Display positions: digit budget

These tests verify the 10-position digit budget (positions 1-10 in standard, 8 positions for significand in sci/exp mode). The formatter is the unit under test.

| # | Test name | Input | Expected output | Status |
|---|-----------|-------|-----------------|--------|
| A1 | `fix2_tenDigitInteger` | x=1234567890.0, Fix(2) | "1,234,567,890" (no decimal, integer fills 10 slots) | [NEW-PASS] |
| A2 | `fix2_elevenDigitInteger_fallsBackToSci` | x=12345678901.0, Fix(2) | contains "e" or "E" | [NEW-FAIL] — current code may not detect 11-digit int |
| A3 | `sci7_capAt7FracDigits` | x=1.23456789, Sci(9) | "1.2345679e+00" (N capped at 7) | [NEW-FAIL] — cap not enforced |
| A4 | `sci2_padWithZeros` | x=2.34, Sci(4) | "2.3400e+00" | [NEW-FAIL] — current fmt may not pad |
| A5 | `eng_oneLead_noOverflow` | x=1.23, Eng(4) | "1.2300e+00" | [EXISTS] approx via eng2_units |
| A6 | `eng_twoLeads` | x=12.3, Eng(4) | "12.300e+00" | [NEW-PASS] |
| A7 | `eng_threeLeads` | x=123.4, Eng(4) | "123.40e+00" | [NEW-PASS] |
| A8 | `eng_threeLeads_capDigits` | x=123.456789, Eng(4) | "123.46e+00" (total int+frac ≤ 8) | [NEW-FAIL] — digit cap for ENG not enforced |

---

## B — Sign slot (position 0)

The sign occupies position 0 and must never consume a digit slot. Tests on Idle-formatted values.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| B1 | `fix2_negative_signInPosition0` | x=-3.14, Fix(2) | "-3.14" (sign present, 3 digit chars) | [EXISTS] via fix2_negative |
| B2 | `fix2_negative_tenDigitInteger` | x=-1234567890.0, Fix(2) | "-1,234,567,890" | [NEW-PASS] |
| B3 | `sci2_negative_exponent` | x=-1.23e4, Sci(2) | "-1.23e+04" | [EXISTS] via sci2_negative |
| B4 | `negativeZero_noSign` | x=-0.0, Fix(2) | "0.00" (no negative sign on zero) | [EXISTS] via negativeZero_fix2 |
| B5 | `negativeZero_sci` | x=-0.0, Sci(2) | "0.00e+00" | [NEW-FAIL] — negative-zero guard may be missing in SCI branch |
| B6 | `negativeZero_all` | x=-0.0, All | "0" | [NEW-FAIL] — negative-zero guard may be missing in ALL branch |

---

## C — Idle → Entry: digit key

Entry via EntryStateMachine. Digit keys clear display and start Standard (Mantissa) entry.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| C1 | `digit_fromIdle_startsMantissa` | idle + press 5 | entryState = Mantissa("5") | [EXISTS] via digit_fromIdle_noLift |
| C2 | `digit_fromIdle_stackLiftEnabled_liftsY` | idle (lift=true, x=9) + press 3 | stack.y=9, Mantissa("3") | [EXISTS] via digit_fromIdle_withLift |
| C3 | `digit_fromIdle_stackLiftDisabled_replacesX` | idle (lift=false, x=9) + press 3 | stack.y stays, Mantissa("3") | [EXISTS] via digit_fromIdle_noLift |
| C4 | `digit_zero_fromIdle` | idle + press 0 | Mantissa("0") | [NEW-PASS] |

---

## D — Idle → Entry: decimal key

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| D1 | `decimal_fromIdle_startsDecimalMantissa` | idle + pressDecimal | Mantissa("", hasDecimal=true) | [EXISTS] via decimal_fromIdle |
| D2 | `decimal_fromIdle_displayShowsLeadingZeroDot` | format that state | "0." | [NEW-PASS] — formatter emits "0." for empty digits + decimal |

---

## E — Idle → Entry: EEX key

EEX from Idle puts "1" in mantissa int-part and jumps straight to Exponent state.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| E1 | `eex_fromIdle_startsExponent` | idle + pressEex | Exponent(mantissaIntPart="1", exponentDigits="") | [EXISTS] via eex_fromIdle |
| E2 | `eex_fromIdle_displayShowsOneE` | format state from E1 | "1 E" (no exponent digits yet) | [NEW-FAIL] — current display may not render exponent entry state correctly in all cases |

---

## F — Entry → Entry (Standard mode): digit keys

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| F1 | `digit_appendsToIntPart` | Mantissa("12") + press 3 | Mantissa("123") | [EXISTS] via digit_appendsNormally |
| F2 | `digit_appendsToFracPart` | Mantissa("3", frac="1", dec=true) + press 4 | frac="14" | [NEW-PASS] |
| F3 | `digit_maxTenIntDigits_ignored` | Mantissa("1234567890") + press 1 | still "1234567890" | [EXISTS] via digit_maxTenDigits |
| F4 | `digit_suppressLeadingZero` | Mantissa("0") + press 0 | still "0" | [EXISTS] via digit_suppressLeadingZero |
| F5 | `digit_replaceLeadingZeroWithNonZero` | Mantissa("0") + press 5 | Mantissa("5") | [NEW-PASS] |
| F6 | `digit_fracPartNoLimit` | Mantissa("1", frac="12345678", dec=true) + press 9 | frac="123456789" (frac can exceed 8, display truncates) | [NEW-PASS] |

---

## G — Entry → Entry (Standard mode): decimal key

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| G1 | `decimal_setsFlag` | Mantissa("3") + pressDecimal | hasDecimal=true | [EXISTS] via decimal_fromMantissa |
| G2 | `decimal_twice_noop` | Mantissa("3", dec=true) + pressDecimal | unchanged | [EXISTS] via decimal_twice_noop |
| G3 | `decimal_displayShowsDot` | format Mantissa("3", dec=true) | "3." | [NEW-PASS] |
| G4 | `decimal_fromEmptyMantissa` | Mantissa("") + pressDecimal | Mantissa("", dec=true) → displays "0." | [NEW-PASS] |

---

## H — Entry → Entry (Standard mode): EEX key

EEX in Standard mode transitions to Exponent. Guard: mantissa integer part must be ≤ 8 digits.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| H1 | `eex_fromMantissa_transitionsToExponent` | Mantissa("12") + pressEex | Exponent(intPart="12", ...) | [EXISTS] via eex_fromMantissa |
| H2 | `eex_fromMantissaZeroX_clearsAndSetsOne` | idle(x=0) state + pressEex | Exponent(intPart="1", ...) | [NEW-FAIL] — spec: if x=0 clear and put "1"; current code may not check x |
| H3 | `eex_nineDigitInt_noop` | Mantissa("123456789") + pressEex | stays Mantissa (9 int digits > 8 guard) | [NEW-FAIL] — guard not implemented |
| H4 | `eex_eightDigitInt_allowed` | Mantissa("12345678") + pressEex | transitions to Exponent | [NEW-PASS] |
| H5 | `eex_withDecimal_fracIgnoredForGuard` | Mantissa("3", frac="14", dec=true) + pressEex | transitions to Exponent (int part "3" is 1 digit) | [NEW-FAIL] — guard logic may count frac digits |

---

## I — Entry → Entry (Standard mode): CHS key

CHS toggles sign. Guard: CHS on zero is a no-op.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| I1 | `chs_posToNeg` | Mantissa("3", neg=false) + pressChs | isNegative=true | [EXISTS] via chs_mantissa_positive |
| I2 | `chs_negToPos` | Mantissa("3", neg=true) + pressChs | isNegative=false | [EXISTS] via chs_mantissa_negative |
| I3 | `chs_zeroMantissa_noop` | Mantissa("0") + pressChs | isNegative stays false | [NEW-FAIL] — no guard for zero in current ESM |
| I4 | `chs_emptyMantissa_noop` | Mantissa("") + pressChs | isNegative stays false | [NEW-FAIL] — no guard for empty in current ESM |
| I5 | `chs_tenDigits_signFits` | Mantissa("1234567890") + pressChs | isNegative=true (sign in pos 0, digits in 1-10) | [NEW-PASS] |

---

## J — Entry → Entry (Standard mode): Backspace key

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| J1 | `backspace_removesLastIntDigit` | Mantissa("123") + pressBackspace | Mantissa("12") | [EXISTS] via backspace_removesLastDigit |
| J2 | `backspace_removesFracDigit` | Mantissa("3", frac="14", dec=true) + pressBackspace | frac="1" | [NEW-PASS] |
| J3 | `backspace_removesDecimal` | Mantissa("3", dec=true) + pressBackspace | hasDecimal=false, digits="3" | [EXISTS] via backspace_removesDecimal |
| J4 | `backspace_singleDigit_transitionsIdle` | Mantissa("5") + pressBackspace | EntryState.Idle, x=0, stackLiftEnabled=false | [NEW-FAIL] — current backspace_emptyMantissa_stays test shows it stays in Mantissa("") instead of going Idle |
| J5 | `backspace_singleDigit_Idle_displaysZero` | fmt of state from J4 | "0.00" (or current format) | [NEW-FAIL] — depends on J4 fix |
| J6 | `backspace_empty_staysEmpty` | Mantissa("") + pressBackspace | Mantissa("") | [EXISTS] via backspace_emptyMantissa_stays (but see J4 — spec says go Idle) |

Note on J4 vs J6: current test `backspace_emptyMantissa_stays` asserts staying in Mantissa(""), but the spec says: "If the position counter is now 1, set x to zero and transition to Idle." This means backspace on a single-digit goes Idle. J6 should be retired or reconciled with J4.

---

## K — Entry → Entry (Standard mode): DegRad key

DegRad is a no-op on the display (numeric content unchanged); it only toggles the angle mode annunciator.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| K1 | `degRad_inMantissa_noDisplayChange` | Mantissa("3.14") + pressDegRad | entryState unchanged | [NEW-PASS] |

---

## L — Entry → Entry (Exponent mode): digit keys

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| L1 | `eExpDigit_firstDigit` | Exponent(eDigits="") + press 4 | exponentDigits="4" | [NEW-PASS] |
| L2 | `eExpDigit_secondDigit` | Exponent(eDigits="4") + press 2 | exponentDigits="42" | [NEW-PASS] |
| L3 | `eExpDigit_thirdDigit_noop` | Exponent(eDigits="42") + press 1 | exponentDigits stays "42" | [NEW-FAIL] — cap at 2 exp digits not enforced |
| L4 | `eExpDigit_displayShowsTwoDigitExp` | format Exponent("1","",false,false,"04",false) | "1 E04" | [EXISTS] via entry_exponent |

---

## M — Entry → Entry (Exponent mode): Decimal, EEX, DegRad

These are no-ops in Exponent mode.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| M1 | `eExpDecimal_noop` | Exponent(...) + pressDecimal | state unchanged | [NEW-PASS] |
| M2 | `eExpEex_noop` | Exponent(...) + pressEex | state unchanged | [NEW-PASS] |
| M3 | `eExpDegRad_noop` | Exponent(...) + pressDegRad | entryState unchanged | [NEW-PASS] |

---

## N — Entry → Entry (Exponent mode): CHS key

CHS in Exponent mode toggles the exponent sign (position 9).

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| N1 | `chs_exponentPositive_toNeg` | Exponent(eNeg=false) + pressChs | exponentIsNegative=true | [EXISTS] via chs_exponent_positive |
| N2 | `chs_exponentNegative_toPos` | Exponent(eNeg=true) + pressChs | exponentIsNegative=false | [EXISTS] via chs_exponent_negative |
| N3 | `chs_exponentNeg_displayShowsMinus` | format Exponent("1","",false,false,"5",true) | "1 E-05" | [EXISTS] via entry_negExp |

---

## O — Entry → Entry (Exponent mode): Backspace key

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| O1 | `eExpBackspace_twoDigits_removesLast` | Exponent(eDigits="42") + pressBackspace | exponentDigits="4" | [EXISTS] approx via backspace_exponentDigit |
| O2 | `eExpBackspace_oneDigit_clearsDigit` | Exponent(eDigits="4") + pressBackspace | exponentDigits="" | [NEW-PASS] |
| O3 | `eExpBackspace_noDigits_clearSign_revertsMantissa` | Exponent("12", eDigits="") + pressBackspace | Mantissa("12") | [EXISTS] via backspace_exponentEmpty_revertsMantissa |
| O4 | `eExpBackspace_noDigits_signSet_clearSignFirst` | Exponent("1", eDigits="", eNeg=true) + pressBackspace | exponentIsNegative=false, still Exponent | [NEW-FAIL] — spec: "Unset the sign" before reverting; current code may revert directly |

---

## P — Entry → Idle transitions

When an operator, ENTER, STO, RCL, or format key is pressed during entry, the entry is committed and the result formatted.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| P1 | `enter_commitsMantissa` | Mantissa("42") + pressEnter | x=42.0, Idle | [EXISTS] via complete_integer in completeEntry tests |
| P2 | `enter_commitsDecimal` | Mantissa("3","14",dec=true) + pressEnter | x=3.14, Idle | [EXISTS] via complete_decimal |
| P3 | `enter_commitsExponent` | Exponent("1","23",true,false,"02",false) + pressEnter | x=1.23e2, Idle | [EXISTS] via complete_withExponent |
| P4 | `fixArg_commitsMantissa` | Mantissa("5") + pressFixArg(2) | x=5.0, Idle, format=Fix(2), display="5.00" | [NEW-FAIL] — pressFixMode etc. don't call commitEntry |
| P5 | `sciArg_commitsMantissa` | Mantissa("5") + pressSciArg(2) | x=5.0, Idle, format=Sci(2) | [NEW-FAIL] — same root cause as P4 |
| P6 | `engArg_commitsMantissa` | Mantissa("5") + pressEngArg(2) | x=5.0, Idle, format=Eng(2) | [NEW-FAIL] — same |
| P7 | `allMode_commitsMantissa` | Mantissa("5") + pressAllMode | x=5.0, Idle, format=All | [NEW-FAIL] — same |

---

## Q — Idle → Idle: backspace acts as CLX

Per spec: backspace in Idle clears x to 0 (CLX).

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| Q1 | `backspace_idle_clearsX` | Idle, x=42.0 + pressBackspace | x=0.0, Idle | [NEW-PASS] — engine already implements this |
| Q2 | `backspace_idle_alreadyZero_noop` | Idle, x=0.0 + pressBackspace | x=0.0, Idle | [NEW-PASS] |

---

## R — FIX formatter: overflow and fallback

Behavior: **strict fixed with SCI fallback** (HP-41C / HP-35s style).
If N fractional slots cannot show at least one significant digit, fall back to SCI. No adaptive expansion of dp.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| R1 | `fix2_normal_rounded` | x=3.14159, Fix(2) | "3.14" | [EXISTS] via fix2_normal |
| R2 | `fix2_zero` | x=0.0, Fix(2) | "0.00" | [EXISTS] via fix2_zero |
| R3 | `fix0_integerRound` | x=3.7, Fix(0) | "4" | [EXISTS] via fix0_rounds |
| R4 | `fix2_large_fallsBackToSci` | x=1.23e15, Fix(2) | contains "e"/"E" | [EXISTS] via fix2_largeOverflow |
| R5 | `fix2_verySmall_fallsBackToSci` | x=1.23e-15, Fix(2) | contains "e"/"E" | [EXISTS] via fix2_tooSmallOverflow |
| R6 | ~~`fix2_borderSmall_noFallback`~~ | ~~x=0.001, Fix(2)~~ | ~~"0.001"~~ | **DELETE** — contradicts strict-fixed behavior; replaced by R7 |
| R7 | `fix2_noSignificantDigit_fallsBack` | x=0.001, Fix(2) | contains "e"/"E" (SCI fallback, "0.00" shows no sig digit) | [NEW-FAIL] |
| R8 | `fix0_noLeadingZeros_singleZero` | x=0.0, Fix(0) | "0" | [NEW-PASS] |
| R9 | `fix4_tenDigitInteger_noDecimal` | x=1234567890.0, Fix(4) | "1,234,567,890" (no room for frac, rounds off) | [NEW-FAIL] — may not handle 10-digit int correctly |

---

## S — SCI formatter

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| S1 | `sci2_normal` | x=12345.0, Sci(2) | "1.23e+04" | [EXISTS] via sci2_normal |
| S2 | `sci0_normal` | x=12345.0, Sci(0) | "1e+04" | [EXISTS] via sci0_normal |
| S3 | `sci2_zero` | x=0.0, Sci(2) | "0.00      00" (sig + two-digit exp) | [NEW-FAIL] — see zeros table |
| S4 | `sci7_capN` | x=1.23456789, Sci(9) | N capped to 7: "1.2345679e+00" | [NEW-FAIL] — cap missing |
| S5 | `sci2_padWithZeros` | x=2.34, Sci(4) | "2.3400e+00" | [NEW-FAIL] — padding missing |
| S6 | `sci0_roundLeft` | x=299792458.0, Sci(0) | "3e+08" | [NEW-PASS] |
| S7 | `sci2_exponentGt99_overflow` | x=1e100, Sci(2) | error state | [NEW-FAIL] — overflow not detected |
| S8 | `sci2_exponentLtNeg99_underflow` | x=1e-100, Sci(2) | error state | [NEW-FAIL] — underflow not detected |
| S9 | `sci2_exactlyOne` | x=1.0, Sci(2) | "1.00e+00" | [EXISTS] via sci2_exactlyOne |

---

## T — ENG formatter

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| T1 | `eng2_thousands` | x=12345.0, Eng(2) | "12.35e+03" | [EXISTS] via eng2_thousands |
| T2 | `eng2_millions` | x=1234567.0, Eng(2) | "1.23e+06" | [EXISTS] via eng2_millions |
| T3 | `eng2_units` | x=1.23, Eng(2) | "1.23e+00" | [EXISTS] via eng2_units |
| T4 | `eng2_small` | x=0.00123, Eng(2) | "1.23e-03" | [EXISTS] via eng2_small |
| T5 | `eng4_threeLeads_digitCap` | x=123.456789, Eng(4) | "123.46e+00" (int+frac ≤ 8) | [NEW-FAIL] — digit cap not enforced |
| T6 | `eng2_zero` | x=0.0, Eng(4) | "0.0000      00" | [NEW-FAIL] — zeros table |
| T7 | `eng2_exponentGt99_overflow` | x=1e100, Eng(2) | error state | [NEW-FAIL] |
| T8 | `eng2_exponentLtNeg99_underflow` | x=1e-100, Eng(2) | error state | [NEW-FAIL] |

---

## U — ALL formatter

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| U1 | `all_noTrailingZeros` | x=3.14, All | "3.14" | [EXISTS] via all_noTrailingZeros |
| U2 | `all_integer` | x=100.0, All | "100" | [EXISTS] via all_integer |
| U3 | `all_pi` | x=Math.PI, All | "3.141592654" | [EXISTS] via all_pi |
| U4 | `all_exactInteger_noDecimal` | x=5.0, All | "5" | [NEW-PASS] |
| U5 | `all_zero` | x=0.0, All | "0" | [NEW-PASS] |
| U6 | `all_elevenDigitInt_fallsBackToSci` | x=12345678901.0, All | sci notation | [NEW-FAIL] — threshold not enforced |
| U7 | `all_tinyValue_fallsBackToSci` | x=0.0000001, All | sci notation (leading zeros consume all frac positions) | [NEW-FAIL] — threshold not enforced |
| U8 | `all_9sigDigits_rounded` | x=1.23456789012, All | "1.234567890" (10 sig digits max) | [NEW-PASS] |

---

## V — Entry display: Exponent state respects display mode

During EEX entry the significand uses positions 1-8 and exponent uses 9-11. Currently `formatExponent` ignores `displaySettings.mode`.

| # | Test name | Input | Expected | Status |
|---|-----------|-------|----------|--------|
| V1 | `entryExponent_alwaysShowsRawDigits` | Exponent("1","23",true,false,"04",false), Fix(4) | "1.23 E04" | [EXISTS] via entry_exponent — passes but ignores Fix(4) |
| V2 | `entryExponent_negativeExp_display` | Exponent("1","",false,false,"5",true), Fix(2) | "1 E-05" | [EXISTS] via entry_negExp |
| V3 | `entryExponent_emptyExp_noTrailingE` | Exponent("3","14",true,false,"",false), any mode | "3.14 E" | [NEW-FAIL] — display during partial EEX entry (before any exp digit) |
| V4 | `entryExponent_respects8SigPositions` | Exponent("12345678","",false,false,"02",false), any | "12345678 E02" (8 sig digits fit) | [NEW-FAIL] — no guard enforced during display |

---

## W — Zeros display table

Per Notes: zero in each format.

| # | Format | Expected string | Status |
|---|--------|-----------------|--------|
| W1 | Fix(3) | "0.000" | [NEW-PASS] |
| W2 | Sci(2) | "0.00e+00" | [NEW-FAIL] — current Sci(2) zero output unconfirmed |
| W3 | Eng(4) | "0.0000e+00" | [NEW-FAIL] |
| W4 | All    | "0" | [NEW-PASS] |

---

## Summary of [NEW-FAIL] tests (bugs to fix)

| ID | Short description |
|----|-------------------|
| A2 | 11-digit integer doesn't fall back to SCI in FIX |
| A3 | SCI N not capped at 7 |
| A4 | SCI doesn't pad with trailing zeros |
| A8 | ENG total digit cap (int+frac ≤ 8) not enforced |
| B5 | Negative-zero shows sign in SCI |
| B6 | Negative-zero shows sign in ALL |
| E2 | Exponent display during partial EEX (empty exp) |
| H2 | EEX from Idle when x=0 doesn't clear/set "1" |
| H3 | EEX guard: 9-digit mantissa int should block EEX |
| H5 | EEX guard uses frac digits in length calculation |
| I3 | CHS on zero mantissa doesn't guard (negative zero) |
| I4 | CHS on empty mantissa doesn't guard |
| J4 | Backspace on single digit doesn't transition to Idle |
| L3 | Third exponent digit not blocked |
| O4 | Backspace in exp with sign set clears sign before reverting |
| P4 | FIX key doesn't commit entry |
| P5 | SCI key doesn't commit entry |
| P6 | ENG key doesn't commit entry |
| P7 | ALL key doesn't commit entry |
| R7 | FIX fallback threshold (no sig digit visible) not enforced |
| S3 | SCI zero display doesn't match spec |
| S4 | SCI N cap at 7 |
| S5 | SCI zero-padding |
| S7 | SCI exponent > 99 not detected as overflow |
| S8 | SCI exponent < -99 not detected as underflow |
| T5 | ENG digit cap (int+frac ≤ 8) |
| T6 | ENG zero display |
| T7 | ENG overflow |
| T8 | ENG underflow |
| U6 | ALL: 11-digit integer not switched to SCI |
| U7 | ALL: tiny value not switched to SCI |
| V3 | Exponent entry with empty exp shows trailing "E" incorrectly |
| V4 | No 8-position significand guard during EEX entry display |
| W2 | Sci(2) zero renders incorrectly |
| W3 | Eng(4) zero renders incorrectly |
