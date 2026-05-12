# Phase B Implementation Plan — UI

## Overview

Phase B builds the Jetpack Compose UI inside `:app`. It wires the Phase A `:logic` module to
a single-screen Android calculator UI that visually replicates the HP-11C: a dark-body
instrument-style calculator with a silver display panel, amber/white key labels, and a
seven-segment LCD display font. Landscape-only. No navigation graph — there is exactly one
screen.

Architecture follows the Route → Screen → ViewModel → Engine pattern from the Android skill
reference. Manual constructor injection is replaced by Hilt for Android-layer classes.
`:logic` classes (`CalculatorEngine`, `MathOperations`, etc.) are injected as Hilt bindings
with no changes to `:logic` itself.

---

## Visual Design Reference

The HP-11C photograph drives all visual decisions:

| Element | HP-11C | Our App |
|---|---|---|
| Body | Near-black brushed surface | `#1C1C1E` (iOS system black, reads as "device") |
| Display bezel | Brushed silver/aluminum | `#C0C0C0` gradient via Box + border |
| Display background | Pale yellow-green LCD | `#B5C99A` (warm LCD tint) |
| Display text | Dark seven-segment | DSEG7 Classic font, `#2B2B2B` |
| Primary key label | White | `#FFFFFF` |
| Shifted key label | Gold/amber (above key) | `#D4A017` (top of key cap) |
| Arithmetic keys (+−×÷) | Slightly lighter than body | `#2C2C2E` |
| ENTER key | Taller than other keys | Spans 2 rows |
| SHIFT key | Orange (HP-11C uses f/g; we use one) | `#E07020` body, white label |
| ON key | Visual only (bottom-left corner) | Render but no action |

**Annunciators** (small text indicators in the display area):
- Left cluster: `FIX` / `SCI` / `ENG` / `ALL` — active mode highlighted
- Center: `DEG` / `RAD`
- Right: `f` (shift active indicator, amber when lit)

---

## Architecture

```
MainActivity  (Hilt entry point, landscape lock, edge-to-edge)
    └── CalculatorRoute          (@Composable, hiltViewModel())
            ├── CalculatorViewModel   (Hilt, SavedStateHandle)
            │       ├── CalculatorEngine   (injected from :logic)
            │       └── StateFlow<CalculatorUiState>
            └── CalculatorScreen  (pure Composable, no ViewModel reference)
                    ├── DisplayPanel
                    │       ├── AnnunciatorRow
                    │       └── MainDisplay (DSEG7 text)
                    └── KeyGrid
                            └── CalcKey (×38)
```

### Key data flow

```
User tap → CalcKey.onClick → CalculatorScreen.onKey(KeyEvent) →
CalculatorViewModel.onKey(KeyEvent) → CalculatorEngine.press*(state) →
new CalculatorState → StateFlow update → recomposition
```

### Pending-operation state (STO/RCL/FIX/SCI/ENG)

These keys require a second digit keystroke (register number or decimal-place count). This
two-step flow is managed entirely in the ViewModel via a `PendingOp` sealed class — not in
`:logic`. When `pendingOp != None`, the next digit key resolves the pending operation instead
of being forwarded to `EntryStateMachine`.

```kotlin
sealed interface PendingOp {
    data object None : PendingOp
    data object Sto : PendingOp
    data object Rcl : PendingOp
    data object FixArg : PendingOp
    data object SciArg : PendingOp
    data object EngArg : PendingOp
}
```

`CalculatorUiState` wraps both:
```kotlin
data class CalculatorUiState(
    val calcState: CalculatorState = CalculatorState(),
    val pendingOp: PendingOp = PendingOp.None,
    val displayString: String = "0.0000"
)
```

---

## Key Layout

The grid is 4 rows × 10 columns. ENTER occupies one column across rows 3–4 (double-height).
Each cell carries a primary label (white, lower) and an optional shifted label (amber, upper).

```
┌──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
│  x²  │  LN  │  LOG │  →P  │  →R  │ ALL  │  FIX │  SCI │  ENG │      │  ← shifted row
│  √x  │  eˣ  │ 10^x │  yˣ  │  1/x │ CHS  │   7  │   8  │   9  │  ÷   │  row 1
├──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤
│DEG/R │ nPr  │ SIN⁻¹│ COS⁻¹│ TAN⁻¹│      │      │      │      │      │  ← shifted row
│  n!  │ nCr  │ SIN  │ COS  │ TAN  │ EEX  │   4  │   5  │   6  │  ×   │  row 2
├──────┼──────┼──────┼──────┼──────┼──────┬──────┼──────┼──────┼──────┤
│  Δ%  │      │ LstX │      │      │      ║      │      │      │      │  ← shifted row
│  %   │ Roll↓│  X↔Y │  ←   │ CLX  │      ║  1   │  2   │  3   │  −   │  row 3
├──────┼──────┼──────┼──────┼──────┤ENTER ╠──────┼──────┼──────┼──────┤
│      │      │      │      │      │      ║      │      │      │      │  ← shifted row
│ (ON) │SHIFT │      │ STO  │ RCL  │      ║  0   │  .   │  π   │  +   │  row 4
└──────┴──────┴──────┴──────┴──────┴──────╨──────┴──────┴──────┴──────┘
```

