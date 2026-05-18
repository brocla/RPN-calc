# Display Rewrite Requirements
Source: Notes on Display.md
Test plan: TDD_PLAN_DISPLAY.md
Date: 2026-05-17

---

## 1. Scope and Goals

Replace the internals of:
- `logic/.../entry/EntryStateMachine.kt`
- `logic/.../display/DisplayFormatter.kt`

Fix behavioral bugs in:
- `logic/.../engine/CalculatorEngine.kt` (format-key commit, backspace-Idle guard)

Update the data model:
- `logic/.../model/EntryState.kt` — rename `Mantissa` to `Standard`

Extract interfaces (DI preference):
- `EntryStateMachine` → `IEntryStateMachine`
- `DisplayFormatter` → `IDisplayFormatter`

The public contract with the `:app` module does **not** change in signature. `CalculatorEngine.getDisplay()` still returns a plain `String`. All other `CalculatorEngine` methods have the same signatures. However, the *content* of the string returned by `getDisplay()` changes: it now always begins with a sign-slot character (see §7.0). This affects `insertThousandsCommas` in `:app` and all test assertions in both modules.


---

## 2. Boundary with the `:app` module

### 2a. Sign slot — handled in the formatter (logic layer)
The formatter always emits a sign-slot character at position 0 of the returned string:
- `" "` (space) for zero or positive values
- `"-"` for negative values

This means `DisplayPanel` renders one string as before — no composable split needed. The DSEG7 font renders a space in the sign position as a blank lamp, matching the physical HP hardware behavior.

### 2b. `insertThousandsCommas` update (app layer)
Because the formatter now always prepends a sign-slot character, `insertThousandsCommas` must treat position 0 as the sign slot and never include it in the digit-grouping logic.

**Current behavior:** strips a leading `"-"` if present, groups the rest, puts `"-"` back.

**Required behavior:** always peel off character at index 0 (either `" "` or `"-"`), group the remaining string (index 1 onwards) exactly as before, then prepend the peeled character.

```kotlin
fun insertThousandsCommas(plain: String): String {
    if (plain.contains('e', ignoreCase = true)) return plain
    if (plain.isEmpty()) return plain

    val signChar = plain[0]          // always ' ' or '-'
    val rest = plain.substring(1)

    val dotIndex = rest.indexOf('.')
    val intPart  = if (dotIndex >= 0) rest.substring(0, dotIndex) else rest
    val decPart  = if (dotIndex >= 0) rest.substring(dotIndex)    else ""

    if (intPart.length <= 3) return plain

    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return signChar + grouped + decPart
}
```

All test assertions in `app/.../DisplayFormatterTest.kt` must be updated to supply and expect a leading sign-slot character. For example:
- `fmt("42")` → `fmt(" 42")`, expects `" 42"`
- `fmt("-1234.5")` → `fmt("-1234.5")`, expects `"-1,234.5"`
- `fmt(" 1234567")` → expects `" 1,234,567"`


---

## 3. Data structure changes

### 3a. Rename `EntryState.Mantissa` → `EntryState.Standard`

**File:** `logic/.../model/EntryState.kt`

Rename the class. Keep `@SerialName("mantissa")` unchanged to preserve DataStore backwards compatibility. No field changes.

```kotlin
@Serializable @SerialName("mantissa")   // SerialName stays "mantissa" — do NOT change
data class Standard(
    val digits: String,
    val fracDigits: String = "",
    val hasDecimal: Boolean = false,
    val isNegative: Boolean = false
) : EntryState()
```

All call sites in `EntryStateMachine`, `DisplayFormatter`, `CalculatorEngine`, and tests must be updated from `EntryState.Mantissa` to `EntryState.Standard`. The rename is mechanical — grep for `EntryState.Mantissa` and `is EntryState.Mantissa`.

### 3b. No other model changes

`DisplayMode`, `DisplaySettings`, `CalculatorState`, `Stack`, `AngleMode` are unchanged.


---

## 4. Interface extraction (DI)

### 4a. `IEntryStateMachine`

**New file:** `logic/.../entry/IEntryStateMachine.kt`

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

`EntryStateMachine` implements `IEntryStateMachine`. `CalculatorEngine` is injected with `IEntryStateMachine`.

