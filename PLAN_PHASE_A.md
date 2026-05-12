# Phase A Implementation Plan — Logic and Math

## Overview

Phase A produces a pure Kotlin/JVM module (`:logic`) containing all calculator logic: data
models, stack operations, math, display formatting, and number entry. It has zero Android
dependencies and is fully covered by JUnit 4 / `kotlin.test` unit tests written test-first
(TDD Red → Green → Refactor). The `:app` module depends on `:logic`; no logic lives in `:app`
during this phase.

Dependency injection is done by manual constructor injection throughout. No DI framework.

---

## Architecture

```
CalculatorEngine          (orchestrator — one method per key press)
    │
    ├── EntryStateMachine (number entry digit-by-digit)
    ├── MathOperations    (all math; returns CalcResult)
    └── DisplayFormatter  (formats CalculatorState → display String)

CalculatorState           (immutable data class — single source of truth)
    ├── Stack             (immutable data class: x, y, z, t)
    ├── EntryState        (sealed class: Idle | Mantissa | Exponent)
    ├── DisplaySettings   (sealed class mode + decimal places)
    ├── AngleMode         (enum: DEG | RAD)
    ├── memory            (List<Double>, 10 elements)
    ├── lastX             (Double)
    ├── stackLiftEnabled  (Boolean)
    └── error             (String?)

CalcResult                (sealed class: Value | Err — returned by MathOperations)
```

Every key press is modelled as a pure function:
```
(CalculatorState, input) → CalculatorState
```
No mutable state anywhere. `CalculatorEngine` holds no state; it transforms it.

---

## File Structure

```
logic/
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/brocla/rpn_calc/logic/
    │   ├── model/
    │   │   ├── AngleMode.kt
    │   │   ├── CalcResult.kt
    │   │   ├── CalculatorState.kt
    │   │   ├── DisplayMode.kt
    │   │   ├── EntryState.kt
    │   │   └── Stack.kt
    │   ├── math/
    │   │   └── MathOperations.kt
    │   ├── display/
    │   │   └── DisplayFormatter.kt
    │   ├── entry/
    │   │   └── EntryStateMachine.kt
    │   └── engine/
    │       └── CalculatorEngine.kt
    └── test/kotlin/com/brocla/rpn_calc/logic/
        ├── model/
        │   └── StackTest.kt
        ├── math/
        │   └── MathOperationsTest.kt
        ├── display/
        │   └── DisplayFormatterTest.kt
        ├── entry/
        │   └── EntryStateMachineTest.kt
        └── engine/
            └── CalculatorEngineTest.kt
```

---

## Step A1 — Gradle Scaffolding

**Goal:** The `:logic` module exists, compiles, and is depended on by `:app`. No logic written yet.

### Changes to existing files

**`settings.gradle.kts`** — add:
```
include(":logic")
```

**`gradle/libs.versions.toml`** — add entries:
- Plugin: `kotlin-jvm` using the existing `kotlin` version ref
- Library: `kotlin-test` using the existing `kotlin` version ref

**`app/build.gradle.kts`** — add to dependencies:
```
implementation(project(":logic"))
```

### New file: `logic/build.gradle.kts`

Applies the `kotlin("jvm")` plugin (via version catalog alias). Test dependencies:
`libs.junit` and `kotlin("test")`. No Android dependencies.

### Acceptance criteria
- `./gradlew :logic:build` succeeds with no source files yet (empty source set is fine).
- `./gradlew :app:build` succeeds with the new dependency.

---

## Step A2 — Core Model Types

Pure data/sealed classes. No behavior; minimal tests confirm defaults and construction.

### Write tests first: `StackTest.kt` (partial — defaults only)

| Test | Assertion |
|---|---|
| `defaultStack_allZero` | x=0.0, y=0.0, z=0.0, t=0.0 |
| `defaultCalculatorState_stackLiftDisabled` | stackLiftEnabled = false |
| `defaultCalculatorState_angleModeIsDeg` | angleMode = AngleMode.DEG |
| `defaultCalculatorState_displayIsFixFour` | displaySettings.mode = DisplayMode.Fix(4) |
| `defaultCalculatorState_memoryAllZero` | all 10 registers = 0.0 |
| `defaultCalculatorState_noError` | error = null |
| `defaultCalculatorState_entryStateIsIdle` | entryState = EntryState.Idle |
| `defaultCalculatorState_shiftNotActive` | shiftActive = false |

