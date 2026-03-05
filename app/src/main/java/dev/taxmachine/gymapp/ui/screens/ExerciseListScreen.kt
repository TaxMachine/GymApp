package dev.taxmachine.gymapp.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.dialogs.AddExerciseDialog
import dev.taxmachine.gymapp.ui.dialogs.LogWeightDialog
import dev.taxmachine.gymapp.utils.CalculationUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(dao: GymDao, split: SplitEntity, onShowGraph: (ExerciseEntity) -> Unit, onBack: () -> Unit) {
    val exercises by remember(dao, split.id) { dao.getExercisesBySplit(split.id) }.collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToLogProgress by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToOverride by remember { mutableStateOf<ExerciseEntity?>(null) }

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
                val onLogProgressSetter = remember { { ex: ExerciseEntity -> exerciseToLogProgress = ex } }
                val onOverrideSetter = remember { { ex: ExerciseEntity -> exerciseToOverride = ex } }
                val onDelete = remember(dao) { { ex: ExerciseEntity -> scope.launch { dao.deleteExercise(ex) }; Unit } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = exercises,
                        key = { it.id },
                        contentType = { "exercise" }
                    ) { exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            onLogProgress = onLogProgressSetter,
                            onOverride = onOverrideSetter,
                            onShowGraph = onShowGraph,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onSave = { name, weight, unit, reps, isBodyweight ->
                scope.launch {
                    val exerciseId = dao.insertExercise(
                        ExerciseEntity(
                            splitId = split.id,
                            name = name,
                            weight = weight,
                            weightUnit = unit,
                            reps = reps,
                            isBodyweight = isBodyweight
                        )
                    )
                    dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseId, weight = weight, reps = reps))
                    showAddExerciseDialog = false
                }
            }
        )
    }

    if (exerciseToLogProgress != null) {
        val currentEx = exerciseToLogProgress!!
        LogWeightDialog(
            exercise = currentEx,
            onDismiss = { exerciseToLogProgress = null },
            onLog = { weight: Float, reps: Int ->
                scope.launch {
                    dao.insertWeightLog(WeightLogEntity(exerciseId = currentEx.id, weight = weight, reps = reps))
                    dao.updateExercise(currentEx.copy(weight = weight, reps = reps))
                    exerciseToLogProgress = null
                }
            }
        )
    }

    if (exerciseToOverride != null) {
        val ex = exerciseToOverride!!
        var newWeight by remember(ex) { mutableStateOf(ex.weight.toString()) }
        var newReps by remember(ex) { mutableStateOf(ex.reps.toString()) }
        var selectedUnit by remember(ex) { mutableStateOf(ex.weightUnit) }
        val units = listOf("kg", "lbs")

        AlertDialog(
            onDismissRequest = { exerciseToOverride = null },
            title = { Text("Override ${ex.name}") },
            text = {
                Column {
                    if (!ex.isBodyweight) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newWeight,
                                onValueChange = { newWeight = it },
                                label = { Text("Weight") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            var unitExpanded by remember { mutableStateOf(false) }
                            dev.taxmachine.gymapp.ui.components.AppDropdownMenu(
                                label = "Unit",
                                options = units,
                                selectedOption = selectedUnit,
                                onOptionSelected = { selectedUnit = it },
                                optionLabel = { it },
                                modifier = Modifier.width(100.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = newReps,
                        onValueChange = { newReps = it },
                        label = { Text("Reps") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    var w = newWeight.toFloatOrNull() ?: 0f
                    if (!ex.isBodyweight && selectedUnit != ex.weightUnit) {
                        w = CalculationUtils.convertWeight(w, selectedUnit, ex.weightUnit)
                    }
                    val r = newReps.toIntOrNull() ?: 0
                    scope.launch {
                        dao.updateExercise(ex.copy(weight = w, reps = r))
                        exerciseToOverride = null
                    }
                }) { Text("Override") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToOverride = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: ExerciseEntity,
    onLogProgress: (ExerciseEntity) -> Unit,
    onOverride: (ExerciseEntity) -> Unit,
    onShowGraph: (ExerciseEntity) -> Unit,
    onDelete: (ExerciseEntity) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE) }
    val displayUnit = remember { prefs.getString("weight_unit", "kg") ?: "kg" }

    val displayWeight = remember(exercise.weight, exercise.weightUnit, displayUnit) {
        CalculationUtils.convertWeight(exercise.weight, exercise.weightUnit, displayUnit)
    }

    val est1RM = remember(exercise.weight, exercise.reps, exercise.isBodyweight) {
        if (exercise.isBodyweight) 0f else CalculationUtils.calculate1RM(exercise.weight, exercise.reps)
    }
    val display1RM = remember(est1RM, exercise.weightUnit, displayUnit) {
        CalculationUtils.convertWeight(est1RM, exercise.weightUnit, displayUnit)
    }

    val volume = remember(exercise.weight, exercise.reps, exercise.isBodyweight) {
        if (exercise.isBodyweight) 0f else exercise.weight * exercise.reps
    }
    val displayVolume = remember(volume, exercise.weightUnit, displayUnit) {
        CalculationUtils.convertWeight(volume, exercise.weightUnit, displayUnit)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onShowGraph(exercise) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            headlineContent = { 
                Text(
                    exercise.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            supportingContent = {
                Column {
                    if (exercise.isBodyweight) {
                        Text("Bodyweight x ${exercise.reps} reps", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            "${displayWeight.roundToInt()}$displayUnit x ${exercise.reps} reps",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Est. 1RM: ${display1RM.roundToInt()}$displayUnit | Vol: ${displayVolume.roundToInt()}$displayUnit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            trailingContent = {
                dev.taxmachine.gymapp.ui.components.ActionButtonColumn {
                    Row {
                        IconButton(onClick = { onLogProgress(exercise) }) {
                            Icon(Icons.Default.History, contentDescription = "Log Progress", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onOverride(exercise) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Override Weight", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    IconButton(onClick = { onDelete(exercise) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }
}
