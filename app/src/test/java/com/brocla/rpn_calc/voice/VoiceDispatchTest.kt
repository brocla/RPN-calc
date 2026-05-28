@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.brocla.rpn_calc.voice

import com.brocla.rpn_calc.testdoubles.FakeVoiceParser
import com.brocla.rpn_calc.ui.calculator.CalcKeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceDispatchTest {

    // Fresh TestScope per test — immune to uncaught exceptions leaked by other test classes
    private fun scope() = TestScope(UnconfinedTestDispatcher())

    @Test fun utterance_parsed_and_dispatched() = scope().runTest {
        val utterances = MutableSharedFlow<String>()
        val parser     = FakeVoiceParser(events = listOf(CalcKeyEvent.Digit(5), CalcKeyEvent.Enter))
        val dispatched = mutableListOf<CalcKeyEvent>()

        val job = launch { collectAndDispatch(utterances, parser) { dispatched.add(it) } }
        utterances.emit("five enter")
        job.cancel()

        assertEquals(listOf(CalcKeyEvent.Digit(5), CalcKeyEvent.Enter), dispatched)
    }

    @Test fun multiple_utterances_both_dispatched() = scope().runTest {
        val utterances = MutableSharedFlow<String>()
        val parser     = FakeVoiceParser(events = listOf(CalcKeyEvent.Digit(3)))
        val dispatched = mutableListOf<CalcKeyEvent>()

        val job = launch { collectAndDispatch(utterances, parser) { dispatched.add(it) } }
        utterances.emit("three")
        utterances.emit("three")
        job.cancel()

        assertEquals(listOf(CalcKeyEvent.Digit(3), CalcKeyEvent.Digit(3)), dispatched)
    }

    @Test fun empty_parse_result_no_dispatch() = scope().runTest {
        val utterances = MutableSharedFlow<String>()
        val parser     = FakeVoiceParser(events = emptyList())
        val dispatched = mutableListOf<CalcKeyEvent>()

        val job = launch { collectAndDispatch(utterances, parser) { dispatched.add(it) } }
        utterances.emit("unknown noise")
        job.cancel()

        assertEquals(emptyList<CalcKeyEvent>(), dispatched)
    }

    @Test fun correct_utterance_passed_to_parser() = scope().runTest {
        val utterances = MutableSharedFlow<String>()
        val parser     = FakeVoiceParser()

        val job = launch { collectAndDispatch(utterances, parser) { } }
        utterances.emit("three add")
        job.cancel()

        assertEquals("three add", parser.lastUtterance)
    }
}
