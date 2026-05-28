package com.brocla.rpn_calc.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateScorerTest {

    private val keywords    = setOf("enter", "plus", "minus", "times", "divide", "clear")
    private val numberWords = setOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "zero")

    @Test fun score_all_keywords() {
        val score = scoreCandidateForCalculator("five enter", keywords, numberWords)
        assertTrue("expected >= 0.7, got $score", score >= 0.7f)
    }

    @Test fun score_all_number_words() {
        val score = scoreCandidateForCalculator("three two five", keywords, numberWords)
        assertTrue("expected >= 0.7, got $score", score >= 0.7f)
    }

    @Test fun score_mixed_vocab() {
        // "five hello" — half vocab, half noise; confidence = 0
        val score = scoreCandidateForCalculator("five hello", keywords, numberWords, confidence = 0f)
        assertTrue("expected in [0.35, 0.7), got $score", score >= 0.35f && score < 0.7f)
    }

    @Test fun score_no_vocab() {
        val score = scoreCandidateForCalculator("blah blah blah", keywords, numberWords)
        assertEquals(0f, score, 0.001f)
    }

    @Test fun score_uses_confidence_weight() {
        // Same utterance, different confidence values
        val low  = scoreCandidateForCalculator("five enter", keywords, numberWords, confidence = 0f)
        val high = scoreCandidateForCalculator("five enter", keywords, numberWords, confidence = 1f)
        assertTrue("higher confidence should yield higher score", high > low)
    }

    @Test fun select_prefers_vocab_over_rank() {
        // rank-0 has no calc words; rank-1 has calc words
        val result = selectBestAlternative(
            alternatives = listOf("the weather is nice", "five enter"),
            confidences  = floatArrayOf(0.9f, 0.5f),
            keywords     = keywords,
            numberWords  = numberWords,
        )
        assertEquals("five enter", result)
    }

    @Test fun select_returns_first_when_tied() {
        // All alternatives have identical scores (all noise, zero confidence)
        val result = selectBestAlternative(
            alternatives = listOf("blah", "blah", "blah"),
            confidences  = floatArrayOf(0f, 0f, 0f),
            keywords     = keywords,
            numberWords  = numberWords,
        )
        assertEquals("blah", result)
    }

    @Test fun select_single_alternative() {
        val result = selectBestAlternative(
            alternatives = listOf("five enter"),
            confidences  = floatArrayOf(0.8f),
            keywords     = keywords,
            numberWords  = numberWords,
        )
        assertEquals("five enter", result)
    }
}