### 4b. `IDisplayFormatter`

**New file:** `logic/.../display/IDisplayFormatter.kt`

```kotlin
interface IDisplayFormatter {
    fun format(state: CalculatorState): String
}
```

`DisplayFormatter` implements `IDisplayFormatter`. `CalculatorEngine` is injected with `IDisplayFormatter`.

### 4c. Hilt bindings

In `di/CalculatorModule.kt`, add bindings for the two new interfaces. `EntryStateMachine` and `DisplayFormatter` are pure-Kotlin classes in the `:logic` module; they are bound `@Singleton` in the Hilt graph exactly as `ConstantsRepository` is.

```kotlin
@Binds @Singleton
abstract fun bindEntryStateMachine(impl: EntryStateMachine): IEntryStateMachine

@Binds @Singleton
abstract fun bindDisplayFormatter(impl: DisplayFormatter): IDisplayFormatter
```

`CalculatorEngine`'s constructor becomes:

```kotlin
class CalculatorEngine @Inject constructor(
    private val entryStateMachine: IEntryStateMachine,
    private val mathOperations: MathOperations,
    private val displayFormatter: IDisplayFormatter
)
```


---

## 5. EntryStateMachine behavioral requirements

Each requirement below references the corresponding test IDs in TDD_PLAN_DISPLAY.md.

### 5.1 Idle → Standard: digit key
No change from current behavior. [TDD C1–C4]

### 5.2 Idle → Standard: decimal key
No change from current behavior. [TDD D1]

### 5.3 Idle → Exponent: EEX key
No change from current behavior. [TDD E1]

### 5.4 Standard: digit key — 10-digit limit
The combined count of `digits.length + fracDigits.length` must not exceed 10. If it is already 10, the keystroke is silently ignored. Current behavior is correct. [TDD F1–F6]

Note: the 10-position budget applies to what is *visible*. Fractional digits are stored after the decimal point; if EEX is later pressed, digits in positions 9 and 10 are cleared from display but their values are retained internally (they are still in `fracDigits`). This is the current behavior and does not change.

### 5.5 Standard: decimal key
No change. Already a no-op when `hasDecimal = true`. [TDD G1–G4]

### 5.6 Standard: EEX key — guards [TDD H2, H3, H5]

**Rule 1 — zero-value guard:**
If the current mantissa value is zero (digits is empty, or `"0"`, and there are no frac digits with a non-zero character), treat the press as if coming from Idle: produce `Exponent(mantissaIntPart="1", ...)`. Do not pass through `digits="0"`.

**Rule 2 — 9-digit integer guard:**
If `digits.length > 8`, the EEX keystroke is silently ignored. The significand has only 8 positions available in Exponent mode. Note: this guard checks `digits.length` (the integer part) only, not `fracDigits`. If the integer part is ≤ 8, EEX is allowed regardless of how many fractional digits there are; fractional digits beyond position 8 will simply not appear in the exponent display.

Implementation note: these two checks are evaluated in order. Check zero first, then check length.

### 5.7 Standard: CHS key — zero guard [TDD I3, I4]

**Rule:** If the current mantissa value is zero, `pressChs` is a no-op. A negative sign must never be attached to a zero value.

Zero is defined as: `digits` is empty or `"0"`, AND `fracDigits` contains only `'0'` characters or is empty.

This guard applies only within `EntryStateMachine.pressChs`. The `CalculatorEngine.pressChs` already guards Idle-mode zero via `-0.0` behavior; that is unchanged.

### 5.8 Standard: Backspace key — last-digit transition to Idle [TDD J4]

**Rule:** When backspace would reduce the mantissa to having no digits at all (i.e., `digits` would become empty after the drop AND `hasDecimal` is false AND `fracDigits` is empty), instead of leaving the state as `Standard(digits="")`, transition to `Idle` with `stack.x = 0.0` and `stackLiftEnabled = false`.

This is the one place where Backspace can transition from Entry to Idle, as specified in Notes on Display.md.

The existing test `backspace_emptyMantissa_stays` asserts `Standard(digits="")` as the result, which contradicts the spec. That test must be updated to assert `Idle` with `x=0`.

