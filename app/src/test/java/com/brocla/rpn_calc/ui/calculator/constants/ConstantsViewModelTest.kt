package com.brocla.rpn_calc.ui.calculator.constants

import com.brocla.rpn_calc.data.ConstantCategory
import com.brocla.rpn_calc.data.ConstantEntry
import com.brocla.rpn_calc.data.ConstantsRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeConstantsRepository : ConstantsRepository {
    val fakeEntries: List<ConstantEntry> = listOf(
        ConstantEntry.Simple("Pi", "π", 3.14159, "", ConstantCategory.MATHEMATICS),
        ConstantEntry.Simple("Planck constant", "h", 6.626e-34, "J·s", ConstantCategory.PHYSICS_UNIVERSAL),
        ConstantEntry.Grouped("Mercury", "Mass", 3.3011e23, "kg", ConstantCategory.ASTRONOMY),
    )

    override fun allEntries(): List<ConstantEntry> = fakeEntries

    override fun search(query: String): List<ConstantEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return fakeEntries.filter { entry ->
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

class ConstantsViewModelTest {

    private val repo = FakeConstantsRepository()
    private val vm = ConstantsViewModel(repo)

    @Test
    fun onQueryChange_updatesSearchQuery() {
        vm.onQueryChange("plan")
        assertEquals("plan", vm.searchQuery)
    }

    @Test
    fun clearQuery_setsQueryToEmpty() {
        vm.onQueryChange("plan")
        vm.clearQuery()
        assertEquals("", vm.searchQuery)
    }

    @Test
    fun isSearchActive_falseWhenBlank() {
        vm.onQueryChange("")
        assertFalse(vm.isSearchActive)
    }

    @Test
    fun isSearchActive_trueWhenNonBlank() {
        vm.onQueryChange("pi")
        assertTrue(vm.isSearchActive)
    }

    @Test
    fun searchResults_delegatesToRepositorySearch() {
        vm.onQueryChange("planck")
        val results = vm.searchResults
        assertEquals(1, results.size)
        assertTrue(results[0] is ConstantEntry.Simple)
        assertEquals("Planck constant", (results[0] as ConstantEntry.Simple).name)
    }

    @Test
    fun allEntries_delegatesToRepositoryAllEntries() {
        assertEquals(repo.fakeEntries, vm.allEntries)
    }
}
