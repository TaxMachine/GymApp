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
        SupplementLogEntity::class,
        CustomThemeColorsEntity::class
    ],
    version = 8
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_theme_colors` (
                        `isDark` INTEGER NOT NULL, 
                        `primary` INTEGER NOT NULL, 
                        `onPrimary` INTEGER NOT NULL, 
                        `secondary` INTEGER NOT NULL, 
                        `onSecondary` INTEGER NOT NULL, 
                        `tertiary` INTEGER NOT NULL, 
                        `onTertiary` INTEGER NOT NULL, 
                        `background` INTEGER NOT NULL, 
                        `onBackground` INTEGER NOT NULL, 
                        `surface` INTEGER NOT NULL, 
                        `onSurface` INTEGER NOT NULL, 
                        `error` INTEGER NOT NULL, 
                        `onError` INTEGER NOT NULL, 
                        PRIMARY KEY(`isDark`)
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
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