### 5.9 Exponent: digit key — 2-digit cap
Already enforced (`exponentDigits.length >= 2` check). [TDD L3 — current code is correct]

Wait — re-reading current code: `if (es.exponentDigits.length >= 2) return state`. This IS correct. TDD_PLAN_DISPLAY.md L3 marked this [NEW-FAIL] in error; verify with a test before assuming it is broken.

### 5.10 Exponent: Backspace — sign-clear-before-revert [TDD O4]

**Current behavior:** When `exponentDigits` is empty, backspace immediately reverts to `Standard` (Mantissa), regardless of whether `exponentIsNegative` is set.

**Required behavior:** When `exponentDigits` is empty AND `exponentIsNegative` is true, backspace first clears the sign (`exponentIsNegative = false`) and stays in `Exponent`. Only when `exponentDigits` is empty AND `exponentIsNegative` is false does backspace revert to `Standard`.

### 5.11 All other ESM behaviors
No change from current implementation. This includes:
- `pressDecimal` in Exponent (no-op)
- `pressEex` in Exponent (no-op)
- `completeEntry` / `currentDisplayValue` / `parseMantissa` / `parseExponent`


---

## 6. CalculatorEngine behavioral requirements

### 6.1 Format keys must commit entry [TDD P4–P7]

`pressFixMode`, `pressSciMode`, `pressEngMode`, `pressAllMode` currently update `displaySettings` without committing any in-progress entry. The result is that the entered number is discarded when the format changes.

**Required:** Each of these four methods must call `commitEntry(state)` before updating `displaySettings`. The committed value becomes the new `stack.x`, then the format is applied.

```kotlin
fun pressFixMode(state: CalculatorState, decimalPlaces: Int): CalculatorState {
    val s = clearErrorIfAny(commitEntry(state))
    return s.copy(
        displaySettings = DisplaySettings(DisplayMode.Fix(decimalPlaces)),
        shiftActive = false
    )
}
```

Apply the same pattern to `pressSciMode`, `pressEngMode`, `pressAllMode`.

### 6.2 No other engine changes


---

## 7. DisplayFormatter behavioral requirements

All formatting is string-operations-based. No position counter is maintained in code.

### 7.0 Sign-slot prefix — applies to ALL format paths

Every string returned by `format()` must start with exactly one sign-slot character:
- `"-"` if the value or entry state is negative
- `" "` (space) otherwise

This applies to Idle (all display modes), Standard entry, and Exponent entry. It does **not** apply to error strings — errors are returned as-is without a sign slot (e.g. `"Error"`, `"Overflow"`).

The sign-slot character is prepended as the final step in each private format method. Each method works on the unsigned/unsigned representation internally and prepends the sign at the end.

**Impact on existing test assertions:** Every expected string in `logic/.../DisplayFormatterTest.kt` that does not currently start with `"-"` must gain a leading `" "`. For example:
- `assertEquals("3.14", ...)` → `assertEquals(" 3.14", ...)`
- `assertEquals("0.00", ...)` → `assertEquals(" 0.00", ...)`
- `assertEquals("-3.14", ...)` — unchanged, already starts with `"-"`
- `assertEquals("Error", ...)` — unchanged, errors have no sign slot

### 7.1 Entry state: formatStandard (was formatMantissa)

Rename `formatMantissa` → `formatStandard`. Apply the §7.0 sign-slot prefix rule. Digit content rules unchanged:
- Empty digits → `"0"` (unsigned part)
- Digits, no decimal → digits
- Decimal set, no frac digits → digits + `"."`
- Decimal set, frac digits → digits + `"."` + fracDigits

Sign slot prepended last: `"-"` if `isNegative`, else `" "`.

Examples after this change:
- `Standard("123")` → `" 123"`
- `Standard("1", "2", true, false)` → `" 1.2"`
- `Standard("5", isNegative=true)` → `"-5"`
- `Standard("")` → `" 0"`

### 7.2 Entry state: formatExponent — partial entry display [TDD V3]

**Current behavior:** When `exponentDigits` is empty, `padStart(2, '0')` produces "00", giving "1 E00" even before any exponent digits have been typed.

