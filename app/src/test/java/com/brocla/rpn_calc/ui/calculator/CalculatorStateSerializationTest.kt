package com.brocla.rpn_calc.ui.calculator

import com.brocla.rpn_calc.logic.model.AngleMode
import com.brocla.rpn_calc.logic.model.CalculatorState
import com.brocla.rpn_calc.logic.model.DisplayMode
import com.brocla.rpn_calc.logic.model.DisplaySettings
import com.brocla.rpn_calc.logic.model.EntryState
import com.brocla.rpn_calc.logic.model.Stack
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorStateSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(state: CalculatorState): CalculatorState =
        json.decodeFromString(json.encodeToString(state))

    @Test
    fun roundTrip_defaultState() {
        val original = CalculatorState()
        val restored = roundTrip(original)
        assertEquals(original, restored)
    }

    @Test
    fun roundTrip_stackWithValues() {
        val original = CalculatorState(
            stack = Stack(x = 1.23, y = 4.56, z = 7.89, t = 0.1)
        )
        val restored = roundTrip(original)
        assertEquals(original.stack.x, restored.stack.x)
        assertEquals(original.stack.y, restored.stack.y)
        assertEquals(original.stack.z, restored.stack.z)
        assertEquals(original.stack.t, restored.stack.t)
    }

    @Test
    fun roundTrip_entryState_mantissa() {
        val original = CalculatorState(
            entryState = EntryState.Mantissa(digits = "314", hasDecimal = true, isNegative = false)
        )
        val restored = roundTrip(original)
        val es = restored.entryState
        assertTrue(es is EntryState.Mantissa)
        assertEquals("314", es.digits)
        assertEquals(true, es.hasDecimal)
        assertEquals(false, es.isNegative)
    }

    @Test
    fun roundTrip_entryState_exponent() {
        val original = CalculatorState(
            entryState = EntryState.Exponent(
                mantissaIntPart = "1",
                mantissaFracPart = "0",
                mantissaHasDecimal = true,
                mantissaIsNegative = false,
                exponentDigits = "07",
                exponentIsNegative = true,
            )
        )
        val restored = roundTrip(original)
        val es = restored.entryState
        assertTrue(es is EntryState.Exponent)
        assertEquals("1", es.mantissaIntPart)
        assertEquals("0", es.mantissaFracPart)
        assertEquals(true, es.mantissaHasDecimal)
        assertEquals(false, es.mantissaIsNegative)
        assertEquals("07", es.exponentDigits)
        assertEquals(true, es.exponentIsNegative)
    }

    @Test
    fun roundTrip_entryState_idle() {
        val original = CalculatorState(entryState = EntryState.Idle)
        val restored = roundTrip(original)
        assertTrue(restored.entryState is EntryState.Idle)
    }

    @Test
    fun roundTrip_displayMode_fix() {
        val original = CalculatorState(displaySettings = DisplaySettings(DisplayMode.Fix(4)))
        val restored = roundTrip(original)
        val mode = restored.displaySettings.mode
        assertTrue(mode is DisplayMode.Fix)
        assertEquals(4, mode.decimalPlaces)
    }

    @Test
    fun roundTrip_displayMode_sci() {
        val original = CalculatorState(displaySettings = DisplaySettings(DisplayMode.Sci(3)))
        val restored = roundTrip(original)
        val mode = restored.displaySettings.mode
        assertTrue(mode is DisplayMode.Sci)
        assertEquals(3, mode.decimalPlaces)
    }

    @Test
    fun roundTrip_displayMode_eng() {
        val original = CalculatorState(displaySettings = DisplaySettings(DisplayMode.Eng(2)))
        val restored = roundTrip(original)
        val mode = restored.displaySettings.mode
        assertTrue(mode is DisplayMode.Eng)
        assertEquals(2, mode.decimalPlaces)
    }

    @Test
    fun roundTrip_displayMode_all() {
        val original = CalculatorState(displaySettings = DisplaySettings(DisplayMode.All))
        val restored = roundTrip(original)
        assertTrue(restored.displaySettings.mode is DisplayMode.All)
    }

    @Test
    fun roundTrip_angleMode_rad() {
        val original = CalculatorState(angleMode = AngleMode.RAD)
        val restored = roundTrip(original)
        assertEquals(AngleMode.RAD, restored.angleMode)
    }

    @Test
    fun roundTrip_memoryRegisters() {
        val memory = listOf(1.1, 2.2, 3.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.9)
        val original = CalculatorState(memory = memory)
        val restored = roundTrip(original)
        assertEquals(memory, restored.memory)
    }

    @Test
    fun roundTrip_errorState() {
        val original = CalculatorState(error = "Error")
        val restored = roundTrip(original)
        assertEquals("Error", restored.error)
    }

    @Test
    fun roundTrip_nullError() {
        val original = CalculatorState(error = null)
        val restored = roundTrip(original)
        assertNull(restored.error)
    }

    @Test
    fun roundTrip_shiftActive() {
        val original = CalculatorState(shiftActive = true)
        val restored = roundTrip(original)
        assertEquals(true, restored.shiftActive)
    }

    @Test
    fun roundTrip_lastX() {
        val original = CalculatorState(lastX = -3.14159265358979)
        val restored = roundTrip(original)
        assertEquals(original.lastX, restored.lastX)
    }
}
