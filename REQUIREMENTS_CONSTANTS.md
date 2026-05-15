# RPN Calculator — Constants Library Feature Requirements

Standalone feature requirement. Intended for integration into a future release. All existing requirements remain in force.

---

## 1. Overview

A **Constants Library** provides one-tap access to mathematical, physical, engineering, and materials constants. The user opens the library, browses or searches, taps a constant, and that value is loaded into the X register. The library then closes.

---

## 2. Activation

### 2.1 CONST Key

A new **CONST** key is added to the keypad. The exact grid position is a design-time decision; it does not require a shift state to activate.

Pressing CONST opens the Constants Library bottom sheet. No other key opens the library.

### 2.2 Haptic Feedback

The CONST key produces the same haptic feedback as all other key presses.

---

## 3. Constants Library UI

### 3.1 Bottom Sheet

The library is presented as a **modal bottom sheet** (Material 3 `ModalBottomSheet`). It opens from the bottom of the screen and covers approximately two-thirds of the display. The calculator display and keypad are dimmed but visible behind it.

The bottom sheet is dismissed by:
- Tapping a constant (loads value, closes sheet)
- Swiping the sheet down
- Tapping the dimmed area behind the sheet

No explicit close button is required; the swipe-to-dismiss gesture is standard Android behavior.

### 3.2 Layout

Top to bottom within the sheet:

1. **Search bar** — single-line text input, always visible at the top, auto-focused when the sheet opens
2. **Category list / results list** — scrollable, takes the remaining height

### 3.3 Search Bar

- Placeholder text: `Search constants…`
- Filters the full constant list in real time as the user types
- Matching is case-insensitive and searches across: constant name, symbol, and category name
- When the search field is empty, the category browse view is shown
- When the search field contains text, the flat filtered results list is shown (no category headers)
- Clearing the search field returns to the category browse view
- A clear (×) button appears inside the field when it contains text

### 3.4 Category Browse View

When search is empty, constants are organized under expandable category headers. Each category header shows the category name. Tapping a header expands or collapses that category. All categories are collapsed by default.

Each constant row shows:
- **Name** (e.g. "Speed of light")
- **Symbol** (e.g. *c*), right-aligned, in italic
- **Value** formatted in scientific notation where appropriate (e.g. `2.99792458 × 10⁸`)
- **Unit** (e.g. `m/s`), subdued text

For the **Materials** category, see §3.5.

### 3.5 Materials Sub-Navigation

Materials constants are multi-property (a given material has several properties, each a distinct value). The Materials category uses two-level navigation:

1. Tapping **Materials** expands a list of material names
2. Tapping a material name expands its properties inline (Yield Strength, Modulus of Elasticity, Coefficient of Thermal Expansion, Ductility)
3. Tapping a property row loads that value into X and closes the sheet

This two-level expand/collapse is local to the Materials category. All other categories are flat (one level).

### 3.6 Search Results View

When a search query is active, results are displayed as a flat list — no category headers. Each row has the same layout as §3.4. If no constants match the query, a single centered message reads `No results`.

---

## 4. Behavior on Selection

When the user taps a constant row:

1. Any in-progress digit entry is committed to the stack (same behavior as pressing ENTER)
2. The stack lifts (T ← Z ← Y ← X)
3. The selected constant's value is placed in X as a `Double`
4. The bottom sheet closes
5. The display updates to show the new X register value

No error state is produced. All constant values are valid finite `Double` values.

---

## 5. Constant Data

### 5.1 Source of Truth

All physical, chemical, and astronomical constants use **NIST CODATA 2018** values. These are fixed at compile time; no network access is required. The CODATA 2018 set is the standard released following the 2019 SI redefinition.

