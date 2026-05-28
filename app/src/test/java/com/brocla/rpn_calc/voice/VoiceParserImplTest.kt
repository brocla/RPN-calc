package com.brocla.rpn_calc.voice

import com.brocla.rpn_calc.testdoubles.FakeDigitSequenceParser
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceParserImplTest {

    private fun parser(fakeResult: List<Int> = emptyList()): Pair<VoiceParserImpl, FakeDigitSequenceParser> {
        val fake = FakeDigitSequenceParser(fakeResult)
        return VoiceParserImpl(fake) to fake
    }

    @Test fun single_operator() {
        val (p, fake) = parser()
        assertEquals(listOf(CalcKeyEvent.Enter), p.parse("enter"))
        assertNull(fake.lastTokens)
    }

    @Test fun number_segment_delegated() {
        val (p, fake) = parser(fakeResult = listOf(3, 2, 5))
        val events = p.parse("three twenty five")
        assertEquals(listOf("three", "twenty", "five"), fake.lastTokens)
        assertEquals(listOf(CalcKeyEvent.Digit(3), CalcKeyEvent.Digit(2), CalcKeyEvent.Digit(5)), events)
    }

    @Test fun number_then_operator() {
        val (p, fake) = parser(fakeResult = listOf(5))
        val events = p.parse("five enter")
        assertEquals(listOf("five"), fake.lastTokens)
        assertEquals(listOf(CalcKeyEvent.Digit(5), CalcKeyEvent.Enter), events)
    }

    @Test fun decimal_splits_segment() {
        val (p, fake) = parser(fakeResult = listOf(5))
        val events = p.parse("five point seven")
        // fake called only for pre-decimal tokens
        assertEquals(listOf("five"), fake.lastTokens)
        assertEquals(
            listOf(CalcKeyEvent.Digit(5), CalcKeyEvent.Decimal, CalcKeyEvent.Digit(7)),
            events
        )
    }

    @Test fun decimal_second_ignored() {
        val (p, fake) = parser(fakeResult = listOf(5))
        val events = p.parse("five point two point one")
        // second "point" treated as unrecognised digit token, not a second Decimal
        val decimals = events.count { it == CalcKeyEvent.Decimal }
        assertEquals(1, decimals)
        assertEquals(CalcKeyEvent.Digit(5), events.first())
        assertEquals(CalcKeyEvent.Decimal, events[1])
    }

    @Test fun multi_word_phrase_matched_first() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Divide), p.parse("divided by"))
    }

    @Test fun multi_word_change_sign() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Chs), p.parse("change sign"))
    }

    @Test fun fuzzy_keyword_match() {
        val (p, _) = parser()
        // "minis" is distance 2 from "minus"
        assertEquals(listOf(CalcKeyEvent.Subtract), p.parse("minis"))
    }

    @Test fun fuzzy_no_match_above_threshold() {
        val (p, _) = parser()
        assertEquals(emptyList<CalcKeyEvent>(), p.parse("xyzzy"))
    }

    @Test fun leading_trailing_whitespace() {
        val (p, fake) = parser(fakeResult = listOf(5))
        val trimmed = p.parse("five enter")
        val padded  = p.parse("  five  enter  ")
        assertEquals(trimmed, padded)
    }

    @Test fun numeric_string_single_digit() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Digit(5)), p.parse("5"))
    }

    @Test fun numeric_string_multi_digit() {
        val (p, _) = parser()
        assertEquals(
            listOf(CalcKeyEvent.Digit(3), CalcKeyEvent.Digit(2), CalcKeyEvent.Digit(5)),
            p.parse("325")
        )
    }

    @Test fun numeric_string_then_operator() {
        val (p, _) = parser()
        assertEquals(
            listOf(CalcKeyEvent.Digit(1), CalcKeyEvent.Digit(2), CalcKeyEvent.Digit(3), CalcKeyEvent.Enter),
            p.parse("123 enter")
        )
    }

    @Test fun all_operator_aliases() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Enter),     p.parse("push"))
        assertEquals(listOf(CalcKeyEvent.Add),       p.parse("plus"))
        assertEquals(listOf(CalcKeyEvent.Subtract),  p.parse("subtract"))
        assertEquals(listOf(CalcKeyEvent.Multiply),  p.parse("multiply"))
        assertEquals(listOf(CalcKeyEvent.Divide),    p.parse("divide"))
        assertEquals(listOf(CalcKeyEvent.Chs),       p.parse("negate"))
        assertEquals(listOf(CalcKeyEvent.Decimal),   p.parse("dot"))
        assertEquals(listOf(CalcKeyEvent.Clx),       p.parse("clear"))
        assertEquals(listOf(CalcKeyEvent.Backspace), p.parse("delete"))
        assertEquals(listOf(CalcKeyEvent.Backspace), p.parse("back space"))
    }

    @Test fun math_ops_single_word() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Sqrt),        p.parse("root"))
        assertEquals(listOf(CalcKeyEvent.Square),      p.parse("square"))
        assertEquals(listOf(CalcKeyEvent.Square),      p.parse("squared"))
        assertEquals(listOf(CalcKeyEvent.Reciprocal),  p.parse("reciprocal"))
        assertEquals(listOf(CalcKeyEvent.Reciprocal),  p.parse("inverse"))
        assertEquals(listOf(CalcKeyEvent.Power),       p.parse("power"))
        assertEquals(listOf(CalcKeyEvent.Power),       p.parse("raise"))
        assertEquals(listOf(CalcKeyEvent.Power),       p.parse("raised"))
        assertEquals(listOf(CalcKeyEvent.Log),         p.parse("log"))
        assertEquals(listOf(CalcKeyEvent.Log),         p.parse("logarithm"))
        assertEquals(listOf(CalcKeyEvent.Ln),          p.parse("ln"))
        assertEquals(listOf(CalcKeyEvent.Exp),         p.parse("exponential"))
        assertEquals(listOf(CalcKeyEvent.Eex),         p.parse("exponent"))
        assertEquals(listOf(CalcKeyEvent.Pi),          p.parse("pi"))
        assertEquals(listOf(CalcKeyEvent.Factorial),   p.parse("factorial"))
        assertEquals(listOf(CalcKeyEvent.Percent),     p.parse("percent"))
        assertEquals(listOf(CalcKeyEvent.NCr),         p.parse("choose"))
        assertEquals(listOf(CalcKeyEvent.NPr),         p.parse("permutations"))
    }

    @Test fun trig_ops() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Sin),    p.parse("sin"))
        assertEquals(listOf(CalcKeyEvent.Sin),    p.parse("sine"))
        assertEquals(listOf(CalcKeyEvent.Cos),    p.parse("cos"))
        assertEquals(listOf(CalcKeyEvent.Cos),    p.parse("cosine"))
        assertEquals(listOf(CalcKeyEvent.Tan),    p.parse("tan"))
        assertEquals(listOf(CalcKeyEvent.Tan),    p.parse("tangent"))
        assertEquals(listOf(CalcKeyEvent.ArcSin), p.parse("arcsin"))
        assertEquals(listOf(CalcKeyEvent.ArcCos), p.parse("arccos"))
        assertEquals(listOf(CalcKeyEvent.ArcTan), p.parse("arctan"))
    }

    @Test fun sin_aliases() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Sin), p.parse("sin"))
        assertEquals(listOf(CalcKeyEvent.Sin), p.parse("sine"))
        // "sign" not in exact table — fuzzy-matches "sin" at distance 1
        assertEquals(listOf(CalcKeyEvent.Sin), p.parse("sign"))
        // CHS reached via "negate" or "change sign"
        assertEquals(listOf(CalcKeyEvent.Chs), p.parse("negate"))
        assertEquals(listOf(CalcKeyEvent.Chs), p.parse("change sign"))
    }

    @Test fun arc_trig_sign_collision() {
        val (p, _) = parser()
        // "arc cos sign" — 3-word phrase prevents "sign" being parsed as Sin after ArcCos
        assertEquals(listOf(CalcKeyEvent.ArcCos), p.parse("arc cos sign"))
        assertEquals(listOf(CalcKeyEvent.ArcCos), p.parse("arc cos sine"))
        assertEquals(listOf(CalcKeyEvent.ArcSin), p.parse("arc sin sign"))
        assertEquals(listOf(CalcKeyEvent.ArcTan), p.parse("arc tan sign"))
        // Normal 2-word forms still work
        assertEquals(listOf(CalcKeyEvent.ArcCos), p.parse("arc cos"))
        assertEquals(listOf(CalcKeyEvent.ArcSin), p.parse("arc sin"))
    }

    @Test fun to_and_too_parse_as_digit_two() {
        // Use real DigitSequenceParser so ONES lookup works correctly
        val p = VoiceParserImpl(DigitSequenceParserImpl())
        assertEquals(listOf(CalcKeyEvent.Digit(2)), p.parse("to"))
        assertEquals(listOf(CalcKeyEvent.Digit(2)), p.parse("too"))
    }

    @Test fun stack_ops() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.RollDown), p.parse("roll"))
        assertEquals(listOf(CalcKeyEvent.Swap),     p.parse("swap"))
        assertEquals(listOf(CalcKeyEvent.Swap),     p.parse("exchange"))
        assertEquals(listOf(CalcKeyEvent.Sto),      p.parse("store"))
        assertEquals(listOf(CalcKeyEvent.Rcl),      p.parse("recall"))
        assertEquals(listOf(CalcKeyEvent.LastX),    p.parse("last"))
    }

    @Test fun display_mode_ops() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.AllMode), p.parse("all"))
        assertEquals(listOf(CalcKeyEvent.FixArg),  p.parse("fix"))
        assertEquals(listOf(CalcKeyEvent.SciArg),  p.parse("sci"))
        assertEquals(listOf(CalcKeyEvent.SciArg),  p.parse("scientific"))
        assertEquals(listOf(CalcKeyEvent.EngArg),  p.parse("eng"))
        assertEquals(listOf(CalcKeyEvent.EngArg),  p.parse("engineering"))
        assertEquals(listOf(CalcKeyEvent.DegRad),  p.parse("angle"))
    }

    @Test fun two_word_phrases() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.Sqrt),          p.parse("square root"))
        assertEquals(listOf(CalcKeyEvent.Ln),            p.parse("natural log"))
        assertEquals(listOf(CalcKeyEvent.Pow10),         p.parse("anti log"))
        assertEquals(listOf(CalcKeyEvent.ArcSin),        p.parse("arc sin"))
        assertEquals(listOf(CalcKeyEvent.ArcCos),        p.parse("arc cos"))
        assertEquals(listOf(CalcKeyEvent.ArcTan),        p.parse("arc tan"))
        assertEquals(listOf(CalcKeyEvent.RollDown),      p.parse("roll down"))
        assertEquals(listOf(CalcKeyEvent.LastX),         p.parse("last x"))
        assertEquals(listOf(CalcKeyEvent.PercentChange), p.parse("percent change"))
        assertEquals(listOf(CalcKeyEvent.ArcTan),        p.parse("arc tangent"))
        assertEquals(listOf(CalcKeyEvent.ArcSin),        p.parse("arc sine"))
        assertEquals(listOf(CalcKeyEvent.ArcCos),        p.parse("arc cosine"))
    }

    @Test fun clipboard_and_help_ops() {
        val (p, _) = parser()
        assertEquals(listOf(CalcKeyEvent.CopyRequest),    p.parse("copy"))
        assertEquals(listOf(CalcKeyEvent.PasteClipboard), p.parse("paste"))
        assertEquals(listOf(CalcKeyEvent.OpenVoiceHelp),  p.parse("help"))
    }

    @Test fun store_with_digit() {
        val fake = FakeDigitSequenceParser(listOf(3))
        val impl = VoiceParserImpl(fake)
        assertEquals(
            listOf(CalcKeyEvent.Sto, CalcKeyEvent.Digit(3)),
            impl.parse("store three")
        )
    }

    @Test fun fix_with_numeric_string() {
        val (p, _) = parser()
        assertEquals(
            listOf(CalcKeyEvent.FixArg, CalcKeyEvent.Digit(2)),
            p.parse("fix 2")
        )
    }
}
