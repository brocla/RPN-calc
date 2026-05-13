# Implementation Plan — RPN Calculator v2.0

Implements the features in REQUIREMENTS_V2.md in the sequence defined by §7 (Phases C–F).
Each phase is independently buildable and mergeable. Later phases depend on earlier ones
only where noted.

---

## Phase C — ENTER Animation

**Goal:** Wrap the X register display value in `AnimatedContent` so it slides up on every ENTER.

### Problem to solve first

`AnimatedContent` animates when its `targetState` changes. If the user presses ENTER twice on
the same value, `displayString` does not change — no animation fires. We need a stable
trigger that increments on every ENTER regardless of value.

### Changes

#### `CalculatorUiState.kt`
Add a sequence counter:
```kotlin
val enterSeq: Int = 0
```

#### `CalculatorViewModel.kt`
In `dispatch()`, when `event == CalcKeyEvent.Enter`, increment `enterSeq` in the returned
`CalculatorUiState`:
```kotlin
displayString = buildDisplay(finalCs),
enterSeq = if (event == CalcKeyEvent.Enter) ui.enterSeq + 1 else ui.enterSeq,
```

#### `DisplayPanel.kt`
Replace the plain `Text` for `uiState.displayString` with an `AnimatedContent` block:

```kotlin
AnimatedContent(
    targetState = uiState.displayString to uiState.enterSeq,
    transitionSpec = {
        (slideInVertically { it } + fadeIn()) togetherWith
        (slideOutVertically { -it } + fadeOut())
    },
    label = "xRegister",
) { (display, _) ->
    Text(
        text = display,
        style = DisplayTextStyle,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth().weight(1f).wrapContentHeight(Alignment.Bottom),
    )
}
```

Duration is the default (300 ms) — no custom `animationSpec` needed.

### Scope note
The animation applies only to the X register display. The annunciator row does not animate.
Portrait mode (Phase F) will extend this to the Y register using the same transition spec —
no additional ViewModel changes needed at that point.

---

## Phase D — Swipe-Up for ENTER

