package com.brocla.rpn_calc.ui.calculator

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brocla.rpn_calc.ui.calculator.components.MicFab
import com.brocla.rpn_calc.ui.calculator.constants.ConstantsBottomSheet
import com.brocla.rpn_calc.ui.layouts.ClassicLandscapeLayout
import com.brocla.rpn_calc.ui.layouts.LayoutDescriptor
import com.brocla.rpn_calc.ui.layouts.LayoutOrientation
import com.brocla.rpn_calc.ui.layouts.PortraitLayout
import com.brocla.rpn_calc.voice.VoiceHelpBottomSheet
import com.brocla.rpn_calc.voice.VoiceInputController
import com.brocla.rpn_calc.voice.VoiceModelLoader
import com.brocla.rpn_calc.voice.VoiceParser
import com.brocla.rpn_calc.voice.VoiceState
import com.brocla.rpn_calc.voice.collectAndDispatch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorRoute(
    voiceController:     VoiceInputController,
    voiceParser:         VoiceParser,
    voiceModelLoader:    VoiceModelLoader,
    onOrientationChange: (Int) -> Unit = {},
    viewModel:           CalculatorViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState  by voiceController.state.collectAsStateWithLifecycle()
    val interimText by voiceController.interimText.collectAsStateWithLifecycle()
    val modelReady  by voiceModelLoader.model.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val haptic  = LocalHapticFeedback.current
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) voiceController.startListening() }

    val onMicToggle: () -> Unit = {
        if (modelReady != null) {
            if (voiceState is VoiceState.Listening) {
                voiceController.stopListening()
            } else {
                val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                              PackageManager.PERMISSION_GRANTED
                if (granted) voiceController.startListening()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    var voiceDebugText by remember { mutableStateOf("") }
    if (interimText.isNotEmpty()) voiceDebugText = interimText

    val layouts: List<LayoutDescriptor> = remember { listOf(PortraitLayout, ClassicLandscapeLayout) }
    var activeLayout by remember { mutableStateOf(layouts.first()) }
    var showLayoutPicker by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }
    var showConstants by remember { mutableStateOf(false) }
    var showVoiceHelp by remember { mutableStateOf(false) }

    // One-time hint banner shown on first mic activation per session
    var hasShownVoiceHint by remember { mutableStateOf(false) }
    var showVoiceHintBanner by remember { mutableStateOf(false) }

    LaunchedEffect(activeLayout) {
        val orientation = when (activeLayout.orientation) {
            LayoutOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            LayoutOrientation.Portrait  -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onOrientationChange(orientation)
    }

    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Listening && !hasShownVoiceHint) {
            hasShownVoiceHint = true
            showVoiceHintBanner = true
            delay(4000)
            showVoiceHintBanner = false
        }
    }

    val onKey: (CalcKeyEvent) -> Unit = { event ->
        when (event) {
            CalcKeyEvent.OpenLayoutPicker -> showLayoutPicker = true
            CalcKeyEvent.ResetRequest    -> showResetConfirmation = true
            CalcKeyEvent.OpenConstants   -> showConstants = true
            CalcKeyEvent.OpenVoiceHelp   -> showVoiceHelp = true
            CalcKeyEvent.CopyRequest     -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString(uiState.displayString))
            }
            CalcKeyEvent.PasteClipboard  -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val text = clipboardManager.getText()?.text ?: ""
                viewModel.pasteFromClipboard(text)
            }
            else -> viewModel.onKey(event)
        }
    }

    // Voice events routed through the same onKey intercept logic as button presses
    LaunchedEffect(voiceController) {
        collectAndDispatch(voiceController.finalUtterance, voiceParser) { onKey(it) }
    }

    if (showConstants) {
        ConstantsBottomSheet(
            onSelected = { value ->
                viewModel.onKey(CalcKeyEvent.PushConstant(value))
                showConstants = false
            },
            onDismiss = { showConstants = false },
        )
    }

    if (showVoiceHelp) {
        VoiceHelpBottomSheet(onDismiss = { showVoiceHelp = false })
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

    Box(Modifier.fillMaxSize()) {
        CalculatorScreen(
            uiState            = uiState,
            activeLayout       = activeLayout,
            onKey              = onKey,
            onDisplayLongPress = { showClipboardDialog = true },
            voiceDebugText     = voiceDebugText,
            voiceState         = voiceState,
        )

        MicFab(
            voiceState  = voiceState,
            onToggle    = onMicToggle,
            onLongPress = { showVoiceHelp = true },
            modifier    = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )

        // One-time hint tooltip that appears beside the mic FAB on first activation
        AnimatedVisibility(
            visible  = showVoiceHintBanner,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 80.dp, bottom = 28.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Text(
                    text     = "Say \"help\" for voice commands",
                    color    = MaterialTheme.colorScheme.inverseOnSurface,
                    style    = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
