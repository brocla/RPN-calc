# RPN Calculator v2.1 — Requirements Document

Extends v2.0. All v1.0 and v2.0 requirements remain in force unless explicitly overridden here.

---

## 1. State Persistence

### 1.1 Scope

The following state is persisted across app kills, device restarts, and process death:

| State | Detail |
|---|---|
| Stack registers | X, Y, Z, T |
| Memory registers | All 10 (M0–M9) |
| Display mode | FIX/SCI/ENG/ALL and decimal places |
| Angle mode | DEG / RAD |

Shift latch (`shiftActive`) is **not** persisted — it always resets to inactive on launch.

Entry state (digits being typed) is **not** persisted — a partially entered number is committed to X before saving, exactly as if ENTER had been pressed.

### 1.2 Storage Mechanism

Persist using **`DataStore<Preferences>`** (Jetpack). This replaces the current `SavedStateHandle` approach, which only survives process death and rotation — not a full device restart.

The DataStore write occurs on every key press that changes persistent state, using a coroutine launched in the ViewModel scope.

### 1.3 Clearing Persistent State

**Android system "Clear data"** (Settings → Apps → RPN Calc → Storage → Clear Data) wipes DataStore. This is the only system-level path to clearing it. "Clear cache" does not affect DataStore and does not reset the calculator.

**In-app reset** is provided (see §2). Users should not need to navigate Android Settings to reset the calculator.

---

## 2. In-App Reset

### 2.1 Trigger

**Long-press on the Backspace (⌫) key.**

Rationale: Backspace is the existing "undo last digit" key. A long-press escalating to a full reset is a natural extension. CLx was removed in v2.0; this fills the gap for destructive clear without adding a new key.

### 2.2 Behavior

Reset:
- Clears all four stack registers to 0
- Clears all 10 memory registers to 0
- Resets display mode to ALL
- Resets angle mode to DEG
- Clears persistent DataStore state

### 2.3 Confirmation Dialog

A confirmation dialog is shown before reset executes. It presents two options:

- **Reset** — proceeds with full reset
- **Cancel** — dismisses without action

The dialog uses Material 3 `AlertDialog`. No custom dialog component.

### 2.4 Haptic Feedback

The long-press that triggers the confirmation dialog produces a haptic pulse (same as a key press). No additional haptic on confirm.

---

## 3. Copy / Paste

### 3.1 Trigger

**Long-press on the display panel** (anywhere within the bezel area) opens the system copy/paste menu via a custom implementation. A standard Android `DropdownMenu` or `PopupMenu` appears near the press point with two items:

- **Copy** — copies the X register display string to the system clipboard
- **Paste** — reads from the system clipboard and loads the value into X

### 3.2 Copy

The string placed on the clipboard is the **exact string shown in the X register display** — including commas, decimal point, sign, and exponent notation as currently formatted. This ensures what the user sees is what gets copied.

### 3.3 Paste

The string is read from the clipboard and preprocessed before loading:

1. **Strip** all characters that are not in `[0-9 . , + - E e]`
2. **Remove** thousands-separator commas (these are display artifacts, not numeric)
3. **Validate** the result is parseable as a `Double`
4. If valid: commit any current entry, lift the stack, and place the parsed value in X
5. If invalid: display an error message in X (same error mechanism as divide-by-zero)

The paste treats `E` / `e` as the exponent separator (scientific notation). A string like `"1.23e+04"` pastes as `12300.0`.

### 3.4 Implementation Notes

The display panel is not a `TextField`. Long-press is detected via `pointerInput` + `detectTapGestures(onLongPress = {...})`. The clipboard is accessed via `LocalClipboardManager`.

No haptic feedback on long-press (the system clipboard popup is sufficient feedback).

---

## 4. Key Reference Changes

| Key | Change |
|---|---|
| Backspace (⌫) | Short press: existing backspace behavior. Long press: opens reset confirmation dialog. |

No new keys are added in v2.1.

---

## 5. Open Questions

- **DataStore vs Room**: DataStore is recommended for a flat key-value structure like this. If future versions add named memory banks or user-defined constants, Room may be more appropriate then.
- **Paste and entry state**: If the user is mid-entry (e.g. has typed `"3.1"`), paste commits the partial entry to the stack before pasting. This matches how arithmetic operators behave.
- **Layout persistence**: v2.0 specifies the active layout is not persisted (§1.6 of REQUIREMENTS_V2.md). v2.1 does not change this. Reconsider in v2.2 once the layout set stabilizes.

---

## 6. Development Sequence

- **Phase G — DataStore persistence**: Replace `SavedStateHandle` with DataStore. Persist stack, memory, display settings, angle mode.
- **Phase H — In-app reset**: Add long-press detection to Backspace. Wire confirmation dialog to a `reset()` method in ViewModel that clears DataStore and resets state.
- **Phase I — Copy/Paste**: Add long-press handler to display panel. Implement copy (format X to clipboard) and paste (parse, validate, push to stack).
