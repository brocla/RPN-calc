package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brocla.rpn_calc.voice.VoiceState

@Composable
fun MicFab(
    voiceState:  VoiceState,
    onToggle:    () -> Unit,
    onLongPress: () -> Unit = {},
    modifier:    Modifier = Modifier,
) {
    val isListening = voiceState is VoiceState.Listening
    val isError     = voiceState is VoiceState.Error

    val pulseAlpha by if (isListening) {
        rememberInfiniteTransition(label = "mic-pulse")
            .animateFloat(
                initialValue  = 0.3f,
                targetValue   = 0f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Restart),
                label         = "pulse-alpha",
            )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val ringColor = MaterialTheme.colorScheme.primary
    val fabColor  = when {
        isListening -> MaterialTheme.colorScheme.primary
        isError     -> MaterialTheme.colorScheme.error
        else        -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .drawBehind {
                        drawCircle(color = ringColor.copy(alpha = pulseAlpha), radius = size.minDimension / 2)
                    }
            )
        }

        FloatingActionButton(
            onClick        = onToggle,
            shape          = CircleShape,
            containerColor = fabColor,
            modifier       = Modifier.pointerInput(onToggle, onLongPress) {
                detectTapGestures(
                    onTap       = { onToggle() },
                    onLongPress = { onLongPress() },
                )
            },
        ) {
            Text(
                text     = if (isListening) "●" else "🎤",
                fontSize = 20.sp,
                color    = if (isListening || isError) Color.White
                           else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
