package com.brocla.rpn_calc.ui.calculator.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// Material "mic" icon path inlined — identical to Icons.Filled.Mic from material-icons-extended
// but without adding that large dependency.
private val MicIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 24f,
        viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black)) {
        // Capsule body
        moveTo(12f, 14f)
        curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
        lineTo(15f, 5f)
        curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
        reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
        lineToRelative(0f, 6f)
        curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
        close()
        // Inner cutout (makes body outline rather than filled block)
        moveTo(11f, 5f)
        curveToRelative(0f, -0.55f, 0.45f, -1f, 1f, -1f)
        reflectiveCurveToRelative(1f, 0.45f, 1f, 1f)
        lineToRelative(0f, 6f)
        curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
        reflectiveCurveToRelative(-1f, -0.45f, -1f, -1f)
        lineTo(11f, 5f)
        close()
        // U-bracket + stand + base
        moveTo(17f, 11f)
        curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
        reflectiveCurveToRelative(-5f, -2.24f, -5f, -5f)
        lineTo(5f, 11f)
        lineTo(3f, 11f)
        curveToRelative(0f, 3.53f, 2.61f, 6.43f, 6f, 6.92f)
        lineTo(9f, 21f)
        lineToRelative(6f, 0f)
        lineToRelative(0f, -3.08f)
        curveToRelative(3.39f, -0.49f, 6f, -3.39f, 6f, -6.92f)
        lineToRelative(-2f, 0f)
        close()
    }.build()
}

@Composable
fun MicLabel(color: Color, fontSize: TextUnit) {
    val sizeDp = with(LocalDensity.current) { (fontSize.toPx() * 0.90f).toDp() }
    Icon(
        imageVector        = MicIcon,
        contentDescription = null,
        tint               = color,
        modifier           = Modifier.size(sizeDp),
    )
}
