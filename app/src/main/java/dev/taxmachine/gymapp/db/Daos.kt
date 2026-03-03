package dev.taxmachine.gymapp.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: BadgeEntity)

    @Delete
    suspend fun deleteBadge(badge: BadgeEntity)

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
}
