# Implementation Plan — Display Rewrite

Implements the features and bug fixes in REQUIREMENTS_DISPLAY_REWRITE.md.
Test specifications are in TDD_PLAN_DISPLAY.md.
Each phase is independently buildable and testable.

---

## Overview

The work divides into five phases executed in strict order. Each phase has a
TDD loop: write failing tests, implement, go green. Interfaces are extracted for
DI before the logic is touched so that tests can use fakes throughout.

```
Phase L — Rename EntryState.Mantissa → Standard (model + mechanical callsite sweep)
Phase M — Extract IEntryStateMachine and IDisplayFormatter interfaces + Hilt bindings
Phase N — EntryStateMachine behavioral fixes
Phase O — DisplayFormatter rewrite (sign-slot + all formatter rules)
Phase P — CalculatorEngine fixes + insertThousandsCommas update
```

All new tests go in sub-packages:
- `logic/.../entry/spec/EntryStateMachineSpec.kt`
- `logic/.../display/spec/DisplayFormatterSpec.kt`

---

## Phase L — Rename `EntryState.Mantissa` to `EntryState.Standard`

**Goal:** Purely mechanical rename. No behavior changes. Project compiles and all
existing tests pass at the end.

This phase has no TDD loop — it is a refactor, not a behavior change.

### L1 — Rename the class in `EntryState.kt`

In `logic/.../model/EntryState.kt`:
- Rename `Mantissa` → `Standard`
- Keep `@SerialName("mantissa")` unchanged (DataStore compatibility)
- No field changes

### L2 — Update all call sites

Grep for `EntryState.Mantissa` and `is EntryState.Mantissa` across all source
and test files. Replace every occurrence with `EntryState.Standard`.

Files expected to change:
- `logic/.../entry/EntryStateMachine.kt`
- `logic/.../display/DisplayFormatter.kt`
- `logic/.../engine/CalculatorEngine.kt`
- `logic/.../entry/EntryStateMachineTest.kt`
- `logic/.../display/DisplayFormatterTest.kt`
- `app/.../ui/calculator/CalculatorViewModel.kt`

### L3 — Verify

Run the full test suite. All existing tests must pass. No new tests in this phase.

---

## Phase M — Interface extraction and Hilt bindings

**Goal:** `CalculatorEngine` depends on interfaces, not concrete classes. This
enables fake implementations in tests for Phase N and Phase O.

This phase has no TDD loop — it is structural plumbing.

### M1 — Create `IEntryStateMachine`

New file: `logic/.../entry/IEntryStateMachine.kt`

```kotlin
interface IEntryStateMachine {
    fun pressDigit(state: CalculatorState, digit: Int): CalculatorState
    fun pressDecimal(state: CalculatorState): CalculatorState
    fun pressChs(state: CalculatorState): CalculatorState
    fun pressEex(state: CalculatorState): CalculatorState
    fun pressBackspace(state: CalculatorState): CalculatorState
    fun completeEntry(state: CalculatorState): CalculatorState
    fun currentDisplayValue(state: CalculatorState): Double
}
```

`EntryStateMachine` adds `implements IEntryStateMachine` — no method changes.

### M2 — Create `IDisplayFormatter`

New file: `logic/.../display/IDisplayFormatter.kt`

```kotlin
interface IDisplayFormatter {
    fun format(state: CalculatorState): String
}
```

`DisplayFormatter` adds `implements IDisplayFormatter` — no method changes.

### M3 — Update `CalculatorEngine` constructor

```kotlin
class CalculatorEngine @Inject constructor(
    private val entryStateMachine: IEntryStateMachine,
    private val mathOperations: MathOperations,
    private val displayFormatter: IDisplayFormatter
)
```

### M4 — Add Hilt bindings in `CalculatorModule.kt`

```kotlin
@Binds @Singleton
abstract fun bindEntryStateMachine(impl: EntryStateMachine): IEntryStateMachine

@Binds @Singleton
abstract fun bindDisplayFormatter(impl: DisplayFormatter): IDisplayFormatter
```

### M5 — Verify

Run the full test suite. All existing tests must pass. No new tests in this phase.

---

## Phase N — EntryStateMachine behavioral fixes

**Goal:** Fix the six behavioral bugs described in REQUIREMENTS_DISPLAY_REWRITE.md §5.
All fixes are covered by tests written before the implementation (TDD).

