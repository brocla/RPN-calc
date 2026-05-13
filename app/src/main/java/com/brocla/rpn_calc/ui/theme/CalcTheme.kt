package com.brocla.rpn_calc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CalcColorScheme = darkColorScheme(
    background   = CalcColors.Body,
    surface      = CalcColors.KeyTop,
    primary      = CalcColors.KeyShift,
    onBackground = CalcColors.LabelPrimary,
    onSurface    = CalcColors.LabelPrimary,
)

@Composable
fun CalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalcColorScheme,
        content = content,
    )
}
