# RPN Calculator — Requirements Document

## 1. Overview

A Reverse Polish Notation (RPN) calculator for Android, styled after the HP-10C series (10C, 11C, 12C, 15C, 16C). The app runs in landscape-only orientation. Programmability features are omitted; those key positions are repurposed for other functions. There is a single shift key to minimize visual clutter.

---

## 2. Platform

| Property | Value |
|---|---|
| minSdk | 35 (Android 15) |
| targetSdk | 35 (Android 15) |
| Orientation | Landscape, locked |
| UI toolkit | Jetpack Compose |
| Target device | Moto G5 Power (2024), 2400 × 1080 px |
| Android Studio | Panda 4 \| 2025.3.4 Patch 1 |
| Kotlin | 2.2.10 |
| Gradle | 9.4.1 |
| Android Gradle Plugin | 9.2.1 |
| Compose BOM | 2026.02.01 |
| Hilt | 2.59.2 (minimum; 2.56.x incompatible with AGP 9.x) |
| KSP | 2.2.10-2.0.2 (version format: `{kotlin}-2.0.{n}`) |
| JUnit | 4.13.2 |
| Test framework | JUnit 4 with `kotlin.test` |

**AGP 9.x build constraints:**
- Do not add `kotlin-android` plugin to `:app` — AGP 9.x bundles Kotlin internally; adding it separately causes a duplicate-extension build failure.
- Add `android.disallowKotlinSourceSets=false` to `gradle.properties` to allow KSP to register generated source sets under AGP 9.x.

**Architecture constraints:**
- All calculator logic (stack, entry state machine, math operations, display formatting) lives in a separate `:logic` Gradle module with no Android dependencies. The `:app` module depends on `:logic`; the reverse is forbidden. This makes the logic independently testable with plain JVM unit tests.
- Math operations return a `CalcResult` sealed type (`Value(Double)` or `Err(String)`) rather than throwing exceptions. Callers pattern-match on the result. This keeps error handling explicit and eliminates try/catch throughout the engine.

**State serialization:** State persistence (§12) is implemented via `kotlinx.serialization` with JSON encoding into `SavedStateHandle`. All persisted model classes must be annotated `@Serializable`.

---

## 3. Number Representation

- Internal precision: `Double` (64-bit IEEE 754, approximately 15 significant digits).
- Maximum enterable exponent: ±99.
- Negative zero is always displayed as `0`, never as `-0`.

---

## 4. Display

### 4.1 Capacity

- The display holds exactly **10 character positions**. The character count is computed by counting every character in the formatted string *except* the decimal point (`.`) and comma (`,`) separators — those are rendered in the font without consuming a digit position.
- A negative sign (`-`) counts as one character position, leaving 9 positions for digits and exponent characters.
- A formatted string that would exceed 10 characters (by this count) is never shown. The formatter must cap or fall back before returning a string.
- The display font uses the DSEG7 seven-segment style.

### 4.1.1 Digit Grouping

- Digits to the left of the decimal point are grouped in threes with comma separators, HP-41C style (e.g. `1,234,567.89`).
- Commas are rendered using the zero-width comma glyph in the modified DSEG7 font so they do not consume digit positions.
- Grouping is applied to the formatted string in the UI layer only; the `:logic` module is unaware of it.

### 4.2 Display Modes

Four display modes are supported. The active mode persists across sessions.

#### FIX — Fixed Decimal

- Shows a fixed number of decimal places: configurable 0–9.
- **Decimal-place capping:** if the requested number of decimal places would make the string exceed 10 display characters, the formatter silently reduces decimal places to the maximum that fits. Example: FIX 9 for `100` would produce 12 characters; the formatter caps to 7 decimal places (`100.0000000`), which is exactly 10.
- **Fallback to SCI:** if the integer part of the value alone (plus sign for negatives) already exceeds 10 characters — i.e., the integer has more than 10 digits for a positive value, or more than 9 digits for a negative value — the display falls back to SCI format using the same requested decimal places (which SCI will also cap to fit).
- **Small-value expansion:** if the value is so small that the requested decimal places would produce all zeros (e.g. `0.000001` in FIX 2 shows `0.00`), the formatter expands the decimal places just enough to show the first significant digit. If even the expanded form would exceed 10 characters, it falls back to SCI.
- Example: `0.001` in FIX 2 displays as `0.001`; `1.23e+47` in FIX 2 falls back to SCI.

#### SCI — Scientific Notation

