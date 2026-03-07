package dev.taxmachine.gymapp.ui.screens

import android.nfc.Tag
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.theme.AppTheme
import dev.taxmachine.gymapp.ui.dialogs.*
import dev.taxmachine.gymapp.service.NfcEmulationService
import dev.taxmachine.gymapp.health.HealthConnectManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isScanning: Boolean,
    onScanningChange: (Boolean) -> Unit,
    scannedTag: Tag?,
    onScannedTagChange: (Tag?) -> Unit,
    scannedData: String?,
    onScannedDataChange: (String?) -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    enableNfcForegroundDispatch: () -> Unit,
    disableNfcForegroundDispatch: () -> Unit,
    healthConnectManager: HealthConnectManager,
    onRequestHealthPermissions: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { GymDatabase.getDatabase(context) }
    val dao = remember { db.gymDao() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    
    val badges by remember(dao) { dao.getAllBadges() }.collectAsState(initial = emptyList())
    val supplements by remember(dao) { dao.getAllSupplements() }.collectAsState(initial = emptyList())
    
    var showNamingDialog by remember { mutableStateOf(false) }
    var selectedBadgeForDetails by remember { mutableStateOf<BadgeEntity?>(null) }
    var emulatingBadgeId by remember { mutableStateOf<String?>(null) }
    var showAddSupplementDialog by remember { mutableStateOf(false) }

    var exerciseToViewProgression by remember { mutableStateOf<ExerciseEntity?>(null) }
    var supplementToViewProgression by remember { mutableStateOf<SupplementEntity?>(null) }
    
    var isInsideSplit by remember { mutableStateOf(false) }
    
    var hasHealthPermissions by remember { mutableStateOf(false) }
    var isSyncingHealth by remember { mutableStateOf(false) }

    val updateEvent by NfcEmulationService.updateEvent.collectAsState()

    LaunchedEffect(isScanning) {
        if (isScanning) {
            enableNfcForegroundDispatch()
        } else {
            disableNfcForegroundDispatch()
        }
    }

    LaunchedEffect(scannedTag) {
        if (scannedTag != null) {
            showNamingDialog = true
        }
    }
    
    LaunchedEffect(selectedTab) {
        if (selectedTab == 4) {
            hasHealthPermissions = healthConnectManager.hasAllPermissions()
        }
    }

    // Unified Back Handler
    BackHandler(enabled = exerciseToViewProgression != null || supplementToViewProgression != null || isInsideSplit || selectedTab != 0) {
        when {
            exerciseToViewProgression != null -> exerciseToViewProgression = null
            supplementToViewProgression != null -> supplementToViewProgression = null
            isInsideSplit -> isInsideSplit = false
            else -> selectedTab = 0
        }
    }

    if (isScanning) {
        NfcScanningDialog(onDismiss = { onScanningChange(false) })
    }

    if (showNamingDialog && scannedTag != null) {
        BadgeNamingDialog(
            tag = scannedTag,
            scannedData = scannedData ?: scannedTag.id.joinToString("") { "%02x".format(it) },
            onDismiss = {
                showNamingDialog = false
                onScannedTagChange(null)
                onScannedDataChange(null)
            },
            onSave = { name, protocol, techList ->
                scope.launch {
                    val uid = scannedTag.id.joinToString("") { "%02x".format(it) }
                    val dataHex = scannedData ?: uid
                    dao.insertBadge(BadgeEntity(
                        id = uid,
                        name = name,
                        tagData = dataHex,
                        protocol = protocol,
                        techList = techList
                    ))
                    onScannedTagChange(null)
                    onScannedDataChange(null)
                    showNamingDialog = false
                }
            }
        )
    }

    if (selectedBadgeForDetails != null) {
        BadgeDetailDialog(
            badge = selectedBadgeForDetails!!,
            onDismiss = { selectedBadgeForDetails = null }
        )
    }

    if (showAddSupplementDialog) {
        SupplementAddDialog(
            onDismiss = { showAddSupplementDialog = false },
            onSave = { supplement ->
                scope.launch {
                    dao.insertSupplement(supplement)
                    showAddSupplementDialog = false
                }
            }
        )
    }

    if (updateEvent != null) {
        AlertDialog(
            onDismissRequest = { NfcEmulationService.clearUpdateEvent() },
            title = { Text("Badge Updated: ${updateEvent!!.badgeName}") },
            text = { 
                Column {
                    Text("The reader has updated your badge data.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("New Code:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        updateEvent!!.newCode,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(onClick = { NfcEmulationService.clearUpdateEvent() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val items = listOf(
                            Triple("Badges", Icons.Default.Badge, 0),
                            Triple("Workouts", Icons.Default.FitnessCenter, 1),
                            Triple("Supps", Icons.Default.MedicalServices, 2),
                            Triple("Calculator", Icons.Default.Calculate, 3),
                            Triple("Health", Icons.Default.Favorite, 4),
                            Triple("Settings", Icons.Default.Settings, 5)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        items.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                                        else Color.Transparent
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .widthIn(min = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        icon, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        label, 
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            },
            floatingActionButton = {
                when (selectedTab) {
                    0 -> if (!isScanning) {
                        FloatingActionButton(
                            onClick = { onScanningChange(true) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Badge")
                        }
                    }
                    2 -> FloatingActionButton(
                        onClick = { showAddSupplementDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Supplement")
                    }
                    4 -> if (hasHealthPermissions) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    isSyncingHealth = true
                                    healthConnectManager.syncHealthData(dao)
                                    isSyncingHealth = false
                                }
                            }
                        ) {
                            if (isSyncingHealth) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync Data")
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                val onShowSupplementGraph = remember { { s: SupplementEntity -> supplementToViewProgression = s } }
                
                when (selectedTab) {
                    0 -> {
                        val onBadgeClick = remember { { badge: BadgeEntity -> selectedBadgeForDetails = badge } }
                        val onEmulateClick = remember {
                            { badge: BadgeEntity ->
                                if (emulatingBadgeId == badge.id) {
                                    emulatingBadgeId = null
                                    NfcEmulationService.activeTagData = null
                                    NfcEmulationService.currentBadgeId = null
                                } else {
                                    emulatingBadgeId = badge.id
                                    NfcEmulationService.activeTagData = badge.tagData
                                    NfcEmulationService.currentBadgeId = badge.id
                                }
                            }
                        }
                        val onDeleteBadge = remember(dao) { { badge: BadgeEntity -> scope.launch { dao.deleteBadge(badge) } ; Unit } }
                        
                        BadgeScreen(
                            badges = badges,
                            onBadgeClick = onBadgeClick,
                            onEmulateClick = onEmulateClick,
                            onDeleteBadge = onDeleteBadge,
                            emulatingBadgeId = emulatingBadgeId
                        )
                    }
                    1 -> WorkoutSplitScreen(
                        dao = dao, 
                        onShowGraph = { exerciseToViewProgression = it },
                        isInsideSplit = isInsideSplit,
                        onInsideSplitChange = { isInsideSplit = it }
                    )
                    2 -> {
                        val onDeleteSupp = remember(dao) { { s: SupplementEntity -> scope.launch { dao.deleteSupplement(s) } ; Unit } }
                        val onToggleActive = remember(dao) { { s: SupplementEntity -> scope.launch { dao.updateSupplement(s.copy(isActive = !s.isActive)) } ; Unit } }
                        val onUpdateDosage = remember(dao) {
                            { s: SupplementEntity, dose: Float ->
                                scope.launch {
                                    dao.insertSupplementLog(SupplementLogEntity(supplementUid = s.uid, dosage = dose))
                                }
                                Unit
                            }
                        }
                        val onOverrideDosage = remember(dao) {
                            { s: SupplementEntity, dose: String ->
                                scope.launch {
                                    dao.updateSupplement(s.copy(dosage = dose))
                                }
                                Unit
                            }
                        }

                        SupplementScreen(
                            dao = dao,
                            supplements = supplements,
                            onDelete = onDeleteSupp,
                            onToggleActive = onToggleActive,
                            onUpdateDosage = onUpdateDosage,
                            onOverrideDosage = onOverrideDosage,
                            onShowGraph = onShowSupplementGraph
                        )
                    }
                    3 -> PeptideCalculatorScreen()
                    4 -> HealthScreen(
                        healthConnectManager = healthConnectManager,
                        dao = dao,
                        onRequestPermissions = {
                            onRequestHealthPermissions()
                            // Refresh permissions after requesting
                            scope.launch {
                                hasHealthPermissions = healthConnectManager.hasAllPermissions()
                            }
                        }
                    )
                    5 -> SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = onDynamicColorChange,
                        dao = dao,
                        db = db
                    )
                }
            }
        }

        // Overlay screens (outside the Scaffold)
        AnimatedVisibility(
            visible = exerciseToViewProgression != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            if (exerciseToViewProgression != null) {
                val logs by remember(dao, exerciseToViewProgression!!.id) { 
                    dao.getWeightLogsForExercise(exerciseToViewProgression!!.id) 
                }.collectAsState(initial = emptyAsList())
                
                ExerciseProgressionScreen(
                    exercise = exerciseToViewProgression!!,
                    logs = logs,
                    onBack = { exerciseToViewProgression = null }
                )
            }
        }

        AnimatedVisibility(
            visible = supplementToViewProgression != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            if (supplementToViewProgression != null) {
                val logs by remember(dao, supplementToViewProgression!!.uid) { 
                    dao.getLogsForSupplement(supplementToViewProgression!!.uid) 
                }.collectAsState(initial = emptyList())
                
                SupplementProgressionScreen(
                    supplement = supplementToViewProgression!!,
                    logs = logs,
                    onBack = { supplementToViewProgression = null }
                )
            }
        }
    }
}

fun <T> emptyAsList(): List<T> = emptyList()