Tests are written in `logic/.../entry/spec/EntryStateMachineSpec.kt`.
Each loop below is self-contained: write the test, confirm it fails, fix, confirm green.

---

### TDD Loop N1 — EEX guards (§5.6) [TDD plan H2, H3, H5]

**Write these tests first:**

```
eex_fromZeroMantissa_clearsAndUsesOne
    Input:  Standard(digits="0") + pressEex
    Expect: Exponent(mantissaIntPart="1", exponentDigits="")

eex_fromEmptyMantissa_clearsAndUsesOne
    Input:  Standard(digits="") + pressEex
    Expect: Exponent(mantissaIntPart="1", exponentDigits="")

eex_nineDigitInteger_isNoop
    Input:  Standard(digits="123456789") + pressEex
    Expect: state unchanged (still Standard("123456789"))

eex_eightDigitInteger_isAllowed
    Input:  Standard(digits="12345678") + pressEex
    Expect: transitions to Exponent

eex_intPartOneDecimalFracPart_usesIntLengthOnly
    Input:  Standard(digits="3", fracDigits="14", hasDecimal=true) + pressEex
    Expect: transitions to Exponent (int part length 1, well under 8)
```

**Verify all fail. Then implement:**

In `EntryStateMachine.pressEex`, `is EntryState.Standard` branch:
1. Compute `isZero`: `es.digits.isEmpty() || (es.digits == "0" && es.fracDigits.all { it == '0' || es.fracDigits.isEmpty() })`
2. If `isZero`, treat as Idle — produce `Exponent(mantissaIntPart="1", ...)`
3. Else if `es.digits.length > 8`, return `state` unchanged
4. Otherwise proceed as now

**Verify all five tests pass. Run full suite — no regressions.**

---

### TDD Loop N2 — CHS zero guard (§5.7) [TDD plan I3, I4]

**Write these tests first:**

```
chs_zeroDigits_isNoop
    Input:  Standard(digits="0") + pressChs
    Expect: isNegative stays false

chs_emptyDigits_isNoop
    Input:  Standard(digits="") + pressChs
    Expect: isNegative stays false

chs_allFracZeros_isNoop
    Input:  Standard(digits="0", fracDigits="000", hasDecimal=true) + pressChs
    Expect: isNegative stays false

chs_nonZero_toggles
    Input:  Standard(digits="1") + pressChs
    Expect: isNegative = true  (existing behavior, confirm still works)
```

**Verify first three fail, fourth passes. Then implement:**

In `EntryStateMachine.pressChs`, `is EntryState.Standard` branch:
- Before toggling, check `isZero`: digits is empty or `"0"`, and fracDigits is blank or all `'0'`
- If zero, return `state` unchanged
- Otherwise toggle as now

**Verify all four tests pass. Run full suite — no regressions.**

---

### TDD Loop N3 — Backspace last-digit → Idle (§5.8) [TDD plan J4, J5]

**Write these tests first:**

```
backspace_singleDigit_transitionsToIdle
    Input:  Standard(digits="5") + pressBackspace
    Expect: entryState = Idle, stack.x = 0.0, stackLiftEnabled = false

backspace_singleDigit_idle_displaysZero
    Input:  (same state as above, pass through DisplayFormatter)
    Expect: formatted string == " 0.0000" (or whatever current Fix(4) zero looks like
            with sign-slot prefix — adjust to match actual formatter output after Phase O)
```

**Also update this existing test (it contradicts the spec):**

```
backspace_emptyMantissa_stays   →   DELETE or rename to backspace_emptyMantissa_transitionsToIdle
```

The new assertion: `Standard("") + pressBackspace` → `Idle`, `x=0.0`, `stackLiftEnabled=false`

**Verify the new tests fail. Then implement:**

In `EntryStateMachine.pressBackspace`, `is EntryState.Standard` branch, in the
final `else` (drop last digit from `digits`):

```kotlin
val newDigits = es.digits.dropLast(1)
if (newDigits.isEmpty() && !es.hasDecimal && es.fracDigits.isEmpty()) {
    state.copy(
        stack = state.stack.withX(0.0),
        entryState = EntryState.Idle,
        stackLiftEnabled = false
    )
} else {
    state.copy(entryState = es.copy(digits = newDigits))
}
```

