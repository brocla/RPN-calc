@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.brocla.rpn_calc.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.vosk.Model

/**
 * Unit tests for VoskVoiceInputController.
 *
 * The real SpeechService / Vosk Model are never instantiated. We drive
 * recognitionListener directly to cover JSON parsing and state transitions.
 */
class VoskVoiceInputControllerTest {

    // ── Setup ─────────────────────────────────────────────────────────────────

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp()    { Dispatchers.setMain(testDispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun scope() = TestScope(testDispatcher)

    /** Minimal VoiceModelProvider whose model StateFlow is permanently null. */
    private fun controller(): VoskVoiceInputController {
        val provider = object : VoiceModelProvider {
            override val model: StateFlow<Model?> = MutableStateFlow(null)
        }
        return VoskVoiceInputController(provider)
    }

    // ── Partial results → interimText ─────────────────────────────────────────

    @Test fun partial_result_updates_interimText() = scope().runTest {
        val ctrl = controller()
        ctrl.recognitionListener.onPartialResult("""{"partial":"three plus"}""")
        assertEquals("three plus", ctrl.interimText.value)
    }

    @Test fun partial_result_empty_string_is_ignored() = scope().runTest {
        val ctrl = controller()
        ctrl.recognitionListener.onPartialResult("""{"partial":""}""")
        assertEquals("", ctrl.interimText.value)   // unchanged
    }

    @Test fun partial_result_missing_key_is_ignored() = scope().runTest {
        val ctrl = controller()
        ctrl.recognitionListener.onPartialResult("""{"other":"nope"}""")
        assertEquals("", ctrl.interimText.value)
    }

    @Test fun partial_result_bad_json_is_ignored() = scope().runTest {
        val ctrl = controller()
        ctrl.recognitionListener.onPartialResult("not json at all")
        assertEquals("", ctrl.interimText.value)
    }

    // ── Final result — "text" field ────────────────────────────────────────────

    @Test fun final_result_text_field_emitted() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        ctrl.recognitionListener.onResult("""{"text":"five enter"}""")
        job.cancel()

        assertEquals(listOf("five enter"), received)
    }

    @Test fun final_result_text_sets_interimText_display() = scope().runTest {
        val ctrl = controller()
        ctrl.recognitionListener.onResult("""{"text":"plus"}""")
        assertEquals("▶ plus", ctrl.interimText.value)
    }

    @Test fun final_result_empty_text_is_not_emitted() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        ctrl.recognitionListener.onResult("""{"text":""}""")
        job.cancel()

        assertTrue(received.isEmpty())
    }

    @Test fun final_result_unk_is_not_emitted() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        ctrl.recognitionListener.onResult("""{"text":"[unk]"}""")
        job.cancel()

        assertTrue(received.isEmpty())
    }

    // ── Final result — "alternatives" field ───────────────────────────────────

    @Test fun final_result_alternatives_uses_first() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        ctrl.recognitionListener.onResult(
            """{"alternatives":[{"text":"divide"},{"text":"vibe"},{"text":"die"}]}"""
        )
        job.cancel()

        assertEquals(listOf("divide"), received)
    }

    @Test fun final_result_alternatives_preferred_over_text() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        // If both keys present, alternatives wins
        ctrl.recognitionListener.onResult(
            """{"text":"fallback","alternatives":[{"text":"preferred"}]}"""
        )
        job.cancel()

        assertEquals(listOf("preferred"), received)
    }

    @Test fun final_result_bad_json_emits_nothing() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        ctrl.recognitionListener.onResult("garbage")
        job.cancel()

        assertTrue(received.isEmpty())
    }

    // ── onFinalResult delegates to onResult ───────────────────────────────────

    @Test fun on_final_result_delegates_to_on_result() = scope().runTest {
        val ctrl = controller()
        val received = mutableListOf<String>()
        val job = launch { ctrl.finalUtterance.collect { received.add(it) } }

        ctrl.recognitionListener.onFinalResult("""{"text":"enter"}""")
        job.cancel()

        assertEquals(listOf("enter"), received)
    }

    // ── Error → VoiceState.Error ───────────────────────────────────────────────

    @Test fun on_error_sets_error_state() = scope().runTest {
        val ctrl = controller()
        ctrl.recognitionListener.onError(RuntimeException("boom"))
        assertTrue(ctrl.state.value is VoiceState.Error)
    }

    // ── stopListening resets state ─────────────────────────────────────────────

    @Test fun stop_listening_clears_interim_text() = scope().runTest {
        val ctrl = controller()
        // Seed interimText with a partial result first
        ctrl.recognitionListener.onPartialResult("""{"partial":"three"}""")
        ctrl.stopListening()
        assertEquals("", ctrl.interimText.value)
    }

    @Test fun stop_listening_sets_idle_state() = scope().runTest {
        val ctrl = controller()
        // Force a non-idle state via an error, then stop
        ctrl.recognitionListener.onError(RuntimeException("err"))
        ctrl.stopListening()
        assertEquals(VoiceState.Idle, ctrl.state.value)
    }

    // ── Grammar completeness ──────────────────────────────────────────────────

    @Test fun grammar_contains_required_words() {
        val grammar = VoskVoiceInputController.GRAMMAR
        val required = listOf(
            "enter", "plus", "minus", "times", "divide",
            "zero", "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "oh",
            "point", "decimal",
            "backspace", "back space", "clear",
            "swap", "roll", "store", "recall",
            "sine", "cosine", "tangent",
            "log", "ln", "pi", "factorial",
            "help", "copy", "paste",
        )
        val missing = required.filter { word -> !grammar.contains("\"$word\"") }
        assertTrue("Grammar is missing: $missing", missing.isEmpty())
    }
}
