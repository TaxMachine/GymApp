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

    @TypeConverter
    fun fromLogLevel(value: AppLogLevel) = value.ordinal

    @TypeConverter
    fun toLogLevel(value: Int) = AppLogLevel.entries[value]
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
        HealthNutritionLogEntity::class,
        AppLogEntity::class
    ],
    version = 17
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

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `level` INTEGER NOT NULL, 
                        `tag` TEXT NOT NULL, 
                        `message` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix health_sleep_logs and health_sleep_stages (due to FK)
                db.execSQL("CREATE TABLE IF NOT EXISTS `health_sleep_logs_new` (`id` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `sleepScore` INTEGER NOT NULL DEFAULT 0, `source` TEXT NOT NULL DEFAULT 'HealthConnect', PRIMARY KEY(`id`))")
                val cursorLogs = db.query("PRAGMA table_info(`health_sleep_logs`)")
                val existingColumnsLogs = mutableListOf<String>()
                val nameIdxLogs = cursorLogs.getColumnIndex("name")
                if (nameIdxLogs != -1) {
                    while (cursorLogs.moveToNext()) { existingColumnsLogs.add(cursorLogs.getString(nameIdxLogs)) }
                }
                cursorLogs.close()
                val columnsToCopyLogs = listOf("id", "startTime", "endTime", "durationMinutes", "sleepScore", "source").filter { it in existingColumnsLogs }
                if (columnsToCopyLogs.isNotEmpty()) {
                    val cols = columnsToCopyLogs.joinToString("`, `", "`", "`")
                    db.execSQL("INSERT INTO `health_sleep_logs_new` ($cols) SELECT $cols FROM `health_sleep_logs`")
                }

                // IMPORTANT: The foreign key must reference the FINAL table name 'health_sleep_logs' to match Room's expected schema
                db.execSQL("CREATE TABLE IF NOT EXISTS `health_sleep_stages_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `stage` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `health_sleep_logs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                val cursorStages = db.query("PRAGMA table_info(`health_sleep_stages`)")
                val existingColumnsStages = mutableListOf<String>()
                val nameIdxStages = cursorStages.getColumnIndex("name")
                if (nameIdxStages != -1) {
                    while (cursorStages.moveToNext()) { existingColumnsStages.add(cursorStages.getString(nameIdxStages)) }
                }
                cursorStages.close()
                val columnsToCopyStages = listOf("id", "sessionId", "startTime", "endTime", "stage").filter { it in existingColumnsStages }
                if (columnsToCopyStages.isNotEmpty()) {
                    val cols = columnsToCopyStages.joinToString("`, `", "`", "`")
                    db.execSQL("INSERT INTO `health_sleep_stages_new` ($cols) SELECT $cols FROM `health_sleep_stages`")
                }

                db.execSQL("DROP TABLE IF EXISTS `health_sleep_stages`")
                db.execSQL("DROP TABLE IF EXISTS `health_sleep_logs`")
                db.execSQL("ALTER TABLE `health_sleep_logs_new` RENAME TO `health_sleep_logs`")
                db.execSQL("ALTER TABLE `health_sleep_stages_new` RENAME TO `health_sleep_stages`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_sleep_stages_sessionId` ON `health_sleep_stages` (`sessionId`)")

                // Fix health_weight_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS `health_weight_logs_new` (`id` TEXT NOT NULL, `weightKg` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `source` TEXT NOT NULL DEFAULT 'HealthConnect', PRIMARY KEY(`id`))")
                val cursorWeight = db.query("PRAGMA table_info(`health_weight_logs`)")
                val existingColumnsWeight = mutableListOf<String>()
                val nameIdxWeight = cursorWeight.getColumnIndex("name")
                if (nameIdxWeight != -1) {
                    while (cursorWeight.moveToNext()) { existingColumnsWeight.add(cursorWeight.getString(nameIdxWeight)) }
                }
                cursorWeight.close()
                val columnsToCopyWeight = listOf("id", "weightKg", "timestamp", "source").filter { it in existingColumnsWeight }
                if (columnsToCopyWeight.isNotEmpty()) {
                    val cols = columnsToCopyWeight.joinToString("`, `", "`", "`")
                    db.execSQL("INSERT INTO `health_weight_logs_new` ($cols) SELECT $cols FROM `health_weight_logs`")
                }
                db.execSQL("DROP TABLE IF EXISTS `health_weight_logs`")
                db.execSQL("ALTER TABLE `health_weight_logs_new` RENAME TO `health_weight_logs`")

                // Fix health_nutrition_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS `health_nutrition_logs_new` (`id` TEXT NOT NULL, `energyKcal` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `name` TEXT, `source` TEXT NOT NULL DEFAULT 'HealthConnect', PRIMARY KEY(`id`))")
                val cursorNutri = db.query("PRAGMA table_info(`health_nutrition_logs`)")
                val existingColumnsNutri = mutableListOf<String>()
                val nameIdxNutri = cursorNutri.getColumnIndex("name")
                if (nameIdxNutri != -1) {
                    while (cursorNutri.moveToNext()) { existingColumnsNutri.add(cursorNutri.getString(nameIdxNutri)) }
                }
                cursorNutri.close()
                val columnsToCopyNutri = listOf("id", "energyKcal", "timestamp", "name", "source").filter { it in existingColumnsNutri }
                if (columnsToCopyNutri.isNotEmpty()) {
                    val cols = columnsToCopyNutri.joinToString("`, `", "`", "`")
                    db.execSQL("INSERT INTO `health_nutrition_logs_new` ($cols) SELECT $cols FROM `health_nutrition_logs`")
                }
                db.execSQL("DROP TABLE IF EXISTS `health_nutrition_logs`")
                db.execSQL("ALTER TABLE `health_nutrition_logs_new` RENAME TO `health_nutrition_logs`")
            }
        }

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