- Format: `M.DDDDDe±EE` where the number of digits after the decimal (D) is configurable 0–9.
- **10-character cap:** the total display width (mantissa + exponent suffix, excluding `.`) must not exceed 10 characters. The formatter computes the maximum permitted decimal places as `9 − signWidth − expSuffixWidth`, where `expSuffixWidth` is 4 for two-digit exponents (`e+47`) or 5 for three-digit exponents (`e+308`). The requested decimal places are silently capped to this maximum.
- **Exponent range:** if the absolute value of the exponent exceeds 99, the value cannot be displayed. The formatter returns `"Overflow"` for exponents > +99 and `"Underflow"` for exponents < −99. These strings are treated as display errors (see §10).
- Example: SCI 3 → `1.234e+07`; SCI 9 for `1.0` → `1.00000e+00` (capped at 5 decimal places to fit).

#### ENG — Engineering Notation

- Same as SCI except the exponent is always a multiple of 3. The mantissa therefore ranges from 1 to 999.
- **10-character cap:** the formatter computes `maxDp = 10 − signWidth − mantissaIntDigits − expSuffixWidth` and silently caps the requested decimal places. The mantissa can be 1–3 integer digits, so the budget varies.
- **Exponent range:** same ±99 limit and `"Overflow"` / `"Underflow"` behaviour as SCI.
- Example: ENG 2 with value 12,345 → `12.35e+03`; ENG 4 for `100` → `100.000e+00` (capped at 3 decimal places to fit).

#### ALL — All Significant Digits

- Displays all significant digits with no trailing zeros, up to the display width.
- **Fixed-notation path:** `%.10g` produces up to 10 significant digits without an exponent for values in the displayable range. Trailing zeros are trimmed, but only from the fractional part — integer trailing zeros (e.g. the `0` in `9999999990`) are preserved.
- **Scientific-notation path:** when `%.10g` produces an exponent form, trailing zeros are trimmed from the mantissa. If the result still exceeds 10 display characters (because the value has no trailing zeros and the exponent is large), the mantissa is shortened to the maximum significant digits that fit.
- **Fallback:** if the integer part alone exceeds 10 characters, ALL falls back to SCI notation exactly as FIX does.

### 4.3 Mode Annunciators

The display area shows small indicators for:
- Active display mode (FIX / SCI / ENG / ALL)
- Active angle mode (DEG / RAD)
- Shift key latch active

---

## 5. The Stack

### 5.1 Structure

The stack is 4 registers deep: **X** (bottom), **Y**, **Z**, **T** (top). All registers are initialized to `0.0` on first launch. No register is ever empty; there is no stack underflow.

### 5.2 Stack-Lift Behavior (Authentic HP)

A **stack-lift flag** tracks whether the next digit entry should lift the stack:

- **Flag enabled**: beginning a new number — by pressing a digit key, the decimal point key, or the EEX key — while in Idle state pushes T←Z←Y←X before starting entry. The flag is cleared after the lift.
- **Flag disabled**: the same transitions in Idle state overwrite X in place (no lift).

Note: the decimal point (`.`) and EEX keys initiate entry exactly like a digit key and must check the stack-lift flag in the same way. Omitting this causes `.5` entered after an operation to overwrite X rather than lifting the stack.

**Entry state model:** Number entry passes through three internal states: **Idle** (not entering a number), **Mantissa** (entering integer/fractional digits), and **Exponent** (entering exponent digits after EEX). Keys behave differently in each state. Operations commit any in-progress entry to X before executing.

Operations that **enable** the stack-lift flag:
- Completing a number with ENTER
- Completing any unary or binary operation
- RCL
- Roll Down, X↔Y, Last X

Operations that **disable** the stack-lift flag:
- ENTER (after duplicating X into Y)
- CLX

### 5.3 ENTER Key

1. Copies X into Y; Y→Z; Z→T (T is lost).
2. Disables the stack-lift flag (so the next digit entry overwrites X).

### 5.4 Last X Register

- A single **Last X** register stores the value of X immediately before any math operation is executed.
- Retrievable via the (shifted) Last X key, which pushes the saved value onto the stack (with stack lift).
- STO and RCL do **not** update Last X.

### 5.5 Stack Operations

| Operation | Behavior |
|---|---|
| Roll Down | X←Y←Z←T←X (circular roll down) |
| X↔Y | Swaps X and Y; Z and T unchanged |
| CLX | Sets X to 0; disables stack-lift flag; does not update Last X |

---

## 6. Memory Registers