**Verify all new tests pass. Run full suite — no regressions.**

---

### TDD Loop N4 — Exponent backspace sign-clear-before-revert (§5.10) [TDD plan O4]

**Write this test first:**

```
eExpBackspace_emptyDigits_signSet_clearSignFirst
    Input:  Exponent(mantissaIntPart="1", exponentDigits="", exponentIsNegative=true)
            + pressBackspace
    Expect: still Exponent, exponentIsNegative=false, exponentDigits still ""

eExpBackspace_emptyDigits_noSign_revertsMantissa
    Input:  Exponent(mantissaIntPart="12", exponentDigits="", exponentIsNegative=false)
            + pressBackspace
    Expect: Standard(digits="12")  — existing behavior, confirm still works
```

**Verify first test fails, second passes. Then implement:**

In `EntryStateMachine.pressBackspace`, `is EntryState.Exponent` branch:

```kotlin
is EntryState.Exponent -> when {
    es.exponentDigits.isNotEmpty() ->
        state.copy(entryState = es.copy(exponentDigits = es.exponentDigits.dropLast(1)))
    es.exponentIsNegative ->
        state.copy(entryState = es.copy(exponentIsNegative = false))
    else ->
        state.copy(
            entryState = EntryState.Standard(
                digits = es.mantissaIntPart,
                fracDigits = es.mantissaFracPart,
                hasDecimal = es.mantissaHasDecimal,
                isNegative = es.mantissaIsNegative
            )
        )
}
```

**Verify both tests pass. Run full suite — no regressions.**

---

### TDD Loop N5 — Exponent 2-digit cap verification (§5.9) [TDD plan L3]

The current code already has this guard. Write the test to confirm it, expecting it to pass.

```
eExpDigit_thirdDigit_isNoop
    Input:  Exponent(exponentDigits="42") + pressDigit(1)
    Expect: exponentDigits still "42"
```

If this test passes immediately, mark L3 as [EXISTS-CONFIRMED] in TDD_PLAN_DISPLAY.md
and move on. If it fails, fix before proceeding.

---

## Phase O — DisplayFormatter rewrite

**Goal:** Rewrite all formatting paths to (a) emit the sign-slot prefix on every
string (§7.0), (b) fix all formatter bugs (§7.1–§7.7).

All new tests go in `logic/.../display/spec/DisplayFormatterSpec.kt`.

**Before writing any tests:** update all existing assertions in
`logic/.../display/DisplayFormatterTest.kt` to add the leading `" "` to every
non-negative expected value. Delete `fix2_smallNoOverflow`. Run the suite — all
existing tests should now fail (because the formatter has not changed yet).
This confirms that the assertion update was applied correctly and gives a clean
red baseline. Then proceed loop by loop.

---

### TDD Loop O1 — Sign-slot prefix on Standard entry (§7.0, §7.1) [TDD plan C, D, F, G]

**Write these tests first (in spec file):**

```
formatStandard_positive_hasLeadingSpace
    Input:  Standard(digits="123")
    Expect: " 123"

formatStandard_negative_hasLeadingMinus
    Input:  Standard(digits="5", isNegative=true)
    Expect: "-5"

formatStandard_empty_showsZeroWithSpace
    Input:  Standard(digits="")
    Expect: " 0"

formatStandard_withDecimal_noFrac
    Input:  Standard(digits="3", hasDecimal=true)
    Expect: " 3."

formatStandard_withDecimalAndFrac
    Input:  Standard(digits="1", fracDigits="2", hasDecimal=true)
    Expect: " 1.2"
```

**Verify all fail. Then implement:**

Rename `formatMantissa` → `formatStandard`. Change sign handling from prepending
`"-"` directly to: build unsigned content string first, then prepend `"-"` if
`isNegative` else `" "`.

**Verify all new tests pass. Re-run existing `DisplayFormatterTest` — the entry
state tests (`entry_digits`, `entry_decimal`, etc.) should now pass with the
updated assertions.**

---

### TDD Loop O2 — Sign-slot prefix on Exponent entry (§7.0, §7.2) [TDD plan V3, N3]

**Write these tests first:**