### Implement

**`AngleMode.kt`**
```
enum class AngleMode { DEG, RAD }
```

**`DisplayMode.kt`** — sealed class:
- `Fix(decimalPlaces: Int)` — 0..9
- `Sci(decimalPlaces: Int)` — 0..6
- `Eng(decimalPlaces: Int)` — 0..6
- `All` — object

**`CalcResult.kt`** — sealed class:
- `Value(value: Double)`
- `Err(message: String)`

**`Stack.kt`** — data class with x, y, z, t all defaulting to 0.0. Includes stack
manipulation extension functions (see Step A3 for their test-driven development):
- `lift()` — makes room for a new X: returns Stack(x=x, y=x, z=y, t=z). Old T is lost.
- `drop()` — binary-op drop: T replicates. Returns Stack(x=y, y=z, z=t, t=t).
- `rollDown()` — circular: Stack(x=y, y=z, z=t, t=x).
- `swap()` — Stack(x=y, y=x, z=z, t=t).
- `withX(v: Double)` — Stack(x=v, y=y, z=z, t=t).
- `applyBinaryResult(result: Double)` — Stack(x=result, y=z, z=t, t=t). Stack drops, T replicates.
- `applyUnaryResult(result: Double)` — Stack(x=result, y=y, z=z, t=t).

**`EntryState.kt`** — sealed class:
- `Idle` — object
- `Mantissa(digits: String, hasDecimal: Boolean, isNegative: Boolean)` — digits are the raw
  digit characters only (no sign, no decimal point).
- `Exponent(mantissaDigits: String, mantissaHasDecimal: Boolean, mantissaIsNegative: Boolean,
  exponentDigits: String, exponentIsNegative: Boolean)`

**`CalculatorState.kt`** — data class with all defaults as above, including `shiftActive: Boolean = false`.

### Acceptance criteria
All A2 tests green.

---

## Step A3 — Stack Operations

Test-drive the Stack extension functions defined in Step A2.

### Write tests first: add to `StackTest.kt`

**lift()**

| Test | Setup | Assertion |
|---|---|---|
| `lift_shiftsYZT` | Stack(x=1.0, y=2.0, z=3.0, t=4.0).lift() | y=1.0, z=2.0, t=3.0 |
| `lift_xUnchanged` | same | x=1.0 |
| `lift_oldTLost` | same | t≠4.0 |

**drop() / applyBinaryResult()**

| Test | Setup | Assertion |
|---|---|---|
| `binaryResult_xIsResult` | Stack(x=2.0,y=3.0,z=5.0,t=7.0).applyBinaryResult(9.0) | x=9.0 |
| `binaryResult_yGetsOldZ` | same | y=5.0 |
| `binaryResult_zGetsOldT` | same | z=7.0 |
| `binaryResult_tReplicates` | same | t=7.0 |

**applyUnaryResult()**

| Test | Setup | Assertion |
|---|---|---|
| `unaryResult_xIsResult` | Stack(x=2.0,y=3.0,z=5.0,t=7.0).applyUnaryResult(9.0) | x=9.0 |
| `unaryResult_yzTUnchanged` | same | y=3.0, z=5.0, t=7.0 |

**rollDown()**

| Test | Setup | Assertion |
|---|---|---|
| `rollDown_xGetsOldY` | Stack(1,2,3,4).rollDown() | x=2.0 |
| `rollDown_yGetsOldZ` | same | y=3.0 |
| `rollDown_zGetsOldT` | same | z=4.0 |
| `rollDown_tGetsOldX` | same | t=1.0 |

**swap()**

| Test | Setup | Assertion |
|---|---|---|
| `swap_xGetsOldY` | Stack(1,2,3,4).swap() | x=2.0 |
| `swap_yGetsOldX` | same | y=1.0 |
| `swap_ztUnchanged` | same | z=3.0, t=4.0 |

**withX()**