**Required behavior:** If `exponentDigits` is empty, omit the exponent digits entirely. Display as `"<mantissa> E"` (with trailing space before E and no digits after). If `exponentDigits` has one or two digits, pad to two with a leading zero as now.

Apply the §7.0 sign-slot prefix rule. Examples after this change:
- `Exponent("1", "", false, false, "", false)` → `" 1 E"`
- `Exponent("1", "", false, false, "4", false)` → `" 1 E04"`
- `Exponent("1", "23", true, false, "04", false)` → `" 1.23 E04"`
- `Exponent("1", "", false, true, "5", true)` → `"-1 E-05"`

### 7.3 Idle state: negative zero suppression [TDD B5, B6]

**Rule:** If `value == -0.0`, treat it as `0.0` before formatting in all modes. This is already done in `formatValue` via `val v = if (value == 0.0) 0.0 else value`, but `value == 0.0` is true for both `0.0` and `-0.0` in Kotlin/JVM, so the existing guard is correct. Verify the guard works for SCI and ALL modes; if tests B5/B6 fail, the guard is being bypassed somewhere.

### 7.4 FIX mode [TDD R5–R9]

This formatter uses **strict fixed with SCI fallback** (HP-41C / HP-35s behavior): FIX N means exactly N decimal places. If the value cannot be represented with at least one visible significant digit within those N places, fall back to SCI. There is no adaptive expansion of dp (that is HP-12C behavior and is not used here).

**Rule — no-significant-digit fallback:**
After applying the decimal-place cap, if the formatted string (excluding sign, decimal point, and commas) consists entirely of zeros and would show no significant digit, fall back to `formatSci(v, dp)`.

Example: `v=0.001, dp=2` → formatted is `"0.00"` → all zeros, no sig digit visible → fall back to SCI.

The existing test `fix2_smallNoOverflow` (which asserts `"0.001"` for `v=0.001, Fix(2)`) must be **deleted** — it asserts adaptive behavior. It is replaced by TDD_PLAN_DISPLAY.md R7.

**Rule — 11-digit integer fallback:** [TDD A2]
If `"%.0f".format(abs(v)).length > 10`, fall back to SCI. Current code checks `signWidth + intPartLen > 10` which means it falls back when int part is 11 digits for positive or 10 for negative. Verify correctness: a negative 10-digit integer has sign in slot 0 and digits in slots 1-10, so it fits. The guard should be `intPartLen > 10` (ignore signWidth for this threshold).

### 7.5 SCI mode [TDD S3–S8, A3, A4]

**N cap at 7:** If `dp > 7`, reduce to `dp = 7` before formatting. The significand has 8 positions (1 leading digit + up to 7 fractional). This must be enforced before any further calculation.

**Zero-padding:** `"%.${cappedDp}e".format(v)` in Kotlin/JVM already pads trailing zeros in the mantissa. Verify the existing code does this; if the test S5 fails, the formatting call is being trimmed before output.

**Zero display:** For `v=0.0, Sci(2)`, the result should be `"0.00e+00"`. The current code's zero branch constructs `"0.${"0".repeat(cappedDp)}e+00"`. Verify this produces the correct form.

**Overflow/underflow:** Already detected (`if (absExp > 99) return if (expIsNegative) "Underflow" else "Overflow"`). Verify tests S7/S8 pass; the current code appears to handle this.

### 7.6 ENG mode [TDD T5–T8, A8]

**Total digit cap (int + frac ≤ 8):**
ENG has 1–3 integer digits in the significand. The significand position budget is **8** — the same as SCI — because positions 9–11 are always occupied by the exponent sign and two exponent digits. "Significand length" means integer digits + fractional digits; the decimal point is zero-width and does not count. After computing `mantissaIntDigits` (1, 2, or 3), calculate:

```
val maxFrac = 8 - mantissaIntDigits
val cappedDp = minOf(dp, maxOf(0, maxFrac))
```

The current code uses 10 as the position budget for ENG, which is wrong. Change 10 → 8 in the `maxDp` calculation.

**Zero display:** Same structure as SCI. For `v=0.0, Eng(4)`, result should be `"0.0000e+00"`.

**Overflow/underflow:** Already detected. Verify.

### 7.7 ALL mode [TDD U4–U8]

