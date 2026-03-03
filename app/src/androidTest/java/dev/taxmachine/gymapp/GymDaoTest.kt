package dev.taxmachine.gymapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.taxmachine.gymapp.db.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class GymDaoTest {
    private lateinit var dao: GymDao
    private lateinit var db: GymDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java).build()
        dao = db.gymDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeBadgeAndReadInList() = runBlocking {
        val badge = BadgeEntity("1234", "My Tag", "NFC Tech")
        dao.insertBadge(badge)
        val allBadges = dao.getAllBadges().first()
        assertEquals(allBadges[0], badge)
    }

    @Test
    @Throws(Exception::class)
    fun writeSplitAndExerciseAndRead() = runBlocking {
        dao.insertSplit(SplitEntity(id = 1, name = "Push"))
        val exercise = ExerciseEntity(splitId = 1, name = "Bench Press", weight = 100f, weightUnit = "kg", reps = 10)
        dao.insertExercise(exercise)
        val splitExercises = dao.getExercisesBySplit(1).first()
        assertEquals(splitExercises[0].name, exercise.name)
        assertEquals(splitExercises[0].weight, 100f)
    }

    @Test
    @Throws(Exception::class)
    fun weightLogsProgression() = runBlocking {
        dao.insertSplit(SplitEntity(id = 1, name = "Push"))
        val exerciseId = dao.insertExercise(ExerciseEntity(splitId = 1, name = "Bench", weight = 100f, weightUnit = "kg", reps = 5))
        
        dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseId, weight = 100f))
        dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseId, weight = 105f))
        
        val logs = dao.getWeightLogsForExercise(exerciseId).first()
        assertEquals(2, logs.size)
        assertEquals(105f, logs[1].weight)
    }

    @Test
    @Throws(Exception::class)
    fun writeSupplementAndReadInList() = runBlocking {
        val supplement = SupplementEntity(
            name = "Creatine",
            dosage = "5",
            unit = DosingUnit.G,
            timing = AdministrationTiming.MORNING_FASTED,
            frequency = AdministrationFrequency.EVERY_DAY,
            isInjectable = false
        )
        dao.insertSupplement(supplement)
        val allSupps = dao.getAllSupplements().first()
        assertEquals(allSupps[0].name, "Creatine")
        assertEquals(allSupps[0].unit, DosingUnit.G)
    }
}