**Goal:** A swipe-up gesture anywhere in the key grid fires ENTER. Depends on Phase C
(the animation is the swipe's visual confirmation per §4.3).

### Changes

#### `CalculatorScreen.kt`
Pass `onKey` down to `KeyGrid` (already done). Add `pointerInput` to the `KeyGrid` modifier:

```kotlin
KeyGrid(
    shiftActive = uiState.calcState.shiftActive,
    onKey = onKey,
    modifier = Modifier
        .fillMaxWidth()
        .weight(0.72f)
        .pointerInput(Unit) {
            var totalDragY = 0f
            var totalDragX = 0f
            detectVerticalDragGestures(
                onDragStart = { totalDragY = 0f; totalDragX = 0f },
                onVerticalDrag = { change, dragAmount ->
                    totalDragY += dragAmount
                    // track horizontal drift via change.position delta if needed
                    val thresholdPx = with(density) { 40.dp.toPx() }
                    if (totalDragY < -thresholdPx) {
                        onKey(CalcKeyEvent.Enter)
                        // consume to prevent further triggers in this gesture
                    }
                }
            )
        },
)
```

Horizontal tolerance (60 dp): `detectVerticalDragGestures` already filters predominantly
vertical motion. The 60 dp horizontal drift tolerance is satisfied by the detector's default
behavior — no extra code needed unless testing reveals false negatives.

No haptic feedback is triggered for the swipe path. The existing haptic call lives inside
`CalcKey`'s click handler and is not reached by this code path.

---

## Phase E — Layout Infrastructure

**Goal:** Define the layout DSL, a generic renderer, extract the v1.0 layout, wire the
Layout key and bottom sheet picker, and lock orientation per layout.

This is the largest phase. Break it into five sub-steps.

### E1 — Define the layout data model

New file: `ui/layouts/LayoutDescriptor.kt`

```kotlin
enum class LayoutOrientation { Landscape, Portrait }

sealed interface KeySlot {
    data class Key(val keyDef: KeyDef, val weight: Float = 1f) : KeySlot
    data class Spacer(val weight: Float) : KeySlot
}

data class KeyRow(val slots: List<KeySlot>)

data class LayoutDescriptor(
    val name: String,
    val orientation: LayoutOrientation,
    val rows: List<KeyRow>,
)
```

`KeyDef` is already in `ui.calculator.components`. Import it directly — no duplication.

### E2 — Generic layout renderer

New file: `ui/layouts/LayoutRenderer.kt`

A `@Composable` that accepts a `LayoutDescriptor`, `shiftActive`, and `onKey`, and renders
the grid:

```kotlin
@Composable
fun LayoutRenderer(
    layout: LayoutDescriptor,
    shiftActive: Boolean,
    onKey: (CalcKeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        layout.rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.slots.forEach { slot ->
                    when (slot) {
                        is KeySlot.Key ->
                            CalcKey(
                                keyDef = slot.keyDef,
                                shiftActive = shiftActive,
                                onKey = onKey,
                                modifier = Modifier.weight(slot.weight).fillMaxHeight(),
                            )
                        is KeySlot.Spacer ->
                            Spacer(modifier = Modifier.weight(slot.weight))
                    }
                }
            }
        }
    }
}
```

All rows share equal height via `weight(1f)` on each row. Key padding is handled inside
`CalcKey` as it is today.

### E3 — Extract the v1.0 landscape layout

New file: `ui/layouts/ClassicLandscapeLayout.kt`

Move the key row definitions out of `KeyGrid.kt` and rewrite them as a `LayoutDescriptor`
using `KeySlot.Key` wrappers. The `KeyDef` instances are unchanged — just wrapped.

The `ON` key becomes:
```kotlin
KeySlot.Key(KeyDef("LAYOUT", "", CalcKeyEvent.OpenLayoutPicker))
```

Add `CalcKeyEvent.OpenLayoutPicker` to `CalcKeyEvent.kt`. The ViewModel handles it by
setting a flag (see E4).

`KeyGrid.kt` is **deleted**. `CalculatorScreen.kt` replaces the `KeyGrid(...)` call with
`LayoutRenderer(layout = activeLayout, ...)`.

### E4 — Layout state and picker in CalculatorRoute / ViewModel

The active layout is ephemeral (not persisted). It lives in `CalculatorRoute` as local
`remember` state — it does not belong in `CalculatorViewModel` because it is UI-only and
has no effect on calculator logic.

#### `CalculatorUiState.kt` / `CalculatorViewModel.kt`
No changes needed for layout selection. The ViewModel continues to emit `CalculatorUiState`
unchanged.

#### `CalculatorRoute.kt`
```kotlin
val layouts = remember { listOf(ClassicLandscapeLayout, /* future layouts */) }
var activeLayout by remember { mutableStateOf(layouts.first()) }
var showLayoutPicker by remember { mutableStateOf(false) }

// Intercept OpenLayoutPicker before passing to ViewModel
val onKey: (CalcKeyEvent) -> Unit = { event ->
    if (event == CalcKeyEvent.OpenLayoutPicker) showLayoutPicker = true
    else viewModel.onKey(event)
}

if (showLayoutPicker) {
    ModalBottomSheet(onDismissRequest = { showLayoutPicker = false }) {
        Text("Select Layout", ...)
        layouts.forEach { layout ->
            // row item — tap activates and dismisses
        }
    }
}

CalculatorScreen(
    uiState = uiState,
    activeLayout = activeLayout,
    onKey = onKey,
)
```

#### `CalculatorScreen.kt`
Add `activeLayout: LayoutDescriptor` parameter. Pass it to `LayoutRenderer`. The display
panel variant (landscape vs portrait) is derived from `activeLayout.orientation`.

### E5 — Orientation locking

#### `MainActivity.kt`
Expose a callback or use a `ViewModel`/`CompositionLocal` to receive orientation change
requests. The simplest approach for this app: pass a lambda from `MainActivity` into
`CalculatorRoute`:

```kotlin
// In MainActivity.onCreate:
setContent {
    CalcTheme {
        CalculatorRoute(onOrientationChange = { requestedOrientation = it })
    }
}
```

In `CalculatorRoute`, call `onOrientationChange` whenever `activeLayout` changes, mapping
`LayoutOrientation` to `ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE` /
`SCREEN_ORIENTATION_PORTRAIT`. Also call it once at first composition (before first frame)
via `LaunchedEffect(activeLayout)`.

---

## Phase F — Portrait Layout

**Goal:** One portrait layout as a UX study. Depends on all of Phase E.

### F1 — Portrait display panel

New file: `ui/calculator/components/PortraitDisplayPanel.kt`

Shows Y above X, both in `DisplayTextStyle`, same font size. No register labels.
Annunciator row below X, unchanged.

`CalculatorUiState` needs to expose the Y register value for the display. Add:
```kotlin
val yDisplayString: String = "0"
```
Populated in `CalculatorViewModel.buildDisplay()` (or a new `buildYDisplay()` helper)
using `engine.getDisplay()` called against a synthetic state with Y in the X position,
or more directly by formatting `calcState.stack.y` through `DisplayFormatter` directly.

`CalculatorScreen.kt` chooses the display panel based on `activeLayout.orientation`:
```kotlin
if (activeLayout.orientation == LayoutOrientation.Portrait)
    PortraitDisplayPanel(uiState = uiState, modifier = ...)
else
    DisplayPanel(uiState = uiState, modifier = ...)
```

The ENTER animation (Phase C) applies to both X and Y register displays in portrait mode.
Each gets its own `AnimatedContent` keyed on the same `enterSeq`.

### F2 — Portrait layout descriptor

New file: `ui/layouts/PortraitLayout.kt`

A `LayoutDescriptor` with `orientation = LayoutOrientation.Portrait`. Key arrangement is
at the author's discretion per §3.2. All keys required for basic RPN arithmetic must be
reachable without shift. Suggested starting point: 5 rows × 4 keys, with ENTER as a
double-wide key (`weight = 2f`) sharing a row with two other keys.

---

## File inventory

| File | Action |
|---|---|
| `ui/calculator/CalculatorUiState.kt` | Add `enterSeq`, `yDisplayString` |
| `ui/calculator/CalculatorViewModel.kt` | Increment `enterSeq` on Enter; populate `yDisplayString` |
| `ui/calculator/CalcKeyEvent.kt` | Add `OpenLayoutPicker` |
| `ui/calculator/CalculatorRoute.kt` | Add layout state, picker sheet, orientation callback |
| `ui/calculator/CalculatorScreen.kt` | Add `activeLayout` param; choose display panel; add swipe detector |
| `ui/calculator/components/DisplayPanel.kt` | Wrap X display in `AnimatedContent` |
| `ui/calculator/components/KeyGrid.kt` | **Delete** |
| `ui/layouts/LayoutDescriptor.kt` | **New** — DSL types |
| `ui/layouts/LayoutRenderer.kt` | **New** — generic renderer |
| `ui/layouts/ClassicLandscapeLayout.kt` | **New** — v1.0 layout as descriptor |
| `ui/layouts/PortraitLayout.kt` | **New** — portrait UX study layout |
| `ui/calculator/components/PortraitDisplayPanel.kt` | **New** — two-register display |
| `MainActivity.kt` | Pass orientation callback; apply on launch |

---

## Dependencies between phases

```
C (animation) ──► D (swipe)
                        \
E1 ► E2 ► E3 ► E4 ► E5 ─► F1 ► F2
```

C and E1–E3 can be developed in parallel. D should follow C. F requires E to be complete.
