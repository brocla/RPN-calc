# Implementation Plan — RPN Calculator v2.1

Implements the features in REQUIREMENTS_V2.1.md in three phases (G–I).
Each phase is independently buildable and mergeable.

---

## Dependencies to add

### `gradle/libs.versions.toml`
```toml
[versions]
datastorePreferences = "1.1.1"

[libraries]
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
```

### `app/build.gradle.kts`
```kotlin
implementation(libs.androidx.datastore.preferences)
```

---

## Phase G — DataStore Persistence

**Goal:** Replace `SavedStateHandle` with `DataStore<Preferences>` so state survives
device restarts, not just process death. Persist stack (X/Y/Z/T), memory registers
(M0–M9), display mode, and angle mode. Partial entry is committed before saving.

### G1 — `data/CalcStateRepository.kt` (new)

Wraps the DataStore. The entire `CalculatorState` is serialized to a single JSON string
under one key — no schema design needed, since `CalculatorState` is already
`@Serializable`.

```kotlin
object CalcPersistenceKeys {
    val CALC_STATE = stringPreferencesKey("calc_state")
}

class CalcStateRepository(
    private val dataStore: DataStore<Preferences>,
    private val entryStateMachine: EntryStateMachine,
) {
    val calcState: Flow<CalculatorState?>   // null on missing/parse failure

    suspend fun save(state: CalculatorState) {
        // Commit partial entry before saving
        val committed = entryStateMachine.completeEntry(state)
        dataStore.edit { it[CalcPersistenceKeys.CALC_STATE] = Json.encodeToString(committed) }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(CalcPersistenceKeys.CALC_STATE) }
    }
}
```

### G2 — `di/CalculatorModule.kt` changes

Provide `DataStore<Preferences>` and `CalcStateRepository` as singletons:

```kotlin
@Provides @Singleton
fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("calc_state") }

@Provides @Singleton
fun provideCalcStateRepository(
    dataStore: DataStore<Preferences>,
    entryStateMachine: EntryStateMachine,
): CalcStateRepository = CalcStateRepository(dataStore, entryStateMachine)
```

### G3 — `CalculatorViewModel.kt` changes

- Inject `CalcStateRepository` (replaces `SavedStateHandle`)
- `init`: collect first emission from `repository.calcState` to set initial state
  (use `runBlocking { repository.calcState.first() }` to avoid a loading state in the UI)
- `saveAndEmit()`: add `viewModelScope.launch { repository.save(newUi.calcState) }`
- Remove `SavedStateHandle` injection and all `Json` serialization from the ViewModel

### G4 — Test infrastructure: in-memory DataStore

The current `CalculatorViewModelTest` constructs the ViewModel directly:
```kotlin
vm = CalculatorViewModel(engine, SavedStateHandle())
```

After Phase G, `SavedStateHandle` is removed. Tests instead construct a
`CalcStateRepository` backed by an **in-memory DataStore** using
`PreferenceDataStoreFactory.createWithDefaults()` (from the
`datastore-preferences` test artifact) or a `TestDataStore` wrapper:

```kotlin
private fun testRepository(): CalcStateRepository {
    val dataStore = PreferenceDataStoreFactory.createWithDefaults(
        scope = testScope,
        produceFile = { testContext.preferencesDataStoreFile("test") },
    )
    return CalcStateRepository(dataStore, EntryStateMachine())
}

// ViewModel setUp:
vm = CalculatorViewModel(engine, testRepository())
```

All existing ViewModel tests continue to work unchanged because the repository
starts empty (equivalent to a fresh `SavedStateHandle`).

### G5 — Tests: `CalcStateRepositoryTest.kt` (new)

Use an in-memory DataStore constructed as above — no file I/O, no Android runtime.

```
save_and_reload_preserves_stack         push 3 ENTER 2 ENTER 1, save, reload → X=1 Y=2 Z=3
save_and_reload_preserves_memory        STO to M3=42, save, reload → M3=42
save_and_reload_preserves_display_mode  set FIX 2, save, reload → FIX 2
save_and_reload_preserves_angle_mode    set RAD, save, reload → RAD
save_commits_partial_entry              mid-entry "3.1", save, reload → X=3.1 (Idle state)
clear_resets_to_default                 save state, clear, reload → X=0, ALL mode, DEG
```

---

## Phase H — In-App Reset

**Goal:** Long-press on Backspace (⌫) shows a confirmation dialog and resets all
calculator state. Depends on Phase G (`repository.clear()`).

### H1 — `CalcKeyEvent.kt`

```kotlin
data object ResetRequest : CalcKeyEvent   // intercepted in CalculatorRoute; never reaches ViewModel dispatch
```

### H2 — `CalculatorViewModel.kt`

