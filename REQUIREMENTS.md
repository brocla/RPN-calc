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
| JUnit | 4.13.2 |
| Test framework | JUnit 4 with `kotlin.test` |

---

## 3. Number Representation

- Internal precision: `Double` (64-bit IEEE 754, approximately 15 significant digits).
- Maximum enterable exponent: ±99.
- Negative zero is always displayed as `0`, never as `-0`.

---

## 4. Display

### 4.1 Capacity

- The display holds up to 10 digits plus a sign character.
- The display font uses the DSEG7 seven-segment style.

### 4.1.1 Digit Grouping

- Digits to the left of the decimal point are grouped in threes with comma separators, HP-41C style (e.g. `1,234,567.89`).
- Commas are rendered using the zero-width comma glyph in the modified DSEG7 font so they do not consume digit positions.
- Grouping is applied to the formatted string in the UI layer only; the `:logic` module is unaware of it.

### 4.2 Display Modes

Four display modes are supported. The active mode persists across sessions.

#### FIX — Fixed Decimal

- Shows a fixed number of decimal places: configurable 0–9.
- If the value cannot be shown within the 10-digit display width at the configured decimal places, the display automatically switches to scientific notation for that value (identical to HP behavior).
- Example: `0.001` in FIX 2 displays as `0.001` (expands past 2 places to show the value); if there is no room it shows in SCI format.

#### SCI — Scientific Notation

- Format: `M.DDDDDe±EE` where the number of digits after the decimal (D) is configurable 0–6.
- Three characters are reserved for the exponent and its sign.
- Example: SCI 3 → `1.234e+07`

#### ENG — Engineering Notation

- Same as SCI except the exponent is always a multiple of 3. The mantissa therefore ranges from 1 to 999.
- Digits after the decimal point in the mantissa: configurable 0–6.
- Example: ENG 2 with value 12,345 → `12.35e+03`

#### ALL — All Significant Digits

- Displays all significant digits with no trailing zeros, up to the display width.
- Follows standard HP "ALL" mode behavior.

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

- **Flag enabled**: pressing a digit pushes T←Z←Y←X before writing the new digit to X.
- **Flag disabled**: pressing a digit overwrites X in place (no lift).

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

---

## 8. Math Operations

All binary operations consume X and Y, compute the result, place it in X, and drop the stack (Z→Y, T→Z, T unchanged). All operations update Last X to the pre-operation value of X before executing.

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

1. An error message is shown on the display in place of a number.
2. X is set to `0.0`.
3. Y, Z, T, and all memory registers are preserved.
4. Last X is updated to the value of X before the operation.
5. **Any key press** dismisses the error and returns to normal operation.

---

## 11. Key Reference

### 11.1 Shift Key Behavior (Latch)

- Tapping SHIFT activates the shift latch; the shift annunciator illuminates.
- Tapping SHIFT again while already active leaves the latch active (it does not toggle off).
- Tapping any key with a shifted function executes that function and deactivates the latch.
- Tapping any key with no shifted function is a no-op and deactivates the latch.

### 11.2 Complete Key List

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

Implementation: `ViewModel` with `SavedStateHandle`.

---

## 13. User Experience

- **Haptic feedback** on every key press.
- **Error recovery**: any key press clears the error state.
- **Shift latch annunciator**: visible indicator when shift is active.
- **Angle mode annunciator**: DEG or RAD always visible.

---

## 14. Assets Required

| Asset | Format | Notes |
|---|---|---|
| App launcher icon | Adaptive icon (vector XML foreground + background) | Required by Android |
| Display font | DSEG7 (.ttf or .otf) | Seven-segment LCD appearance; modified to include a zero-width comma glyph (U+002C) for digit grouping |
| Key label font | Roboto Condensed (system) or bundled .ttf | For primary and shifted key labels |

---

## 15. Development Sequence

- **Phase A — Logic and Math**: Implement stack, memory registers, number-entry state machine, display formatting, and all math operations as plain Kotlin classes with no Android dependencies. Prove correctness with JUnit 4 / `kotlin.test` unit tests.
- **Phase B — UI**: Build the Jetpack Compose interface: display panel, key grid, shift latch state, annunciators, haptic feedback, landscape layout sized for the target device.