| Test | Setup | Assertion |
|---|---|---|
| `withX_setsX` | Stack(1,2,3,4).withX(9.0) | x=9.0 |
| `withX_yztUnchanged` | same | y=2.0, z=3.0, t=4.0 |

### Acceptance criteria
All A3 tests green.

---

## Step A4 — MathOperations

The largest step. `MathOperations` is a class with no constructor dependencies. It uses
`kotlin.math` throughout. All methods return `CalcResult`.

Exception: `toPolar` and `toRectangular` return `Pair<CalcResult, CalcResult>` (first=new Y,
second=new X) since both stack registers are replaced.

### Write tests first: `MathOperationsTest.kt`

Instantiate once: `val math = MathOperations()`

**Arithmetic**

| Test | Input | Expected |
|---|---|---|
| `add_happyPath` | y=3.0, x=2.0 | Value(5.0) |
| `subtract_happyPath` | y=5.0, x=3.0 | Value(2.0) |
| `subtract_negativeResult` | y=3.0, x=5.0 | Value(-2.0) |
| `multiply_happyPath` | y=3.0, x=4.0 | Value(12.0) |
| `divide_happyPath` | y=6.0, x=2.0 | Value(3.0) |
| `divide_byZero` | y=6.0, x=0.0 | Err |
| `divide_negativeByZero` | y=-6.0, x=0.0 | Err |

**Powers and roots**

| Test | Input | Expected |
|---|---|---|
| `sqrt_positive` | x=9.0 | Value(3.0) |
| `sqrt_zero` | x=0.0 | Value(0.0) |
| `sqrt_negative` | x=-1.0 | Err |
| `square_positive` | x=3.0 | Value(9.0) |
| `square_negative` | x=-3.0 | Value(9.0) |
| `reciprocal_nonzero` | x=4.0 | Value(0.25) |
| `reciprocal_zero` | x=0.0 | Err |
| `power_normalCase` | y=2.0, x=10.0 | Value(1024.0) |
| `power_negativeBaseIntegerExp` | y=-2.0, x=3.0 | Value(-8.0) |
| `power_negativeBaseFractionalExp` | y=-2.0, x=0.5 | Err |
| `power_zeroToZero` | y=0.0, x=0.0 | Err |
| `pow10_normal` | x=3.0 | Value(1000.0) |
| `pow10_overflow` | x=999.0 | Err |

**Logarithms**

| Test | Input | Expected |
|---|---|---|
| `log10_positive` | x=100.0 | Value(2.0) |
| `log10_zero` | x=0.0 | Err |
| `log10_negative` | x=-1.0 | Err |
| `ln_positive` | x=Math.E | Value(1.0) |
| `ln_zero` | x=0.0 | Err |
| `ln_negative` | x=-1.0 | Err |
| `exp_normal` | x=1.0 | Value(Math.E) |
| `exp_overflow` | x=1000.0 | Err |

**Trigonometry — DEG mode**

| Test | Input | Expected |
|---|---|---|
| `sin_zero_deg` | x=0.0, DEG | Value(0.0) |
| `sin_90_deg` | x=90.0, DEG | Value(1.0) |
| `sin_180_deg` | x=180.0, DEG | Value(≈0.0) |
| `cos_zero_deg` | x=0.0, DEG | Value(1.0) |
| `cos_90_deg` | x=90.0, DEG | Value(≈0.0) |
| `cos_180_deg` | x=180.0, DEG | Value(-1.0) |
| `tan_zero_deg` | x=0.0, DEG | Value(0.0) |
| `tan_45_deg` | x=45.0, DEG | Value(≈1.0) |
| `tan_90_deg` | x=90.0, DEG | Err (infinity) |
| `arcsin_zero_deg` | x=0.0, DEG | Value(0.0) |
| `arcsin_one_deg` | x=1.0, DEG | Value(90.0) |
| `arcsin_outOfRange` | x=2.0, DEG | Err |
| `arccos_one_deg` | x=1.0, DEG | Value(0.0) |
| `arccos_zero_deg` | x=0.0, DEG | Value(90.0) |
| `arccos_outOfRange` | x=-2.0, DEG | Err |
| `arctan_one_deg` | x=1.0, DEG | Value(45.0) |
| `arctan_large` | x=1e15, DEG | Value(≈90.0) |