Notes:
- **(ON)** — renders as a key visually but produces no action.
- **ENTER** — double-height, column 6, rows 3–4.
- Row 1 col 6 shift (`ALL`) — ALL display mode shares the CHS key column.
- Row 2 col 1 shift (`DEG/R`) — abbreviated for display; full label `DEG/RAD`.
- Row 2 col 2 shift (`nPr`) — nCr and nPr share the same key column.
- Row 3 col 1 shift (`Δ%`) — percent-change lives above the percent key.
- Row 4 col 3 — blank/unused.
- Row 4 col 9 (`π`) — unshifted; no shifted function on this key.

---

## File Structure

```
app/src/main/
├── AndroidManifest.xml               (add screenOrientation="landscape")
├── res/
│   └── font/
│       └── dseg7classic_regular.ttf  (bundled asset — see B1)
└── java/com/brocla/rpn_calc/
    ├── MainActivity.kt               (Hilt + edge-to-edge, B8)
    ├── RpnCalcApplication.kt         (new — @HiltAndroidApp, B1)
    ├── ui/
    │   ├── theme/
    │   │   ├── CalcColors.kt         (HP-11C palette, B2)
    │   │   ├── CalcTheme.kt          (MaterialTheme wrapper, B2)
    │   │   └── CalcType.kt           (DSEG7 + Roboto Condensed typography, B2)
    │   └── calculator/
    │       ├── CalculatorRoute.kt    (hiltViewModel(), B7)
    │       ├── CalculatorScreen.kt   (pure Composable, B7)
    │       ├── CalculatorViewModel.kt (B3)
    │       ├── CalculatorUiState.kt  (B3)
    │       ├── PendingOp.kt          (B3)
    │       ├── DisplayFormatter.kt   (insertThousandsCommas — UI layer only, B4)
    │       └── components/
    │           ├── DisplayPanel.kt   (B4)
    │           ├── AnnunciatorRow.kt (B4)
    │           ├── KeyGrid.kt        (B6)
    │           ├── CalcKey.kt        (B5)
    │           └── KeyDef.kt         (B6)
    └── di/
        └── CalculatorModule.kt       (Hilt bindings for :logic classes, B3)
```

---

## Step B1 — Gradle & Dependencies

### `gradle/libs.versions.toml` — add

```toml
[versions]
hilt = "2.56.2"
ksp = "2.2.10-1.0.29"           # matches kotlin = "2.2.10"
lifecycleViewmodelCompose = "2.9.1"
lifecycleRuntimeCompose = "2.9.1"
kotlinxSerializationJson = "1.8.1"
kotlinxCoroutines = "1.10.2"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### `build.gradle.kts` (root) — add
```kotlin
alias(libs.plugins.hilt) apply false
alias(libs.plugins.ksp) apply false
alias(libs.plugins.kotlin.serialization) apply false
```

### `app/build.gradle.kts` — add plugins and dependencies
```kotlin
plugins {
    // existing...
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // existing...
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
```

### `logic/build.gradle.kts` — add for state serialization
```kotlin
dependencies {
    // existing...
    implementation(libs.kotlinx.serialization.json)
}
```
Also add `alias(libs.plugins.kotlin.serialization)` to `logic/build.gradle.kts` plugins block.

### Font asset

The font file is already present at:
```
app/src/main/res/font/DSEG7Classic-BoldItalic.ttf
```
This is a **modified** DSEG7 Classic Bold Italic build. A zero-width comma glyph (U+002C,
advance width = 0) was added via FontForge so that comma thousands-separators can be
inserted between digit characters without shifting their positions — identical to how the
built-in period glyph works in the unmodified font. Do not replace this file with the
upstream DSEG7 release; the upstream fonts contain no comma glyph.

### Acceptance criteria
- `./gradlew :app:build` succeeds with all new dependencies resolved.
- Font file present at the above path.

---

## Step B2 — Theme & Design System

Replace the scaffolded `ui/theme/` with HP-11C-accurate theming. No Material3 dynamic color.

### `CalcColors.kt`

```kotlin
object CalcColors {
    val Body          = Color(0xFF1C1C1E)   // calculator body
    val DisplayBezel  = Color(0xFFA8A8A8)   // silver frame
    val DisplayBg     = Color(0xFFB8C99A)   // warm LCD green
    val DisplayText   = Color(0xFF1A2410)   // dark segment ink
    val DisplayOff    = Color(0xFF9AAD80)   // "off" segment ghost
    val KeyTop        = Color(0xFF2A2A2C)   // standard key surface
    val KeyArith      = Color(0xFF323234)   // arithmetic keys (slightly lighter)
    val KeyEnter      = Color(0xFF2A2A2C)   // ENTER same as standard
    val KeyShift      = Color(0xFFD06010)   // orange SHIFT key
    val LabelPrimary  = Color(0xFFEEEEEE)   // white key label
    val LabelShifted  = Color(0xFFD4A017)   // amber shifted label (above key)
    val LabelShiftKey = Color(0xFFFFFFFF)   // white label on orange SHIFT
    val AnnunciatorOn = Color(0xFF1A2410)   // active annunciator
    val AnnunciatorOff= Color(0xFF8A9A70)   // inactive annunciator
    val KeyPressed    = Color(0xFF484848)   // highlight on touch down
}
```

### `CalcType.kt`

```kotlin
val Dseg7 = FontFamily(Font(R.font.dseg7classic_regular))

val CalcTypography = Typography(
    // Main display number
    displayLarge = TextStyle(
        fontFamily = Dseg7,
        fontSize = 36.sp,
        letterSpacing = 2.sp,
        color = CalcColors.DisplayText
    ),
    // Annunciator labels
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        letterSpacing = 0.5.sp
    )
)
```

### `CalcTheme.kt`

Wraps `MaterialTheme` with fixed dark color scheme derived from `CalcColors`. No dynamic
color, no light theme variant — the calculator is always dark.

```kotlin
@Composable
fun CalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = CalcColors.Body,
            surface = CalcColors.KeyTop,
            primary = CalcColors.KeyShift,
            onBackground = CalcColors.LabelPrimary,
            onSurface = CalcColors.LabelPrimary,
        ),
        typography = CalcTypography,
        content = content
    )
}
```

### `AndroidManifest.xml` — update Activity entry

```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="landscape"
    android:windowSoftInputMode="stateAlwaysHidden"
    ...>
