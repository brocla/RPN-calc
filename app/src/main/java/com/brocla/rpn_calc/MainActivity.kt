package com.brocla.rpn_calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.brocla.rpn_calc.ui.calculator.CalculatorRoute
import com.brocla.rpn_calc.ui.theme.CalcTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CalcTheme {
                CalculatorRoute(onOrientationChange = { requestedOrientation = it })
            }
        }
    }
}