**Trigonometry — RAD mode**

| Test | Input | Expected |
|---|---|---|
| `sin_halfPi_rad` | x=π/2, RAD | Value(1.0) |
| `cos_pi_rad` | x=π, RAD | Value(-1.0) |
| `arcsin_one_rad` | x=1.0, RAD | Value(π/2) |

**Percentage**

| Test | Input | Expected |
|---|---|---|
| `percent_of` | y=200.0, x=15.0 | Value(30.0) |
| `percent_of_zero_base` | y=0.0, x=15.0 | Value(0.0) |
| `percentChange_increase` | y=100.0, x=150.0 | Value(50.0) |
| `percentChange_decrease` | y=200.0, x=100.0 | Value(-50.0) |
| `percentChange_zeroBase` | y=0.0, x=5.0 | Err (divide by zero) |

**Combinatorics**

| Test | Input | Expected |
|---|---|---|
| `factorial_zero` | x=0.0 | Value(1.0) |
| `factorial_five` | x=5.0 | Value(120.0) |
| `factorial_nonInteger` | x=2.5 | Err |
| `factorial_negative` | x=-1.0 | Err |
| `factorial_overflow` | x=171.0 | Err |
| `combinations_5c2` | y=5.0, x=2.0 | Value(10.0) |
| `combinations_5c0` | y=5.0, x=0.0 | Value(1.0) |
| `combinations_5c5` | y=5.0, x=5.0 | Value(1.0) |
| `combinations_nLessThanR` | y=3.0, x=5.0 | Err |
| `combinations_nonInteger` | y=5.1, x=2.0 | Err |
| `permutations_5p2` | y=5.0, x=2.0 | Value(20.0) |
| `permutations_nLessThanR` | y=3.0, x=5.0 | Err |

**Polar / Rectangular**

Note: results use `assertNear` (tolerance 1e-10) for floating-point comparisons.

| Test | Input | Expected first (new Y) | Expected second (new X) |
|---|---|---|---|
| `toPolar_xAxis` | y=0.0, x=1.0, DEG | angle=0.0 | radius=1.0 |
| `toPolar_yAxis` | y=1.0, x=0.0, DEG | angle=90.0 | radius=1.0 |
| `toPolar_rad` | y=1.0, x=1.0, RAD | angle=π/4 | radius=√2 |
| `toRect_zeroAngle` | y=0.0, x=1.0, DEG | y-coord=0.0 | x-coord=1.0 |
| `toRect_90deg` | y=90.0, x=1.0, DEG | y-coord=1.0 | x-coord≈0.0 |

**Constants**

| Test | Expected |
|---|---|
| `pi_value` | Math.PI |

### Implement: `MathOperations.kt`

One public method per operation. Private helper `toRadians(x, mode)` /
`fromRadians(x, mode)` for angle conversion. Overflow is detected by checking
`result.isInfinite() || result.isNaN()`.

Error message strings are defined as internal constants so tests can reference them if needed,
but tests should check for `is CalcResult.Err` rather than exact message text, to keep tests
decoupled from error wording.

### Acceptance criteria
All A4 tests green.

---

## Step A5 — DisplayFormatter

`DisplayFormatter` is a class with no constructor dependencies. Its single public method:

```kotlin
fun format(state: CalculatorState): String
```

Rules:
1. If `state.error != null`, return the error string.
2. If `state.entryState != EntryState.Idle`, format the entry buffer (see below).
3. Otherwise format `state.stack.x` according to `state.displaySettings`.
4. Negative zero: if the formatted value would be `-0`, return `0`.

**FIX overflow rule:** if a value cannot fit in 10 characters at the configured decimal places,
fall back to SCI with the same number of decimal places (capped at 6).

**Entry buffer formatting:**
- `Mantissa`: show digits with sign and decimal point as typed. If empty, show `0`.
- `Exponent`: show mantissa portion `E` exponent digits (e.g., `1.23 E04`).
  The exact separator character is a display detail; return a parseable string for testing.

### Write tests first: `DisplayFormatterTest.kt`

Instantiate once: `val fmt = DisplayFormatter()`

