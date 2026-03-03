package dev.taxmachine.gymapp

import dev.taxmachine.gymapp.db.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun testDosingUnitConversion() {
        assertEquals(DosingUnit.MG.ordinal, converters.fromDosingUnit(DosingUnit.MG))
        assertEquals(DosingUnit.MG, converters.toDosingUnit(DosingUnit.MG.ordinal))
        
        assertEquals(DosingUnit.G.ordinal, converters.fromDosingUnit(DosingUnit.G))
        assertEquals(DosingUnit.G, converters.toDosingUnit(DosingUnit.G.ordinal))
    }

    @Test
    fun testTimingConversion() {
        assertEquals(AdministrationTiming.PRE_WORKOUT.ordinal, converters.fromTiming(AdministrationTiming.PRE_WORKOUT))
        assertEquals(AdministrationTiming.PRE_WORKOUT, converters.toTiming(AdministrationTiming.PRE_WORKOUT.ordinal))
    }

    @Test
    fun testFrequencyConversion() {
        assertEquals(AdministrationFrequency.EVERY_OTHER_DAY.ordinal, converters.fromFrequency(AdministrationFrequency.EVERY_OTHER_DAY))
        assertEquals(AdministrationFrequency.EVERY_OTHER_DAY, converters.toFrequency(AdministrationFrequency.EVERY_OTHER_DAY.ordinal))
    }
}
