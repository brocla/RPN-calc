# RPN Calculator v2.0 — Requirements Document

Extends v1.0. All v1.0 requirements remain in force unless explicitly overridden here.

---

## 1. Interchangeable Keyboard Layouts

### 1.1 Purpose

Layout switching is a UX research facility. It is explicitly temporary: layout code must be written to be discarded. Layouts must be self-contained and must not couple to the core calculator logic, ViewModel, or display code.

### 1.2 Architecture Constraint

Each layout is a standalone Kotlin file (or small group of files) in an isolated package such as `ui.layouts`. It declares its own key grid, key definitions, and target orientation. No layout-specific code appears anywhere outside that package. The rest of the app is unaware of which layout is active beyond receiving a `KeyGrid`-equivalent composable to display.

### 1.3 Layout Definition

A layout is defined by a **declarative data structure**, not imperative Compose code. The app ships a single generic renderer that interprets any layout descriptor and produces the key grid UI. Layout authors write data — not composables.

#### Layout descriptor

A layout descriptor provides:
- A **name** (short string shown in the picker, e.g. `"Classic Landscape"`)
- A **fixed orientation** (portrait or landscape) — see §2
- A **sequence of rows**, each row being a sequence of **key slots**

#### Key slots

Each key slot is one of:
- **Key** — references a `KeyDef` plus a **weight** (`Float`, default `1f`). A double-wide ENTER key has weight `2f`.
- **Spacer** — an invisible gap with a given weight. Used to push keys apart or align columns across rows that have different key counts.

Weights within a row are relative — the renderer divides available width proportionally. A row of five `1f` keys produces narrower keys than a row of four `1f` keys. A double-wide ENTER in a five-slot row is expressed as weight `2f` alongside four keys of weight `1f` — the ENTER takes 2 of the 5 units of width.

All rows share the same height. There is no per-row height weight.

#### What this enables

- Rows with different effective key counts (5-wide top section, 4-wide bottom section)
- Double-wide keys (e.g. ENTER, `+`)
- Ragged right edges via trailing spacers
- HP-41 style layouts: top block of 5 narrow keys, lower blocks of 4 wider keys with a double-wide ENTER

#### What this does not enable

Arbitrary 2D spanning (a key that is both double-wide and double-tall) is out of scope for v2.0.

The app ships with at least two layouts at v2.0 launch:
- The existing v1.0 landscape layout
- One portrait layout (see §2)

### 1.4 Layout Picker — Trigger Key

The **ON key** (column 1, row 4 of the current grid) is repurposed as the **Layout key**. Pressing it opens the layout picker. It has no shifted function.

### 1.5 Layout Picker — UI

When the Layout key is pressed, a **bottom sheet drawer** slides up over the key grid. It contains:

- A title: `"Select Layout"`
- A vertically scrolling list of available layout names
- The currently active layout is highlighted
- Tapping a name: activates that layout and dismisses the drawer
- Tapping outside the drawer (scrim): dismisses without changing layout

The drawer uses Material 3 `ModalBottomSheet`. No custom drawer component.

### 1.6 Layout Persistence

The selected layout is **not** persisted across sessions. The app always launches with the default layout (the v1.0 landscape layout).

---

## 2. Orientation

### 2.1 Per-Layout Fixed Orientation

Each layout carries a declared orientation (portrait or landscape). When a layout is activated, the app locks to that orientation programmatically. Rotating the physical device has no effect — the screen does not follow the device orientation.

### 2.2 Implementation

Orientation is set via `Activity.requestedOrientation` when the layout changes. The current layout's declared orientation is applied on launch (before the first frame is drawn) and whenever the layout changes.

---

## 3. Portrait Layout

### 3.1 Display Area

In portrait mode the display panel shows **two registers**: Y above X. Both use the same DSEG7 font and the same font size. Neither register is labelled — their position (Y above X) is sufficient.

```
┌─────────────────────────────┐
│  [Y register value]         │
│  [X register value]         │
│  FIX  SCI  ENG  ALL   DEG  f│  ← annunciator row
└─────────────────────────────┘
```

The annunciator row remains below X, unchanged from v1.0.

### 3.2 Key Grid

The portrait key grid layout is defined by the layout author (UX study). No specific key arrangement is mandated here beyond: all keys required for basic RPN arithmetic must be reachable without shift.

### 3.3 Key Size

In portrait, the key grid occupies the lower portion of the screen below the display panel. Key sizing is proportional and determined by the layout composable.

---

## 4. Swipe-Up for ENTER

### 4.1 Gesture

A **swipe-up gesture** anywhere within the key grid area executes ENTER. It is exactly equivalent to pressing the ENTER key — same state machine path, same animation (see §5).

### 4.2 Detection

- Minimum vertical travel: **40 dp** upward before the gesture is recognized
- Horizontal tolerance: the gesture may drift up to **60 dp** horizontally and still be recognized as a swipe-up (i.e. it does not need to be perfectly vertical)
- The gesture is detected using Compose `detectVerticalDragGestures` (via `pointerInput`) applied to the key grid container

### 4.3 Feedback

- **No haptic feedback** on swipe-up. This distinguishes it clearly from a key press.
- The ENTER animation (§5) fires as confirmation.

### 4.4 Conflict with Key Presses

Individual key `clickable` handlers take priority over the swipe detector for short touches. The swipe is only recognized after the 40 dp threshold is crossed, so incidental movement during a tap does not trigger it.

---

## 5. ENTER Animation

### 5.1 Trigger

The animation fires whenever ENTER executes, regardless of whether it was triggered by the ENTER key press or a swipe-up gesture.

### 5.2 Mechanism

The X register display value is wrapped in Compose `AnimatedContent`. The transition spec is:

- **Incoming content** (new X value, which is a duplicate of the old X after ENTER): `slideInVertically { height -> height } + fadeIn`
- **Outgoing content** (old X value): `slideOutVertically { height -> -height } + fadeOut`

The effect: the old value slides upward out of frame as the new value slides in from below — visually suggesting the stack moving up.

### 5.3 Scope

The animation applies only to the **X register display**. Y, Z, T and the annunciator row do not animate. In portrait mode (§3), both X and Y register displays animate on ENTER using the same transition spec.

### 5.4 Duration

Default `AnimatedContent` duration (300 ms). No custom timing.

---

## 6. Key Reference Additions

| Key Label | Location | Function |
|---|---|---|
| LAYOUT | Row 4, Col 1 (replaces ON) | Opens layout picker drawer |

The ON key (`CalcKeyEvent.NoOp`) is replaced by a new `CalcKeyEvent.OpenLayoutPicker` in the landscape layout only. Portrait layouts may assign this position differently.

---

## 7. Development Sequence

- **Phase C — ENTER animation**: Wrap X register display in `AnimatedContent`. Verify on both landscape and portrait display panels.
- **Phase D — Swipe-up**: Add `pointerInput` drag detector to the key grid container. Wire to the same ENTER handler.
- **Phase E — Layout infrastructure**: Define the layout interface. Extract the existing v1.0 layout into the first concrete implementation. Wire the Layout key and bottom sheet picker.
- **Phase F — Portrait layout**: Implement one portrait layout as a UX study, including the two-register display panel.
