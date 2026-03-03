package dev.taxmachine.gymapp.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tagData: String
)

@Entity(tableName = "splits")
data class SplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = SplitEntity::class,
            parentColumns = ["id"],
            childColumns = ["splitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitId: Long,
    val name: String,
    val weight: Float,
    val weightUnit: String,
    val reps: Int
)

@Entity(
    tableName = "weight_logs",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val weight: Float,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DosingUnit(val label: String) {
    MCG("mcg"), MG("mg"), G("g"), IU("IU")
}

enum class AdministrationTiming(val label: String) {
    MORNING_FASTED("Morning (Fasted)"),
    MORNING_FULL("Morning (Full)"),
    PRE_WORKOUT("Pre-workout"),
    POST_WORKOUT("Post-workout"),
    BEFORE_BED("Before bed")
}

enum class AdministrationFrequency(val label: String) {
    EVERY_DAY("Every day"),
    EVERY_OTHER_DAY("Every other day (Mon, Wed, Fri)"),
    PRE_WORKOUT("Pre-workout")
}

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val name: String,
    val dosage: String,
    val unit: DosingUnit,
    val timing: AdministrationTiming,
    val frequency: AdministrationFrequency,
    val isInjectable: Boolean
)

@Entity(
    tableName = "supplement_logs",
    foreignKeys = [
        ForeignKey(
            entity = SupplementEntity::class,
            parentColumns = ["uid"],
            childColumns = ["supplementUid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SupplementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementUid: Int,
    val dosage: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_theme_colors")
data class CustomThemeColorsEntity(
    @PrimaryKey val isDark: Boolean,
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val error: Long,
    val onError: Long
)
