package com.brocla.rpn_calc.voice

import javax.inject.Inject

class DigitSequenceParserImpl @Inject constructor() : DigitSequenceParser {

    override fun parse(tokens: List<String>): List<Int> {
        if (tokens.isEmpty()) return emptyList()

        return if (tokens.any { it in MAGNITUDE_WORDS }) {
            parseMagnitude(tokens).toDigits()
        } else {
            parseDigitByDigit(tokens)
        }
    }

    // ── Magnitude mode ──────────────────────────────────────────────────────

    private fun parseMagnitude(tokens: List<String>): Long {
        var result = 0L
        var current = 0L
        for (token in tokens) {
            when (token) {
                "hundred"  -> current *= 100
                "thousand" -> { result += current * 1_000L;         current = 0 }
                "million"  -> { result += current * 1_000_000L;     current = 0 }
                "billion"  -> { result += current * 1_000_000_000L;     current = 0 }
                "trillion" -> { result += current * 1_000_000_000_000L; current = 0 }
                else       -> current += magnitudeValue(token)
            }
        }
        return result + current
    }

    private fun magnitudeValue(token: String): Long =
        ONES[token]?.toLong()
            ?: TEENS[token]?.toLong()
            ?: TENS[token]?.let { it.toLong() * 10 }  // 2→20, 3→30, …
            ?: 0L

    // ── Digit-by-digit mode ─────────────────────────────────────────────────

    private fun parseDigitByDigit(tokens: List<String>): List<Int> {
        val digits = mutableListOf<Int>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val teen = TEENS[token]
            if (teen != null) {
                digits.addAll(teen.toDigits())
                i++
                continue
            }
            val tensDigit = TENS[token]
            if (tensDigit != null) {
                val next = tokens.getOrNull(i + 1)
                val onesDigit = next?.let { ONES[it] }
                if (onesDigit != null) {
                    digits.add(tensDigit)
                    digits.add(onesDigit)
                    i += 2
                } else {
                    digits.add(tensDigit)
                    digits.add(0)
                    i++
                }
                continue
            }
            val ones = ONES[token]
            if (ones != null) {
                digits.add(ones)
                i++
                continue
            }
            i++ // unknown token — skip
        }
        return digits
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun Long.toDigits(): List<Int> =
        toString().map { it.digitToInt() }

    private fun Int.toDigits(): List<Int> =
        toString().map { it.digitToInt() }

    // ── Word tables ──────────────────────────────────────────────────────────

    companion object {
        val ONES = mapOf(
            "zero" to 0, "oh" to 0,
            "one" to 1, "two" to 2, "to" to 2, "too" to 2,
            "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        )

        // teens map to their integer value (for magnitude mode) and digit pair (for digit-by-digit)
        val TEENS = mapOf(
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16,
            "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
        )

        // tens map to their leading digit (digit-by-digit mode) or integer value (magnitude)
        val TENS = mapOf(
            "twenty" to 2, "thirty" to 3, "forty" to 4, "fifty" to 5,
            "sixty" to 6, "seventy" to 7, "eighty" to 8, "ninety" to 9,
        )

        val MAGNITUDE_WORDS = setOf("hundred", "thousand", "million", "billion", "trillion")
    }
}