```
formatExponent_noExpDigits_showsTrailingE
    Input:  Exponent("1", "", false, false, "", false)
    Expect: " 1 E"

formatExponent_oneExpDigit_padded
    Input:  Exponent("1", "", false, false, "4", false)
    Expect: " 1 E04"

formatExponent_twoExpDigits
    Input:  Exponent("1", "23", true, false, "04", false)
    Expect: " 1.23 E04"

formatExponent_negativeExp
    Input:  Exponent("1", "", false, false, "5", true)
    Expect: " 1 E-05"

formatExponent_negativeMantissa
    Input:  Exponent("1", "", false, true, "5", false)
    Expect: "-1 E05"
```

**Verify all fail. Then implement:**

In `formatExponent`:
- Build mantissa string without sign
- Build exp suffix: if `exponentDigits.isEmpty()` emit `""`, else `exponentDigits.padStart(2, '0')`
- Assemble: `"$mantissaStr E$expSign$expStr"` (no digits after E if empty)
- Prepend sign slot

**Verify all new tests pass. Run full suite.**

---

### TDD Loop O3 — Sign-slot prefix on Idle/FIX (§7.0, §7.3, §7.4) [TDD plan R, B]

**Write these tests first:**

```
fix2_positive_hasLeadingSpace
    Input:  x=3.14, Fix(2)
    Expect: " 3.14"

fix2_negative_hasLeadingMinus
    Input:  x=-3.14, Fix(2)
    Expect: "-3.14"

fix2_zero_hasLeadingSpace
    Input:  x=0.0, Fix(2)
    Expect: " 0.00"

fix2_negativeZero_treatedAsPositive
    Input:  x=-0.0, Fix(2)
    Expect: " 0.00"

fix2_noSignificantDigit_fallsBackToSci
    Input:  x=0.001, Fix(2)
    Expect: starts with " " and contains "e" (SCI fallback)

fix2_elevenDigitInteger_fallsBackToSci
    Input:  x=12345678901.0, Fix(2)
    Expect: starts with " " and contains "e"

fix2_tenDigitInteger_fits
    Input:  x=1234567890.0, Fix(0)
    Expect: " 1234567890"

fix4_tenDigitInteger_noFracRoom
    Input:  x=1234567890.0, Fix(4)
    Expect: " 1234567890"  (no decimal places fit; rounds off)
```

**Verify all fail. Then implement `formatFix`:**
- Strip `-0.0` → `0.0` at entry
- Compute sign slot char: `" "` or `"-"`
- Work on `abs(v)` internally
- After formatting unsigned value, check: is result all zeros with no sig digit?
  If yes, fall back to `formatSci(v, dp)` (which will also gain a sign slot)
- Guard: `intPartLen > 10` → fall back to SCI
- Prepend sign slot

**Verify all new tests pass. Re-run existing FIX tests (updated assertions). Run full suite.**

---

### TDD Loop O4 — SCI formatter (§7.5) [TDD plan S, A3, A4]

**Write these tests first:**

```
sci2_positive_hasLeadingSpace
    Input:  x=12345.0, Sci(2)
    Expect: " 1.23e+04"

sci2_negative_hasLeadingMinus
    Input:  x=-12345.0, Sci(2)
    Expect: "-1.23e+04"

sci2_zero_hasLeadingSpace
    Input:  x=0.0, Sci(2)
    Expect: " 0.00e+00"

sci7_nCapAt7
    Input:  x=1.23456789, Sci(9)
    Expect: " 1.2345679e+00"  (N=9 capped to 7, 8 sig digits total)

sci4_padWithTrailingZeros
    Input:  x=2.34, Sci(4)
    Expect: " 2.3400e+00"

sci2_negativeZero_noSign
    Input:  x=-0.0, Sci(2)
    Expect: " 0.00e+00"
```

**Verify all fail. Then implement `formatSci`:**
- Cap `dp` to `min(dp, 7)` before any other calculation
- Strip `-0.0` → `0.0`
- Compute and prepend sign slot
- Overflow/underflow paths return error strings without sign slot (unchanged)

**Verify all new tests pass. Re-run existing SCI tests (updated assertions). Run full suite.**

---

### TDD Loop O5 — ENG formatter (§7.6) [TDD plan T, A8]

**Write these tests first:**

