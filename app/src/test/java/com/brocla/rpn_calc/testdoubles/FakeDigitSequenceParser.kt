package com.brocla.rpn_calc.testdoubles

import com.brocla.rpn_calc.voice.DigitSequenceParser

class FakeDigitSequenceParser(
    private val result: List<Int> = emptyList()
) : DigitSequenceParser {
    var lastTokens: List<String>? = null

    override fun parse(tokens: List<String>): List<Int> {
        lastTokens = tokens
        return result
    }
}