- Ten memory registers, addressed 0–9.
- All registers initialized to `0.0` on first launch.
- No register arithmetic (no STO+, STO−, etc.).
- STO and RCL are unshifted primary keys.

| Operation | Behavior |
|---|---|
| STO n | Copies X into register n; stack unchanged |
| RCL n | Pushes register n onto the stack (with stack lift); register unchanged |

After pressing STO or RCL the calculator waits for a digit key (0–9) to identify the register. Any other key cancels the operation.

---

## 7. Number Entry

### 7.1 Digit Entry

- Digits 0–9 and a decimal point key are available.
- Maximum entry length: 10 digits (decimal point does not count toward the limit).
- Pressing a second decimal point during entry is a no-op.
- Leading zeros are suppressed: entering `0`, `0`, `5` produces `5`.
- Pressing `.` as the first key of an entry begins a number with an implicit leading zero. The display shows `0.` and subsequent digits fill the fractional part.

### 7.2 Backspace

- During digit entry: deletes the last entered digit (or the decimal point).
- When not in entry mode: no-op. (Use CLX to clear X.)

### 7.3 CHS — Change Sign

- During entry: toggles the sign of the mantissa being entered.
- Outside entry mode: negates X in place. Does **not** update Last X (CHS is a sign toggle, not a math operation).

### 7.4 EEX — Enter Exponent

- Switches entry to exponent input mode.
- The user then types up to two digits for the exponent magnitude.
- CHS during exponent entry toggles the exponent sign.
- Backspace during exponent entry removes the last exponent digit.
- Exponent range: ±99.
- Pressing EEX from Idle initiates entry. If the stack-lift flag is enabled, the stack lifts before entry begins, exactly as for a digit or decimal press from Idle.

---

## 8. Math Operations

All binary operations consume X and Y, compute the result, place it in X, and drop the stack (Z→Y, T→Z, T unchanged). All operations update Last X to the pre-operation value of X before executing.

**On error:** Last X is updated to the pre-operation value of X. X is set to `0.0`. Y, Z, T, and memory registers are preserved. The error message is shown on the display (see §10).

### 8.1 Arithmetic

| Key | Operation |
|---|---|
| + | Y + X |
| − | Y − X |
| × | Y × X |
| ÷ | Y ÷ X |

### 8.2 Powers and Logarithms

| Key | Shift? | Operation |
|---|---|---|
| 1/x | No | 1 ÷ X |
| √x | No | √X |
| x² | Yes | X² |
| 10^x | No | 10^X |
| LOG x | Yes | log₁₀(X) |
| eˣ | No | eˣ |
| LN x | Yes | ln(X) |
| yˣ | No | Y^X |

### 8.3 Constants

| Key | Shift? | Operation |
|---|---|---|
| π | No | Pushes π onto the stack (stack lifts) |

### 8.4 Percentage

HP convention: Y is the base value, X is the percentage figure.

Both `%` and `Δ%` are unary operations on X that use Y as a parameter. Y is read but not consumed — it remains in the Y register after the operation unchanged. The stack does not drop.

| Key | Shift? | Operation | Stack effect |
|---|---|---|---|
| % | No | X ← Y × X ÷ 100 | Y preserved, stack does not lift |
| Δ% | Yes | X ← (X − Y) ÷ Y × 100 | Y preserved, stack does not lift |

### 8.5 Trigonometry

Angles are interpreted and returned in the active angle mode (DEG or RAD).

| Key | Shift? | Operation |
|---|---|---|
| SIN | No | sin(X) |
| COS | No | cos(X) |
| TAN | No | tan(X) |
| SIN⁻¹ | Yes | arcsin(X) |
| COS⁻¹ | Yes | arccos(X) |
| TAN⁻¹ | Yes | arctan(X) |

### 8.6 Combinatorics

Y = n (total), X = r (chosen). Inputs must be non-negative integers with n ≥ r.

| Key | Shift? | Operation |
|---|---|---|
| nCr | No | C(Y, X) = Y! ÷ (X! × (Y−X)!) |
| nPr | Yes | P(Y, X) = Y! ÷ (Y−X)! |

### 8.7 Factorial

| Key | Shift? | Operation |
|---|---|---|
| n! | No | X! where X must be a non-negative integer |

### 8.8 Polar / Rectangular Conversion

Angles use the active angle mode (DEG or RAD). Two separate shifted functions are required:

