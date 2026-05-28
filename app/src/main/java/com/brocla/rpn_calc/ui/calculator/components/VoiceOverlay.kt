package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VoiceOverlay(
    interimText: String,
    visible:     Boolean,
    modifier:    Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && interimText.isNotEmpty(),
        enter   = fadeIn(),
        exit    = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text      = interimText,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