```kotlin
fun reset() {
    viewModelScope.launch {
        repository.clear()
        val default = CalculatorState()
        _uiState.value = CalculatorUiState(
            calcState      = default,
            displayString  = buildDisplay(default),
            yDisplayString = buildYDisplay(default),
        )
    }
}
```

### H3 — `ui/layouts/LayoutDescriptor.kt`

Add `longPressEvent` to `KeySlot.Key`:

```kotlin
data class Key(
    val keyDef: KeyDef,
    val weight: Float = 1f,
    val longPressEvent: CalcKeyEvent = CalcKeyEvent.NoOp,
) : KeySlot
```

### H4 — `ui/calculator/components/CalcKey.kt`

Add `onLongPress: (() -> Unit)? = null` parameter. Add a second `pointerInput` block
using `detectTapGestures(onLongPress = { haptic.performHapticFeedback(...); onLongPress?.invoke() })`.

### H5 — `ui/layouts/LayoutRenderer.kt`

Pass `slot.longPressEvent` through to `CalcKey.onLongPress`:

```kotlin
CalcKey(
    ...
    onLongPress = if (slot.longPressEvent != CalcKeyEvent.NoOp)
        { onKey(slot.longPressEvent) } else null,
)
```

### H6 — `kbLayout.kt` and `ClassicLandscapeLayout.kt`

Add `longPressEvent = CalcKeyEvent.ResetRequest` to the Backspace slot in both layouts.

### H7 — `CalculatorRoute.kt`

Intercept `ResetRequest` (alongside `OpenLayoutPicker`):

```kotlin
var showResetConfirmation by remember { mutableStateOf(false) }

val onKey: (CalcKeyEvent) -> Unit = { event ->
    when (event) {
        CalcKeyEvent.OpenLayoutPicker -> showLayoutPicker = true
        CalcKeyEvent.ResetRequest    -> showResetConfirmation = true
        else                         -> viewModel.onKey(event)
    }
}

if (showResetConfirmation) {
    AlertDialog(
        onDismissRequest = { showResetConfirmation = false },
        title = { Text("Reset Calculator") },
        text  = { Text("Clear all stack and memory registers?") },
        confirmButton = {
            TextButton(onClick = { viewModel.reset(); showResetConfirmation = false }) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
        },
    )
}
```

### H8 — Tests (in `CalculatorViewModelTest.kt`)

```
reset_clearsStack           push 5 ENTER 3, reset(), assert X=0 Y=0 Z=0 T=0
reset_clearsMemory          STO M2=7, reset(), assert M2=0
reset_clearsDisplayMode     set FIX 4, reset(), assert mode is ALL
reset_clearsAngleMode       set RAD, reset(), assert angleMode == DEG
```

---

## Phase I — Copy / Paste

**Goal:** Long-press on the display panel shows Copy / Paste menu. Copy puts the
X register display string on the system clipboard. Paste reads, preprocesses, validates,
and pushes to X. Independent of Phases G and H.

### I1 — `ui/calculator/ClipboardParser.kt` (new)

Defined as an **interface + default implementation**, both injectable via Hilt.
Pure Kotlin — no Android dependencies — fully unit testable on the JVM.

```kotlin
interface ClipboardParser {
    sealed interface Result {
        data class Success(val value: Double) : Result
        data object Invalid : Result
    }
    fun parse(raw: String): Result
}

class ClipboardParserImpl @Inject constructor() : ClipboardParser {
    override fun parse(raw: String): ClipboardParser.Result {
        // 1. Strip chars not in [0-9 . , + - E e]
        // 2. Remove commas
        // 3. Attempt toDoubleOrNull()
        // 4. Return Success or Invalid
    }
}
```

Provide in `CalculatorModule.kt`:
```kotlin
@Binds @Singleton
abstract fun bindClipboardParser(impl: ClipboardParserImpl): ClipboardParser
```

Inject `ClipboardParser` into `CalculatorViewModel`. Tests instantiate
`ClipboardParserImpl` directly or supply a fake via the interface.

### I2 — `CalcKeyEvent.kt`

```kotlin
data class PasteValue(val value: Double) : CalcKeyEvent
```

### I3 — `CalculatorViewModel.kt`

Handle `PasteValue` in `dispatch()` — commit entry, lift stack, place value in X,
`AnimationType.None`.

Add:
```kotlin
fun pasteFromClipboard(raw: String) {
    when (val result = ClipboardParser.parse(raw)) {
        is ClipboardParser.Result.Success ->
            onKey(CalcKeyEvent.PasteValue(result.value))
        ClipboardParser.Result.Invalid    ->
            saveAndEmit(_uiState.value.copy(
                calcState = _uiState.value.calcState.copy(error = "Paste: invalid input"),
            ))
    }
}
```

### I4 — `DisplayPanel.kt` and `PortraitDisplayPanel.kt`

