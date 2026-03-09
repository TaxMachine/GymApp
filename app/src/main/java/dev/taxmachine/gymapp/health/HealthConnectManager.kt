package dev.taxmachine.gymapp.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
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
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            grantedPermissions.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncHealthData(dao: GymDao) {
        if (!hasAllPermissions()) {
            Log.w("HealthConnect", "Missing permissions for sync")
            return
        }

        val now = Instant.now()
        val startTime = ZonedDateTime.now().minusDays(90).toInstant() // Increased to 90 days

        // Sync Sleep
        try {
            val sleepRecords = readSleepSessions(startTime, now)
            val sleepLogs = mutableListOf<HealthSleepLogEntity>()
            val stageEntities = mutableListOf<HealthSleepStageEntity>()

            for (it in sleepRecords) {
                val duration = Duration.between(it.startTime, it.endTime)
                val minutes = duration.toMinutes()
                val derivedScore = min(100, ((minutes.toFloat() / 480f) * 100).toInt())

                var avgHr = 0
                try {
                    val hrRequest = AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG),
                        timeRangeFilter = TimeRangeFilter.between(it.startTime, it.endTime)
                    )
                    val hrAggregate = healthConnectClient.aggregate(hrRequest)
                    avgHr = hrAggregate[HeartRateRecord.BPM_AVG]?.toInt() ?: 0
                    
                    if (avgHr == 0) {
                        val hrRecords = readHeartRateRecords(it.startTime, it.endTime)
                        if (hrRecords.isNotEmpty()) {
                            val allSamples = hrRecords.flatMap { record -> record.samples }
                            if (allSamples.isNotEmpty()) {
                                avgHr = allSamples.map { sample -> sample.beatsPerMinute }.average().toInt()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HealthConnect", "Error fetching heart rate: ${e.message}")
                }

                sleepLogs.add(HealthSleepLogEntity(
                    id = it.metadata.id,
                    startTime = it.startTime.toEpochMilli(),
                    endTime = it.endTime.toEpochMilli(),
                    durationMinutes = minutes,
                    sleepScore = derivedScore,
                    source = it.metadata.dataOrigin.packageName,
                    avgHeartRate = avgHr
                ))

                it.stages.forEach { stage ->
                    stageEntities.add(HealthSleepStageEntity(
                        sessionId = it.metadata.id,
                        startTime = stage.startTime.toEpochMilli(),
                        endTime = stage.endTime.toEpochMilli(),
                        stage = stage.stage
                    ))
                }
            }
            dao.insertHealthSleepLogs(sleepLogs)
            if (stageEntities.isNotEmpty()) dao.insertHealthSleepStages(stageEntities)
            Log.d("HealthConnect", "Synced ${sleepLogs.size} sleep logs")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Sleep sync failed: ${e.message}")
        }

        // Sync Weight
        try {
            val weightRecords = readWeightRecords(startTime, now)
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
            Log.d("HealthConnect", "Synced ${weightRecords.size} weight logs")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Weight sync failed: ${e.message}")
        }

        // Sync Nutrition
        try {
            val nutritionRecords = readNutritionRecords(startTime, now)
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
            Log.d("HealthConnect", "Synced ${nutritionRecords.size} nutrition logs")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Nutrition sync failed: ${e.message}")
        }
    }

    private suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun readHeartRateRecords(start: Instant, end: Instant): List<HeartRateRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun readWeightRecords(start: Instant, end: Instant): List<WeightRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun readNutritionRecords(start: Instant, end: Instant): List<NutritionRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Exception) { emptyList() }
    }

    fun isHealthConnectAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }
}
