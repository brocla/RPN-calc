package com.brocla.rpn_calc.voice

/**
 * Score a single STT candidate string for calculator relevance.
 * combined = vocab_coverage × 0.7 + stt_confidence × 0.3
 * vocab_coverage = fraction of tokens that are keywords or number words.
 */
fun scoreCandidateForCalculator(
    utterance:   String,
    keywords:    Set<String>,
    numberWords: Set<String>,
    confidence:  Float = 0f,
): Float {
    val tokens = utterance.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return 0f
    val vocabCount = tokens.count { it in keywords || it in numberWords }
    val vocabCoverage = vocabCount.toFloat() / tokens.size
    return vocabCoverage * 0.7f + confidence * 0.3f
}

/**
 * Given the parallel alternatives / confidences arrays from onResults,
 * return whichever alternative scores highest for the calculator vocabulary.
 */
fun selectBestAlternative(
    alternatives: List<String>,
    confidences:  FloatArray,
    keywords:     Set<String>,
    numberWords:  Set<String>,
): String {
    if (alternatives.isEmpty()) return ""
    return alternatives.indices.maxByOrNull { i ->
        scoreCandidateForCalculator(
            utterance   = alternatives[i],
            keywords    = keywords,
            numberWords = numberWords,
            confidence  = confidences.getOrElse(i) { 0f },
        )
    }.let { alternatives[it ?: 0] }
}
