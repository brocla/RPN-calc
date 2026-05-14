package com.brocla.rpn_calc.ui.calculator

import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brocla.rpn_calc.ui.layouts.ClassicLandscapeLayout
import com.brocla.rpn_calc.ui.layouts.LayoutDescriptor
import com.brocla.rpn_calc.ui.layouts.LayoutOrientation
import com.brocla.rpn_calc.ui.layouts.PortraitLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorRoute(
    onOrientationChange: (Int) -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val layouts: List<LayoutDescriptor> = remember { listOf(PortraitLayout, ClassicLandscapeLayout) }
    var activeLayout by remember { mutableStateOf(layouts.first()) }
    var showLayoutPicker by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeLayout) {
        val orientation = when (activeLayout.orientation) {
            LayoutOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            LayoutOrientation.Portrait  -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onOrientationChange(orientation)
    }

    val onKey: (CalcKeyEvent) -> Unit = { event ->
        when (event) {
            CalcKeyEvent.OpenLayoutPicker -> showLayoutPicker = true
            CalcKeyEvent.ResetRequest    -> showResetConfirmation = true
            else                         -> viewModel.onKey(event)
        }
    }

    if (showLayoutPicker) {
        ModalBottomSheet(onDismissRequest = { showLayoutPicker = false }) {
            Text(
                text = "Select Layout",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            layouts.forEach { layout ->
                ListItem(
                    headlineContent = { Text(layout.name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeLayout = layout
                            showLayoutPicker = false
                        },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Calculator") },
            text  = { Text("Clear all stack and memory registers?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    showResetConfirmation = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showClipboardDialog) {
        AlertDialog(
            onDismissRequest = { showClipboardDialog = false },
            title = { Text("Clipboard") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString(uiState.displayString))
                    showClipboardDialog = false
                }) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val text = clipboardManager.getText()?.text ?: ""
                    viewModel.pasteFromClipboard(text)
                    showClipboardDialog = false
                }) {
                    Text("Paste")
                }
            },
        )
    }

    CalculatorScreen(
        uiState            = uiState,
        activeLayout       = activeLayout,
        onKey              = onKey,
        onDisplayLongPress = { showClipboardDialog = true },
    )
}