```

### Acceptance criteria
- `CalcTheme` compiles and previews without error.
- Font loads correctly in a Preview annotated composable.

---

## Step B3 — ViewModel, State Serialization & DI

### State serialization

`CalculatorState` must survive process death via `SavedStateHandle`. The `:logic` model
classes are annotated with `@Serializable` (pure Kotlin, no Android dependency). A JSON
string is stored under a single `SavedStateHandle` key.

**Additions to `:logic` model classes:**
- Add `@Serializable` to: `CalculatorState`, `Stack`, `EntryState`, `DisplayMode`,
  `DisplaySettings`, `AngleMode`, `CalcResult` (and their nested types).
- `EntryState.Idle` is an `object` — use `@Serializable` with `@SerialName`.
- `DisplayMode.All` is an `object` — same treatment.

**`CalculatorModule.kt`** (Hilt `@Module`):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CalculatorModule {
    @Provides @Singleton
    fun provideEntryStateMachine() = EntryStateMachine()

    @Provides @Singleton
    fun provideMathOperations() = MathOperations()

    @Provides @Singleton
    fun provideDisplayFormatter() = DisplayFormatter()

    @Provides @Singleton
    fun provideCalculatorEngine(
        esm: EntryStateMachine,
        math: MathOperations,
        fmt: DisplayFormatter
    ) = CalculatorEngine(esm, math, fmt)
}
```

### `CalculatorViewModel.kt`

```kotlin
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val engine: CalculatorEngine,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onKey(event: KeyEvent) { /* see below */ }

    private fun loadState(): CalculatorUiState { /* deserialize from SavedStateHandle */ }
    private fun saveState(state: CalculatorUiState) { /* serialize to SavedStateHandle */ }
}
```

**Key event routing in `onKey`:**

```
if (calcState.error != null && event is not system key) → clear error, return
if (pendingOp != None && event is DigitKey(d)) → resolve pending op with d, clear pendingOp
else → dispatch to CalculatorEngine by event type
```

After every state change: `saveState(newUiState)`.

**`KeyEvent` sealed class** (defined in `ui/calculator/`):

```kotlin
sealed interface KeyEvent {
    data class Digit(val d: Int) : KeyEvent
    data object Decimal : KeyEvent
    data object Enter : KeyEvent
    data object Chs : KeyEvent
    data object Eex : KeyEvent
    data object Backspace : KeyEvent
    data object Clx : KeyEvent
    data object RollDown : KeyEvent
    data object Swap : KeyEvent
    data object Sto : KeyEvent          // arms pendingOp = Sto
    data object Rcl : KeyEvent          // arms pendingOp = Rcl
    data object Add : KeyEvent
    data object Subtract : KeyEvent
    data object Multiply : KeyEvent
    data object Divide : KeyEvent
    data object Reciprocal : KeyEvent
    data object Sqrt : KeyEvent
    data object Square : KeyEvent       // shifted
    data object Pow10 : KeyEvent
    data object Log : KeyEvent          // shifted
    data object Exp : KeyEvent
    data object Ln : KeyEvent           // shifted
    data object Power : KeyEvent
    data object Pi : KeyEvent
    data object Percent : KeyEvent
    data object PercentChange : KeyEvent // shifted
    data object Sin : KeyEvent
    data object Cos : KeyEvent
    data object Tan : KeyEvent
    data object ArcSin : KeyEvent       // shifted
    data object ArcCos : KeyEvent       // shifted
    data object ArcTan : KeyEvent       // shifted
    data object NCr : KeyEvent
    data object NPr : KeyEvent          // shifted
    data object Factorial : KeyEvent
    data object ToPolar : KeyEvent      // shifted
    data object ToRect : KeyEvent       // shifted
    data object LastX : KeyEvent        // shifted
    data object FixArg : KeyEvent       // arms pendingOp = FixArg
    data object SciArg : KeyEvent       // arms pendingOp = SciArg
    data object EngArg : KeyEvent       // arms pendingOp = EngArg
    data object AllMode : KeyEvent      // shifted, immediate
    data object DegRad : KeyEvent       // shifted
    data object Shift : KeyEvent
    data object NoOp : KeyEvent         // ON key, blank keys
}
```