```
eng2_positive_hasLeadingSpace
    Input:  x=12345.0, Eng(2)
    Expect: " 12.35e+03"

eng4_threeLeadDigits_capsAt5FracDigits
    Input:  x=123.456789, Eng(4)
    Expect: " 123.46e+00"  (3 int digits + 4 frac = 7 ≤ 8; but 4 fits so show 4)

eng6_threeLeadDigits_capsAt5FracDigits
    Input:  x=123.456789, Eng(6)
    Expect: " 123.46000e+00"  (cap: 8-3=5 frac max, not 6)

eng2_zero_hasLeadingSpace
    Input:  x=0.0, Eng(2)
    Expect: " 0.00e+00"

eng2_negativeZero_noSign
    Input:  x=-0.0, Eng(2)
    Expect: " 0.00e+00"
```

**Verify all fail. Then implement `formatEng`:**
- Change position budget from 10 → 8
- `val maxFrac = 8 - mantissaIntDigits`
- `val cappedDp = minOf(dp, maxOf(0, maxFrac))`
- Strip `-0.0` → `0.0`
- Compute and prepend sign slot

**Verify all new tests pass. Re-run existing ENG tests (updated assertions). Run full suite.**

---

### TDD Loop O6 — ALL formatter (§7.7) [TDD plan U]

**Write these tests first:**

```
all_positive_hasLeadingSpace
    Input:  x=3.14, All
    Expect: " 3.14"

all_integer_hasLeadingSpace
    Input:  x=100.0, All
    Expect: " 100"

all_zero_hasLeadingSpace
    Input:  x=0.0, All
    Expect: " 0"

all_negativeZero_noSign
    Input:  x=-0.0, All
    Expect: " 0"

all_exactInteger_noDecimal
    Input:  x=5.0, All
    Expect: " 5"

all_elevenDigitInt_switchesToSci
    Input:  x=12345678901.0, All
    Expect: starts with " " and contains "e"

all_tinyValue_switchesToSci
    Input:  x=0.0000001, All
    Expect: starts with " " and contains "e"
```

**Verify all fail. Then implement `formatAll`:**
- Strip `-0.0` → `0.0`
- Compute and prepend sign slot
- Verify large/small thresholds produce SCI fallback as expected

**Verify all new tests pass. Re-run existing ALL tests (updated assertions). Run full suite.**

---

### O7 — Error strings (no sign slot)

Verify that the error path (`state.error?.let { return it }`) is unchanged — error
strings have no sign slot prefix. The existing `error_state` test must still pass
with its assertion of `"Error"` (no leading space). Confirm this without changing anything.

---

## Phase P — Engine fixes and `insertThousandsCommas` update

**Goal:** Fix the four format-key commit bugs in `CalculatorEngine`. Update
`insertThousandsCommas` and its tests to handle the sign-slot prefix. These are
the last two pieces that complete the public contract change.

---

### TDD Loop P1 — Format keys commit entry (§6.1) [TDD plan P4–P7]

Tests go in `logic/.../engine/` or the existing `CalculatorViewModelTest.kt` —
wherever engine integration tests live. Write before implementing.

```
fixArg_whileInEntry_commitsThenFormats
    Setup:  pressDigit(5) (entry in progress, Standard("5"))
    Action: pressFixMode(state, 2)
    Expect: entryState = Idle, stack.x = 5.0, displaySettings.mode = Fix(2)

sciArg_whileInEntry_commitsThenFormats
    Setup:  pressDigit(5)
    Action: pressSciMode(state, 2)
    Expect: entryState = Idle, stack.x = 5.0, displaySettings.mode = Sci(2)

engArg_whileInEntry_commitsThenFormats
    Setup:  pressDigit(5)
    Action: pressEngMode(state, 2)
    Expect: entryState = Idle, stack.x = 5.0, displaySettings.mode = Eng(2)

allMode_whileInEntry_commits
    Setup:  pressDigit(5)
    Action: pressAllMode(state)
    Expect: entryState = Idle, stack.x = 5.0, displaySettings.mode = All
```

**Verify all four fail. Then implement in `CalculatorEngine.kt`:**

```kotlin
fun pressFixMode(state: CalculatorState, decimalPlaces: Int): CalculatorState {
    val s = clearErrorIfAny(commitEntry(state))
    return s.copy(displaySettings = DisplaySettings(DisplayMode.Fix(decimalPlaces)), shiftActive = false)
}
```

