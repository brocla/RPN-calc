package com.brocla.rpn_calc.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class DigitSequenceParserTest {

    private val parser = DigitSequenceParserImpl()

    @Test fun single_digit() =
        assertEquals(listOf(5), parser.parse(listOf("five")))

    @Test fun digit_by_digit_sequence() =
        assertEquals(listOf(3, 2, 5), parser.parse(listOf("three", "two", "five")))

    @Test fun teens() =
        assertEquals(listOf(1, 5), parser.parse(listOf("fifteen")))

    @Test fun tens_alone() =
        assertEquals(listOf(2, 0), parser.parse(listOf("twenty")))

    @Test fun tens_with_digit() =
        assertEquals(listOf(2, 5), parser.parse(listOf("twenty", "five")))

    @Test fun oh_alias_for_zero() =
        assertEquals(listOf(0, 3), parser.parse(listOf("oh", "three")))

    @Test fun repeated_oh_each_becomes_zero() =
        assertEquals(listOf(5, 0, 0, 0), parser.parse(listOf("five", "oh", "oh", "oh")))

    @Test fun magnitude_hundreds() =
        assertEquals(listOf(3, 2, 5), parser.parse(listOf("three", "hundred", "twenty", "five")))

    @Test fun magnitude_thousands() =
        assertEquals(listOf(1, 2, 4, 0, 0), parser.parse(listOf("twelve", "thousand", "four", "hundred")))

    @Test fun magnitude_millions() =
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0), parser.parse(listOf("one", "million")))

    @Test fun magnitude_billions() =
        assertEquals(listOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0), parser.parse(listOf("two", "billion")))

    @Test fun colloquial_mixed() =
        assertEquals(listOf(3, 2, 5), parser.parse(listOf("three", "twenty", "five")))

    @Test fun empty_tokens() =
        assertEquals(emptyList<Int>(), parser.parse(emptyList()))
}