| Function | Input (Y, X) | Output (Y, X) |
|---|---|---|
| →P (to Polar) | Y = y-coordinate, X = x-coordinate | Y = angle, X = radius |
| →R (to Rectangular) | Y = angle, X = radius | Y = y-coordinate, X = x-coordinate |

---

## 9. Angle Mode

Two modes: **DEG** (degrees) and **RAD** (radians). Default: DEG.

- Toggled with a shifted key.
- The active mode is shown as an annunciator on the display.
- The mode persists across sessions.

---

## 10. Error Handling

### 10.1 Error Conditions

| Condition | Example |
|---|---|
| Division by zero | X ÷ 0 |
| Square root of negative | √(−4) |
| LOG of zero or negative | LOG(0), LOG(−1) |
| LN of zero or negative | LN(0), LN(−1) |
| Inverse trig out of domain | arcsin(2) |
| Overflow | Result > ~10⁹⁹ or Double overflow |
| yˣ with negative base and non-integer exponent | (−2)^0.5 |
| Factorial of non-integer or negative integer | 2.5!, (−3)! |
| nCr / nPr with non-integer inputs or n < r | C(3, 5) |

### 10.2 Error Response

1. An error message is shown on the X-register display in place of a number.
2. The **entire calculator state** (stack, memory, entry state, display mode, angle mode) is frozen at its value immediately before the error occurred. No values are modified.
3. **Any key press** dismisses the error and restores the calculator to its exact pre-error state. The key press is **consumed** — it only clears the error; it does not also execute its normal function. The user must press the key a second time to execute it.
4. The display format is not changed when an error is displayed, and is not changed when the error is cleared.

**Implementation note:** When an error is introduced, the pre-error `CalculatorState` is saved in `CalculatorUiState.savedState`. On the first key tap, `savedState` is restored as the current state and `savedState` is cleared to `null`. This mechanism applies equally to math errors (division by zero, domain errors, etc.) and to display-range errors (Overflow, Underflow).

### 10.3 Display-Range Errors

When a result cannot be displayed because the exponent magnitude exceeds 99 (the limit of the SCI/ENG display format), the display shows:

- **`Overflow`** — when the exponent is > +99
- **`Underflow`** — when the exponent is < −99

These are treated identically to math errors: the pre-error state is saved, the message is shown, and any key tap restores the pre-error state exactly.

Display-range errors are detected at the formatter level: `formatSci` and `formatEng` return the string `"Overflow"` or `"Underflow"` when the exponent is out of range, and the ViewModel promotes those strings to `state.error` via `finalizeState()`.

---

## 11. Key Reference

### 11.1 Shift Key Behavior (Latch)

- Tapping SHIFT activates the shift latch; the shift annunciator illuminates.
- Tapping SHIFT again while already active leaves the latch active (it does not toggle off). This matches HP-1xC behavior — SHIFT is not a toggle, it only activates.
- Tapping any key with a shifted function executes that function and deactivates the latch.
- Tapping any key with no shifted function is a no-op and deactivates the latch.

### 11.2 Key Label Typography

Primary labels use Helvetica (bundled `.ttf`). The characters `x`, `y`, `Y`, and `ˣ` (U+02E3 modifier letter small x) render in Times New Roman Italic at the same nominal size, to match HP-1xC styling. The `√x` key uses a custom drawn radical symbol (Canvas-based composable) rather than a text glyph.

Each key has two label positions:
- **Shifted label**: smaller text, top of key, always visible, dims when shift is inactive.
- **Primary label**: larger text, vertically centered in the remaining key area.

### 11.3 Complete Key List