Helper: `fun state(x: Double, mode: DisplayMode) = CalculatorState(stack=Stack(x=x), displaySettings=DisplaySettings(mode))`

**FIX mode**

| Test | Input | Expected |
|---|---|---|
| `fix2_normal` | 3.14159, Fix(2) | "3.14" |
| `fix2_rounds` | 3.145, Fix(2) | "3.15" |
| `fix0_rounds` | 3.7, Fix(0) | "4" |
| `fix2_zero` | 0.0, Fix(2) | "0.00" |
| `fix2_negative` | -3.14, Fix(2) | "-3.14" |
| `fix9_tenDigits` | 1.23456789, Fix(9) | "1.234567890" |
| `fix2_largeOverflow` | 1.23e15, Fix(2) | fallback SCI |
| `fix2_smallNoOverflow` | 0.001, Fix(2) | "0.001" |
| `fix2_tooSmallOverflow` | 1.23e-15, Fix(2) | fallback SCI |

**SCI mode**

| Test | Input | Expected |
|---|---|---|
| `sci2_normal` | 12345.0, Sci(2) | "1.23e+04" |
| `sci0_normal` | 12345.0, Sci(0) | "1e+04" |
| `sci2_negative` | -12345.0, Sci(2) | "-1.23e+04" |
| `sci2_small` | 0.00123, Sci(2) | "1.23e-03" |
| `sci2_exactlyOne` | 1.0, Sci(2) | "1.00e+00" |

**ENG mode**

| Test | Input | Expected |
|---|---|---|
| `eng2_thousands` | 12345.0, Eng(2) | "12.35e+03" |
| `eng2_millions` | 1234567.0, Eng(2) | "1.23e+06" |
| `eng2_units` | 1.23, Eng(2) | "1.23e+00" |
| `eng2_small` | 0.00123, Eng(2) | "1.23e-03" |

**ALL mode**

| Test | Input | Expected |
|---|---|---|
| `all_noTrailingZeros` | 3.14, All | "3.14" |
| `all_integer` | 100.0, All | "100" |
| `all_pi` | Math.PI, All | "3.141592653" (10 sig digits) |

**Special cases**

| Test | Input | Expected |
|---|---|---|
| `negativeZero_fix2` | -0.0, Fix(2) | "0.00" |
| `error_state` | state with error="Error" | "Error" |

**Entry state formatting**

| Test | EntryState | Expected display |
|---|---|---|
| `entry_digits` | Mantissa("123", false, false) | "123" |
| `entry_decimal` | Mantissa("12", true, false) | "12." |
| `entry_negative` | Mantissa("5", false, true) | "-5" |
| `entry_empty` | Mantissa("", false, false) | "0" |
| `entry_exponent` | Exponent("123",false,false,"04",false) | "1.23 E04" |
| `entry_negExp` | Exponent("1",false,false,"5",true) | "1 E-05" |

### Implement: `DisplayFormatter.kt`

Use `String.format` / `kotlin.math` for rounding and formatting.

### Acceptance criteria
All A5 tests green.

---

## Step A6 — EntryStateMachine

`EntryStateMachine` is a class with no constructor dependencies. All methods take a
`CalculatorState` and return a new `CalculatorState`. No method mutates anything.

### Public API

```kotlin
class EntryStateMachine {
    fun pressDigit(state: CalculatorState, digit: Int): CalculatorState
    fun pressDecimal(state: CalculatorState): CalculatorState
    fun pressChs(state: CalculatorState): CalculatorState
    fun pressEex(state: CalculatorState): CalculatorState
    fun pressBackspace(state: CalculatorState): CalculatorState
    fun completeEntry(state: CalculatorState): CalculatorState
    fun currentDisplayValue(state: CalculatorState): Double  // for internal use by engine
}
```

### Key behaviors

**`pressDigit`**:
- If `Idle` and `stackLiftEnabled`: lift stack, set entryState = `Mantissa(digit.toString(), ...)`
- If `Idle` and not `stackLiftEnabled`: set entryState = `Mantissa(digit.toString(), ...)`
- If `Mantissa` and digit count < 10: append digit
- If `Mantissa` and digit count = 10: no-op
- If `Exponent` and exponent digit count < 2: append digit; 2 digits already: no-op
- Suppress leading zeros: if digits is "0" or "" and new digit is 0, stay as "0"