Materials property values are sourced from standard engineering references (ASM Handbook, Shigley's Mechanical Engineering Design). These are nominal/typical values for common material grades; exact values vary by heat treatment and processing. Where a range is specified in the source, the midpoint or most commonly cited value is used. Each materials row is annotated with its source reference in the data definition.

### 5.2 Data Model

```
Constant(
    name: String,           // display name
    symbol: String,         // mathematical symbol, may be empty
    value: Double,          // the numeric value loaded into X
    unit: String,           // display unit, may be empty
    category: Category
)
```

For materials, a `MaterialProperty` extends this with a `material: String` field (the material name).

### 5.3 Categories and Constants

#### Mathematics

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Pi | π | 3.14159265358979324 | |
| Euler's number | e | 2.71828182845904524 | |
| Golden ratio | φ | 1.61803398874989485 | |
| Square root of 2 | √2 | 1.41421356237309505 | |

#### Physics — Universal

Source: NIST CODATA 2018

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Speed of light in vacuum | c | 2.99792458 × 10⁸ | m/s |
| Planck constant | h | 6.62607015 × 10⁻³⁴ | J·s |
| Reduced Planck constant | ℏ | 1.054571817 × 10⁻³⁴ | J·s |
| Gravitational constant | G | 6.67430 × 10⁻¹¹ | N·m²/kg² |
| Boltzmann constant | k | 1.380649 × 10⁻²³ | J/K |

#### Physics — Electromagnetic

Source: NIST CODATA 2018

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Elementary charge | e | 1.602176634 × 10⁻¹⁹ | C |
| Electric constant (permittivity of free space) | ε₀ | 8.8541878128 × 10⁻¹² | F/m |
| Magnetic constant (permeability of free space) | μ₀ | 1.25663706212 × 10⁻⁶ | N/A² |
| Impedance of free space | Z₀ | 376.730313668 | Ω |

#### Physics — Atomic

Source: NIST CODATA 2018

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Electron mass | mₑ | 9.1093837015 × 10⁻³¹ | kg |
| Proton mass | mₚ | 1.67262192369 × 10⁻²⁷ | kg |
| Avogadro constant | Nₐ | 6.02214076 × 10²³ | /mol |
| Bohr radius | a₀ | 5.29177210903 × 10⁻¹¹ | m |
| Fine-structure constant | α | 7.2973525693 × 10⁻³ | |

#### Chemistry

Source: NIST CODATA 2018

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Molar gas constant | R | 8.314462618 | J/(mol·K) |
| Faraday constant | F | 96485.33212 | C/mol |
| Molar volume of ideal gas (273.15 K, 101.325 kPa) | Vₘ | 2.241396954 × 10⁻² | m³/mol |
| Atomic mass unit | u | 1.66053906660 × 10⁻²⁷ | kg |

#### Astronomy

Source: IAU 2012 / NIST CODATA 2018 / NASA Planetary Fact Sheets

**General**

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Astronomical unit | AU | 1.495978707 × 10¹¹ | m |
| Light-year | ly | 9.4607304725808 × 10¹⁵ | m |
| Parsec | pc | 3.085677581491367 × 10¹⁶ | m |
| Solar mass | M☉ | 1.98892 × 10³⁰ | kg |
| Solar radius | R☉ | 6.957 × 10⁸ | m |
| Speed of light | c | 2.99792458 × 10⁸ | m/s |

**Planets and Pluto**

Planetary data uses two-level navigation (same pattern as Materials): tapping the Planets sub-section expands a list of bodies; tapping a body expands its three properties; tapping a property loads the value into X and closes the sheet.

Properties provided for each body:

| Property | Unit |
|---|---|
| Mass | kg |
| Mean radius | m |
| Mean distance to Sun | m |

Source: NASA Planetary Fact Sheets. Mean distance is the semi-major axis of the orbit.

| Body | Mass (kg) | Mean Radius (m) | Mean Distance to Sun (m) |
|---|---|---|---|
| Mercury | 3.3011 × 10²³ | 2.4397 × 10⁶ | 5.7909 × 10¹⁰ |
| Venus | 4.8675 × 10²⁴ | 6.0518 × 10⁶ | 1.0821 × 10¹¹ |
| Earth | 5.9722 × 10²⁴ | 6.3710 × 10⁶ | 1.4960 × 10¹¹ |
| Mars | 6.4171 × 10²³ | 3.3895 × 10⁶ | 2.2794 × 10¹¹ |
| Jupiter | 1.8982 × 10²⁷ | 6.9911 × 10⁷ | 7.7857 × 10¹¹ |
| Saturn | 5.6834 × 10²⁶ | 5.8232 × 10⁷ | 1.4335 × 10¹² |
| Uranus | 8.6810 × 10²⁵ | 2.5362 × 10⁷ | 2.8750 × 10¹² |
| Neptune | 1.02413 × 10²⁶ | 2.4622 × 10⁷ | 4.4951 × 10¹² |
| Pluto | 1.303 × 10²² | 1.1883 × 10⁶ | 5.9064 × 10¹² |

**The Moon**

The Moon's meaningful distance reference is to Earth, not the Sun. It is presented as a separate sub-section with its own property label.

Source: NASA Lunar Fact Sheet. Mean distance is the semi-major axis of the lunar orbit.

| Property | Value | Unit |
|---|---|---|
| Mass | 7.342 × 10²² | kg |
| Mean radius | 1.7374 × 10⁶ | m |
| Mean distance to Earth | 3.84400 × 10⁸ | m |

#### Engineering

Source: NIST / SI definitions / IUPAC

**Mechanics**

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Standard gravity | gₙ | 9.80665 | m/s² |
| Standard atmosphere | atm | 101325 | Pa |

**Thermodynamics**

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Stefan-Boltzmann constant | σ | 5.670374419 × 10⁻⁸ | W/(m²·K⁴) |
| Absolute zero | | −273.15 | °C |
| Triple point of water | | 273.16 | K |

**Conversion Constants**

| Name | Symbol | Value | Unit |
|---|---|---|---|
| Inch to millimetre | | 25.4 | mm/in |
| Pound-mass to kilogram | | 0.45359237 | kg/lb |
| BTU to joule | | 1055.05585262 | J/BTU |
| Horsepower (mechanical) to watt | | 745.69987158227 | W/hp |
| PSI to pascal | | 6894.757293168 | Pa/psi |
| US gallon to litre | | 3.785411784 | L/gal |
| Foot to metre | | 0.3048 | m/ft |
| Mile to metre | | 1609.344 | m/mi |
| Nautical mile to metre | | 1852 | m/nmi |

#### Materials

Source: ASM Handbook; Shigley's Mechanical Engineering Design, 10th ed. Values are nominal/typical for the most common grade or condition of each material. Property values are held as `Double`; units are shown in the UI.

**Properties provided for each material:**

| Property | Symbol | Unit |
|---|---|---|
| Yield Strength | Sᵧ | MPa |
| Modulus of Elasticity | E | GPa |
| Coefficient of Thermal Expansion | α | × 10⁻⁶ /°C |
| Ductility (elongation in 50 mm) | | % |

**Materials and nominal values:**

| Material | Yield Strength (MPa) | Modulus E (GPa) | CTE (×10⁻⁶/°C) | Ductility (%) |
|---|---|---|---|---|
| Steel — 1020 HR | 210 | 207 | 11.7 | 36 |
| Steel — 1020 CD | 380 | 207 | 11.7 | 15 |
| Steel — 1040 HR | 290 | 207 | 11.3 | 28 |
| Steel — 1040 CD | 490 | 207 | 11.3 | 12 |
| Steel — 4140 QT (315°C) | 1570 | 207 | 12.3 | 11 |
| Steel — 304 Stainless | 215 | 193 | 17.2 | 47 |
| Cast Iron — Gray (ASTM 20) | 130 | 100 | 11.7 | 0.6 |
| Cast Iron — Ductile (Grade 80-55-06) | 380 | 169 | 11.2 | 6 |
| Aluminum — 1100-H14 | 117 | 69 | 23.6 | 9 |
| Aluminum — 6061-T6 | 276 | 69 | 23.6 | 12 |
| Aluminum — 2024-T4 | 325 | 73 | 23.2 | 19 |
| Yellow Brass (C26000) | 100 | 110 | 20.0 | 66 |
| Phosphor Bronze (C51000) | 160 | 110 | 17.8 | 48 |
| Timber — Douglas Fir | 38 | 12.4 | — | — |
| Timber — Southern Yellow Pine | 34 | 12.4 | — | — |
| Concrete (compressive, fc') | 28 | 25 | 11.0 | — |
| Nylon 6/6 (dry) | 83 | 2.8 | 79 | 60 |
| Rubber (natural, vulcanized) | 17 | 0.004 | 162 | 650 |
| Granite | — | 52 | 8.0 | — |
| Glass (soda-lime) | — | 69 | 9.0 | — |

Notes:
- Timber does not have a meaningful CTE or ductility value in the engineering sense; those cells display `—` in the UI and the property is not selectable for those materials.
- Concrete and glass are brittle; ductility is not applicable and is not selectable.
- Rubber modulus is the approximate tangent modulus at low strain; it is highly nonlinear.

---

## 6. Key Reference Changes

| Key | Change |
|---|---|
| CONST | New key. Short press opens Constants Library bottom sheet. |

No existing keys are modified.

---

## 7. Open Questions

- **CONST key placement**: The physical position of the CONST key in the key grid is not specified here. It must be determined during layout design to fit without displacing existing keys.
- **Symbol rendering**: Some symbols (ℏ, ε₀, μ₀, Nₐ, etc.) require Unicode characters that may not render in the current key label fonts. The bottom sheet uses system fonts and is not subject to this constraint, but the CONST key label itself is.
- **Materials source precision**: The nominal materials values should be reviewed by a mechanical engineer before shipping. Shigley's is authoritative for design purposes but users doing precision work should be warned values are nominal.
- **CODATA 2022**: NIST published CODATA 2022 values in 2024. The delta from 2018 is negligible for most constants but the version should be specified in the UI (e.g. a small "CODATA 2018" label or info icon in the sheet header).
- **Units system**: All values are in SI. A future enhancement could present equivalent values in Imperial/US customary units, but this is out of scope for the initial implementation.