**SCI fallback threshold — large values:** [TDD U6]
If the plain-decimal formatted string's integer part has more than 10 digits, fall back to SCI. The current `formatAll` uses `"%.10g"` which may auto-switch, but the threshold and fallback format must be verified.

**SCI fallback threshold — small values:** [TDD U7]
If the number is so small that a plain-decimal representation would require leading zeros that consume all 10 positions without showing any significant digit, fall back to SCI. The existing `"%.10g"` format handles this implicitly; verify with tests.

**Trailing-zero suppression:** Already implemented. No change.

**Integer values — no decimal:** Already implemented. `"100.0"` → `"100"`. No change.

### 7.8 Digit budget summary

| Mode     | Sign  | Integer digits | Sep | Fractional digits | Exponent sign | Exp digits |
|----------|-------|---------------:|-----|------------------:|:-------------:|:----------:|
| Standard | pos 0 |            1–10 | "." |              0–9  |      —        |     —      |
| Exponent | pos 0 |              1–8 | "." |           0–(8-n) |     pos 9     |   pos 10–11 |
| FIX idle | pos 0 |            1–10 | "." |              0–N  |      —        |     —      |
| SCI idle | pos 0 |              1   | "." |              0–7  |     pos 9     |   pos 10–11 |
| ENG idle | pos 0 |            1–3   | "." |     0–(8-intLen)  |     pos 9     |   pos 10–11 |
| ALL idle | pos 0 |            1–10 | "." |           0–(9-n) |   if needed   |   if needed |

Sign, decimal point, and commas are zero-width — they do not count against any position budget.


---

## 8. What is NOT changing

- `CalculatorState` — no field changes
- `DisplayMode` — no changes
- `DisplaySettings` — no changes
- `Stack` — no changes
- `MathOperations` — no changes
- `CalculatorEngine` public method signatures — no changes (except internal body of 4 format methods)
- `CalculatorEngine.getDisplay()` return type — still `String`
- `@SerialName("mantissa")` on the renamed `Standard` class — kept for DataStore compatibility
- `DisplayPanel` composable — no changes (renders one string as before)
- All non-display `CalculatorEngine` methods — no changes


---

## 9. Files to create or modify

| File | Action |
|------|--------|
| `logic/.../model/EntryState.kt` | Rename `Mantissa` → `Standard` (keep `@SerialName`) |
| `logic/.../entry/IEntryStateMachine.kt` | Create interface |
| `logic/.../entry/EntryStateMachine.kt` | Implement interface; fix §5.6–§5.10 |
| `logic/.../display/IDisplayFormatter.kt` | Create interface |
| `logic/.../display/DisplayFormatter.kt` | Implement interface; fix §7.0–§7.7 (sign-slot prefix + all formatter bugs) |
| `logic/.../engine/CalculatorEngine.kt` | Use interfaces; add commitEntry to 4 format methods |
| `app/.../di/CalculatorModule.kt` | Add 2 `@Binds` entries for new interfaces |
| `app/.../ui/calculator/DisplayFormatter.kt` | Fix `insertThousandsCommas` to treat position 0 as sign slot (§2b) |
| `app/.../ui/calculator/DisplayFormatterTest.kt` | Update all test inputs/assertions for sign-slot prefix (§2b) |
| `logic/.../display/DisplayFormatterTest.kt` | Update all non-negative assertions to have leading `" "`; delete `fix2_smallNoOverflow` |
| `logic/.../entry/EntryStateMachineTest.kt` | Update `backspace_emptyMantissa_stays` to assert Idle; rename Mantissa → Standard |
| All other test files referencing `EntryState.Mantissa` | Update to `EntryState.Standard` |


---

## 10. Test plan reference

Do not create new tests for behavior marked [EXISTS] in TDD_PLAN_DISPLAY.md. For the 34 [NEW-FAIL] tests, write each test before implementing its fix (red → green). For [NEW-PASS] tests, write them and confirm they pass before the fix cycle begins; if any [NEW-PASS] test fails, investigate before proceeding.

All new tests go in sub-packages:
- `logic.display.spec.DisplayFormatterSpec`
- `logic.entry.spec.EntryStateMachineSpec`

See TDD_PLAN_DISPLAY.md §A–§W for full test specifications.