**`pressDecimal`**: only valid in `Mantissa` state. If no decimal yet, set `hasDecimal=true`.
  If already has decimal: no-op. If `Idle`: begin Mantissa with decimal ("0.").

**`pressChs`**:
- `Mantissa`: toggle `isNegative`
- `Exponent`: toggle `exponentIsNegative`
- `Idle`: handled by `CalculatorEngine` (not entry state machine)

**`pressEex`**: only valid from `Mantissa` or `Idle`. Moves to `Exponent` state, carrying
  current mantissa data. If called from `Idle`, mantissa is treated as "1".

**`pressBackspace`**:
- `Mantissa` with digits: remove last digit. If digits becomes empty and no decimal:
  remain in `Mantissa("")` (displays as "0").
- `Mantissa` with only a decimal: remove it, `hasDecimal=false`.
- `Exponent` with exponent digits: remove last exponent digit.
- `Exponent` with no exponent digits: revert to `Mantissa` state.
- `Idle`: no-op.

**`completeEntry`**: if `Idle`, returns state unchanged. Otherwise, parses the entry buffer
  into a `Double`, sets `stack = stack.withX(parsedValue)`, sets `entryState = Idle`,
  sets `stackLiftEnabled = true`.

### Write tests first: `EntryStateMachineTest.kt`

Instantiate: `val esm = EntryStateMachine()`

Base state helper: `val idle = CalculatorState()`

**pressDigit from Idle**

| Test | Initial state | Action | Assertion |
|---|---|---|---|
| `digit_fromIdle_noLift` | idle (liftEnabled=false) | pressDigit(1) | entryState=Mantissa("1",...), stack.x=0 (not lifted) |
| `digit_fromIdle_withLift` | idle.copy(stackLiftEnabled=true, stack=Stack(x=5.0)) | pressDigit(1) | stack.y=5.0 (lifted) |
| `digit_maxTenDigits` | Mantissa with 10 digits | pressDigit(1) | entryState unchanged |
| `digit_suppressLeadingZero` | Mantissa("0",...) | pressDigit(0) | digits remains "0" |
| `digit_appendsNormally` | Mantissa("12",...) | pressDigit(3) | digits="123" |

**pressDecimal**

| Test | Initial | Assertion |
|---|---|---|
| `decimal_fromIdle` | idle | Mantissa("", hasDecimal=true) |
| `decimal_fromMantissa` | Mantissa("3") | hasDecimal=true |
| `decimal_twice_noop` | Mantissa("3", hasDecimal=true) | unchanged |

**pressChs**

| Test | Initial | Assertion |
|---|---|---|
| `chs_mantissa_positive` | Mantissa("3", isNegative=false) | isNegative=true |
| `chs_mantissa_negative` | Mantissa("3", isNegative=true) | isNegative=false |
| `chs_exponent_positive` | Exponent(..., exponentIsNegative=false) | exponentIsNegative=true |
| `chs_exponent_negative` | Exponent(..., exponentIsNegative=true) | exponentIsNegative=false |

**pressEex**

| Test | Initial | Assertion |
|---|---|---|
| `eex_fromMantissa` | Mantissa("12", false, false) | Exponent("12", false, false, "", false) |
| `eex_fromIdle` | idle | Exponent("1", false, false, "", false) |

**pressBackspace**

| Test | Initial | Assertion |
|---|---|---|
| `backspace_removesLastDigit` | Mantissa("123") | Mantissa("12") |
| `backspace_removesDecimal` | Mantissa("3", hasDecimal=true) | Mantissa("3", hasDecimal=false) |
| `backspace_emptyMantissa_stays` | Mantissa("") | Mantissa("") |
| `backspace_exponentDigit` | Exponent(..., exponentDigits="04") | exponentDigits="0" |
| `backspace_exponentEmpty_revertsMantissa` | Exponent("12",false,false,"",false) | Mantissa("12") |
| `backspace_idle_noop` | idle | unchanged |

**completeEntry**

