package com.brocla.rpn_calc.voice

interface DigitSequenceParser {
    fun parse(tokens: List<String>): List<Int>
}
