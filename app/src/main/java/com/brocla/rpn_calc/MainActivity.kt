package com.brocla.rpn_calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.brocla.rpn_calc.ui.calculator.CalculatorRoute
import com.brocla.rpn_calc.ui.calculator.CalculatorViewModel
import com.brocla.rpn_calc.ui.theme.CalcTheme
import com.brocla.rpn_calc.voice.VoiceInputController
import com.brocla.rpn_calc.voice.VoiceModelLoader
import com.brocla.rpn_calc.voice.VoiceParser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    @Inject lateinit var voiceController:  VoiceInputController
    @Inject lateinit var voiceParser:      VoiceParser
    @Inject lateinit var voiceModelLoader: VoiceModelLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { viewModel.isLoading.value }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CalcTheme {
                CalculatorRoute(
                    voiceController     = voiceController,
                    voiceParser         = voiceParser,
                    voiceModelLoader    = voiceModelLoader,
                    onOrientationChange = { requestedOrientation = it },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.destroy()
    }
}
