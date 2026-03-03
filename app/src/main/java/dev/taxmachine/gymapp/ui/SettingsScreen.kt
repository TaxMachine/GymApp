package dev.taxmachine.gymapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    dao: GymDao,
    db: GymDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatabaseViewer by remember { mutableStateOf(false) }

    if (showDatabaseViewer) {
        DatabaseViewer(dao = dao, onBack = { showDatabaseViewer = false })
        return
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                exportDataToJson(context, it, dao)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                importDataFromJson(context, it, dao, db)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
            ThemeOption("System Default", AppTheme.SYSTEM, currentTheme, onThemeChange)
            ThemeOption("Light", AppTheme.LIGHT, currentTheme, onThemeChange)
            ThemeOption("Dark", AppTheme.DARK, currentTheme, onThemeChange)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Data Management", icon = Icons.Default.Storage) {
            OutlinedButton(
                onClick = { exportLauncher.launch("gymapp_backup.json") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Database (JSON)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Database (JSON)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDatabaseViewer = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Database Viewer")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Data")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Data?") },
            text = { Text("This action cannot be undone. All your progress, supplements, and badges will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.clearAllTables()
                            }
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        content()
    }
}

@Composable
fun ThemeOption(label: String, theme: AppTheme, current: AppTheme, onClick: (AppTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == theme, onClick = { onClick(theme) })
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private suspend fun exportDataToJson(context: Context, uri: Uri, dao: GymDao) {
    withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            
            // Badges
            val badges = dao.getAllBadges().first()
            val badgesArray = JSONArray()
            badges.forEach {
                badgesArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("tagData", it.tagData)
                })
            }
            root.put("badges", badgesArray)

            // Splits
            val splits = dao.getAllSplits().first()
            val splitsArray = JSONArray()
            splits.forEach { split ->
                val splitObj = JSONObject().apply {
                    put("name", split.name)
                    
                    val exercises = dao.getExercisesBySplit(split.id).first()
                    val exercisesArray = JSONArray()
                    exercises.forEach { ex ->
                        val exObj = JSONObject().apply {
                            put("name", ex.name)
                            put("weight", ex.weight)
                            put("weightUnit", ex.weightUnit)
                            put("reps", ex.reps)
                            
                            val logs = dao.getWeightLogsForExercise(ex.id).first()
                            val logsArray = JSONArray()
                            logs.forEach { log ->
                                logsArray.put(JSONObject().apply {
                                    put("weight", log.weight)
                                    put("timestamp", log.timestamp)
                                })
                            }
                            put("logs", logsArray)
                        }
                        exercisesArray.put(exObj)
                    }
                    put("exercises", exercisesArray)
                }
                splitsArray.put(splitObj)
            }
            root.put("splits", splitsArray)

            // Supplements
            val supps = dao.getAllSupplements().first()
            val suppsArray = JSONArray()
            supps.forEach { s ->
                val sObj = JSONObject().apply {
                    put("name", s.name)
                    put("dosage", s.dosage)
                    put("unit", s.unit.name)
                    put("timing", s.timing.name)
                    put("frequency", s.frequency.name)
                    put("isInjectable", s.isInjectable)
                    
                    val logs = dao.getLogsForSupplement(s.uid).first()
                    val logsArray = JSONArray()
                    logs.forEach { log ->
                        logsArray.put(JSONObject().apply {
                            put("dosage", log.dosage)
                            put("timestamp", log.timestamp)
                        })
                    }
                    put("logs", logsArray)
                }
                suppsArray.put(sObj)
            }
            root.put("supplements", suppsArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(root.toString(4))
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun importDataFromJson(context: Context, uri: Uri, dao: GymDao, db: GymDatabase) {
    withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                val root = JSONObject(content)
                
                // Badges
                val badges = root.optJSONArray("badges")
                for (i in 0 until (badges?.length() ?: 0)) {
                    val b = badges!!.getJSONObject(i)
                    dao.insertBadge(BadgeEntity(b.getString("id"), b.getString("name"), b.getString("tagData")))
                }

                // Splits
                val splits = root.optJSONArray("splits")
                for (i in 0 until (splits?.length() ?: 0)) {
                    val s = splits!!.getJSONObject(i)
                    val splitId = dao.insertSplit(SplitEntity(name = s.getString("name")))
                    
                    val exercises = s.optJSONArray("exercises")
                    for (j in 0 until (exercises?.length() ?: 0)) {
                        val ex = exercises!!.getJSONObject(j)
                        val exerciseId = dao.insertExercise(ExerciseEntity(
                            splitId = splitId,
                            name = ex.getString("name"),
                            weight = ex.getDouble("weight").toFloat(),
                            weightUnit = ex.getString("weightUnit"),
                            reps = ex.getInt("reps")
                        ))
                        
                        val logs = ex.optJSONArray("logs")
                        for (k in 0 until (logs?.length() ?: 0)) {
                            val log = logs!!.getJSONObject(k)
                            dao.insertWeightLog(WeightLogEntity(
                                exerciseId = exerciseId,
                                weight = log.getDouble("weight").toFloat(),
                                timestamp = log.getLong("timestamp")
                            ))
                        }
                    }
                }

                // Supplements
                val supps = root.optJSONArray("supplements")
                for (i in 0 until (supps?.length() ?: 0)) {
                    val s = supps!!.getJSONObject(i)
                    val suppId = dao.insertSupplement(SupplementEntity(
                        name = s.getString("name"),
                        dosage = s.getString("dosage"),
                        unit = DosingUnit.valueOf(s.getString("unit")),
                        timing = AdministrationTiming.valueOf(s.getString("timing")),
                        frequency = AdministrationFrequency.valueOf(s.getString("frequency")),
                        isInjectable = s.getBoolean("isInjectable")
                    ))
                    
                    val logs = s.optJSONArray("logs")
                    for (j in 0 until (logs?.length() ?: 0)) {
                        val log = logs!!.getJSONObject(j)
                        dao.insertSupplementLog(SupplementLogEntity(
                            supplementUid = suppId.toInt(),
                            dosage = log.getDouble("dosage").toFloat(),
                            timestamp = log.getLong("timestamp")
                        ))
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
