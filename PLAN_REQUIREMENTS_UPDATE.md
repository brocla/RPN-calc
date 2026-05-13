# Plan — Update REQUIREMENTS.md Based on v1.1 Development

This document lists every addition or correction to REQUIREMENTS.md that would have saved
time, prevented bugs, or eliminated ambiguity if present from the start. Organized by
section. Approve this plan before changes are applied to REQUIREMENTS.md.

---

## §2 — Platform

### Add: Architecture constraints

The requirements say nothing about how the code should be structured. Two decisions made
early had significant downstream consequences and should be prescribed:

> **Logic module isolation.** All calculator logic (stack, entry state machine, math
> operations, display formatting) must live in a separate `:logic` Gradle module with no
> Android dependencies. The `:app` module depends on `:logic`; the reverse is forbidden.
> This makes the logic independently testable with plain JVM unit tests.

> **Result type.** Math operations return a `CalcResult` sealed type (`Value(Double)` or
> `Err(String)`) rather than throwing exceptions. Callers pattern-match on the result.
> This keeps error handling explicit and eliminates try/catch throughout the engine.

### Add: State serialization

> State persistence (§12) is implemented via `kotlinx.serialization` with JSON encoding
> into `SavedStateHandle`. All persisted model classes must be annotated `@Serializable`.

---

## §5 — The Stack

### Fix §5.2: Stack-lift flag applies to ALL entry-initiating keys, not just digits

The current text says:

> "Flag enabled: pressing a **digit** pushes T←Z←Y←X before writing the new digit to X."

This is incomplete. The decimal point (`.`) and EEX keys also initiate entry from Idle
state. Both must check the stack-lift flag in exactly the same way. Omitting this caused
a real bug: pressing `.` after an operation started entry without lifting the stack, so
`10 ENTER .5 −` produced `-0.5` instead of `9`.

**Proposed replacement:**

> **Flag enabled:** beginning a new number — by pressing a digit key, the decimal point
> key, or the EEX key — while in Idle state pushes T←Z←Y←X before starting entry.
> **Flag disabled:** the same transitions in Idle state clear the flag after lifting.

### Add §5.2: Entry-state model

The requirements are silent on how entry works internally. Without this, the entry behavior
is ambiguous at the edges. Add a note:

> Number entry passes through three internal states: **Idle** (not entering a number),
> **Mantissa** (entering integer/fractional digits), and **Exponent** (entering exponent
> digits after EEX). Keys behave differently in each state. Operations commit any
> in-progress entry to X before executing.

---

## §7 — Number Entry

### Fix §7.1: Decimal-first entry

The requirements do not address what happens when `.` is the first key pressed (no leading
digit). This must be specified:

> Pressing `.` as the first key of an entry begins a number with an implicit leading zero.
> The display shows `0.` and subsequent digits fill the fractional part. The result is
> equivalent to having typed `0` then `.`.

### Add §7.4: EEX stack-lift behavior

EEX from Idle should be stated explicitly (mirrors the decimal fix above):

> Pressing EEX from Idle initiates entry of a number in scientific notation. If the
> stack-lift flag is enabled, the stack lifts before entry begins, exactly as for a digit
> or decimal press from Idle.

---

## §8 — Math Operations

### Add §8.x: Error behavior is explicit per operation (or add to §10)

The requirements list error conditions in §10 but don't state what happens to the stack
when an error occurs. The implementation behavior (which matches HP) should be specified:

> On error: Last X is updated to the pre-operation value of X. X is set to `0.0`. Y, Z,
> T, and memory registers are preserved. The error message is shown on the display.

This is partially covered in §10.2 but belongs in §8 as well for discoverability.

### Clarify §8.4: Percent — Y register after operation

The current text says "Y preserved" but doesn't explicitly state that Y stays on the
stack unchanged (it is not consumed or dropped). Clarify:

> Both `%` and `Δ%` are unary operations on X using Y as a parameter. Y is read but not
> consumed — it remains in the Y register after the operation. The stack does not drop.

---

## §10 — Error Handling

### Clarify §10.2 #5: Error dismissal swallows the triggering key

The current text says "any key press dismisses the error and returns to normal operation."
This is ambiguous about whether the key also executes. The actual behavior (and correct HP
behavior) is:

> The key press that dismisses the error is **consumed** — it clears the error state and
> stops there. The key does not also execute its normal function. The user must press the
> key a second time to execute it.

This is a meaningful behavioral distinction. Without it, a developer would reasonably
implement "dismiss and execute" which would produce wrong results.

---

## §11 — Key Reference

