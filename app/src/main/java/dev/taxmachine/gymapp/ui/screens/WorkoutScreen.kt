package dev.taxmachine.gymapp.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.dialogs.AddExerciseDialog
import dev.taxmachine.gymapp.ui.dialogs.AddSplitDialog
import dev.taxmachine.gymapp.ui.dialogs.ExerciseProgressionDialog
import dev.taxmachine.gymapp.ui.dialogs.LogWeightDialog
import dev.taxmachine.gymapp.utils.CalculationUtils
import kotlinx.coroutines.launch

@Composable
fun WorkoutSplitScreen(dao: GymDao) {
    var selectedSplit by remember { mutableStateOf<SplitEntity?>(null) }
    
    val splits by remember(dao) { dao.getAllSplits() }.collectAsState(initial = emptyList())
    
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
                    val onSplitClick = remember { { split: SplitEntity -> selectedSplit = split } }
                    val onDeleteSplit = remember(dao) { { split: SplitEntity -> scope.launch { dao.deleteSplit(split) }; Unit } }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = splits,
                            key = { it.id },
                            contentType = { "split" }
                        ) { split ->
                            SplitItem(
                                split = split,
                                onClick = onSplitClick,
                                onDelete = onDeleteSplit
                            )
                        }
                    }
                }
            }
        }

        if (showAddSplitDialog) {
            AddSplitDialog(
                onDismiss = { showAddSplitDialog = false },
                onConfirm = { splitName ->
                    scope.launch {
                        dao.insertSplit(SplitEntity(name = splitName))
                        showAddSplitDialog = false
                    }
                }
            )
        }
    } else {
        ExerciseListScreen(dao, selectedSplit!!) { selectedSplit = null }
    }
}

@Composable
fun SplitItem(
    split: SplitEntity,
    onClick: (SplitEntity) -> Unit,
    onDelete: (SplitEntity) -> Unit
) {
    Card(
        onClick = { onClick(split) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
                IconButton(onClick = { onDelete(split) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Split", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(dao: GymDao, split: SplitEntity, onBack: () -> Unit) {
    val exercises by remember(dao, split.id) { dao.getExercisesBySplit(split.id) }.collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToShowGraph by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToLogWeight by remember { mutableStateOf<ExerciseEntity?>(null) }
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
                val onLogWeight = remember { { ex: ExerciseEntity -> exerciseToLogWeight = ex } }
                val onOverride = remember { { ex: ExerciseEntity -> exerciseToOverride = ex } }
                val onShowGraph = remember { { ex: ExerciseEntity -> exerciseToShowGraph = ex } }
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
                            onLogWeight = onLogWeight,
                            onOverride = onOverride,
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
            onSave = { name, weight, unit, reps ->
                scope.launch {
                    val exerciseId = dao.insertExercise(
                        ExerciseEntity(
                            splitId = split.id,
                            name = name,
                            weight = weight,
                            weightUnit = unit,
                            reps = reps
                        )
                    )
                    dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseId, weight = weight))
                    showAddExerciseDialog = false
                }
            }
        )
    }

    if (exerciseToLogWeight != null) {
        LogWeightDialog(
            exercise = exerciseToLogWeight!!,
            onDismiss = { exerciseToLogWeight = null },
            onLog = { weight ->
                scope.launch {
                    dao.insertWeightLog(WeightLogEntity(exerciseId = exerciseToLogWeight!!.id, weight = weight))
                    dao.updateExercise(exerciseToLogWeight!!.copy(weight = weight))
                    exerciseToLogWeight = null
                }
            }
        )
    }

    if (exerciseToOverride != null) {
        val ex = exerciseToOverride!!
        var newWeight by remember(ex) { mutableStateOf(ex.weight.toString()) }
        var newReps by remember(ex) { mutableStateOf(ex.reps.toString()) }
        AlertDialog(
            onDismissRequest = { exerciseToOverride = null },
            title = { Text("Override ${ex.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newWeight,
                        onValueChange = { newWeight = it },
                        label = { Text("Weight (${ex.weightUnit})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                    val w = newWeight.toFloatOrNull() ?: 0f
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

    if (exerciseToShowGraph != null) {
        val exerciseId = exerciseToShowGraph!!.id
        val logs by remember(dao, exerciseId) { dao.getWeightLogsForExercise(exerciseId) }.collectAsState(initial = emptyList())
        ExerciseProgressionDialog(
            exercise = exerciseToShowGraph!!,
            logs = logs,
            onDismiss = { exerciseToShowGraph = null }
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: ExerciseEntity,
    onLogWeight: (ExerciseEntity) -> Unit,
    onOverride: (ExerciseEntity) -> Unit,
    onShowGraph: (ExerciseEntity) -> Unit,
    onDelete: (ExerciseEntity) -> Unit
) {
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
                    IconButton(onClick = { onLogWeight(exercise) }) {
                        Icon(Icons.Default.History, contentDescription = "Log Progress", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onOverride(exercise) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Override Weight", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { onShowGraph(exercise) }) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Show Graph", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { onDelete(exercise) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }
}
