package dev.taxmachine.gymapp.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE id = :id")
    suspend fun getBadgeById(id: String): BadgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: BadgeEntity)

    @Update
    suspend fun updateBadge(badge: BadgeEntity)

    @Delete
    suspend fun deleteBadge(badge: BadgeEntity)

    // Badge History
    @Insert
    suspend fun insertBadgeHistory(history: BadgeHistoryEntity)

    @Query("SELECT * FROM badge_history WHERE badgeId = :badgeId ORDER BY timestamp DESC")
    fun getHistoryForBadge(badgeId: String): Flow<List<BadgeHistoryEntity>>

    // Splits
    @Query("SELECT * FROM splits")
    fun getAllSplits(): Flow<List<SplitEntity>>

    @Insert
    suspend fun insertSplit(split: SplitEntity): Long

    @Delete
    suspend fun deleteSplit(split: SplitEntity)

    // Exercises
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE splitId = :splitId")
    fun getExercisesBySplit(splitId: Long): Flow<List<ExerciseEntity>>

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    // Weight Logs
    @Query("SELECT * FROM weight_logs")
    fun getAllWeightLogs(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE exerciseId = :exerciseId ORDER BY timestamp ASC")
    fun getWeightLogsForExercise(exerciseId: Long): Flow<List<WeightLogEntity>>

    @Insert
    suspend fun insertWeightLog(log: WeightLogEntity)

    // Supplements
    @Query("SELECT * FROM supplements")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Insert
    suspend fun insertSupplement(supplement: SupplementEntity): Long

    @Update
    suspend fun updateSupplement(supplement: SupplementEntity)

    @Delete
    suspend fun deleteSupplement(supplement: SupplementEntity)

    // Supplement Logs
    @Query("SELECT * FROM supplement_logs")
    fun getAllSupplementLogs(): Flow<List<SupplementLogEntity>>

    @Query("SELECT * FROM supplement_logs WHERE supplementUid = :supplementUid ORDER BY timestamp ASC")
    fun getLogsForSupplement(supplementUid: Int): Flow<List<SupplementLogEntity>>

    @Insert
    suspend fun insertSupplementLog(log: SupplementLogEntity)

    // Custom Theme Colors
    @Query("SELECT * FROM custom_theme_colors WHERE isDark = :isDark")
    fun getCustomThemeColors(isDark: Boolean): Flow<CustomThemeColorsEntity?>

    @Query("SELECT * FROM custom_theme_colors")
    fun getAllCustomThemeColors(): Flow<List<CustomThemeColorsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomThemeColors(colors: CustomThemeColorsEntity)

    @Query("DELETE FROM custom_theme_colors")
    suspend fun deleteAllCustomThemeColors()

    // Health Sleep Logs
    @Query("SELECT * FROM health_sleep_logs ORDER BY startTime DESC")
    fun getAllHealthSleepLogs(): Flow<List<HealthSleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthSleepLogs(logs: List<HealthSleepLogEntity>)

    // Health Sleep Stages
    @Query("SELECT * FROM health_sleep_stages WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun getSleepStagesForSession(sessionId: String): Flow<List<HealthSleepStageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthSleepStages(stages: List<HealthSleepStageEntity>)

    // Health Weight Logs
    @Query("SELECT * FROM health_weight_logs ORDER BY timestamp DESC")
    fun getAllHealthWeightLogs(): Flow<List<HealthWeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthWeightLogs(logs: List<HealthWeightLogEntity>)

    // Health Nutrition Logs
    @Query("SELECT * FROM health_nutrition_logs ORDER BY timestamp DESC")
    fun getAllHealthNutritionLogs(): Flow<List<HealthNutritionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthNutritionLogs(logs: List<HealthNutritionLogEntity>)
}