Apply same pattern to `pressSciMode`, `pressEngMode`, `pressAllMode`.

**Verify all four tests pass. Run full suite.**

---

### P2 — Update `insertThousandsCommas` (§2b)

No TDD loop here — this is an app-layer function with its own test file.

**First: update all test assertions in `app/.../DisplayFormatterTest.kt`**

Every input string that currently lacks a leading sign character must be updated
to have one (`" "` or `"-"`), and every expected output must match. For example:

| Before | After |
|--------|-------|
| `fmt("42")` → `"42"` | `fmt(" 42")` → `" 42"` |
| `fmt("1234567")` → `"1,234,567"` | `fmt(" 1234567")` → `" 1,234,567"` |
| `fmt("-1234.5")` → `"-1,234.5"` | unchanged |
| `fmt("1.234e+07")` → `"1.234e+07"` | `fmt(" 1.234e+07")` → `" 1.234e+07"` |

Run the suite — all `DisplayFormatterTest` tests should now fail.

**Then implement `insertThousandsCommas`:**

```kotlin
fun insertThousandsCommas(plain: String): String {
    if (plain.contains('e', ignoreCase = true)) return plain
    if (plain.isEmpty()) return plain

    val signChar = plain[0]           // always ' ' or '-'
    val rest = plain.substring(1)

    val dotIndex = rest.indexOf('.')
    val intPart  = if (dotIndex >= 0) rest.substring(0, dotIndex) else rest
    val decPart  = if (dotIndex >= 0) rest.substring(dotIndex)    else ""

    if (intPart.length <= 3) return plain

    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return signChar + grouped + decPart
}
```

**Verify all `DisplayFormatterTest` tests pass. Run full suite.**

---

### P3 — Final full-suite run and sign-off

Run the complete test suite across both modules.

Expected counts at sign-off:
- All pre-existing tests pass (with updated assertions where noted)
- All [NEW-PASS] tests from TDD_PLAN_DISPLAY.md pass
- All [NEW-FAIL] tests from TDD_PLAN_DISPLAY.md now pass (bugs fixed)
- The deleted test `fix2_smallNoOverflow` is gone
- The updated test `backspace_emptyMantissa_stays` asserts Idle

---

## Dependency order

```
Phase L (rename)
    ↓
Phase M (interfaces + Hilt bindings)
    ↓
Phase N (ESM behavioral fixes)   ←── can overlap with Phase O once M is done
    ↓
Phase O (formatter rewrite)
    ↓
Phase P (engine fixes + insertThousandsCommas)
```

N and O are independent of each other after M. They can be worked in parallel
if two branches are used, but must both be merged before P begins.

---

## File inventory

| File | Phase | Action |
|------|-------|--------|
| `logic/.../model/EntryState.kt` | L | Rename `Mantissa` → `Standard`; keep `@SerialName` |
| `logic/.../entry/IEntryStateMachine.kt` | M | **New** — interface |
| `logic/.../display/IDisplayFormatter.kt` | M | **New** — interface |
| `logic/.../entry/EntryStateMachine.kt` | M, N | Implement interface; fix EEX/CHS/Backspace/ExpBackspace |
| `logic/.../display/DisplayFormatter.kt` | M, O | Implement interface; sign-slot prefix; all formatter fixes |
| `logic/.../engine/CalculatorEngine.kt` | M, P | Use interfaces; commitEntry in 4 format methods |
| `app/.../di/CalculatorModule.kt` | M | 2 new `@Binds` entries |
| `app/.../ui/calculator/DisplayFormatter.kt` | P | Fix `insertThousandsCommas` for sign-slot prefix |
| `logic/.../entry/spec/EntryStateMachineSpec.kt` | N | **New** — all ESM behavioral tests |
| `logic/.../display/spec/DisplayFormatterSpec.kt` | O | **New** — all formatter behavioral tests |
| `logic/.../display/DisplayFormatterTest.kt` | O | Update assertions (add `" "`); delete `fix2_smallNoOverflow` |
| `logic/.../entry/EntryStateMachineTest.kt` | N | Update `backspace_emptyMantissa_stays` |
| `app/.../ui/calculator/DisplayFormatterTest.kt` | P | Update all assertions for sign-slot prefix |