| Test | Initial | Assertion |
|---|---|---|
| `complete_idle_unchanged` | idle | same state |
| `complete_integer` | Mantissa("42") | stack.x=42.0, Idle, liftEnabled=true |
| `complete_decimal` | Mantissa("3", hasDecimal=true, then "14") | stack.x=3.14, Idle |
| `complete_negative` | Mantissa("5", isNegative=true) | stack.x=-5.0 |
| `complete_withExponent` | Exponent("1","23",false,false,"02",false) | stack.x=1.23e+02 |
| `complete_negativeExponent` | Exponent("1","0",false,false,"03",true) | stack.x=1.0e-03 |

### Acceptance criteria
All A6 tests green.

---

## Step A7 — CalculatorEngine

The orchestrator. Constructor-injected dependencies:

```kotlin
class CalculatorEngine(
    private val entryStateMachine: EntryStateMachine,
    private val mathOperations: MathOperations,
    private val displayFormatter: DisplayFormatter
)
```

One public method per key listed in the requirements. All return `CalculatorState`.

Internal helper `private fun commitEntry(state: CalculatorState): CalculatorState` — calls
`entryStateMachine.completeEntry(state)` before any operation that needs a resolved X value.

Internal helper `private fun applyUnary(state: CalculatorState, op: (Double) -> CalcResult): CalculatorState`
— commits entry, saves lastX, applies op, on Err sets error + clears X, on Value applies
unary result. Sets stackLiftEnabled=true.

Internal helper `private fun applyBinary(state: CalculatorState, op: (Double, Double) -> CalcResult): CalculatorState`
— commits entry, saves lastX, applies op with (stack.y, stack.x), on Err sets error + clears
X, on Value applies binary result (stack drops, T replicates). Sets stackLiftEnabled=true.

### Write tests first: `CalculatorEngineTest.kt`

Instantiate:
```kotlin
val engine = CalculatorEngine(EntryStateMachine(), MathOperations(), DisplayFormatter())
fun s() = CalculatorState()
```

**Basic arithmetic sequences**

| Test | Key sequence | Expected X |
|---|---|---|
| `add_twoNumbers` | 2 ENTER 3 + | 5.0 |
| `subtract_twoNumbers` | 5 ENTER 3 − | 2.0 |
| `multiply_twoNumbers` | 3 ENTER 4 × | 12.0 |
| `divide_twoNumbers` | 6 ENTER 2 ÷ | 3.0 |
| `divide_byZero_error` | 6 ENTER 0 ÷ | error≠null, X=0.0 |

**Stack lift behavior**

| Test | Key sequence | Assertion |
|---|---|---|
| `stackLift_disabledAfterEnter` | ENTER then digit 5 | X=5, Y=0 (not 5 again) |
| `stackLift_enabledAfterOp` | 2 ENTER 3 + then digit 5 | Y=5.0 pushed onto stack |
| `enter_duplicatesX` | digit 3, ENTER | X=3, Y=3 |
| `enter_tripleEnter` | 3 ENTER ENTER ENTER | X=3, Y=3, Z=3, T=3 |

**CLX**

| Test | Key sequence | Assertion |
|---|---|---|
| `clx_clearsX` | digit 5, CLX | X=0.0 |
| `clx_preservesYZT` | 5 ENTER 3 CLX | Y=5.0, X=0.0 |
| `clx_disablesLift` | CLX, digit 7 | X=7.0, stack not lifted |

**Last X**

| Test | Key sequence | Assertion |
|---|---|---|
| `lastX_savedBeforeOp` | 3 ENTER 5 + then LAST_X | lastX=5.0, X pushed=5.0 |
| `lastX_notSavedBySto` | 7 ENTER STO 0 then LAST_X | lastX=0.0 (default) |
| `lastX_notSavedByChs` | 7 then CHS, LAST_X | lastX=0.0 |

**Roll Down and Swap**

| Test | Key sequence | Assertion |
|---|---|---|
| `rollDown_correct` | 1 ENTER 2 ENTER 3 ENTER 4 Roll↓ | X=3.0, Y=2.0, Z=1.0, T=4.0 |
| `swap_correct` | 1 ENTER 2 X↔Y | X=1.0, Y=2.0 |

**Memory**

