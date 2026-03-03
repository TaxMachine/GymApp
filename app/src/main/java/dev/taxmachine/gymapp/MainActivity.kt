package dev.taxmachine.gymapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.BadgeScreen
import dev.taxmachine.gymapp.ui.SupplementAddDialog
import dev.taxmachine.gymapp.ui.SupplementScreen
import dev.taxmachine.gymapp.ui.WorkoutSplitScreen
import dev.taxmachine.gymapp.ui.PeptideCalculatorScreen
import dev.taxmachine.gymapp.ui.SettingsScreen
import dev.taxmachine.gymapp.ui.AppTheme
import dev.taxmachine.gymapp.ui.theme.GymAppTheme
import dev.taxmachine.gymapp.receiver.SupplementReminderReceiver
import dev.taxmachine.gymapp.service.NfcEmulationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var isScanning = mutableStateOf(false)
    private var scannedTag = mutableStateOf<Tag?>(null)
    private var scannedData = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Please enable NFC.", Toast.LENGTH_LONG).show()
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE) }
            var appTheme by remember { 
                mutableStateOf(AppTheme.valueOf(prefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)) 
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        SupplementReminderReceiver.scheduleReminder(context)
                    }
                }
                
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        SupplementReminderReceiver.scheduleReminder(context)
                    }
                }
            } else {
                LaunchedEffect(Unit) {
                    SupplementReminderReceiver.scheduleReminder(context)
                }
            }

            GymAppTheme(appTheme = appTheme) {
                MainScreen(
                    isScanning = isScanning,
                    scannedTag = scannedTag,
                    scannedData = scannedData,
                    nfcAdapter = nfcAdapter,
                    currentTheme = appTheme,
                    onThemeChange = { newTheme ->
                        appTheme = newTheme
                        prefs.edit().putString("theme", newTheme.name).apply()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isScanning.value) {
            enableNfcReaderMode()
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun enableNfcReaderMode() {
        val options = Bundle()
        nfcAdapter?.enableReaderMode(this, { tag ->
            // Immediately read the full data while the tag is in the field
            val data = readTagFullData(tag)
            runOnUiThread {
                if (isScanning.value) {
                    scannedTag.value = tag
                    scannedData.value = data
                }
            }
        }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or 
           NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or 
           NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, options)
    }

    @Composable
    fun MainScreen(
        isScanning: MutableState<Boolean>,
        scannedTag: MutableState<Tag?>,
        scannedData: MutableState<String?>,
        nfcAdapter: NfcAdapter?,
        currentTheme: AppTheme,
        onThemeChange: (AppTheme) -> Unit
    ) {
        val context = LocalContext.current
        val db = remember { GymDatabase.getDatabase(context) }
        val dao = remember { db.gymDao() }
        val scope = rememberCoroutineScope()

        var selectedTab by remember { mutableIntStateOf(0) }
        
        val badges by dao.getAllBadges().collectAsState(initial = emptyList())
        val supplements by dao.getAllSupplements().collectAsState(initial = emptyList())
        
        var showNamingDialog by remember { mutableStateOf(false) }
        var badgeName by remember { mutableStateOf("") }
        var selectedBadgeForDetails by remember { mutableStateOf<BadgeEntity?>(null) }
        var emulatingBadgeId by remember { mutableStateOf<String?>(null) }

        var showAddSupplementDialog by remember { mutableStateOf(false) }

        LaunchedEffect(isScanning.value) {
            if (isScanning.value) {
                enableNfcReaderMode()
            } else {
                nfcAdapter?.disableReaderMode(this@MainActivity)
            }
        }

        LaunchedEffect(scannedTag.value) {
            if (scannedTag.value != null) {
                showNamingDialog = true
                isScanning.value = false
            }
        }

        if (showNamingDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showNamingDialog = false
                    scannedTag.value = null
                    scannedData.value = null
                },
                title = { Text("Name your Badge") },
                text = {
                    OutlinedTextField(
                        value = badgeName,
                        onValueChange = { badgeName = it },
                        label = { Text("Badge Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val tag = scannedTag.value
                        if (tag != null) {
                            val id = tag.id.joinToString("") { "%02x".format(it) }
                            scope.launch {
                                val dataHex = scannedData.value ?: tag.id.joinToString("") { "%02x".format(it) }
                                dao.insertBadge(BadgeEntity(id, badgeName, dataHex))
                                showNamingDialog = false
                                scannedTag.value = null
                                scannedData.value = null
                                badgeName = ""
                            }
                        }
                    }) {
                        Text("Save")
                    }
                }
            )
        }

        if (selectedBadgeForDetails != null) {
            AlertDialog(
                onDismissRequest = { selectedBadgeForDetails = null },
                title = { Text(selectedBadgeForDetails!!.name) },
                text = {
                    Column {
                        Text("ID: ${selectedBadgeForDetails!!.id}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Full Memory Dump (Hexdump):", style = MaterialTheme.typography.labelMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(vertical = 4.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = formatHexDump(selectedBadgeForDetails!!.tagData),
                                modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            val badge = selectedBadgeForDetails
                            if (badge != null) {
                                val nfcContent = convertToNfcFormat(badge)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, nfcContent)
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TITLE, "${badge.name}.nfc")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Share .nfc")
                        }
                        Button(onClick = { selectedBadgeForDetails = null }) {
                            Text("Close")
                        }
                    }
                }
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
                if (selectedTab == 0 && !isScanning.value) {
                    FloatingActionButton(onClick = { isScanning.value = true }) {
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
                    0 -> BadgeScreen(
                        badges = badges,
                        isScanning = isScanning,
                        onBadgeClick = { selectedBadgeForDetails = it },
                        onEmulateClick = { badge ->
                            if (emulatingBadgeId == badge.id) {
                                emulatingBadgeId = null
                                NfcEmulationService.activeTagData = null
                            } else {
                                emulatingBadgeId = badge.id
                                NfcEmulationService.activeTagData = badge.tagData
                            }
                        },
                        onDeleteBadge = { badge -> scope.launch { dao.deleteBadge(badge) } },
                        emulatingBadgeId = emulatingBadgeId
                    )
                    1 -> WorkoutSplitScreen(dao)
                    2 -> SupplementScreen(
                        dao = dao,
                        supplements = supplements,
                        onDelete = { supplement -> scope.launch { dao.deleteSupplement(supplement) } }
                    )
                    3 -> PeptideCalculatorScreen()
                    4 -> SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        dao = dao,
                        db = db
                    )
                }
            }
        }
    }

    private fun readTagFullData(tag: Tag): String {
        val mu = MifareUltralight.get(tag)
        if (mu != null) {
            try {
                mu.connect()
                // NTAG213 has 45 pages. 215 has 135. 216 has 231.
                // We read up to 40 pages (160 bytes) which covers common badges.
                val fullData = ByteArray(160)
                for (i in 0 until 40 step 4) {
                    val data = try { mu.readPages(i) } catch (e: Exception) { null }
                    if (data != null && data.size >= 16) {
                        System.arraycopy(data, 0, fullData, i * 4, 16)
                    }
                }
                mu.close()
                return fullData.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e("NFC", "Ultralight read error", e)
            }
        }

        val mc = MifareClassic.get(tag)
        if (mc != null) {
            try {
                mc.connect()
                val fullData = mutableListOf<Byte>()
                val keys = listOf(
                    MifareClassic.KEY_DEFAULT, // FFFFFFFFFFFF
                    byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
                    ByteArray(6) { 0 }
                )
                for (s in 0 until mc.sectorCount) {
                    var authenticated = false
                    for (key in keys) {
                        if (mc.authenticateSectorWithKeyA(s, key)) {
                            authenticated = true
                            break
                        }
                    }
                    if (authenticated) {
                        val firstBlock = mc.sectorToBlock(s)
                        for (b in 0 until mc.getBlockCountInSector(s)) {
                            try {
                                val blockData = mc.readBlock(firstBlock + b)
                                fullData.addAll(blockData.toList())
                            } catch (e: Exception) {}
                        }
                    } else {
                        // Fill unreadable sector with zeros to maintain structure
                        repeat(mc.getBlockCountInSector(s) * 16) { fullData.add(0) }
                    }
                }
                mc.close()
                return fullData.toByteArray().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e("NFC", "MifareClassic read error", e)
            }
        }

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                nfcA.connect()
                val fullData = ByteArray(160)
                for (page in 0 until 40 step 4) {
                    val response = try {
                        nfcA.transceive(byteArrayOf(0x30, page.toByte()))
                    } catch (e: Exception) { null }
                    
                    if (response != null && response.size >= 16) {
                        System.arraycopy(response, 0, fullData, page * 4, 16)
                    }
                }
                nfcA.close()
                return fullData.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e("NFC", "Generic NfcA read error", e)
            }
        }
        
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                // For ISO-DEP, we store the UID + any extra data we can get
                val result = isoDep.hiLayerResponse ?: tag.id
                isoDep.close()
                return result.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e("NFC", "ISO-DEP read error", e)
            }
        }
        return tag.id.joinToString("") { "%02x".format(it) }
    }

    private fun formatHexDump(hex: String): String {
        if (hex.isEmpty()) return "No data"
        val bytes = try {
            hex.chunked(2).map { it.toInt(16).toByte() }
        } catch (e: Exception) {
            return "Invalid data format: $hex"
        }
        
        val sb = StringBuilder()
        for (i in bytes.indices step 16) {
            sb.append("%04x: ".format(i))
            val chunk = bytes.subList(i, minOf(i + 16, bytes.size))
            for (j in 0 until 16) {
                if (j < chunk.size) sb.append("%02x ".format(chunk[j]))
                else sb.append("   ")
                if (j == 7) sb.append(" ")
            }
            sb.append(" |")
            for (j in chunk.indices) {
                val c = chunk[j].toInt().toChar()
                if (c in ' '..'~') sb.append(c) else sb.append('.')
            }
            sb.append("|\n")
        }
        return sb.toString()
    }

    private fun convertToNfcFormat(badge: BadgeEntity): String {
        val hex = badge.tagData
        if (hex.isEmpty()) return ""
        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }
        val sb = StringBuilder()
        sb.append("Filetype: Flipper NFC device\n")
        sb.append("Version: 3\n")
        sb.append("Device type: NTAG213\n")
        sb.append("UID: ${badge.id.chunked(2).joinToString(" ").uppercase()}\n")
        sb.append("ATQA: 44 00\n")
        sb.append("SAK: 00\n")
        for (i in 0 until (bytes.size / 4)) {
            val pageData = bytes.subList(i * 4, minOf((i + 1) * 4, bytes.size)).joinToString(" ") { "%02X".format(it) }
            sb.append("Page $i: $pageData\n")
        }
        return sb.toString()
    }
}