### Acceptance criteria
- `:app:build` compiles with Hilt.
- `CalculatorViewModel` injects without error (verified by running on emulator).
- State round-trips through JSON serialization without data loss.

---

## Step B4 — Display Panel

The display occupies the full width of the screen at the top, roughly 28% of screen height.
It is divided into two bands:

1. **Annunciator row** — thin strip at the top of the display area.
2. **Main number row** — the DSEG7 formatted number, right-aligned.

### Layout proportions (for 2400×1080 landscape = 2400w × 1080h)

| Region | Height | Notes |
|---|---|---|
| Display panel | ~280dp | Silver bezel border, green background |
| Annunciator strip | 24dp | Inside display, top edge |
| Main number | fills rest | DSEG7, right-aligned, single line |
| Key grid | remaining | fills rest of screen height |

### `AnnunciatorRow.kt`

A `Row` of small `Text` composables. Each indicator is either `AnnunciatorOn` or
`AnnunciatorOff` color.

Indicators (left to right):
- Display mode: `FIX` `SCI` `ENG` `ALL` — only the active one is "on"
- Angle mode: `DEG` `RAD`
- Shift latch: `f` (amber when shiftActive)

```kotlin
@Composable
fun AnnunciatorRow(state: CalculatorState, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        // Display mode group
        DisplayModeLabel("FIX", state.displaySettings.mode is DisplayMode.Fix, ...)
        DisplayModeLabel("SCI", state.displaySettings.mode is DisplayMode.Sci, ...)
        DisplayModeLabel("ENG", state.displaySettings.mode is DisplayMode.Eng, ...)
        DisplayModeLabel("ALL", state.displaySettings.mode is DisplayMode.All, ...)
        Spacer(Modifier.weight(1f))
        // Angle mode
        AngleLabel("DEG", state.angleMode == AngleMode.DEG, ...)
        AngleLabel("RAD", state.angleMode == AngleMode.RAD, ...)
        Spacer(Modifier.weight(1f))
        // Shift
        ShiftIndicator(state.shiftActive)
    }
}
```

### `DisplayPanel.kt`

```kotlin
@Composable
fun DisplayPanel(
    uiState: CalculatorUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(3.dp, CalcColors.DisplayBezel, RoundedCornerShape(6.dp))
            .background(CalcColors.DisplayBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            AnnunciatorRow(uiState.calcState)
            Text(
                text = uiState.displayString,
                fontFamily = Dseg7,
                fontSize = 36.sp,
                color = CalcColors.DisplayText,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}
```

The `displayString` is computed by `CalculatorEngine.getDisplay(state)` inside the ViewModel
whenever state changes. **After** obtaining that plain string from `:logic`, the ViewModel
(or a pure formatting helper in `:app`) inserts comma separators into the integer part before
storing the result in `CalculatorUiState.displayString`.

### Digit grouping (UI layer only)

`:logic`'s `DisplayFormatter` produces plain strings such as `"1234567.89"`. The `:app`
layer post-processes the string to insert zero-width commas:

- Split on the decimal point (or treat the whole string as the integer part if there is none).
- Group the integer digits in threes from the right: `"1234567"` → `"1,234,567"`.
- Rejoin with the decimal portion: `"1,234,567.89"`.
- The comma character (U+002C) renders via the zero-width glyph in the modified DSEG7 font,
  so it does not consume display width.
- Negative sign and exponent strings (`e+07`) are never comma-grouped.
- SCI / ENG formatted strings (mantissa + exponent) are not grouped — only the mantissa
  integer part would be at most one or two digits, making grouping meaningless.
- `:logic` is never modified for this feature. Its contract is: produce a plain numeric
  string. Comma insertion lives entirely in `:app`.

A pure function `fun insertThousandsCommas(plain: String): String` in
`ui/calculator/DisplayFormatter.kt` (`:app` side) handles this transformation and is
independently unit-testable with standard JVM tests (no Android dependency needed).

### Acceptance criteria
- Display renders DSEG7 font correctly.
- Annunciators reflect live state (FIX mode lit, DEG lit by default, shift unlit).
- Negative zero never shows `-0`.
- Values ≥ 1000 show comma separators; values in SCI/ENG format do not.

