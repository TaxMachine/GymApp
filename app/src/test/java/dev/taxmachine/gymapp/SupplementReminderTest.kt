package dev.taxmachine.gymapp

import dev.taxmachine.gymapp.db.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplementReminderTest {

    @Test
    fun testSupplementGrouping() {
        val supplements = listOf(
            SupplementEntity(
                uid = 1,
                name = "Vitamin D",
                dosage = "2000",
                unit = DosingUnit.MCG,
                timing = AdministrationTiming.MORNING_FASTED,
                frequency = AdministrationFrequency.EVERY_DAY,
                isInjectable = false
            ),
            SupplementEntity(
                uid = 2,
                name = "Caffeine",
                dosage = "200",
                unit = DosingUnit.MG,
                timing = AdministrationTiming.PRE_WORKOUT,
                frequency = AdministrationFrequency.PRE_WORKOUT,
                isInjectable = false
            ),
            SupplementEntity(
                uid = 3,
                name = "Omega 3",
                dosage = "1000",
                unit = DosingUnit.MG,
                timing = AdministrationTiming.MORNING_FULL,
                frequency = AdministrationFrequency.EVERY_DAY,
                isInjectable = false
            )
        )

        val morningSupps = supplements.filter { it.timing.name.startsWith("MORNING") }
        
        assertEquals(2, morningSupps.size)
        assertTrue(morningSupps.any { it.name == "Vitamin D" })
        assertTrue(morningSupps.any { it.name == "Omega 3" })
    }
}
