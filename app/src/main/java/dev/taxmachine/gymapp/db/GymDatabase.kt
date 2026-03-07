package dev.taxmachine.gymapp.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromDosingUnit(value: DosingUnit) = value.ordinal

    @TypeConverter
    fun toDosingUnit(value: Int) = DosingUnit.entries[value]

    @TypeConverter
    fun fromTiming(value: AdministrationTiming) = value.ordinal

    @TypeConverter
    fun toTiming(value: Int) = AdministrationTiming.entries[value]

    @TypeConverter
    fun fromFrequency(value: AdministrationFrequency) = value.ordinal

    @TypeConverter
    fun toFrequency(value: Int) = AdministrationFrequency.entries[value]
}

@Database(
    entities = [
        BadgeEntity::class,
        BadgeHistoryEntity::class,
        SplitEntity::class,
        ExerciseEntity::class,
        WeightLogEntity::class,
        SupplementEntity::class,
        SupplementLogEntity::class,
        CustomThemeColorsEntity::class,
        HealthSleepLogEntity::class,
        HealthSleepStageEntity::class,
        HealthWeightLogEntity::class,
        HealthNutritionLogEntity::class
    ],
    version = 15
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_sleep_logs` (
                        `id` TEXT NOT NULL, 
                        `startTime` INTEGER NOT NULL, 
                        `endTime` INTEGER NOT NULL, 
                        `durationMinutes` INTEGER NOT NULL, 
                        `sleepScore` INTEGER NOT NULL DEFAULT 0,
                        `source` TEXT NOT NULL DEFAULT 'HealthConnect', 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_weight_logs` (
                        `id` TEXT NOT NULL, 
                        `weightKg` REAL NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `source` TEXT NOT NULL DEFAULT 'HealthConnect', 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_nutrition_logs` (
                        `id` TEXT NOT NULL, 
                        `energyKcal` REAL NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `name` TEXT, 
                        `source` TEXT NOT NULL DEFAULT 'HealthConnect', 
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix potential missing sleepScore column in health_sleep_logs if migration 13-14 was botched
                val cursor = db.query("PRAGMA table_info(`health_sleep_logs`)")
                var hasSleepScore = false
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == "sleepScore") {
                            hasSleepScore = true
                            break
                        }
                    }
                }
                cursor.close()
                
                if (!hasSleepScore) {
                    db.execSQL("ALTER TABLE `health_sleep_logs` ADD COLUMN `sleepScore` INTEGER NOT NULL DEFAULT 0")
                }

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_sleep_stages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `sessionId` TEXT NOT NULL, 
                        `startTime` INTEGER NOT NULL, 
                        `endTime` INTEGER NOT NULL, 
                        `stage` INTEGER NOT NULL, 
                        FOREIGN KEY(`sessionId`) REFERENCES `health_sleep_logs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_sleep_stages_sessionId` ON `health_sleep_stages` (`sessionId`)")
            }
        }

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                .addMigrations(MIGRATION_13_14, MIGRATION_14_15)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
