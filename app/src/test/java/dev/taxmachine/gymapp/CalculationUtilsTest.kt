package dev.taxmachine.gymapp

import dev.taxmachine.gymapp.db.SupplementLogEntity
import dev.taxmachine.gymapp.db.WeightLogEntity
import dev.taxmachine.gymapp.utils.CalculationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculationUtilsTest {

    @Test
    fun testCalculate1RM() {
        // 100kg for 10 reps -> 100 * (1 + 10/30) = 133.33
        assertEquals(133.33f, CalculationUtils.calculate1RM(100f, 10), 0.01f)
        
        // 100kg for 1 rep -> 100 * (1 + 1/30) = 103.33
        assertEquals(103.33f, CalculationUtils.calculate1RM(100f, 1), 0.01f)
        
        // 0 reps should return weight
        assertEquals(100f, CalculationUtils.calculate1RM(100f, 0), 0.01f)
    }

    @Test
    fun testCalculatePeptideDose() {
        // 5mg vial, 2ml water, 250mcg dose
        // Concentration: 5000mcg / 2ml = 2500 mcg/ml
        // Dose: 250mcg / 2500mcg/ml = 0.1ml
        // Units: 0.1ml * 100 = 10 units
        assertEquals(10.0, CalculationUtils.calculatePeptideDose(5.0, 2.0, 250.0), 0.01)

        // Edge cases
        assertEquals(0.0, CalculationUtils.calculatePeptideDose(0.0, 2.0, 250.0), 0.01)
        assertEquals(0.0, CalculationUtils.calculatePeptideDose(5.0, 0.0, 250.0), 0.01)
        assertEquals(0.0, CalculationUtils.calculatePeptideDose(5.0, 2.0, 0.0), 0.01)
    }

    @Test
    fun testCalculateTotalDoses() {
        // 5mg vial, 250mcg dose -> 5000 / 250 = 20 doses
        assertEquals(20.0, CalculationUtils.calculateTotalDoses(5.0, 250.0), 0.01)
        
        assertEquals(0.0, CalculationUtils.calculateTotalDoses(0.0, 250.0), 0.01)
        assertEquals(0.0, CalculationUtils.calculateTotalDoses(5.0, 0.0), 0.01)
    }

    @Test
    fun testCalculateWorkoutStats() {
        val logs = listOf(
            WeightLogEntity(id = 1, exerciseId = 1, weight = 100f, timestamp = 1000),
            WeightLogEntity(id = 2, exerciseId = 1, weight = 110f, timestamp = 2000),
            WeightLogEntity(id = 3, exerciseId = 1, weight = 105f, timestamp = 3000),
            WeightLogEntity(id = 4, exerciseId = 1, weight = 120f, timestamp = 4000)
        )

        val stats = CalculationUtils.calculateWorkoutStats(logs)!!
        
        assertEquals(100f, stats.start)
        assertEquals(120f, stats.current)
        assertEquals(20f, stats.difference)
        assertEquals(20f, stats.percentage)
        assertEquals(435f, stats.volume)
        assertEquals(120f, stats.personalBest)
    }

    @Test
    fun testCalculateWorkoutStatsEmpty() {
        assertNull(CalculationUtils.calculateWorkoutStats(emptyList()))
    }

    @Test
    fun testCalculateSupplementStats() {
        val logs = listOf(
            SupplementLogEntity(id = 1, supplementUid = 1, dosage = 10f, timestamp = 1000),
            SupplementLogEntity(id = 2, supplementUid = 1, dosage = 15f, timestamp = 2000),
            SupplementLogEntity(id = 3, supplementUid = 1, dosage = 12f, timestamp = 3000)
        )

        val stats = CalculationUtils.calculateSupplementStats(logs)!!
        
        assertEquals(10f, stats.start)
        assertEquals(12f, stats.current)
        assertEquals(2f, stats.difference)
        assertEquals(15f, stats.peak)
    }

    @Test
    fun testCalculateSupplementStatsEmpty() {
        assertNull(CalculationUtils.calculateSupplementStats(emptyList()))
    }
}
