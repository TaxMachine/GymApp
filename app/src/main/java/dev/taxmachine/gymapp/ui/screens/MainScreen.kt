package dev.taxmachine.gymapp.ui.screens

import android.nfc.Tag
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.theme.AppTheme
import dev.taxmachine.gymapp.ui.dialogs.*
import dev.taxmachine.gymapp.service.NfcEmulationService
import kotlinx.coroutines.launch

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
    disableNfcForegroundDispatch: () -> Unit
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Badge, contentDescription = "Badges") },
                    label = { Text("Badges") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Exercises") },
                    label = { Text("Workouts") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Supps") },
                    label = { Text("Supps") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Peptide Calc") },
                    label = { Text("Peptide Calc") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && !isScanning) {
                FloatingActionButton(onClick = { onScanningChange(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Badge")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(onClick = { showAddSupplementDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Supplement")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                1 -> WorkoutSplitScreen(dao)
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
                        onOverrideDosage = onOverrideDosage
                    )
                }
                3 -> PeptideCalculatorScreen()
                4 -> SettingsScreen(
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
}
