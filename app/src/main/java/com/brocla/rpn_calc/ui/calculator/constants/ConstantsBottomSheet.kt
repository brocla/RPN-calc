package com.brocla.rpn_calc.ui.calculator.constants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.brocla.rpn_calc.data.ConstantCategory
import com.brocla.rpn_calc.data.ConstantEntry
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstantsBottomSheet(
    onSelected: (Double) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ConstantsViewModel = hiltViewModel(),
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search constants…") },
            singleLine = true,
            trailingIcon = {
                if (viewModel.searchQuery.isNotBlank()) {
                    IconButton(onClick = viewModel::clearQuery) {
                        Text("✕")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (viewModel.isSearchActive) {
            SearchResultsList(viewModel.searchResults, onSelected)
        } else {
            CategoryBrowseList(viewModel.allEntries, onSelected)
        }
    }
}

@Composable
private fun SearchResultsList(results: List<ConstantEntry>, onSelected: (Double) -> Unit) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No results", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(results) { entry -> ConstantRow(entry, onSelected) }
        }
    }
}

@Composable
private fun CategoryBrowseList(entries: List<ConstantEntry>, onSelected: (Double) -> Unit) {
    val expandedCategories = remember { mutableStateMapOf<ConstantCategory, Boolean>() }
    val expandedGroups    = remember { mutableStateMapOf<String, Boolean>() }

    val byCategory = ConstantCategory.entries.map { cat ->
        cat to entries.filter { it.category == cat }
    }.filter { (_, list) -> list.isNotEmpty() }

    LazyColumn {
        byCategory.forEach { (category, catEntries) ->
            val isCatExpanded = expandedCategories[category] == true
            item(key = category.name) {
                CategoryHeader(
                    name = category.displayName,
                    expanded = isCatExpanded,
                    onClick = { expandedCategories[category] = !isCatExpanded },
                )
            }
            if (isCatExpanded) {
                val simples = catEntries.filterIsInstance<ConstantEntry.Simple>()
                val grouped = catEntries.filterIsInstance<ConstantEntry.Grouped>()

                items(simples, key = { "${category.name}:${it.name}" }) { entry ->
                    ConstantRow(entry, onSelected)
                }

                val groups = grouped.map { it.groupName }.distinct()
                groups.forEach { groupName ->
                    val groupKey = "${category.name}:$groupName"
                    val isGroupExpanded = expandedGroups[groupKey] == true
                    item(key = groupKey) {
                        GroupHeader(
                            name = groupName,
                            expanded = isGroupExpanded,
                            onClick = { expandedGroups[groupKey] = !isGroupExpanded },
                        )
                    }
                    if (isGroupExpanded) {
                        val props = grouped.filter { it.groupName == groupName }
                        items(props, key = { "${groupKey}:${it.propertyName}" }) { entry ->
                            ConstantRow(entry, onSelected)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String, expanded: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(name, style = MaterialTheme.typography.titleSmall)
        },
        trailingContent = {
            Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.labelSmall)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@Composable
private fun GroupHeader(name: String, expanded: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(name, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.labelSmall)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ConstantRow(entry: ConstantEntry, onSelected: (Double) -> Unit) {
    val (headline, symbol, value, unit) = when (entry) {
        is ConstantEntry.Simple  -> Quad(entry.name,         entry.symbol, entry.value, entry.unit)
        is ConstantEntry.Grouped -> Quad(entry.propertyName, "",           entry.value, entry.unit)
    }
    ListItem(
        overlineContent = { Text(formatValue(value), style = MaterialTheme.typography.labelSmall) },
        headlineContent = { Text(headline) },
        supportingContent = if (unit.isNotBlank()) {
            { Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = if (symbol.isNotBlank()) {
            { Text(symbol, fontStyle = FontStyle.Italic) }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(value) },
    )
}

private data class Quad(val headline: String, val symbol: String, val value: Double, val unit: String)

/**
 * Formats a Double in scientific notation when the magnitude warrants it,
 * otherwise as a plain decimal. Examples:
 *   2.99792458e8  → "2.99792458 × 10⁸"
 *   9.80665       → "9.80665"
 *   -273.15       → "−273.15"
 */
private fun formatValue(v: Double): String {
    if (v == 0.0) return "0"
    val absV = abs(v)
    val exponent = floor(log10(absV)).toInt()
    return if (exponent >= 6 || exponent <= -3) {
        val mantissa = v / Math.pow(10.0, exponent.toDouble())
        val mantissaStr = mantissa.toBigDecimal().stripTrailingZeros().toPlainString()
        val expStr = exponent.toString()
            .replace("-", "⁻")
            .map { superscriptDigit(it) }
            .joinToString("")
        "$mantissaStr × 10$expStr"
    } else {
        v.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}

private fun superscriptDigit(c: Char): Char = when (c) {
    '0' -> '⁰'; '1' -> '¹'; '2' -> '²'; '3' -> '³'; '4' -> '⁴'
    '5' -> '⁵'; '6' -> '⁶'; '7' -> '⁷'; '8' -> '⁸'; '9' -> '⁹'
    '⁻' -> '⁻'
    else -> c
}
