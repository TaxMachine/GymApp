package dev.taxmachine.gymapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.theme.AppTheme
import dev.taxmachine.gymapp.ui.theme.toColorScheme
import dev.taxmachine.gymapp.ui.theme.toEntity
import dev.taxmachine.gymapp.ui.dialogs.ColorPickerDialog
import dev.taxmachine.gymapp.utils.DataUtils
import dev.taxmachine.gymapp.receiver.SupplementReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    dao: GymDao,
    db: GymDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatabaseViewer by remember { mutableStateOf(false) }

    val customLightColors by dao.getCustomThemeColors(false).collectAsState(initial = null)
    val customDarkColors by dao.getCustomThemeColors(true).collectAsState(initial = null)

    var showAdvanced by remember { mutableStateOf(false) }
    var editingDarkTheme by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<Triple<String, Color, (Color) -> Unit>?>(null) }

    // Notification Settings
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var morningOffset by remember { mutableStateOf(prefs.getInt("morning_offset_minutes", 10)) }
    var nightHour by remember { mutableStateOf(prefs.getInt("night_hour", 22)) }

    // Weight Unit Settings
    var weightUnit by remember { mutableStateOf(prefs.getString("weight_unit", "kg") ?: "kg") }

    if (showDatabaseViewer) {
        DatabaseViewer(dao = dao, onBack = { showDatabaseViewer = false })
        return
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                DataUtils.exportDataToJson(context, it, dao)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                DataUtils.importDataFromJson(context, it, dao)
                // Refresh local state after import
                notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
                morningOffset = prefs.getInt("morning_offset_minutes", 10)
                nightHour = prefs.getInt("night_hour", 22)
                weightUnit = prefs.getString("weight_unit", "kg") ?: "kg"
                onThemeChange(AppTheme.valueOf(prefs.getString("theme", AppTheme.SYSTEM.name)!!))
                onDynamicColorChange(prefs.getBoolean("dynamic_color", true))
                SupplementReminderReceiver.scheduleReminder(context)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                Text("Dynamic Color (Android 12+)", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced Theme Settings" else "Show Advanced Theme Settings")
            }

            if (showAdvanced) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit Mode: ")
                    FilterChip(
                        selected = !editingDarkTheme,
                        onClick = { editingDarkTheme = false },
                        label = { Text("Light") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = editingDarkTheme,
                        onClick = { editingDarkTheme = true },
                        label = { Text("Dark") }
                    )
                }

                val currentScheme = if (editingDarkTheme) {
                    customDarkColors?.toColorScheme() ?: darkColorScheme()
                } else {
                    customLightColors?.toColorScheme() ?: lightColorScheme()
                }

                val updateColor: (String, Color) -> Unit = { name, newColor ->
                    scope.launch {
                        val newScheme = when (name) {
                            "Primary" -> currentScheme.copy(primary = newColor)
                            "On Primary" -> currentScheme.copy(onPrimary = newColor)
                            "Secondary" -> currentScheme.copy(secondary = newColor)
                            "On Secondary" -> currentScheme.copy(onSecondary = newColor)
                            "Tertiary" -> currentScheme.copy(tertiary = newColor)
                            "On Tertiary" -> currentScheme.copy(onTertiary = newColor)
                            "Background" -> currentScheme.copy(background = newColor)
                            "On Background" -> currentScheme.copy(onBackground = newColor)
                            "Surface" -> currentScheme.copy(surface = newColor)
                            "On Surface" -> currentScheme.copy(onSurface = newColor)
                            "Error" -> currentScheme.copy(error = newColor)
                            "On Error" -> currentScheme.copy(onError = newColor)
                            else -> currentScheme
                        }
                        dao.insertCustomThemeColors(newScheme.toEntity(editingDarkTheme))
                    }
                }

                ColorGrid(currentScheme) { name, color ->
                    colorToEdit = Triple(name, color) { newColor ->
                        updateColor(name, newColor)
                        colorToEdit = null
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            dao.deleteAllCustomThemeColors()
                            Toast.makeText(context, "Themes reset to default", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Reset All Themes to Default")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Units", icon = Icons.Default.Straighten) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Preferred Weight Unit", modifier = Modifier.weight(1f))
                val units = listOf("kg", "lbs")
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(weightUnit.uppercase())
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.uppercase()) },
                                onClick = {
                                    weightUnit = unit
                                    prefs.edit().putString("weight_unit", unit).apply()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Text(
                "Weights will be converted and displayed in this unit throughout the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Notifications", icon = Icons.Default.Notifications) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        prefs.edit().putBoolean("notifications_enabled", it).apply()
                        SupplementReminderReceiver.scheduleReminder(context)
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Enable Supplement Reminders")
            }

            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Morning Notification Delay", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = morningOffset.toFloat(),
                        onValueChange = { morningOffset = it.toInt() },
                        onValueChangeFinished = {
                            prefs.edit().putInt("morning_offset_minutes", morningOffset).apply()
                            SupplementReminderReceiver.scheduleReminder(context)
                        },
                        valueRange = 0f..60f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${morningOffset}m", modifier = Modifier.width(40.dp))
                }
                Text("Minutes after your morning alarm clock.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Night Notification Time", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = nightHour.toFloat(),
                        onValueChange = { nightHour = it.toInt() },
                        onValueChangeFinished = {
                            prefs.edit().putInt("night_hour", nightHour).apply()
                            SupplementReminderReceiver.scheduleReminder(context)
                        },
                        valueRange = 18f..23f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${nightHour}:00", modifier = Modifier.width(40.dp))
                }
            }
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
                Text("Export Database & Settings")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Database & Settings")
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

    colorToEdit?.let { (name, color, onSelected) ->
        ColorPickerDialog(
            initialColor = color,
            onDismiss = { colorToEdit = null },
            onColorSelected = onSelected
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorGrid(scheme: ColorScheme, onColorClick: (String, Color) -> Unit) {
    val colors = listOf(
        "Primary" to scheme.primary,
        "On Primary" to scheme.onPrimary,
        "Secondary" to scheme.secondary,
        "On Secondary" to scheme.onSecondary,
        "Tertiary" to scheme.tertiary,
        "On Tertiary" to scheme.onTertiary,
        "Background" to scheme.background,
        "On Background" to scheme.onBackground,
        "Surface" to scheme.surface,
        "On Surface" to scheme.onSurface,
        "Error" to scheme.error,
        "On Error" to scheme.onError
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (name, color) ->
            ColorCard(name, color) { onColorClick(name, color) }
        }
    }
}

@Composable
fun ColorCard(name: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
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
