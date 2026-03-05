package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.dialogs.AddSplitDialog
import kotlinx.coroutines.launch

@Composable
fun WorkoutSplitScreen(
    dao: GymDao, 
    onShowGraph: (ExerciseEntity) -> Unit,
    isInsideSplit: Boolean,
    onInsideSplitChange: (Boolean) -> Unit
) {
    var selectedSplit by remember { mutableStateOf<SplitEntity?>(null) }
    
    // Sync external isInsideSplit state with local selectedSplit state
    LaunchedEffect(isInsideSplit) {
        if (!isInsideSplit) {
            selectedSplit = null
        }
    }

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
                    val onSplitClick = remember { { split: SplitEntity -> 
                        selectedSplit = split 
                        onInsideSplitChange(true)
                    } }
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
        ExerciseListScreen(dao, selectedSplit!!, onShowGraph) { 
            selectedSplit = null 
            onInsideSplitChange(false)
        }
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