---

## Step B5 — CalcKey Component

### `KeyDef.kt`

```kotlin
data class KeyDef(
    val primaryLabel: String,
    val shiftedLabel: String = "",          // empty = no shifted function
    val event: KeyEvent,
    val shiftedEvent: KeyEvent = KeyEvent.NoOp,
    val keyColor: Color = CalcColors.KeyTop,
    val labelColor: Color = CalcColors.LabelPrimary,
    val widthWeight: Float = 1f,            // for ENTER (2f spans 2 rows visually)
    val heightWeight: Float = 1f,
)
```

### `CalcKey.kt`

Each key is a `Box` with:
- Rounded rectangle shape (corner radius ~4.dp)
- Two `Text` labels: shifted label (small, amber, top) + primary label (larger, white, bottom)
- `indication = null` on `clickable` — we supply our own pressed visual + haptic
- Pressed state via `interactionSource` → tint key surface with `KeyPressed`
- Haptic feedback via `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`

```kotlin
@Composable
fun CalcKey(
    def: KeyDef,
    shiftActive: Boolean,
    onKey: (KeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectiveEvent = if (shiftActive && def.shiftedEvent != KeyEvent.NoOp)
        def.shiftedEvent else def.event

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(...)   // see B6 for sizing
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isPressed) CalcColors.KeyPressed else def.keyColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKey(effectiveEvent)
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (def.shiftedLabel.isNotEmpty()) {
                Text(
                    text = def.shiftedLabel,
                    color = CalcColors.LabelShifted,
                    fontSize = 8.sp,
                    lineHeight = 10.sp
                )
            }
            Text(
                text = def.primaryLabel,
                color = def.labelColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

### Acceptance criteria
- Key renders both labels.
- Haptic fires on every tap.
- Pressed state visually darkens key.
- Shift-active key shows shifted label more prominently (optional: bold or slightly larger).

---

## Step B6 — Key Layout Data & KeyGrid

### Key grid definition

The grid is 4 rows × 10 columns. ENTER is a special double-height key occupying column 5
(0-indexed) of rows 2–3. It is implemented as a `Box` placed with a `Layout` or by using
`Modifier.weight` in a nested `Column`/`Row` structure.

**Implementation approach:** Use a `Column` of four `Row`s. ENTER is excluded from rows 3
and 4 and instead placed using an `overlapping Box` or via a custom `Layout`. The simplest
approach that avoids a custom layout:

- Row 3 has 9 keys + a `Spacer` of 1 column width in column 5.
- Row 4 has 4 keys, then ENTER (2-row-height Box placed using `Modifier.fillMaxHeight()` in
  an outer box), then 4 keys.
- To achieve true double-height ENTER without a custom Layout: wrap rows 3 and 4 in a `Row`
  where the left 5 columns are a `Column(rows 3a+4a)`, ENTER is a single tall key in the
  center, and the right 4 columns are a `Column(rows 3b+4b)`.

**Row grouping:**

```
BottomHalf = Row {
    Column(weight=5) {         // left 5 key columns, rows 3 and 4
        Row { keys row3[0..4] }
        Row { keys row4[0..4] }
    }
    EnterKey(weight=1)         // spans full height of BottomHalf
    Column(weight=4) {         // right 4 key columns, rows 3 and 4
        Row { keys row3[5..8] }
        Row { keys row4[5..8] }
    }
}
```

### Full key list (`KeyDef` instances)

```kotlin
val keyRow1 = listOf(
    KeyDef("√x",  "x²",    KeyEvent.Sqrt,       KeyEvent.Square),
    KeyDef("eˣ",  "LN",    KeyEvent.Exp,        KeyEvent.Ln),
    KeyDef("10ˣ", "LOG",   KeyEvent.Pow10,      KeyEvent.Log),
    KeyDef("yˣ",  "→P",    KeyEvent.Power,      KeyEvent.ToPolar),
    KeyDef("1/x", "→R",    KeyEvent.Reciprocal, KeyEvent.ToRect),
    KeyDef("CHS", "ALL",   KeyEvent.Chs,        KeyEvent.AllMode),
    KeyDef("7",   "FIX",   KeyEvent.Digit(7),   KeyEvent.FixArg),
    KeyDef("8",   "SCI",   KeyEvent.Digit(8),   KeyEvent.SciArg),
    KeyDef("9",   "ENG",   KeyEvent.Digit(9),   KeyEvent.EngArg),
    KeyDef("÷",   "",      KeyEvent.Divide,     keyColor = CalcColors.KeyArith),
)

