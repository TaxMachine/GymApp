package dev.taxmachine.gymapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.utils.CalculationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutSplitScreen(dao: GymDao) {
    var selectedSplit by remember { mutableStateOf<SplitEntity?>(null) }
    val splits by dao.getAllSplits().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddSplitDialog by remember { mutableStateOf(false) }

    if (selectedSplit == null) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddSplitDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Split")
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (splits.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No splits created. Press + to create one.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(splits, key = { it.id }) { split ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSplit = split },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = { 
                                        Text(
                                            split.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        ) 
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { scope.launch { dao.deleteSplit(split) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Split", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddSplitDialog) {
            var splitName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddSplitDialog = false },
                title = { Text("New Split") },
                text = {
                    OutlinedTextField(
                        value = splitName,
                        onValueChange = { splitName = it },
                        label = { Text("Split Name (e.g. Push)") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (splitName.isNotBlank()) {
                            scope.launch {
                                dao.insertSplit(SplitEntity(name = splitName))
                                showAddSplitDialog = false
                            }
                        }
                    }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showAddSplitDialog = false }) { Text("Cancel") } }
            )
        }
    } else {
        ExerciseListScreen(dao, selectedSplit!!) { selectedSplit = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(dao: GymDao, split: SplitEntity, onBack: () -> Unit) {
    val exercises by dao.getExercisesBySplit(split.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToShowGraph by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToLogWeight by remember { mutableStateOf<ExerciseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(split.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExerciseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (exercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No exercises in this split.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        val est1RM = remember(exercise.weight, exercise.reps) {
                            CalculationUtils.calculate1RM(exercise.weight, exercise.reps)
                        }
                        val volume = remember(exercise.weight, exercise.reps) {
                            exercise.weight * exercise.reps
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            )
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { 
                                    Text(
                                        exercise.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    ) 
                                },
                                supportingContent = {
                                    Column {
                                        Text("${exercise.weight}${exercise.weightUnit} x ${exercise.reps} reps", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "Est. 1RM: ${"%.1f".format(est1RM)}${exercise.weightUnit} | Vol: ${"%.0f".format(volume)}${exercise.weightUnit}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { exerciseToLogWeight = exercise }) {
                                            Icon(Icons.Default.History, contentDescription = "Log Progress", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { exerciseToShowGraph = exercise }) {
                                            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Show Graph", tint = MaterialTheme.colorScheme.secondary)
                                        }
                                        IconButton(onClick = { scope.launch { dao.deleteExercise(exercise) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        var name by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("kg") }
        var reps by remember { mutableStateOf("") }
        val units = listOf("kg", "lbs")

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Add Exercise") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        var unitExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = !unitExpanded },
                            modifier = Modifier.width(100.dp)
                        ) {
                            OutlinedTextField(
                                value = unit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            unit = u
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            val w = weight.toFloatOrNull() ?: 0f
                            val exerciseId = dao.insertExercise(
                                ExerciseEntity(
                                    splitId = split.id,
                                    name = name,
                                    weight = w,
                                    weightUnit = unit,
                                    reps = reps.toIntOrNull() ?: 0
                                )
                            )
                            dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseId, weight = w))
                            showAddExerciseDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancel") } }
        )
    }

    if (exerciseToLogWeight != null) {
        var newWeight by remember { mutableStateOf(exerciseToLogWeight!!.weight.toString()) }
        AlertDialog(
            onDismissRequest = { exerciseToLogWeight = null },
            title = { Text("Log New Weight") },
            text = {
                OutlinedTextField(
                    value = newWeight,
                    onValueChange = { newWeight = it },
                    label = { Text("New Weight (${exerciseToLogWeight!!.weightUnit})") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val w = newWeight.toFloatOrNull() ?: 0f
                    scope.launch {
                        dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseToLogWeight!!.id, weight = w))
                        dao.updateExercise(exerciseToLogWeight!!.copy(weight = w))
                        exerciseToLogWeight = null
                    }
                }) { Text("Log") }
            },
            dismissButton = { TextButton(onClick = { exerciseToLogWeight = null }) { Text("Cancel") } }
        )
    }

    if (exerciseToShowGraph != null) {
        val logs by dao.getWeightLogsForExercise(exerciseToShowGraph!!.id).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { exerciseToShowGraph = null },
            title = { Text("${exerciseToShowGraph!!.name} Progression") },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
                        if (logs.size < 2) {
                            Text("Need at least 2 logs to show progression.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            ProgressionGraph(logs, exerciseToShowGraph!!.weightUnit)
                        }
                    }
                    if (logs.isNotEmpty()) {
                        PerformanceMetricsSummary(logs, exerciseToShowGraph!!.weightUnit)
                    }
                }
            },
            confirmButton = { Button(onClick = { exerciseToShowGraph = null }) { Text("Close") } }
        )
    }
}

@Composable
fun PerformanceMetricsSummary(logs: List<WeightLogEntity>, unit: String) {
    val stats = remember(logs) {
        CalculationUtils.calculateWorkoutStats(logs)
    }

    if (stats == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Stats Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Starting Weight: ${stats.start}$unit", style = MaterialTheme.typography.bodySmall)
            Text("Personal Best: ${stats.personalBest}$unit", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Volume: ${"%.0f".format(stats.volume)}$unit", style = MaterialTheme.typography.bodySmall)
            Text("Current: ${stats.current}$unit", style = MaterialTheme.typography.bodySmall)
        }
        
        val progressColor = if (stats.difference >= 0) Color(0xFF4CAF50) else Color.Red
        Text(
            text = "Total Progress: ${if (stats.difference >= 0) "+" else ""}${"%.1f".format(stats.difference)}$unit (${"%.1f".format(stats.percentage)}%)",
            color = progressColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ProgressionGraph(logs: List<WeightLogEntity>, unit: String) {
    val weights = remember(logs) { logs.map { it.weight } }
    val minWeight = remember(weights) { (weights.minOrNull() ?: 0f) * 0.9f }
    val maxWeight = remember(weights) { (weights.maxOrNull() ?: 1f) * 1.1f }
    val weightRange = remember(minWeight, maxWeight) { if (maxWeight == minWeight) 1f else maxWeight - minWeight }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${maxWeight.toInt()}$unit", style = MaterialTheme.typography.labelSmall, color = labelColor)
            val timeRange = if (logs.size >= 2) {
                val duration = logs.last().timestamp - logs.first().timestamp
                val days = duration / (1000 * 60 * 60 * 24)
                "${days}d range"
            } else ""
            Text(timeRange, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.Bold)
        }
        
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            if (logs.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val spacing = width / (logs.size - 1)

            val points = logs.mapIndexed { index, log ->
                val x = index * spacing
                val y = height - ((log.weight - minWeight) / weightRange * height)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )
            
            points.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 6f,
                    center = point
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateFormat.format(Date(logs.first().timestamp)), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text("${minWeight.toInt()}$unit", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(dateFormat.format(Date(logs.last().timestamp)), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}