Add `onLongPress: () -> Unit = {}` parameter. Add `pointerInput` with
`detectTapGestures(onLongPress = { onLongPress() })` to the Column modifier.

### I5 — `CalculatorScreen.kt`

Add `onDisplayLongPress: () -> Unit` parameter and pass it to whichever display panel
is active.

### I6 — `CalculatorRoute.kt`

```kotlin
val clipboardManager = LocalClipboardManager.current
var showClipboardMenu by remember { mutableStateOf(false) }

// Passed to CalculatorScreen:
val onDisplayLongPress = { showClipboardMenu = true }

if (showClipboardMenu) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { showClipboardMenu = false },
    ) {
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = {
                clipboardManager.setText(AnnotatedString(uiState.displayString))
                showClipboardMenu = false
            },
        )
        DropdownMenuItem(
            text = { Text("Paste") },
            onClick = {
                val text = clipboardManager.getText()?.text ?: ""
                viewModel.pasteFromClipboard(text)
                showClipboardMenu = false
            },
        )
    }
}
```

### I7 — Tests: `ClipboardParserTest.kt` (new, pure JVM)

```
parse_plainInteger              "12345"            → 12345.0
parse_withCommas                "1,234,567"        → 1234567.0
parse_negativeWithCommas        "-1,234.56"        → -1234.56
parse_scientificUpperE          "1.5E+04"          → 15000.0
parse_scientificLowerE          "1.5e-3"           → 0.0015
parse_displayString_allMode     "3.141592653"      → 3.141592653
parse_displayString_fixMode     "3.14"             → 3.14
parse_displayString_sciMode     "3.14e+02"         → 314.0
parse_leadingTrailingSpaces     "  42  "           → 42.0
parse_embeddedCurrencySymbol    "$1,234.56"        → 1234.56
parse_emptyString               ""                 → Invalid
parse_lettersOnly               "abc"              → Invalid
parse_multipleDecimalPoints     "1.2.3"            → Invalid
parse_onlyCommas                ",,,,"             → Invalid
parse_onlySign                  "-"                → Invalid
parse_validAfterHeavyStripping  "USD 1,234.00 cr"  → 1234.0
```

### I8 — Tests (in `CalculatorViewModelTest.kt`)

```
paste_liftsStackAndPlacesValue  X=5 ENTER, paste 3.0 → Y=5 X=3
paste_fromIdleLiftsStack        paste 7.0 from idle   → X=7
paste_invalidShowsError         paste "abc"           → error != null
paste_commitsPartialEntry       type "3.1", paste 9.0 → Y=3.1 X=9
```

---

## File Inventory

| File | Action |
|---|---|
| `gradle/libs.versions.toml` | Add DataStore version + library alias |
| `app/build.gradle.kts` | Add DataStore dependency |
| `di/CalculatorModule.kt` | Provide `DataStore`, `CalcStateRepository`; bind `ClipboardParserImpl` |
| `data/CalcStateRepository.kt` | **New** — DataStore read / write / clear |
| `ui/calculator/CalcKeyEvent.kt` | Add `ResetRequest`, `PasteValue` |
| `ui/calculator/CalculatorViewModel.kt` | Inject repository; add `reset()`, `pasteFromClipboard()` |
| `ui/calculator/ClipboardParser.kt` | **New** — `ClipboardParser` interface + `ClipboardParserImpl` |
| `ui/calculator/CalculatorRoute.kt` | Intercept `ResetRequest`; AlertDialog; copy/paste menu |
| `ui/calculator/CalculatorScreen.kt` | Add `onDisplayLongPress` param |
| `ui/calculator/components/DisplayPanel.kt` | Add `onLongPress` + pointerInput |
| `ui/calculator/components/PortraitDisplayPanel.kt` | Same |
| `ui/calculator/components/CalcKey.kt` | Add `onLongPress` param + detectTapGestures |
| `ui/layouts/LayoutDescriptor.kt` | Add `longPressEvent` to `KeySlot.Key` |
| `ui/layouts/LayoutRenderer.kt` | Thread `longPressEvent` to `CalcKey` |
| `ui/layouts/kbLayout.kt` | Add `longPressEvent = ResetRequest` to Backspace |
| `ui/layouts/ClassicLandscapeLayout.kt` | Same |
| `test/.../CalcStateRepositoryTest.kt` | **New** — 6 persistence tests |
| `test/.../ClipboardParserTest.kt` | **New** — 16 parse tests |
| `test/.../CalculatorViewModelTest.kt` | Add 4 reset + 4 paste tests |

---

## Dependencies between phases

```
G (DataStore) ──► H (Reset needs repository.clear())

I (Copy/Paste) — independent; ClipboardParser tests can be written first
```

C and E1–E3 can be developed in parallel. D should follow C. F requires E to be complete.