val keyRow2 = listOf(
    KeyDef("n!",  "D/R",   KeyEvent.Factorial,  KeyEvent.DegRad),
    KeyDef("nCr", "nPr",   KeyEvent.NCr,        KeyEvent.NPr),
    KeyDef("SIN", "SIN⁻¹", KeyEvent.Sin,        KeyEvent.ArcSin),
    KeyDef("COS", "COS⁻¹", KeyEvent.Cos,        KeyEvent.ArcCos),
    KeyDef("TAN", "TAN⁻¹", KeyEvent.Tan,        KeyEvent.ArcTan),
    KeyDef("EEX", "",      KeyEvent.Eex),
    KeyDef("4",   "",      KeyEvent.Digit(4)),
    KeyDef("5",   "",      KeyEvent.Digit(5)),
    KeyDef("6",   "",      KeyEvent.Digit(6)),
    KeyDef("×",   "",      KeyEvent.Multiply,   keyColor = CalcColors.KeyArith),
)

val keyRow3Left = listOf(     // columns 0–4 of row 3
    KeyDef("%",   "Δ%",    KeyEvent.Percent,    KeyEvent.PercentChange),
    KeyDef("R↓",  "",      KeyEvent.RollDown),
    KeyDef("X↔Y", "LstX",  KeyEvent.Swap,       KeyEvent.LastX),
    KeyDef("←",   "",      KeyEvent.Backspace),
    KeyDef("CLX", "",      KeyEvent.Clx),
)

val enterKey = KeyDef("ENTER", "", KeyEvent.Enter, heightWeight = 2f)

val keyRow3Right = listOf(    // columns 6–9 of row 3
    KeyDef("1",   "",      KeyEvent.Digit(1)),
    KeyDef("2",   "",      KeyEvent.Digit(2)),
    KeyDef("3",   "",      KeyEvent.Digit(3)),
    KeyDef("−",   "",      KeyEvent.Subtract,   keyColor = CalcColors.KeyArith),
)

val keyRow4Left = listOf(     // columns 0–4 of row 4
    KeyDef("ON",  "",      KeyEvent.NoOp),
    KeyDef("SHIFT","",     KeyEvent.Shift,      keyColor = CalcColors.KeyShift,
                                                  labelColor = CalcColors.LabelShiftKey),
    KeyDef("",    "",      KeyEvent.NoOp),      // blank column
    KeyDef("STO", "",      KeyEvent.Sto),
    KeyDef("RCL", "",      KeyEvent.Rcl),
)

val keyRow4Right = listOf(    // columns 6–9 of row 4
    KeyDef("0",   "",      KeyEvent.Digit(0)),
    KeyDef(".",   "",      KeyEvent.Decimal),
    KeyDef("π",   "",      KeyEvent.Pi),
    KeyDef("+",   "",      KeyEvent.Add,        keyColor = CalcColors.KeyArith),
)
```

### `KeyGrid.kt`

```kotlin
@Composable
fun KeyGrid(
    shiftActive: Boolean,
    onKey: (KeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        // Rows 1 and 2 — simple full-width rows
        KeyRow(keyRow1, shiftActive, onKey, Modifier.weight(1f))
        KeyRow(keyRow2, shiftActive, onKey, Modifier.weight(1f))

        // Rows 3+4 combined with double-height ENTER
        Row(modifier = Modifier.weight(2f)) {
            // Left 5 columns
            Column(modifier = Modifier.weight(5f)) {
                KeyRow(keyRow3Left, shiftActive, onKey, Modifier.weight(1f))
                KeyRow(keyRow4Left, shiftActive, onKey, Modifier.weight(1f))
            }
            // ENTER — double height
            CalcKey(
                def = enterKey,
                shiftActive = shiftActive,
                onKey = onKey,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            // Right 4 columns
            Column(modifier = Modifier.weight(4f)) {
                KeyRow(keyRow3Right, shiftActive, onKey, Modifier.weight(1f))
                KeyRow(keyRow4Right, shiftActive, onKey, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<KeyDef>,
    shiftActive: Boolean,
    onKey: (KeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        keys.forEach { def ->
            CalcKey(
                def = def,
                shiftActive = shiftActive,
                onKey = onKey,
                modifier = Modifier.weight(def.widthWeight).fillMaxHeight()
            )
        }
    }
}
```

### Acceptance criteria
- All 38 key positions render at correct sizes.
- ENTER is visually taller than other keys.
- No key overflows or clips.
- Layout fills the screen with no scrolling.

---

## Step B7 — Calculator Screen & Route

### `CalculatorScreen.kt`

Pure Composable — receives all data and callbacks, holds no ViewModel reference.

```kotlin
@Composable
fun CalculatorScreen(
    uiState: CalculatorUiState,
    onKey: (KeyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcColors.Body)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        DisplayPanel(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f)   // ~28% of screen height for display
        )
        Spacer(Modifier.height(6.dp))
        KeyGrid(
            shiftActive = uiState.calcState.shiftActive,
            onKey = onKey,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.72f)
        )
    }
}
```

### `CalculatorRoute.kt`

```kotlin
@Composable
fun CalculatorRoute(
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalculatorScreen(
        uiState = uiState,
        onKey = viewModel::onKey,
    )
}
```

### Previews

```kotlin
@Preview(
    name = "Calculator — Landscape",
    widthDp = 800, heightDp = 360,
    showBackground = true, backgroundColor = 0xFF1C1C1E
)
@Composable
private fun CalculatorScreenPreview() {
    CalcTheme {
        CalculatorScreen(
            uiState = CalculatorUiState(displayString = "3.1416"),
            onKey = {},
        )
    }
}
```

### Acceptance criteria
- Screen renders end-to-end in the preview at 800×360dp.
- All keys visible and labelled.
- Display shows the formatted number from `CalculatorEngine.getDisplay()`.

---

## Step B8 — MainActivity & System UI

### `RpnCalcApplication.kt` (new file)

```kotlin
@HiltAndroidApp
class RpnCalcApplication : Application()
```

### `AndroidManifest.xml`

```xml
<application
    android:name=".RpnCalcApplication"
    ...>
    <activity
        android:name=".MainActivity"
        android:screenOrientation="landscape"
        android:windowSoftInputMode="stateAlwaysHidden"
        android:configChanges="orientation|screenSize|keyboard|keyboardHidden"
        ...>
```

### `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CalcTheme {
                CalculatorRoute()
            }
        }
    }
}
```

### System UI styling

Status bar and navigation bar should be transparent so the dark calculator body bleeds to
the edges. Handled by `enableEdgeToEdge()` plus the dark `CalcTheme` background color.

### Acceptance criteria
- App launches in landscape on the Moto G5 Power (2400×1080).
- No portrait rotation.
- Status bar is transparent / blends with body.
- `./gradlew :app:build` is clean.
- `./gradlew :logic:test` still passes (zero regressions).

---

## Step B9 — App-Layer Unit Tests

All tests in this step run on the JVM with `./gradlew :app:test`. No emulator required.
Test files live under `app/src/test/java/com/brocla/rpn_calc/`.

---

### B9.1 — `DisplayFormatterTest` (comma insertion)

File: `ui/calculator/DisplayFormatterTest.kt`

The function under test: `fun insertThousandsCommas(plain: String): String`

```
// Values below 1000 — no comma inserted
noComma_twoDigits         "42"          → "42"
noComma_threeDigits       "999"         → "999"
noComma_withDecimal       "42.5"        → "42.5"

