package dev.taxmachine.gymapp.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.taxmachine.gymapp.db.GymDao
import dev.taxmachine.gymapp.db.HealthNutritionLogEntity
import dev.taxmachine.gymapp.db.HealthSleepLogEntity
import dev.taxmachine.gymapp.db.HealthSleepStageEntity
import dev.taxmachine.gymapp.db.HealthWeightLogEntity
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.min

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        return grantedPermissions.containsAll(permissions)
    }

    suspend fun syncHealthData(dao: GymDao) {
        if (!hasAllPermissions()) {
            Log.w("HealthConnect", "Missing permissions for sync")
            return
        }

        val now = Instant.now()
        // Increase range to 60 days to ensure we catch older entries
        val startTime = ZonedDateTime.now().minusDays(60).toInstant()

        try {
            // Sync Sleep Sessions
            val sleepRecords = readSleepSessions(startTime, now)
            Log.d("HealthConnect", "Read ${sleepRecords.size} sleep records")
            dao.insertHealthSleepLogs(sleepRecords.map {
                val duration = Duration.between(it.startTime, it.endTime)
                val minutes = duration.toMinutes()
                val derivedScore = min(100, ((minutes.toFloat() / 480f) * 100).toInt())

                HealthSleepLogEntity(
                    id = it.metadata.id,
                    startTime = it.startTime.toEpochMilli(),
                    endTime = it.endTime.toEpochMilli(),
                    durationMinutes = minutes,
                    sleepScore = derivedScore,
                    source = it.metadata.dataOrigin.packageName
                )
            })

            // Sync Sleep Stages
            val stageEntities = mutableListOf<HealthSleepStageEntity>()
            sleepRecords.forEach { session ->
                session.stages.forEach { stage ->
                    stageEntities.add(HealthSleepStageEntity(
                        sessionId = session.metadata.id,
                        startTime = stage.startTime.toEpochMilli(),
                        endTime = stage.endTime.toEpochMilli(),
                        stage = stage.stage
                    ))
                }
            }
            if (stageEntities.isNotEmpty()) {
                dao.insertHealthSleepStages(stageEntities)
            }

            // Sync Weight
            val weightRecords = readWeightRecords(startTime, now)
            Log.d("HealthConnect", "Read ${weightRecords.size} weight records")
            if (weightRecords.isNotEmpty()) {
                dao.insertHealthWeightLogs(weightRecords.map {
                    HealthWeightLogEntity(
                        id = it.metadata.id,
                        weightKg = it.weight.inKilograms.toFloat(),
                        timestamp = it.time.toEpochMilli(),
                        source = it.metadata.dataOrigin.packageName
                    )
                })
            }

            // Sync Nutrition
            val nutritionRecords = readNutritionRecords(startTime, now)
            Log.d("HealthConnect", "Read ${nutritionRecords.size} nutrition records")
            if (nutritionRecords.isNotEmpty()) {
                dao.insertHealthNutritionLogs(nutritionRecords.map {
                    HealthNutritionLogEntity(
                        id = it.metadata.id,
                        energyKcal = it.energy?.inKilocalories?.toFloat() ?: 0f,
                        timestamp = it.startTime.toEpochMilli(),
                        name = it.name,
                        source = it.metadata.dataOrigin.packageName
                    )
                })
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error during sync: ${e.message}", e)
        }
    }

    private suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records
    }

    private suspend fun readWeightRecords(start: Instant, end: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records
    }

    private suspend fun readNutritionRecords(start: Instant, end: Instant): List<NutritionRecord> {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records
    }

    fun isHealthConnectAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }
}
