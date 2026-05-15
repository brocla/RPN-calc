package com.brocla.rpn_calc.ui.calculator.constants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.brocla.rpn_calc.data.ConstantEntry
import com.brocla.rpn_calc.data.ConstantsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConstantsViewModel @Inject constructor(
    private val repository: ConstantsRepository,
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    fun onQueryChange(query: String) { searchQuery = query }
    fun clearQuery() { searchQuery = "" }

    /** Non-empty only when a search query is active. */
    val searchResults: List<ConstantEntry>
        get() = repository.search(searchQuery)

    /** All entries, for use by the browse view. */
    val allEntries: List<ConstantEntry>
        get() = repository.allEntries()

    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank()
}