// Exactly 4+ integer digits
comma_fourDigits          "1000"        → "1,000"
comma_sevenDigits         "1234567"     → "1,234,567"
comma_tenDigits           "1234567890"  → "1,234,567,890"

// With decimal portion
comma_withDecimal         "1234567.89"  → "1,234,567.89"
comma_decimalOnly         "0.001"       → "0.001"        // no integer grouping needed

// Negative numbers
comma_negative            "-1234.5"     → "-1,234.5"
noComma_negativeSmall     "-42.0"       → "-42.0"

// SCI/ENG strings — must NOT be grouped (contains 'e')
noComma_sciPositive       "1.234e+07"   → "1.234e+07"
noComma_sciNegative       "-3.5e-12"    → "-3.5e-12"
noComma_engFormat         "12.35e+03"   → "12.35e+03"

// Edge cases
noComma_zero              "0"           → "0"
noComma_zero_decimal      "0.0"         → "0.0"
comma_exactlyOneGroup     "1000000"     → "1,000,000"
```

---

### B9.2 — State Serialization Round-Trip

File: `ui/calculator/CalculatorStateSerializationTest.kt`

Tests that `CalculatorState` survives a JSON encode → decode cycle without data loss.
Uses `kotlinx.serialization.json.Json` directly — no Android dependencies.

```
roundTrip_defaultState
    Encode CalculatorState() to JSON string, decode back.
    Assert all fields equal the original.

roundTrip_stackWithValues
    State with stack X=1.23, Y=4.56, Z=7.89, T=0.1
    Assert stack registers survive exactly (no floating-point drift from serialization).

roundTrip_entryState_mantissa
    EntryState.Mantissa(digits="314", hasDecimal=true, isNegative=false)
    Assert EntryState type and fields survive.

roundTrip_entryState_exponent
    EntryState.Exponent with mantissa "1", hasDecimal=true, exponentDigits="07", exponentIsNegative=true
    Assert all six fields survive.

roundTrip_displayMode_fix
    DisplaySettings(mode=DisplayMode.Fix(4))
    Assert mode type and decimalPlaces survive.

roundTrip_displayMode_sci
    DisplaySettings(mode=DisplayMode.Sci(3))

roundTrip_displayMode_all
    DisplaySettings(mode=DisplayMode.All)
    Assert object singleton survives (not a new instance with wrong type).

roundTrip_angleMode_rad
    AngleMode.RAD survives.

roundTrip_memoryRegisters
    memory = listOf(1.1, 2.2, 3.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.9)
    Assert all 10 registers survive in order.

roundTrip_errorState
    error = "Error"
    Assert non-null error string survives.

roundTrip_shiftActive
    shiftActive = true
    Assert boolean survives.

roundTrip_lastX
    lastX = -3.14159
    Assert value survives with full precision.
