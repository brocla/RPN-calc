package com.brocla.rpn_calc.ui.calculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
