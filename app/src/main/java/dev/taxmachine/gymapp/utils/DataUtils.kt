package dev.taxmachine.gymapp.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.taxmachine.gymapp.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object DataUtils {

    fun captureViewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
    }

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String) {
        withContext(Dispatchers.IO) {
            val name = "$fileName.png"
            val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GymApp")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/GymApp"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdir()
                val image = File(imagesDir, name)
                FileOutputStream(image)
            }

            try {
                outputStream?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/$fileName.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(context.cacheDir, "images")
            val newFile = File(imagePath, "$fileName.png")
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Graph"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun exportDataToJson(context: Context, uri: Uri, dao: GymDao) {
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
                                put("isBodyweight", ex.isBodyweight)
                                
                                val logs = dao.getWeightLogsForExercise(ex.id).first()
                                val logsArray = JSONArray()
                                logs.forEach { log ->
                                    logsArray.put(JSONObject().apply {
                                        put("weight", log.weight)
                                        put("reps", log.reps)
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
                        put("isActive", s.isActive)
                        
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

                // Custom Themes
                val themes = dao.getAllCustomThemeColors().first()
                val themesArray = JSONArray()
                themes.forEach { t ->
                    themesArray.put(JSONObject().apply {
                        put("isDark", t.isDark)
                        put("primary", t.primary)
                        put("onPrimary", t.onPrimary)
                        put("secondary", t.secondary)
                        put("onSecondary", t.onSecondary)
                        put("tertiary", t.tertiary)
                        put("onTertiary", t.onTertiary)
                        put("background", t.background)
                        put("onBackground", t.onBackground)
                        put("surface", t.surface)
                        put("onSurface", t.onSurface)
                        put("error", t.error)
                        put("onError", t.onError)
                    })
                }
                root.put("customThemes", themesArray)

                // Preferences
                val prefs = context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE)
                val prefsObj = JSONObject().apply {
                    put("notifications_enabled", prefs.getBoolean("notifications_enabled", true))
                    put("morning_offset_minutes", prefs.getInt("morning_offset_minutes", 10))
                    put("night_hour", prefs.getInt("night_hour", 22))
                    put("theme", prefs.getString("theme", "SYSTEM"))
                    put("dynamic_color", prefs.getBoolean("dynamic_color", true))
                }
                root.put("preferences", prefsObj)

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

    suspend fun importDataFromJson(context: Context, uri: Uri, dao: GymDao) {
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
                                reps = ex.getInt("reps"),
                                isBodyweight = ex.optBoolean("isBodyweight", false)
                            ))
                            
                            val logs = ex.optJSONArray("logs")
                            for (k in 0 until (logs?.length() ?: 0)) {
                                val log = logs!!.getJSONObject(k)
                                dao.insertWeightLog(WeightLogEntity(
                                    exerciseId = exerciseId,
                                    weight = log.getDouble("weight").toFloat(),
                                    reps = log.optInt("reps", 0),
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
                            isInjectable = s.getBoolean("isInjectable"),
                            isActive = s.optBoolean("isActive", true)
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

                    // Custom Themes
                    val themes = root.optJSONArray("customThemes")
                    for (i in 0 until (themes?.length() ?: 0)) {
                        val t = themes!!.getJSONObject(i)
                        dao.insertCustomThemeColors(CustomThemeColorsEntity(
                            isDark = t.getBoolean("isDark"),
                            primary = t.getLong("primary"),
                            onPrimary = t.getLong("onPrimary"),
                            secondary = t.getLong("secondary"),
                            onSecondary = t.getLong("onSecondary"),
                            tertiary = t.getLong("tertiary"),
                            onTertiary = t.getLong("onTertiary"),
                            background = t.getLong("background"),
                            onBackground = t.getLong("onBackground"),
                            surface = t.getLong("surface"),
                            onSurface = t.getLong("onSurface"),
                            error = t.getLong("error"),
                            onError = t.getLong("onError")
                        ))
                    }

                    // Preferences
                    val prefsObj = root.optJSONObject("preferences")
                    if (prefsObj != null) {
                        val prefs = context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putBoolean("notifications_enabled", prefsObj.optBoolean("notifications_enabled", true))
                            putInt("morning_offset_minutes", prefsObj.optInt("morning_offset_minutes", 10))
                            putInt("night_hour", prefsObj.optInt("night_hour", 22))
                            putString("theme", prefsObj.optString("theme", "SYSTEM"))
                            putBoolean("dynamic_color", prefsObj.optBoolean("dynamic_color", true))
                        }.apply()
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
}
