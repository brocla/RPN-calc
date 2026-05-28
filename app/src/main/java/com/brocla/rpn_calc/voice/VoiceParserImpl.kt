package com.brocla.rpn_calc.voice

import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import javax.inject.Inject

class VoiceParserImpl @Inject constructor(
    private val numberParser: DigitSequenceParser,
) : VoiceParser {

    override fun parse(utterance: String): List<CalcKeyEvent> {
        val normalized = utterance.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()

        val tokens = normalized.split(WHITESPACE).filter { it.isNotEmpty() }
        val events = mutableListOf<CalcKeyEvent>()

        // Accumulator for the current number segment (pre-decimal tokens)
        val pending = mutableListOf<String>()
        var seenDecimal = false   // true after the first decimal keyword in this number segment

        fun flushPreDecimal() {
            if (pending.isNotEmpty()) {
                numberParser.parse(pending.toList()).forEach { events.add(CalcKeyEvent.Digit(it)) }
                pending.clear()
            }
        }

        fun onOperator(event: CalcKeyEvent) {
            if (seenDecimal) {
                // Flush any remaining post-decimal tokens (accumulated in pending)
                for (t in pending) {
                    DigitSequenceParserImpl.ONES[t]?.let { events.add(CalcKeyEvent.Digit(it)) }
                }
                pending.clear()
                seenDecimal = false
            } else {
                flushPreDecimal()
            }
            events.add(event)
        }

        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]

            // 1. Multi-word phrase — check 3-word window first, then 2-word
            val t1 = tokens.getOrNull(i + 1)
            val t2 = tokens.getOrNull(i + 2)
            val phrase3 = if (t1 != null && t2 != null) "$t $t1 $t2" else null
            val phrase3Event = phrase3?.let { THREE_WORD_OPS[it] }
            if (phrase3Event != null) {
                onOperator(phrase3Event)
                i += 3
                continue
            }
            val phrase2 = if (t1 != null) "$t $t1" else null
            val phraseEvent = phrase2?.let { MULTI_WORD_OPS[it] }
            if (phraseEvent != null) {
                onOperator(phraseEvent)
                i += 2
                continue
            }

            // 2. Single-word exact operator
            val exactEvent = SINGLE_WORD_OPS[t]
            if (exactEvent != null) {
                if (exactEvent == CalcKeyEvent.OpenVoiceHelp && tokens.size > 1) {
                    // "help" spoken alone → open help; "help <cmd>" → spurious onset, skip "help"
                    i++
                    continue
                }
                if (exactEvent == CalcKeyEvent.Decimal && !seenDecimal) {
                    flushPreDecimal()
                    events.add(CalcKeyEvent.Decimal)
                    seenDecimal = true
                } else if (exactEvent != CalcKeyEvent.Decimal) {
                    onOperator(exactEvent)
                }
                // second decimal keyword: silently ignore (drop token)
                i++
                continue
            }

            // 3. Numeric digit string from STT (e.g. "1", "23", "325")
            if (t.all { it.isDigit() } && t.isNotEmpty()) {
                if (seenDecimal) {
                    t.forEach { events.add(CalcKeyEvent.Digit(it.digitToInt())) }
                } else {
                    // Flush any pending word-based number first, then emit these digits directly
                    flushPreDecimal()
                    t.forEach { events.add(CalcKeyEvent.Digit(it.digitToInt())) }
                }
                i++
                continue
            }

            // 4. Number word — accumulate
            val isNumberWord = t in DigitSequenceParserImpl.ONES ||
                               t in DigitSequenceParserImpl.TEENS ||
                               t in DigitSequenceParserImpl.TENS  ||
                               t in DigitSequenceParserImpl.MAGNITUDE_WORDS
            if (isNumberWord) {
                if (seenDecimal) {
                    // Post-decimal: only single-digit words are meaningful
                    DigitSequenceParserImpl.ONES[t]?.let { events.add(CalcKeyEvent.Digit(it)) }
                } else {
                    pending.add(t)
                }
                i++
                continue
            }

            // 4. Fuzzy operator match (distance ≤ 2)
            val fuzzyEvent = fuzzyMatch(t)
            if (fuzzyEvent != null) {
                if (fuzzyEvent == CalcKeyEvent.Decimal && !seenDecimal) {
                    flushPreDecimal()
                    events.add(CalcKeyEvent.Decimal)
                    seenDecimal = true
                } else if (fuzzyEvent != CalcKeyEvent.Decimal) {
                    onOperator(fuzzyEvent)
                }
                i++
                continue
            }

            // 5. Unknown token — skip
            i++
        }

        // Flush any trailing number segment
        if (seenDecimal) {
            for (t in pending) {
                DigitSequenceParserImpl.ONES[t]?.let { events.add(CalcKeyEvent.Digit(it)) }
            }
        } else {
            flushPreDecimal()
        }

        return events
    }

    // ── Fuzzy matching ───────────────────────────────────────────────────────

    private fun fuzzyMatch(token: String): CalcKeyEvent? {
        var best: CalcKeyEvent? = null
        var bestDist = Int.MAX_VALUE
        for ((keyword, event) in ALL_KEYWORDS) {
            val dist = levenshtein(token, keyword)
            if (dist < bestDist) { bestDist = dist; best = event }
        }
        return if (bestDist <= 2) best else null
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length; val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[m][n]
    }

    // ── Keyword tables ────────────────────────────────────────────────────────

    companion object {
        // 3-word phrases — checked before 2-word to catch STT collisions like "arc cos sign"
        val THREE_WORD_OPS: Map<String, CalcKeyEvent> = mapOf(
            "arc cos sign"  to CalcKeyEvent.ArcCos,
            "arc cos sine"  to CalcKeyEvent.ArcCos,
            "arc sin sign"  to CalcKeyEvent.ArcSin,
            "arc tan sign"  to CalcKeyEvent.ArcTan,
            "arc tan sine"  to CalcKeyEvent.ArcTan,
        )

        val MULTI_WORD_OPS: Map<String, CalcKeyEvent> = mapOf(
            "divided by"     to CalcKeyEvent.Divide,
            "back space"     to CalcKeyEvent.Backspace,
            "change sign"    to CalcKeyEvent.Chs,
            "clear x"        to CalcKeyEvent.Clx,
            "square root"    to CalcKeyEvent.Sqrt,
            "natural log"    to CalcKeyEvent.Ln,
            "arc sin"        to CalcKeyEvent.ArcSin,
            "arc cos"        to CalcKeyEvent.ArcCos,
            "arc tan"        to CalcKeyEvent.ArcTan,
            "arc tangent"    to CalcKeyEvent.ArcTan,
            "arc sine"       to CalcKeyEvent.ArcSin,
            "arc cosine"     to CalcKeyEvent.ArcCos,
            "roll down"      to CalcKeyEvent.RollDown,
            "last x"         to CalcKeyEvent.LastX,
            "anti log"       to CalcKeyEvent.Pow10,
            "percent change" to CalcKeyEvent.PercentChange,
        )

        val SINGLE_WORD_OPS: Map<String, CalcKeyEvent> = mapOf(
            // Arithmetic / entry
            "enter"       to CalcKeyEvent.Enter,
            "push"        to CalcKeyEvent.Enter,
            "plus"        to CalcKeyEvent.Add,
            "add"         to CalcKeyEvent.Add,
            "minus"       to CalcKeyEvent.Subtract,
            "subtract"    to CalcKeyEvent.Subtract,
            "times"       to CalcKeyEvent.Multiply,
            "multiply"    to CalcKeyEvent.Multiply,
            "divide"      to CalcKeyEvent.Divide,
            "negate"      to CalcKeyEvent.Chs,
            // "sign" removed — sounds identical to "sine"; use "negate" or "change sign"
            "point"       to CalcKeyEvent.Decimal,
            "decimal"     to CalcKeyEvent.Decimal,
            "dot"         to CalcKeyEvent.Decimal,
            "clear"       to CalcKeyEvent.Clx,
            "backspace"   to CalcKeyEvent.Backspace,
            "delete"      to CalcKeyEvent.Backspace,
            "exponent"    to CalcKeyEvent.Eex,
            // Stack / memory
            "roll"        to CalcKeyEvent.RollDown,
            "swap"        to CalcKeyEvent.Swap,
            "exchange"    to CalcKeyEvent.Swap,
            "store"       to CalcKeyEvent.Sto,
            "recall"      to CalcKeyEvent.Rcl,
            "last"        to CalcKeyEvent.LastX,
            // Math
            "root"        to CalcKeyEvent.Sqrt,
            "square"      to CalcKeyEvent.Square,
            "squared"     to CalcKeyEvent.Square,
            "reciprocal"  to CalcKeyEvent.Reciprocal,
            "inverse"     to CalcKeyEvent.Reciprocal,
            "power"       to CalcKeyEvent.Power,
            "raise"       to CalcKeyEvent.Power,
            "raised"      to CalcKeyEvent.Power,
            "log"         to CalcKeyEvent.Log,
            "logarithm"   to CalcKeyEvent.Log,
            // "antilog" removed — heard as [unk] or "enter log"; use "anti log" (two words)
            "ln"          to CalcKeyEvent.Ln,
            "exponential" to CalcKeyEvent.Exp,
            "pi"          to CalcKeyEvent.Pi,
            "factorial"   to CalcKeyEvent.Factorial,
            "percent"     to CalcKeyEvent.Percent,
            "choose"      to CalcKeyEvent.NCr,
            "permutations" to CalcKeyEvent.NPr,
            // Trig — full-word aliases preferred; short forms kept as fallback
            "sine"        to CalcKeyEvent.Sin,
            "sin"         to CalcKeyEvent.Sin,
            // "sign" omitted — fuzzy-matches "sin" at distance 1; bare "sign" → Sin via fuzzy
            // "arc cos sign" etc. handled in THREE_WORD_OPS
            "cosine"      to CalcKeyEvent.Cos,
            "cos"         to CalcKeyEvent.Cos,
            "tangent"     to CalcKeyEvent.Tan,
            "tan"         to CalcKeyEvent.Tan,
            "arcsin"      to CalcKeyEvent.ArcSin,
            "arccos"      to CalcKeyEvent.ArcCos,
            "arctan"      to CalcKeyEvent.ArcTan,
            // Display modes
            "all"         to CalcKeyEvent.AllMode,
            "fix"         to CalcKeyEvent.FixArg,
            "sci"         to CalcKeyEvent.SciArg,
            "scientific"  to CalcKeyEvent.SciArg,
            "eng"         to CalcKeyEvent.EngArg,
            "engineering" to CalcKeyEvent.EngArg,
            "angle"       to CalcKeyEvent.DegRad,
            // Clipboard (intercepted in CalculatorRoute)
            "copy"        to CalcKeyEvent.CopyRequest,
            "paste"       to CalcKeyEvent.PasteClipboard,
            // Help — only fires when spoken alone; spurious onset prefix is dropped by solo-token guard above
            "help"        to CalcKeyEvent.OpenVoiceHelp,
        )

        // Flat list for fuzzy search — multi-word phrases are excluded (too long for ≤2 edit distance)
        val ALL_KEYWORDS: List<Pair<String, CalcKeyEvent>> =
            SINGLE_WORD_OPS.entries.map { it.key to it.value }

        private val WHITESPACE = Regex("\\s+")
    }
}
