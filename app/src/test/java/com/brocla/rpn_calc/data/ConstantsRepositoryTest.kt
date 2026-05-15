package com.brocla.rpn_calc.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConstantsRepositoryTest {

    private val repo = ConstantsRepositoryImpl()

    // -----------------------------------------------------------------------
    // Content correctness
    // -----------------------------------------------------------------------

    @Test
    fun allEntries_mathCategoryHasFourEntries() {
        val math = repo.allEntries().filterIsInstance<ConstantEntry.Simple>()
            .filter { it.category == ConstantCategory.MATHEMATICS }
        assertEquals(4, math.size)
    }

    @Test
    fun allEntries_piValueMatchesJavaMathPi() {
        val pi = repo.allEntries().filterIsInstance<ConstantEntry.Simple>()
            .first { it.name == "Pi" }
        assertEquals(Math.PI, pi.value, 1e-15)
    }

    @Test
    fun allEntries_planckConstantValue() {
        val planck = repo.allEntries().filterIsInstance<ConstantEntry.Simple>()
            .first { it.name == "Planck constant" }
        assertEquals(6.62607015e-34, planck.value, 1e-44)
    }

    @Test
    fun allEntries_moonDistanceToEarth() {
        val moon = repo.allEntries().filterIsInstance<ConstantEntry.Grouped>()
            .first { it.groupName == "Moon" && it.propertyName == "Mean distance to Earth" }
        assertEquals(3.84400e8, moon.value, 1.0)
    }

    @Test
    fun allEntries_mercuryMass() {
        val mercury = repo.allEntries().filterIsInstance<ConstantEntry.Grouped>()
            .first { it.groupName == "Mercury" && it.propertyName == "Mass" }
        assertEquals(3.3011e23, mercury.value, 1e15)
    }

    @Test
    fun allEntries_steel1020HRYieldStrength() {
        val steel = repo.allEntries().filterIsInstance<ConstantEntry.Grouped>()
            .first { it.groupName == "Steel — 1020 HR" && it.propertyName == "Yield Strength" }
        assertEquals(210.0, steel.value, 1e-9)
        assertEquals("MPa", steel.unit)
    }

    @Test
    fun allEntries_timberDouglasFirHasNoDuctility() {
        val ductility = repo.allEntries().filterIsInstance<ConstantEntry.Grouped>()
            .none { it.groupName == "Timber — Douglas Fir" && it.propertyName == "Ductility" }
        assertTrue(ductility, "Douglas Fir should have no Ductility entry")
    }

    @Test
    fun allEntries_graniteHasNoYieldStrength() {
        val absent = repo.allEntries().filterIsInstance<ConstantEntry.Grouped>()
            .none { it.groupName == "Granite" && it.propertyName == "Yield Strength" }
        assertTrue(absent, "Granite should have no Yield Strength entry")
    }

    // -----------------------------------------------------------------------
    // Structural invariants
    // -----------------------------------------------------------------------

    @Test
    fun allEntries_allValuesFiniteAndNonNaN() {
        repo.allEntries().forEach { entry ->
            val value = when (entry) {
                is ConstantEntry.Simple  -> entry.value
                is ConstantEntry.Grouped -> entry.value
            }
            assertTrue(value.isFinite(), "Non-finite value in entry: $entry")
        }
    }

    @Test
    fun allEntries_noBlankNamesOrGroupNames() {
        repo.allEntries().forEach { entry ->
            when (entry) {
                is ConstantEntry.Simple  -> assertTrue(entry.name.isNotBlank(), "Blank name: $entry")
                is ConstantEntry.Grouped -> {
                    assertTrue(entry.groupName.isNotBlank(), "Blank groupName: $entry")
                    assertTrue(entry.propertyName.isNotBlank(), "Blank propertyName: $entry")
                }
            }
        }
    }

    @Test
    fun allEntries_allSimpleEntriesHaveCategory() {
        repo.allEntries().filterIsInstance<ConstantEntry.Simple>().forEach { entry ->
            assertNotNull(entry.category)
        }
    }

    @Test
    fun allEntries_allGroupedEntriesHaveGroupAndProperty() {
        repo.allEntries().filterIsInstance<ConstantEntry.Grouped>().forEach { entry ->
            assertTrue(entry.groupName.isNotBlank())
            assertTrue(entry.propertyName.isNotBlank())
        }
    }

    // -----------------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------------

    @Test
    fun search_emptyQueryReturnsEmptyList() {
        assertTrue(repo.search("").isEmpty())
    }

    @Test
    fun search_blankQueryReturnsEmptyList() {
        assertTrue(repo.search("   ").isEmpty())
    }

    @Test
    fun search_caseInsensitive_piFindsPI() {
        val results = repo.search("PI")
        assertTrue(results.any { it is ConstantEntry.Simple && (it as ConstantEntry.Simple).name == "Pi" })
    }

    @Test
    fun search_bySymbol_piGlyphFindsPi() {
        val results = repo.search("π")
        assertTrue(results.any { it is ConstantEntry.Simple && (it as ConstantEntry.Simple).name == "Pi" })
    }

    @Test
    fun search_byCategory_mathFindsMathEntries() {
        val results = repo.search("mathematics")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == ConstantCategory.MATHEMATICS })
    }

    @Test
    fun search_partialName_planFindsPlancks() {
        val results = repo.search("plan")
        assertTrue(results.any { it is ConstantEntry.Simple && (it as ConstantEntry.Simple).name.contains("Planck") })
    }

    @Test
    fun search_noMatch_returnsEmpty() {
        assertTrue(repo.search("xyzxyzxyz").isEmpty())
    }

    @Test
    fun search_groupName_mercuryFindsPlanetEntries() {
        val results = repo.search("mercury")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it is ConstantEntry.Grouped && (it as ConstantEntry.Grouped).groupName == "Mercury" })
    }

    @Test
    fun search_propertyName_massFindsAllMassEntries() {
        val results = repo.search("mass")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { entry ->
            when (entry) {
                is ConstantEntry.Simple  -> entry.name.contains("mass", ignoreCase = true)
                is ConstantEntry.Grouped -> entry.propertyName.contains("mass", ignoreCase = true)
            }
        })
    }

    @Test
    fun search_categoryName_materialsFindsMaterialEntries() {
        val results = repo.search("materials")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == ConstantCategory.MATERIALS })
    }
}