### Add: Shift latch toggle-off behavior

§11.1 currently says:

> "Tapping SHIFT again while already active leaves the latch active (it does not toggle off)."

This is correct but the rationale is invisible. Add:

> This matches HP-1xC behavior. SHIFT does not act as a toggle — it only activates.
> The latch deactivates only when a key with a shifted function is pressed (executing
> that function) or when a key with no shifted function is pressed (no-op and deactivate).

### Add: Key typography specification

The requirements say nothing about how key labels are rendered. This caused significant
implementation work and visual iteration. A new subsection:

> **§11.3 Key Label Typography**
>
> Primary labels use Helvetica (bundled `.ttf`). The characters `x`, `y`, `Y`, and `ˣ`
> (U+02E3 modifier letter small x) render in Times New Roman Italic at the same nominal
> size, to match HP-1xC styling. The `√x` key uses a custom drawn radical symbol
> (Canvas-based composable) rather than a text glyph.
>
> Each key has two label positions:
> - **Shifted label**: smaller text, top of key, always visible, dims when shift is inactive.
> - **Primary label**: larger text, vertically centered in the remaining key area.

---

## §12 — State Persistence

### Clarify: What is NOT persisted

The current text lists what is saved. Add what is explicitly not saved:

> The following state is **not** persisted and resets on every launch:
> - Shift latch state (always resets to inactive)
> - Entry state (any in-progress number entry is discarded; X retains its last committed value)
> - Error state (cleared on relaunch)

---

## §13 — User Experience

### Clarify: Haptic feedback type

The current text says "haptic feedback on every key press." The Android `HapticFeedbackType`
enum has multiple options and the default (`TextHandleMove`) does not produce audible/
tactile feedback on many devices. Specify:

> Haptic feedback uses `HapticFeedbackType.LongPress`. Other types (`TextHandleMove`,
> `VirtualKey`) are inaudible on the target device.

---

## §14 — Assets

### Expand: Display font details

The comma glyph modification is mentioned but not specified. Add:

> The DSEG7 font is modified to add a **zero-width comma glyph** at U+002C. The glyph
> has advance width 0 (it does not occupy horizontal space) so commas in digit-grouped
> numbers do not consume display positions. The glyph shape is a period dot plus a
> slightly descending tail, styled to match the seven-segment aesthetic.

### Expand: Key label fonts

> Key label fonts required:
> - **Helvetica** — bundled as `res/font/helvetica.ttf`. Used for all primary labels
>   except the characters listed in §11.3.
> - **Times New Roman Italic** — bundled as `res/font/timesi.ttf`. Used for the
>   characters listed in §11.3.

---

## New section: §15 — Architecture (rename current §15 to §16)

Add a section that prescribes the high-level code structure so it is established as a
requirement rather than left to developer discretion:

> **§15. Architecture**
>
> ### 15.1 Module structure
> - `:logic` — pure Kotlin module. Contains all calculator state, entry logic, math
>   operations, and display formatting. No Android or Compose imports permitted.
> - `:app` — Android module. Contains all Compose UI, ViewModel, DI, and resources.
>   Depends on `:logic`.
>
> ### 15.2 State flow
> `CalculatorState` is an immutable data class. All state transitions are pure functions
> that accept a `CalculatorState` and return a new `CalculatorState`. No mutable state
> exists in the logic layer.
>
> ### 15.3 Entry state machine
> Number entry is handled by a dedicated `EntryStateMachine` class, separate from
> `CalculatorEngine`. `EntryStateMachine` manages the three entry states (Idle, Mantissa,
> Exponent). `CalculatorEngine` calls `commitEntry()` at the start of every operation to
> finalize any in-progress number.
>
> ### 15.4 ViewModel
> A single `CalculatorViewModel` holds `CalculatorUiState` as a `StateFlow`. The UI is
> stateless — it reads from the flow and sends `CalcKeyEvent` values. State is persisted
> via `SavedStateHandle`.

---

## Summary of impact

| Category | Items | Would have prevented |
|---|---|---|
| Stack lift spec | §5.2, §7.1, §7.4 | Stack lift bug for `.` and EEX entry |
| Error dismissal | §10.2 | Incorrect "dismiss + execute" implementation |
| Haptic type | §13 | Haptic not working at all on target device |
| Architecture | new §15 | Ad-hoc structure decisions during implementation |
| Typography | §11.3, §14 | Multiple rounds of font/label visual iteration |
| Shift behavior | §11.1 | Ambiguity about toggle-off |
| State not persisted | §12 | Ambiguity about shift/entry state on relaunch |
