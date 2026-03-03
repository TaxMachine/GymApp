package dev.taxmachine.gymapp

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.taxmachine.gymapp.ui.theme.AppTheme
import dev.taxmachine.gymapp.ui.screens.MainScreen
import dev.taxmachine.gymapp.ui.theme.GymAppTheme
import dev.taxmachine.gymapp.receiver.SupplementReminderReceiver
import dev.taxmachine.gymapp.utils.NfcUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private val isScanning = mutableStateOf(false)
    private val scannedTag = mutableStateOf<Tag?>(null)
    private val scannedData = mutableStateOf<String?>(null)

    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Please enable NFC.", Toast.LENGTH_LONG).show()
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE) }
            
            var appTheme by remember { 
                val savedTheme = prefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
                mutableStateOf(AppTheme.valueOf(savedTheme)) 
            }
            var dynamicColor by remember {
                mutableStateOf(prefs.getBoolean("dynamic_color", true))
            }
            
            val onThemeChange = remember(prefs) {
                { newTheme: AppTheme ->
                    appTheme = newTheme
                    prefs.edit().putString("theme", newTheme.name).apply()
                }
            }
            
            val onDynamicColorChange = remember(prefs) {
                { enabled: Boolean ->
                    dynamicColor = enabled
                    prefs.edit().putBoolean("dynamic_color", enabled).apply()
                }
            }

            val onScanningChange = remember { { enabled: Boolean -> isScanning.value = enabled } }
            val onScannedTagChange = remember { { tag: Tag? -> scannedTag.value = tag } }
            val onScannedDataChange = remember { { data: String? -> scannedData.value = data } }
            
            val enableNfcForegroundDispatchLambda = remember { { enableNfcForegroundDispatch() } }
            val disableNfcForegroundDispatchLambda = remember {
                {
                    nfcAdapter?.disableForegroundDispatch(this@MainActivity)
                    Unit
                }
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

            GymAppTheme(appTheme = appTheme, dynamicColor = dynamicColor) {
                MainScreen(
                    isScanning = isScanning.value,
                    onScanningChange = onScanningChange,
                    scannedTag = scannedTag.value,
                    onScannedTagChange = onScannedTagChange,
                    scannedData = scannedData.value,
                    onScannedDataChange = onScannedDataChange,
                    currentTheme = appTheme,
                    onThemeChange = onThemeChange,
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = onDynamicColorChange,
                    enableNfcForegroundDispatch = enableNfcForegroundDispatchLambda,
                    disableNfcForegroundDispatch = disableNfcForegroundDispatchLambda
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isScanning.value) {
            enableNfcForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun enableNfcForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null && isScanning.value) {
            processTag(tag)
        }
    }

    private fun processTag(tag: Tag) {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = NfcUtils.readTagFullData(tag)
            withContext(Dispatchers.Main) {
                scannedTag.value = tag
                scannedData.value = data
                isScanning.value = false
            }
        }
    }
}