| Test | Key sequence | Assertion |
|---|---|---|
| `sto_rcl_roundtrip` | 7 STO_0 CLX RCL_0 | X=7.0 |
| `rcl_liftsStack` | 3 ENTER STO_1 RCL_1 | Y=3.0, X=3.0 |
| `sto_noStackChange` | 5 ENTER 3 STO_0 | X=3.0, Y=5.0 |

**Error recovery**

| Test | Key sequence | Assertion |
|---|---|---|
| `error_anyKey_clears` | 0 ÷ (error state) then digit 5 | error=null |
| `error_xIsZero` | 6 ENTER 0 ÷ | X=0.0 |
| `error_yztPreserved` | 6 ENTER 0 ÷ | Y=6.0 preserved |

**CHS outside entry**

| Test | Key sequence | Assertion |
|---|---|---|
| `chs_negatesX` | digit 5 ENTER CHS | X=-5.0 |
| `chs_doesNotUpdateLastX` | digit 5 ENTER CHS | lastX=0.0 |

**Shift and display mode**

| Test | Key sequence | Assertion |
|---|---|---|
| `shiftLatch_activates` | SHIFT | shiftActive=true in returned state |
| `shiftLatch_secondShift_staysActive` | SHIFT SHIFT | shiftActive=true |
| `shiftLatch_clearedByShiftedKey` | SHIFT FIX(2) | shiftActive=false |
| `fixMode_set` | SHIFT FIX(2) | displaySettings.mode = Fix(2) |
| `sciMode_set` | SHIFT SCI(3) | displaySettings.mode = Sci(3) |
| `degRad_toggle` | SHIFT DEG/RAD | angleMode = RAD |
| `degRad_toggleTwice` | SHIFT DEG/RAD SHIFT DEG/RAD | angleMode = DEG |

**Pi**

| Test | Key sequence | Assertion |
|---|---|---|
| `pi_pushesValue` | PI | X=Math.PI |
| `pi_liftsStack` | 5 ENTER PI | Y=5.0, X=Math.PI |

**Unary math spot-checks** (full coverage already in MathOperationsTest)

| Test | Key sequence | Assertion |
|---|---|---|
| `sqrt_happyPath` | 9 ENTER √x | X=3.0 |
| `sqrt_negative_error` | -4 ENTER √x | error≠null |
| `square_happyPath` | 3 ENTER x² | X=9.0 |
| `log_happyPath` | 100 ENTER LOG | X=2.0 |
| `factorial_five` | 5 ENTER n! | X=120.0 |

### Implement: `CalculatorEngine.kt`

### Acceptance criteria
All A7 tests green.

---

## Step A8 — Test Review

With the full implementation complete, review for gaps. Expected areas to examine:

- **Boundary values**: FIX 0, FIX 9, SCI 6, ENG 0 — are edge-case decimal place counts
  formatted correctly?
- **Stack depth sequences**: 5+ number entries — does T replicate correctly throughout?
- **Chained operations**: multi-step arithmetic sequences verifying full stack behavior at each
  step, not just the final result.
- **Entry edge cases**: entering just `.` (decimal only) → 0.0; entering just `EEX` from idle;
  EEX after a completed number.
- **Mode interactions**: trig result in RAD mode stored in register, then mode switched to DEG,
  RCL'd — value is unchanged (mode only affects computation, not stored values).
- **Shift latch + unshifted key**: pressing SHIFT then a key with no shift function → no-op,
  latch deactivated.
- **STO/RCL with all 10 registers** (0–9): spot-check that register addressing is not off-by-one.

Add any missing tests found during this review. Re-run the full suite. All tests must be green
before Phase A is complete.

---

## Phase A Completion Criteria

- `./gradlew :logic:test` runs with **zero failures**.
- `./gradlew :app:build` succeeds (`:app` still compiles against `:logic`).
- No Android imports anywhere in `:logic/src/main/`.
- All classes in `:logic` are instantiable via constructor injection (no singletons, no static
  state).
- `CalculatorEngine`, `MathOperations`, `EntryStateMachine`, and `DisplayFormatter` are each
  independently unit-testable without instantiating the others (except `CalculatorEngineTest`,
  which wires them together as a real integration).
