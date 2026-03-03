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
        SplitEntity::class,
        ExerciseEntity::class,
        WeightLogEntity::class,
        SupplementEntity::class,
        SupplementLogEntity::class
    ],
    version = 6
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop and recreate workout tables to "reset" them while keeping others
                db.execSQL("DROP TABLE IF EXISTS `weight_logs`")
                db.execSQL("DROP TABLE IF EXISTS `exercises`")
                db.execSQL("DROP TABLE IF EXISTS `splits`")

                db.execSQL("CREATE TABLE IF NOT EXISTS `splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercises` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `splitId` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `weight` REAL NOT NULL, 
                        `weightUnit` TEXT NOT NULL, 
                        `reps` INTEGER NOT NULL, 
                        FOREIGN KEY(`splitId`) REFERENCES `splits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `weight_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `exerciseId` INTEGER NOT NULL, 
                        `weight` REAL NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `supplement_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `supplementUid` INTEGER NOT NULL, 
                        `dosage` REAL NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        FOREIGN KEY(`supplementUid`) REFERENCES `supplements`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
            }
        }

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