| Key Label | Shift? | Function |
|---|---|---|
| 0–9 | No | Digit entry |
| . | No | Decimal point entry |
| ENTER | No | Duplicate X into Y; disable stack-lift flag |
| SHIFT | No | Activate shift latch |
| Backspace | No | Delete last entered digit during entry; no-op otherwise |
| CLX | No | Clear X to 0; disable stack-lift flag |
| CHS | No | Change sign of X (or mantissa during entry) |
| EEX | No | Begin exponent entry |
| Roll↓ | No | Roll stack down (X←Y←Z←T←X) |
| X↔Y | No | Swap X and Y |
| STO | No | Store X into register 0–9 (waits for digit) |
| RCL | No | Recall register 0–9 to X (waits for digit) |
| + | No | Y + X |
| − | No | Y − X |
| × | No | Y × X |
| ÷ | No | Y ÷ X |
| 1/x | No | 1 ÷ X |
| √x | No | √X |
| 10^x | No | 10^X |
| eˣ | No | e^X |
| yˣ | No | Y^X |
| π | No | Push π |
| % | No | Y × X ÷ 100 → X |
| SIN | No | sin(X) |
| COS | No | cos(X) |
| TAN | No | tan(X) |
| nCr | No | Combinations C(Y, X) |
| n! | No | X factorial |
| FIX n | Yes | Set FIX display mode; waits for digit 0–9 |
| SCI n | Yes | Set SCI display mode; waits for digit 0–6 |
| ENG n | Yes | Set ENG display mode; waits for digit 0–6 |
| ALL | Yes | Set ALL display mode |
| Last X | Yes | Push Last X register onto stack |
| x² | Yes | X² |
| LOG | Yes | log₁₀(X) |
| LN | Yes | ln(X) |
| Δ% | Yes | (X − Y) ÷ Y × 100 → X |
| SIN⁻¹ | Yes | arcsin(X) |
| COS⁻¹ | Yes | arccos(X) |
| TAN⁻¹ | Yes | arctan(X) |
| DEG/RAD | Yes | Toggle angle mode |
| nPr | Yes | Permutations P(Y, X) |
| →P | Yes | Rectangular to Polar conversion |
| →R | Yes | Polar to Rectangular conversion |

---

## 12. State Persistence

The following state survives app backgrounding, process death, and relaunch:

- X, Y, Z, T stack registers
- Last X register
- Memory registers 0–9
- Active display mode and decimal-place setting
- Active angle mode

Implementation: `ViewModel` with `SavedStateHandle`. State is encoded as JSON using `kotlinx.serialization`.

The following state is **not** persisted and resets on every launch:
- Shift latch state (always resets to inactive)
- Entry state (any in-progress number entry is discarded; X retains its last committed value)
- Error state (cleared on relaunch)

---

## 13. User Experience

- **Haptic feedback** on every key press using `HapticFeedbackType.LongPress`. Other types (`TextHandleMove`, `VirtualKey`) are inaudible on the target device.
- **Error recovery**: any key press clears the error state.
- **Shift latch annunciator**: visible indicator when shift is active.
- **Angle mode annunciator**: DEG or RAD always visible.

---

## 14. Assets Required

| Asset | Format | Notes |
|---|---|---|
| App launcher icon | Adaptive icon (PNG foreground + background per density bucket) | Required by Android; `mipmap-anydpi` XML references `@mipmap/` not `@drawable/` |
| Display font | DSEG7 (.ttf) | Seven-segment LCD appearance; modified to include a zero-width comma glyph (U+002C) for digit grouping — advance width 0 so commas do not consume digit positions; glyph is a period dot plus a slightly descending tail styled to match the seven-segment aesthetic |
| Primary key label font | Helvetica (.ttf) | Bundled as `res/font/helvetica.ttf`; used for all key labels except characters listed in §11.2 |
| Italic key label font | Times New Roman Italic (.ttf) | Bundled as `res/font/timesi.ttf`; used for `x`, `y`, `Y`, `ˣ` per §11.2 |

---

## 15. Architecture

### 15.1 Module structure

- `:logic` — pure Kotlin module. Contains all calculator state, entry logic, math operations, and display formatting. No Android or Compose imports permitted.
- `:app` — Android module. Contains all Compose UI, ViewModel, DI, and resources. Depends on `:logic`.

### 15.2 State flow

`CalculatorState` is an immutable data class. All state transitions are pure functions that accept a `CalculatorState` and return a new `CalculatorState`. No mutable state exists in the logic layer.

### 15.3 Entry state machine

Number entry is handled by a dedicated `EntryStateMachine` class, separate from `CalculatorEngine`. `EntryStateMachine` manages the three entry states (Idle, Mantissa, Exponent). `CalculatorEngine` calls `commitEntry()` at the start of every operation to finalize any in-progress number.

### 15.4 ViewModel

A single `CalculatorViewModel` holds `CalculatorUiState` as a `StateFlow`. The UI is stateless — it reads from the flow and sends `CalcKeyEvent` values. State is persisted via `SavedStateHandle`.

---

## 16. Development Sequence

- **Phase A — Logic and Math**: Implement stack, memory registers, number-entry state machine, display formatting, and all math operations as plain Kotlin classes with no Android dependencies. Prove correctness with JUnit 4 / `kotlin.test` unit tests.
- **Phase B — UI**: Build the Jetpack Compose interface: display panel, key grid, shift latch state, annunciators, haptic feedback, landscape layout sized for the target device.
