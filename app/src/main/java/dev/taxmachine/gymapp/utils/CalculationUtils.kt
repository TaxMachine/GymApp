package dev.taxmachine.gymapp.utils

import dev.taxmachine.gymapp.db.WeightLogEntity
import dev.taxmachine.gymapp.db.SupplementLogEntity

object CalculationUtils {
    /**
     * Performance metric: Estimated One-Rep Max (Epley Formula)
     * 1RM = Weight * (1 + Reps / 30)
     */
    fun calculate1RM(weight: Float, reps: Int): Float {
        if (reps <= 0) return weight
        return weight * (1f + reps / 30f)
    }

    fun kgToLbs(kg: Float): Float = kg * 2.20462f
    fun lbsToKg(lbs: Float): Float = lbs / 2.20462f

    fun convertWeight(weight: Float, fromUnit: String, toUnit: String): Float {
        if (fromUnit == toUnit) return weight
        return if (fromUnit == "kg") kgToLbs(weight) else lbsToKg(weight)
    }

    fun calculatePeptideDose(massMg: Double, waterMl: Double, desiredDoseMcg: Double): Double {
        if (massMg <= 0 || waterMl <= 0 || desiredDoseMcg <= 0) return 0.0
        val concentrationMcgPerMl = (massMg * 1000) / waterMl
        return (desiredDoseMcg / concentrationMcgPerMl) * 100 // 100 units per ml
    }

    fun calculateTotalDoses(massMg: Double, desiredDoseMcg: Double): Double {
        if (massMg <= 0 || desiredDoseMcg <= 0) return 0.0
        return (massMg * 1000) / desiredDoseMcg
    }

    data class WorkoutStats(
        val start: Float,
        val current: Float,
        val difference: Float,
        val percentage: Float,
        val volume: Float,
        val personalBest: Float
    )

    fun calculateWorkoutStats(logs: List<WeightLogEntity>): WorkoutStats? {
        if (logs.isEmpty()) return null
        val firstWeight = logs.first().weight
        val lastWeight = logs.last().weight
        val diff = lastWeight - firstWeight
        val percent = if (firstWeight != 0f) (diff / firstWeight * 100) else 0f
        val totalVolume = logs.sumOf { it.weight.toDouble() }.toFloat()
        val pb = logs.maxOf { it.weight }
        
        return WorkoutStats(
            start = firstWeight,
            current = lastWeight,
            difference = diff,
            percentage = percent,
            volume = totalVolume,
            personalBest = pb
        )
    }

    data class SupplementStats(
        val start: Float,
        val current: Float,
        val difference: Float,
        val peak: Float
    )

    fun calculateSupplementStats(logs: List<SupplementLogEntity>): SupplementStats? {
        if (logs.isEmpty()) return null
        val firstDose = logs.first().dosage
        val lastDose = logs.last().dosage
        val diff = lastDose - firstDose
        val maxDose = logs.maxOf { it.dosage }
        
        return SupplementStats(
            start = firstDose,
            current = lastDose,
            difference = diff,
            peak = maxDose
        )
    }
}