```

---

### B9.3 — `CalculatorViewModel` Unit Tests

File: `ui/calculator/CalculatorViewModelTest.kt`

The ViewModel is constructed with a real `CalculatorEngine` (same as Phase A tests) and a
`FakeSavedStateHandle` (a simple `MutableMap<String, Any?>` wrapper — no Android runtime
needed). Use `kotlinx-coroutines-test` and `runTest` for coroutine collection.

#### PendingOp — STO

```
sto_digitStores
    Press STO. Assert pendingOp == Sto, stack unchanged.
    Press Digit(3). Assert pendingOp == None, memory[3] == X.

sto_nonDigitCancels
    Press STO. Press Add. Assert pendingOp == None, no memory write, Add executes normally.

sto_cancelDoesNotConsumeKey
    Press 5, ENTER, 3, STO, Add.
    Assert result is 8.0 (Add executed after cancel, not swallowed).
```

#### PendingOp — RCL

```
rcl_digitRecalls
    Store 99.0 in register 2. Press RCL, Digit(2).
    Assert pendingOp == None, stack.x == 99.0.

rcl_nonDigitCancels
    Press RCL. Press Subtract. Assert pendingOp == None, Subtract executes normally.
```

#### PendingOp — FIX / SCI / ENG

```
fix_digitSetsMode
    Press Shift, FixArg, Digit(4).
    Assert displaySettings.mode == DisplayMode.Fix(4).

fix_nonDigitCancels
    Press Shift, FixArg, Add.
    Assert mode unchanged, Add executes normally.

sci_digitSetsMode
    Press Shift, SciArg, Digit(2).
    Assert displaySettings.mode == DisplayMode.Sci(2).

eng_digitSetsMode
    Press Shift, EngArg, Digit(3).
    Assert displaySettings.mode == DisplayMode.Eng(3).
```

#### Shift Latch

```
shift_activatesLatch
    Press Shift. Assert calcState.shiftActive == true.

shift_againKeepsLatchActive
    Press Shift, Shift. Assert shiftActive == true.
    (HP spec §11.1: second SHIFT does not toggle off.)

shift_clearedByShiftedKey
    Press Shift, Square (shifted √x). Assert shiftActive == false after key.

shift_clearedByUnshiftedKeyWithNoShiftedFunction
    Press Shift, then a key with no shifted function (e.g. Add).
    Assert shiftActive == false, key was a no-op (no math executed).
```

#### Error Handling

```
error_anyKeyClearsError
    Force an error (e.g. Digit(0), Reciprocal → divide by zero).
    Assert calcState.error != null.
    Press any key (e.g. Add). Assert calcState.error == null.

error_firstKeyAfterErrorDoesNotExecute
    Force error. Press Add.
    Assert error cleared but Add did NOT execute (Y+X not computed).
    Press Add again. Assert Add executes normally.

error_clearsBeforePendingOp
    Force error. Press STO.
    Assert error cleared, pendingOp still == None (STO not armed while in error).
```

#### Display String

```
displayString_hasCommasForLargeValues
    Set stack.x = 1234567.0. Assert uiState.displayString contains ","

displayString_noCommasForSciFormat
    Set displayMode to Sci(3). Set stack.x = 1234567.0.
    Assert uiState.displayString matches SCI pattern (no comma).

displayString_updatesAfterEveryKey
    Press Digit(5), assert displayString reflects "5".
    Press Enter, Digit(3), Add, assert displayString reflects "8".
```

#### State Persistence

```
persistence_stateRoundTrips
    Set a non-default state (stack values, FIX mode, RAD angle mode, memory value).
    Call saveState(). Construct a new ViewModel with the same FakeSavedStateHandle.
    Assert the new ViewModel's uiState matches the saved state.
```

---

### Acceptance criteria

- All B9 tests pass with `./gradlew :app:test`.
- No Android SDK imports in any test file (confirms pure JVM execution).
- Combined app test count ≥ 40 new tests across the three files.

---

## Phase B Completion Criteria

1. `./gradlew :app:build` succeeds with zero warnings about missing resources.
2. `./gradlew :app:test` — all B9 tests pass (≥ 40 tests across DisplayFormatter, serialization, ViewModel).
3. `./gradlew :logic:test` — 203 tests, zero failures (Phase A unaffected).
3. App runs on Moto G5 Power (2400×1080, landscape):
   - Display shows formatted value with DSEG7 font.
   - All 38 keys are tappable and produce correct results.
   - Haptic feedback fires on every key.
   - Shift key lights the `f` annunciator and executes the shifted function on the next key.
   - STO → digit and RCL → digit round-trip correctly.
   - FIX → digit, SCI → digit, ENG → digit update the display mode.
   - Error state shows error text; any key clears it.
   - State (stack, memory, display mode, angle mode) survives app backgrounding and process kill.
4. No Android imports anywhere in `:logic/src/main/`.
5. `CalculatorScreen` is testable independently of `CalculatorViewModel` (verified by Preview
   rendering without Hilt).
