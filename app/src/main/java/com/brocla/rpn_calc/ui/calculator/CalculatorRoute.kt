package com.brocla.rpn_calc.ui.calculator

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.flow.drop

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

    val onMicStart: () -> Unit = {
        if (modelReady != null && voiceState !is VoiceState.Listening) {
            val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                          PackageManager.PERMISSION_GRANTED
            if (granted) voiceController.startListening()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val onMicStop: () -> Unit = {
        if (voiceState is VoiceState.Listening) voiceController.stopListening()
    }

    val onMicToggle: () -> Unit = {
        if (modelReady != null) {
            if (voiceState is VoiceState.Listening) onMicStop() else onMicStart()
        }
    }

    // Stop listening when the app leaves the foreground; resume when it returns.
    var resumeVoiceOnForeground by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumeVoiceOnForeground = voiceState is VoiceState.Listening
                    if (resumeVoiceOnForeground) voiceController.stopListening()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (resumeVoiceOnForeground) {
                        resumeVoiceOnForeground = false
                        onMicStart()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    val window = (context as? Activity)?.window

    // Wake lock: fires on every state change including initial value (correct — we want the
    // flag cleared at startup too).
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Listening) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Chimes: drop(1) skips the initial StateFlow emission so no sound plays at startup.
    LaunchedEffect(Unit) {
        voiceController.state.drop(1).collect { state ->
            when (state) {
                is VoiceState.Listening -> chime(MIDI_C5, MIDI_E5)
                is VoiceState.Idle      -> chime(MIDI_E5, MIDI_C5)
                else                    -> Unit
            }
        }
    }

    // Separate effect (different key) so the banner delay is never cancelled by other voiceState
    // LaunchedEffect restarts. Key is Unit so it runs exactly once per composition lifecycle.
    LaunchedEffect(Unit) {
        voiceController.state.collect { state ->
            if (state is VoiceState.Listening && !hasShownVoiceHint) {
                hasShownVoiceHint = true
                showVoiceHintBanner = true
                delay(4000)
                showVoiceHintBanner = false
            }
        }
    }

    // Reset the 30 s silence timer on every partial result.
    // LaunchedEffect cancels the previous coroutine automatically when interimText changes,
    // so each new word of speech restarts the countdown.
    LaunchedEffect(interimText) {
        if (voiceState is VoiceState.Listening && interimText.isNotEmpty()) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            delay(30_000)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val onKey: (CalcKeyEvent) -> Unit = { event ->
        when (event) {
            CalcKeyEvent.OpenLayoutPicker -> showLayoutPicker = true
            CalcKeyEvent.ResetRequest    -> showResetConfirmation = true
            CalcKeyEvent.OpenConstants   -> showConstants = true
            CalcKeyEvent.ToggleMic       -> onMicToggle()
            CalcKeyEvent.StartMic        -> onMicStart()
            CalcKeyEvent.StopMic         -> onMicStop()
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
            voiceDebugText     = interimText,
            voiceState         = voiceState,
        )

        // One-time hint tooltip on first voice activation
        AnimatedVisibility(
            visible  = showVoiceHintBanner,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Text(
                    text     = "Swipe down or tap 🎤 for voice",
                    color    = MaterialTheme.colorScheme.inverseOnSurface,
                    style    = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// MIDI note numbers — C5 = 72, E5 = 76
private const val MIDI_C5 = 72
private const val MIDI_E5 = 76
private const val CHIME_NOTE_MS = 120        // duration of each note in the chime
private const val CHIME_SAMPLE_RATE = 44100

private fun midiHz(note: Int): Double = 440.0 * Math.pow(2.0, (note - 69) / 12.0)

/** Play two sequential notes as a chime on a daemon thread. */
private fun chime(note1: Int, note2: Int, noteMs: Int = CHIME_NOTE_MS) {
    kotlin.concurrent.thread(isDaemon = true) {
        val n = CHIME_SAMPLE_RATE * noteMs / 1000
        val fadeLen = (CHIME_SAMPLE_RATE * 0.018).toInt()  // 18 ms fade between notes
        val total = n * 2
        val samples = ShortArray(total)
        val freqs = listOf(midiHz(note1), midiHz(note2))
        for (seg in 0..1) {
            val hz   = freqs[seg]
            val step = 2.0 * Math.PI * hz / CHIME_SAMPLE_RATE
            val base = seg * n
            for (j in 0 until n) {
                val i = base + j
                // Fade out last 18 ms of each note to avoid inter-note click
                val env = if (j >= n - fadeLen) (n - j).toFloat() / fadeLen else 1f
                samples[i] = (Math.sin(step * j) * Short.MAX_VALUE * 0.45 * env).toInt().toShort()
            }
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(CHIME_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(total * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, total)
        track.play()
        Thread.sleep(noteMs.toLong() * 2 + 80)
        track.stop()
        track.release()
    }
}
