package com.brocla.rpn_calc.data

import javax.inject.Inject

interface ConstantsRepository {
    /** All entries in display order. */
    fun allEntries(): List<ConstantEntry>

    /**
     * Case-insensitive search across name / symbol / groupName / propertyName /
     * category displayName. Returns empty list for blank query.
     */
    fun search(query: String): List<ConstantEntry>
}

class ConstantsRepositoryImpl @Inject constructor() : ConstantsRepository {

    private val entries: List<ConstantEntry> = buildList {
        // ── Mathematics ──────────────────────────────────────────────────────
        fun math(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.MATHEMATICS)

        add(math("Pi",               "π",  Math.PI))
        add(math("Euler's number",   "e",  Math.E))
        add(math("Golden ratio",     "φ",  1.6180339887498948))
        add(math("Square root of 2", "√2", 1.4142135623730950))

        // ── Physics — Universal ───────────────────────────────────────────────
        fun phys(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.PHYSICS_UNIVERSAL)

        add(phys("Speed of light in vacuum", "c",  2.99792458e8,    "m/s"))
        add(phys("Planck constant",          "h",  6.62607015e-34,  "J·s"))
        add(phys("Reduced Planck constant",  "ℏ",  1.054571817e-34, "J·s"))
        add(phys("Gravitational constant",   "G",  6.67430e-11,     "N·m²/kg²"))
        add(phys("Boltzmann constant",       "k",  1.380649e-23,    "J/K"))

        // ── Physics — Electromagnetic ─────────────────────────────────────────
        fun em(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.PHYSICS_ELECTROMAGNETIC)

        add(em("Elementary charge",                "e",  1.602176634e-19,   "C"))
        add(em("Electric constant (permittivity)", "ε₀", 8.8541878128e-12,  "F/m"))
        add(em("Magnetic constant (permeability)", "μ₀", 1.25663706212e-6,  "N/A²"))
        add(em("Impedance of free space",          "Z₀", 376.730313668,     "Ω"))

        // ── Physics — Atomic ──────────────────────────────────────────────────
        fun atom(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.PHYSICS_ATOMIC)

        add(atom("Electron mass",          "mₑ", 9.1093837015e-31,  "kg"))
        add(atom("Proton mass",            "mₚ", 1.67262192369e-27, "kg"))
        add(atom("Avogadro constant",      "Nₐ", 6.02214076e23,     "/mol"))
        add(atom("Bohr radius",            "a₀", 5.29177210903e-11, "m"))
        add(atom("Fine-structure constant","α",   7.2973525693e-3,   ""))

        // ── Chemistry ─────────────────────────────────────────────────────────
        fun chem(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.CHEMISTRY)

        add(chem("Molar gas constant",        "R",  8.314462618,       "J/(mol·K)"))
        add(chem("Faraday constant",          "F",  96485.33212,       "C/mol"))
        add(chem("Molar volume of ideal gas", "Vₘ", 2.241396954e-2,    "m³/mol"))
        add(chem("Atomic mass unit",          "u",  1.66053906660e-27, "kg"))

        // ── Astronomy — general ───────────────────────────────────────────────
        fun astro(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.ASTRONOMY)

        add(astro("Astronomical unit", "AU",  1.495978707e11,       "m"))
        add(astro("Light-year",        "ly",  9.4607304725808e15,   "m"))
        add(astro("Parsec",            "pc",  3.085677581491367e16, "m"))
        add(astro("Solar mass",        "M☉",  1.98892e30,           "kg"))
        add(astro("Solar radius",      "R☉",  6.957e8,              "m"))
        add(astro("Speed of light",    "c",   2.99792458e8,         "m/s"))

        // ── Astronomy — Planets and Pluto ─────────────────────────────────────
        fun planet(body: String, property: String, value: Double, unit: String) =
            ConstantEntry.Grouped(body, property, value, unit, ConstantCategory.ASTRONOMY)

        data class PlanetData(val name: String, val mass: Double, val radius: Double, val distToSun: Double)
        listOf(
            PlanetData("Mercury", 3.3011e23,  2.4397e6,  5.7909e10),
            PlanetData("Venus",   4.8675e24,  6.0518e6,  1.0821e11),
            PlanetData("Earth",   5.9722e24,  6.3710e6,  1.4960e11),
            PlanetData("Mars",    6.4171e23,  3.3895e6,  2.2794e11),
            PlanetData("Jupiter", 1.8982e27,  6.9911e7,  7.7857e11),
            PlanetData("Saturn",  5.6834e26,  5.8232e7,  1.4335e12),
            PlanetData("Uranus",  8.6810e25,  2.5362e7,  2.8750e12),
            PlanetData("Neptune", 1.02413e26, 2.4622e7,  4.4951e12),
            PlanetData("Pluto",   1.303e22,   1.1883e6,  5.9064e12),
        ).forEach { p ->
            add(planet(p.name, "Mass",                 p.mass,      "kg"))
            add(planet(p.name, "Mean radius",          p.radius,    "m"))
            add(planet(p.name, "Mean distance to Sun", p.distToSun, "m"))
        }

        // ── Astronomy — The Moon ──────────────────────────────────────────────
        add(planet("Moon", "Mass",                   7.342e22,  "kg"))
        add(planet("Moon", "Mean radius",            1.7374e6,  "m"))
        add(planet("Moon", "Mean distance to Earth", 3.84400e8, "m"))

        // ── Engineering — Mechanics ───────────────────────────────────────────
        fun eng(name: String, symbol: String, value: Double, unit: String = "") =
            ConstantEntry.Simple(name, symbol, value, unit, ConstantCategory.ENGINEERING)

        add(eng("Standard gravity",    "gₙ",  9.80665,   "m/s²"))
        add(eng("Standard atmosphere", "atm", 101325.0,  "Pa"))

        // ── Engineering — Thermodynamics ──────────────────────────────────────
        add(eng("Stefan-Boltzmann constant", "σ", 5.670374419e-8, "W/(m²·K⁴)"))
        add(eng("Absolute zero",             "",  -273.15,         "°C"))
        add(eng("Triple point of water",     "",   273.16,         "K"))

        // ── Engineering — Conversion constants ────────────────────────────────
        add(eng("Inch to millimetre",     "", 25.4,             "mm/in"))
        add(eng("Pound-mass to kilogram", "", 0.45359237,       "kg/lb"))
        add(eng("BTU to joule",           "", 1055.05585262,    "J/BTU"))
        add(eng("Horsepower to watt",     "", 745.69987158227,  "W/hp"))
        add(eng("PSI to pascal",          "", 6894.757293168,   "Pa/psi"))
        add(eng("US gallon to litre",     "", 3.785411784,      "L/gal"))
        add(eng("Foot to metre",          "", 0.3048,           "m/ft"))
        add(eng("Mile to metre",          "", 1609.344,         "m/mi"))
        add(eng("Nautical mile to metre", "", 1852.0,           "m/nmi"))

        // ── Materials ─────────────────────────────────────────────────────────
        // Source: Shigley's Mechanical Engineering Design, 10th ed.; ASM Handbook.
        // null means "not applicable" — those entries are omitted entirely.
        fun mat(
            material: String,
            yieldMPa: Double?,
            modulusGPa: Double?,
            ctePer6C: Double?,
            ductilityPct: Double?,
        ) {
            if (yieldMPa    != null) add(ConstantEntry.Grouped(material, "Yield Strength",          yieldMPa,    "MPa",      ConstantCategory.MATERIALS))
            if (modulusGPa  != null) add(ConstantEntry.Grouped(material, "Modulus of Elasticity",   modulusGPa,  "GPa",      ConstantCategory.MATERIALS))
            if (ctePer6C    != null) add(ConstantEntry.Grouped(material, "Coeff Thermal Expansion", ctePer6C,    "×10⁻⁶/°C", ConstantCategory.MATERIALS))
            if (ductilityPct!= null) add(ConstantEntry.Grouped(material, "Ductility",               ductilityPct,"%",        ConstantCategory.MATERIALS))
        }

        mat("Steel — 1020 HR",              210.0,  207.0, 11.7,  36.0)
        mat("Steel — 1020 CD",              380.0,  207.0, 11.7,  15.0)
        mat("Steel — 1040 HR",              290.0,  207.0, 11.3,  28.0)
        mat("Steel — 1040 CD",              490.0,  207.0, 11.3,  12.0)
        mat("Steel — 4140 QT (315°C)",     1570.0,  207.0, 12.3,  11.0)
        mat("Steel — 304 Stainless",        215.0,  193.0, 17.2,  47.0)
        mat("Cast Iron — Gray (ASTM 20)",   130.0,  100.0, 11.7,   0.6)
        mat("Cast Iron — Ductile 80-55-06", 380.0,  169.0, 11.2,   6.0)
        mat("Aluminum — 1100-H14",          117.0,   69.0, 23.6,   9.0)
        mat("Aluminum — 6061-T6",           276.0,   69.0, 23.6,  12.0)
        mat("Aluminum — 2024-T4",           325.0,   73.0, 23.2,  19.0)
        mat("Yellow Brass (C26000)",        100.0,  110.0, 20.0,  66.0)
        mat("Phosphor Bronze (C51000)",     160.0,  110.0, 17.8,  48.0)
        mat("Timber — Douglas Fir",          38.0,   12.4, null,  null)
        mat("Timber — Southern Yellow Pine", 34.0,   12.4, null,  null)
        mat("Concrete (compressive fc')",    28.0,   25.0, 11.0,  null)
        mat("Nylon 6/6 (dry)",               83.0,    2.8, 79.0,  60.0)
        mat("Rubber (natural, vulcanized)",  17.0,  0.004, 162.0, 650.0)
        mat("Granite",                       null,   52.0,  8.0,  null)
        mat("Glass (soda-lime)",             null,   69.0,  9.0,  null)
    }

    override fun allEntries(): List<ConstantEntry> = entries

    override fun search(query: String): List<ConstantEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return entries.filter { entry ->
            when (entry) {
                is ConstantEntry.Simple  ->
                    entry.name.lowercase().contains(q) ||
                    entry.symbol.lowercase().contains(q) ||
                    entry.category.displayName.lowercase().contains(q)
                is ConstantEntry.Grouped ->
                    entry.groupName.lowercase().contains(q) ||
                    entry.propertyName.lowercase().contains(q) ||
                    entry.category.displayName.lowercase().contains(q)
            }
        }
    }
}
