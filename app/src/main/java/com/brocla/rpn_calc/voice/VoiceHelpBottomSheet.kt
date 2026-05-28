package com.brocla.rpn_calc.voice

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceHelpBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text     = "Voice Commands",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
            VOICE_HELP_ENTRIES.forEach { (category, rows) ->
                item(key = "header:$category") {
                    Text(
                        text     = category,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
                    )
                    HorizontalDivider()
                }
                items(rows, key = { "$category:${it.first}" }) { (keyLabel, voiceWord) ->
                    ListItem(
                        headlineContent  = {
                            Text(voiceWord, fontFamily = FontFamily.Monospace,
                                 style = MaterialTheme.typography.bodyMedium)
                        },
                        trailingContent  = {
                            Text(keyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 style = MaterialTheme.typography.bodySmall)
                        },
                    )
                }
            }
        }
    }
}

/** Key label (right column) → voice word(s) (left column). Only non-obvious mappings. */
private val VOICE_HELP_ENTRIES: List<Pair<String, List<Pair<String, String>>>> = listOf(
    "Numbers" to listOf(
        "0 digit"  to "zero  (say 'zero', not 'oh', when repeating)",
        "2 digit"  to "two  /  to  /  too",
        "decimal"  to "point  /  decimal  /  dot",
    ),
    "Stack & Memory" to listOf(
        "R↓"    to "roll  /  roll down",
        "x⇄y"   to "swap  /  exchange",
        "STO n" to "store <digit>",
        "RCL n" to "recall <digit>",
        "LAST"  to "last  /  last x",
    ),
    "Math" to listOf(
        "√x"    to "root  /  square root",
        "x²"    to "square  /  squared",
        "1/x"   to "reciprocal  /  inverse",
        "yˣ"    to "power  /  raise  /  raised",
        "log"   to "logarithm  (or 'log')",
        "10ˣ"   to "anti log  (two words)",
        "ln"    to "ln  /  natural log",
        "eˣ"    to "exponential",
        "EEX"   to "exponent",
        "n!"    to "factorial",
        "nCr"   to "choose",
        "nPr"   to "permutations",
        "%"     to "percent",
        "Δ%"    to "percent change",
    ),
    "Trig" to listOf(
        "sin"   to "sine  (not 'sign')",
        "cos"   to "cosine",
        "tan"   to "tangent",
        "sin⁻¹" to "arcsin  /  arc sin",
        "cos⁻¹" to "arccos  /  arc cos",
        "tan⁻¹" to "arctan  /  arc tan",
    ),
    "Display Mode" to listOf(
        "ALL"   to "all",
        "FIX n" to "fix <digit>",
        "SCI n" to "sci  /  scientific <digit>",
        "ENG n" to "eng  /  engineering <digit>",
        "°/rad" to "angle",
    ),
    "Clipboard" to listOf(
        "Copy X"  to "copy",
        "Paste"   to "paste",
    ),
    "Help" to listOf(
        "This sheet" to "help  /  long-press mic",
    ),
)
