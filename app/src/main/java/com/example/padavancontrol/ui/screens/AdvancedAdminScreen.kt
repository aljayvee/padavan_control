package com.example.padavancontrol.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.AdvancedAdminViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.AdminConfig
import com.example.padavancontrol.theme.ArcherTeal
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAdminScreen(
    section: String, // "System", "Services", "OpMode", "Firmware", "Backup", "ButtonsLed"
    viewModel: AdvancedAdminViewModel,
    repository: PadavanRepository,
    onNavigateBack: () -> Unit,
    onNavigateToTtydWebShell: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            "System" -> "Advanced_System_Content.asp"
            "Services" -> "Advanced_Services_Content.asp"
            "OpMode" -> "Advanced_OperationMode_Content.asp"
            "Firmware" -> "Advanced_FirmwareUpgrade_Content.asp"
            "Backup" -> "Advanced_SettingBackup_Content.asp"
            "ButtonsLed" -> "Advanced_Tweaks_Content.asp" // Tweaks contains WPS/LED mappings in recent builds
            else -> "Advanced_System_Content.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            "System" -> "Admin - System Settings"
            "Services" -> "Admin - SSH / Telnet Services"
            "OpMode" -> "Admin - Operation Mode"
            "Firmware" -> "Admin - Firmware Upgrade"
            "Backup" -> "Admin - Settings Backup"
            "ButtonsLed" -> "Admin - Buttons & LEDs"
            else -> "Administration"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Forms fields
    var timezoneExpanded by remember { mutableStateOf(false) }
    var opModeExpanded by remember { mutableStateOf(false) }
    var wpsModeExpanded by remember { mutableStateOf(false) }
    var ledModeExpanded by remember { mutableStateOf(false) }
    var wpsShortExpanded by remember { mutableStateOf(false) }
    var wpsLongExpanded by remember { mutableStateOf(false) }
    var ledInternetExpanded by remember { mutableStateOf(false) }
    var ledUsbExpanded by remember { mutableStateOf(false) }
    var ledWifiExpanded by remember { mutableStateOf(false) }
    var ledPowerExpanded by remember { mutableStateOf(false) }
    var ledEthernetExpanded by remember { mutableStateOf(false) }
    var httpProtoExpanded by remember { mutableStateOf(false) }
    var httpAccessExpanded by remember { mutableStateOf(false) }
    var stSambaLmbExpanded by remember { mutableStateOf(false) }

    var ntpPeriodExpanded by remember { mutableStateOf(false) }
    var logFloatUiExpanded by remember { mutableStateOf(false) }
    var selectLangExpanded by remember { mutableStateOf(false) }
    var sshdEnableExpanded by remember { mutableStateOf(false) }
    
    var nvramManualExpanded by remember { mutableStateOf(false) }
    var rstatsStoredExpanded by remember { mutableStateOf(false) }
    var stimeStoredExpanded by remember { mutableStateOf(false) }
    var mtdRwfsMountExpanded by remember { mutableStateOf(false) }

    // Backup & Upgrade file selectors
    var selectedFirmwareUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFirmwareName by remember { mutableStateOf("") }
    var selectedSettingsUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSettingsName by remember { mutableStateOf("") }
    var selectedStorageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedStorageName by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }

    // Dialog control for storage reset
    var showStorageResetDialog by remember { mutableStateOf(false) }
    var isStorageResetting by remember { mutableStateOf(false) }

    val firmwarePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFirmwareUri = uri
            selectedFirmwareName = getFileNameHelper(context, uri) ?: "firmware.trx"
        }
    }

    val settingsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedSettingsUri = uri
            selectedSettingsName = getFileNameHelper(context, uri) ?: "settings.CFG"
        }
    }

    val storagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedStorageUri = uri
            selectedStorageName = getFileNameHelper(context, uri) ?: "storage.TBZ"
        }
    }

    // Passwords management
    var passwordChangeInput by remember { mutableStateOf("") }
    var passwordVerifyInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Reset settings validation dialog
    var showResetDialog by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    // Mock upgrade checking
    var isCheckingUpgrade by remember { mutableStateOf(false) }

    // Input errors validation
    var sshPortError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var httpPortError by remember { mutableStateOf<String?>(null) }
    var httpsPortError by remember { mutableStateOf<String?>(null) }
    var ttydPortError by remember { mutableStateOf<String?>(null) }

    fun loadConfig() {
        viewModel.loadConfig(pagePath)
    }

    LaunchedEffect(key1 = section) {
        loadConfig()
    }

    fun validateInputs(): Boolean {
        var isValid = true
        sshPortError = null
        passwordError = null
        httpPortError = null
        httpsPortError = null
        ttydPortError = null

        if (section == "System" && passwordChangeInput.isNotEmpty()) {
            if (passwordChangeInput != passwordVerifyInput) {
                passwordError = "Passwords do not match."
                isValid = false
            } else if (passwordChangeInput.length < 5) {
                passwordError = "Password must be at least 5 characters."
                isValid = false
            }
        }

        if (section == "Services") {
            if (uiState.config.enableSsh) {
                val port = uiState.config.sshPort.toIntOrNull()
                if (port == null || port < 1 || port > 65535) {
                    sshPortError = "Invalid port range 1 - 65535"
                    isValid = false
                }
            }
            val httpProto = uiState.config.httpProto
            if (httpProto == "0" || httpProto == "2") {
                val port = uiState.config.httpLanPort.toIntOrNull()
                if (port == null || port < 80 || port > 65535) {
                    httpPortError = "Invalid HTTP port range 80 - 65535"
                    isValid = false
                }
            }
            if (httpProto == "1" || httpProto == "2") {
                val port = uiState.config.httpsLPort.toIntOrNull()
                if (port == null || port < 81 || port > 65535) {
                    httpsPortError = "Invalid HTTPS port range 81 - 65535"
                    isValid = false
                }
            }
            if (httpProto == "2") {
                if (uiState.config.httpLanPort == uiState.config.httpsLPort) {
                    httpsPortError = "HTTP and HTTPS ports must be different"
                    isValid = false
                }
            }
            if (uiState.config.ttydEnable) {
                val port = uiState.config.ttydPort.toIntOrNull()
                if (port == null || port < 1 || port > 65535) {
                    ttydPortError = "Invalid port range 1 - 65535"
                    isValid = false
                }
            }
        }

        return isValid
    }

    fun saveConfig() {
        if (!validateInputs()) {
            Toast.makeText(context, "Please fix form validation errors first.", Toast.LENGTH_SHORT).show()
            return
        }
        val finalConfig = if (section == "System" && passwordChangeInput.isNotEmpty()) {
            uiState.config.copy(adminPass = passwordChangeInput)
        } else {
            uiState.config
        }
        viewModel.updateConfig(finalConfig)
        viewModel.saveConfig(pagePath)
        passwordChangeInput = ""
        passwordVerifyInput = ""
    }

    fun triggerReset() {
        showResetDialog = false
        isResetting = true
        coroutineScope.launch {
            val success = repository.restoreNvramDefaults()
            isResetting = false
            if (success) {
                Toast.makeText(context, "Router is restoring factory defaults and rebooting... 🔄", Toast.LENGTH_LONG).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, "Failed to send reset instruction.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun triggerStorageReset() {
        showStorageResetDialog = false
        isStorageResetting = true
        coroutineScope.launch {
            val success = repository.restoreStorageDefaults()
            isStorageResetting = false
            if (success) {
                Toast.makeText(context, "Internal storage defaults restored. Router is restarting... 🔄", Toast.LENGTH_LONG).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, "Failed to restore internal storage defaults.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkFirmwareUpgrade() {
        isCheckingUpgrade = true
        coroutineScope.launch {
            val checkRes = repository.executeCommand("ping -c 2 -W 3 github.com")
            isCheckingUpgrade = false
            if (checkRes.contains("100% packet loss") || checkRes.contains("bad address") || checkRes.contains("Error")) {
                Toast.makeText(context, "No internet access to retrieve updates.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Your firmware version ${uiState.config.firmwareVersion} is completely up to date! ✅", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArcherTeal
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.isSaving) {
                        IconButton(onClick = { viewModel.loadConfig(pagePath) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload", tint = ArcherTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = uiState.loadProgress,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing),
                label = "loadProgressAnimation"
            )
            val showLoader = uiState.isLoading || animatedProgress < 0.99f

            if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Error loading settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.errorMessage ?: "", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.loadConfig(pagePath) },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                    ) {
                        Text("Retry")
                    }
                }
            } else if (showLoader) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = ArcherTeal,
                        trackColor = Color(0xFFE0F2F1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        color = ArcherTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.loadStatus.ifEmpty { "Fetching Administration settings..." },
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // Render UI sections
                    when (section) {
                        "System" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Change Administrator Credentials", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    OutlinedTextField(
                                        value = uiState.config.deviceName,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(deviceName = it)) },
                                        label = { Text("Device Name") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.adminUser,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(adminUser = it)) },
                                        label = { Text("Web Console Username") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = passwordChangeInput,
                                        onValueChange = { passwordChangeInput = it },
                                        label = { Text("New Login Password") },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        isError = passwordError != null,
                                        supportingText = passwordError?.let { { Text(it) } },
                                        trailingIcon = {
                                            val text = if (showPassword) "Hide" else "Show"
                                            Text(
                                                text = text,
                                                color = ArcherTeal,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.clickable { showPassword = !showPassword }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    if (passwordChangeInput.isNotEmpty()) {
                                        OutlinedTextField(
                                            value = passwordVerifyInput,
                                            onValueChange = { passwordVerifyInput = it },
                                            label = { Text("Re-type Password") },
                                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("NTP System Time Sync", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // Timezone selector
                                    Text("GMT Timezone", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { timezoneExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(uiState.config.timezone, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = timezoneExpanded, onDismissRequest = { timezoneExpanded = false }) {
                                            listOf("GMT-12:00", "GMT-8:00", "GMT-5:00", "GMT", "GMT+1:00", "GMT+2:00", "GMT+3:00", "GMT+8:00 (Asia/Shanghai)", "GMT+9:00", "GMT+10:00").forEach { tz ->
                                                DropdownMenuItem(text = { Text(tz) }, onClick = { viewModel.updateConfig(uiState.config.copy(timezone = tz)); timezoneExpanded = false })
                                            }
                                        }
                                    }

                                    // NTP sync period selector
                                    Text("NTP Synchronization Period", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { ntpPeriodExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.ntpPeriod) {
                                                "6" -> "6 hours"
                                                "12" -> "12 hours"
                                                "24" -> "1 day (*)"
                                                "48" -> "2 days"
                                                "72" -> "3 days"
                                                else -> "1 day (*)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = ntpPeriodExpanded, onDismissRequest = { ntpPeriodExpanded = false }) {
                                            DropdownMenuItem(text = { Text("6 hours") }, onClick = { viewModel.updateConfig(uiState.config.copy(ntpPeriod = "6")); ntpPeriodExpanded = false })
                                            DropdownMenuItem(text = { Text("12 hours") }, onClick = { viewModel.updateConfig(uiState.config.copy(ntpPeriod = "12")); ntpPeriodExpanded = false })
                                            DropdownMenuItem(text = { Text("1 day (*)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ntpPeriod = "24")); ntpPeriodExpanded = false })
                                            DropdownMenuItem(text = { Text("2 days") }, onClick = { viewModel.updateConfig(uiState.config.copy(ntpPeriod = "48")); ntpPeriodExpanded = false })
                                            DropdownMenuItem(text = { Text("3 days") }, onClick = { viewModel.updateConfig(uiState.config.copy(ntpPeriod = "72")); ntpPeriodExpanded = false })
                                        }
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.ntpServer1,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(ntpServer1 = it)) },
                                        label = { Text("Primary NTP Server") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.ntpServer2,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(ntpServer2 = it)) },
                                        label = { Text("Backup NTP Server") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Miscellaneous System Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    OutlinedTextField(
                                        value = uiState.config.logIpAddr,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(logIpAddr = it)) },
                                        label = { Text("Remote Log Server IP") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.logPort,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(logPort = it)) },
                                        label = { Text("Remote Log Server Port") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Text("Enable Syslog Floating Toolbar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { logFloatUiExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.logFloatUi) {
                                                "0" -> "No"
                                                "1" -> "Yes (*)"
                                                "2" -> "Last 100 lines"
                                                else -> "Yes (*)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = logFloatUiExpanded, onDismissRequest = { logFloatUiExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No") }, onClick = { viewModel.updateConfig(uiState.config.copy(logFloatUi = "0")); logFloatUiExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes (*)") }, onClick = { viewModel.updateConfig(uiState.config.copy(logFloatUi = "1")); logFloatUiExpanded = false })
                                            DropdownMenuItem(text = { Text("Last 100 lines") }, onClick = { viewModel.updateConfig(uiState.config.copy(logFloatUi = "2")); logFloatUiExpanded = false })
                                        }
                                    }

                                    Text("Select WebUI Language", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { selectLangExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.selectLang) {
                                                "EN" -> "English"
                                                "CN" -> "简体中文"
                                                else -> "English"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = selectLangExpanded, onDismissRequest = { selectLangExpanded = false }) {
                                            DropdownMenuItem(text = { Text("English") }, onClick = { viewModel.updateConfig(uiState.config.copy(selectLang = "EN")); selectLangExpanded = false })
                                            DropdownMenuItem(text = { Text("简体中文") }, onClick = { viewModel.updateConfig(uiState.config.copy(selectLang = "CN")); selectLangExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Enable Context Help", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Show explanatory tooltips on fields click", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.helpEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(helpEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }

                        "Services" -> {
                            // 1. HTTP Web Server Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("HTTP Web Server", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // Protocol Dropdown
                                    Text("Web Access Protocol", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { httpProtoExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.httpProto) {
                                                "0" -> "HTTP Only"
                                                "1" -> "HTTPS Only"
                                                "2" -> "HTTP & HTTPS"
                                                else -> "HTTP Only"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = httpProtoExpanded, onDismissRequest = { httpProtoExpanded = false }) {
                                            DropdownMenuItem(text = { Text("HTTP Only") }, onClick = { viewModel.updateConfig(uiState.config.copy(httpProto = "0")); httpProtoExpanded = false })
                                            DropdownMenuItem(text = { Text("HTTPS Only") }, onClick = { viewModel.updateConfig(uiState.config.copy(httpProto = "1")); httpProtoExpanded = false })
                                            DropdownMenuItem(text = { Text("HTTP & HTTPS") }, onClick = { viewModel.updateConfig(uiState.config.copy(httpProto = "2")); httpProtoExpanded = false })
                                        }
                                    }

                                    // HTTP Port
                                    if (uiState.config.httpProto == "0" || uiState.config.httpProto == "2") {
                                        OutlinedTextField(
                                            value = uiState.config.httpLanPort,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(httpLanPort = it)) },
                                            label = { Text("Port of Web Access from LAN (HTTP)") },
                                            isError = httpPortError != null,
                                            supportingText = httpPortError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // HTTPS Port
                                    if (uiState.config.httpProto == "1" || uiState.config.httpProto == "2") {
                                        OutlinedTextField(
                                            value = uiState.config.httpsLPort,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(httpsLPort = it)) },
                                            label = { Text("Port of Web Access from LAN (HTTPS)") },
                                            isError = httpsPortError != null,
                                            supportingText = httpsPortError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // Access Restrictions Dropdown
                                    Text("Restricting Web Access from LAN", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { httpAccessExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.httpAccess) {
                                                "0" -> "No (Allow All clients)"
                                                "1" -> "Wired clients only"
                                                "2" -> "Wired and MainAP clients"
                                                else -> "No"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = httpAccessExpanded, onDismissRequest = { httpAccessExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No (Allow All clients)") }, onClick = { viewModel.updateConfig(uiState.config.copy(httpAccess = "0")); httpAccessExpanded = false })
                                            DropdownMenuItem(text = { Text("Wired clients only") }, onClick = { viewModel.updateConfig(uiState.config.copy(httpAccess = "1")); httpAccessExpanded = false })
                                            DropdownMenuItem(text = { Text("Wired and MainAP clients") }, onClick = { viewModel.updateConfig(uiState.config.copy(httpAccess = "2")); httpAccessExpanded = false })
                                        }
                                    }
                                }
                            }

                            // 2. Telnet Console Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Telnet Console Service", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable Telnet Daemon", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Permit unencrypted terminal link", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.enableTelnet,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(enableTelnet = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.enableTelnet) {
                                        OutlinedTextField(
                                            value = uiState.config.telnetPort,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(telnetPort = it)) },
                                            label = { Text("Telnet Port (Readonly)") },
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // 3. SSH Console & SFTP Service Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("SSH Console & SFTP Service", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Text("Enable SSH Server?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { sshdEnableExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.sshdEnable) {
                                                "0" -> "No"
                                                "1" -> "Yes"
                                                "2" -> "Yes (authorized_keys only)"
                                                else -> "Yes"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = sshdEnableExpanded, onDismissRequest = { sshdEnableExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No") }, onClick = { viewModel.updateConfig(uiState.config.copy(sshdEnable = "0", enableSsh = false)); sshdEnableExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes") }, onClick = { viewModel.updateConfig(uiState.config.copy(sshdEnable = "1", enableSsh = true)); sshdEnableExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes (authorized_keys only)") }, onClick = { viewModel.updateConfig(uiState.config.copy(sshdEnable = "2", enableSsh = true)); sshdEnableExpanded = false })
                                        }
                                    }

                                    if (uiState.config.sshdEnable != "0") {
                                        OutlinedTextField(
                                            value = uiState.config.sshPort,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(sshPort = it)) },
                                            label = { Text("SSH Port") },
                                            isError = sshPortError != null,
                                            supportingText = sshPortError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        ) {
                                            Column {
                                                Text("Enable SFTP Filesystem Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Allows encrypted files transfer via SSH subsystem", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.enableSftp,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(enableSftp = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Enable WebDAV Service", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Exposes HTTP file-sharing endpoints for networks", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.enableWebdav,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(enableWebdav = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            // 4. Windows Internet Name Service (WINS) Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("WINS Name Service", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable WINS Service", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Resolve NetBIOS names on local network", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.winsEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(winsEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.winsEnable) {
                                        OutlinedTextField(
                                            value = uiState.config.stSambaWorkgroup,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(stSambaWorkgroup = it)) },
                                            label = { Text("Workgroup Name") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        Text("Local Master Browser Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { stSambaLmbExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.stSambaLmb) {
                                                    "0" -> "No"
                                                    "1" -> "Local Master Browser"
                                                    "2" -> "Local & Domain Master Browser"
                                                    else -> "Local Master Browser"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = stSambaLmbExpanded, onDismissRequest = { stSambaLmbExpanded = false }) {
                                                DropdownMenuItem(text = { Text("No") }, onClick = { viewModel.updateConfig(uiState.config.copy(stSambaLmb = "0")); stSambaLmbExpanded = false })
                                                DropdownMenuItem(text = { Text("Local Master Browser") }, onClick = { viewModel.updateConfig(uiState.config.copy(stSambaLmb = "1")); stSambaLmbExpanded = false })
                                                DropdownMenuItem(text = { Text("Local & Domain Master Browser") }, onClick = { viewModel.updateConfig(uiState.config.copy(stSambaLmb = "2")); stSambaLmbExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. ttyd Config Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("ttyd Web Terminal Service", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable ttyd Web Shell", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allows Web access to command shell terminal", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ttydEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ttydEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.ttydEnable) {
                                        OutlinedTextField(
                                            value = uiState.config.ttydPort,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ttydPort = it)) },
                                            label = { Text("ttyd Bind Port") },
                                            isError = ttydPortError != null,
                                            supportingText = ttydPortError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        Button(
                                            onClick = onNavigateToTtydWebShell,
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Open ttyd Web Shell")
                                        }
                                    }
                                }
                            }

                            // 6. Miscellaneous Services Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Miscellaneous System Services", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Text("Enable vlmcsd (KMS Server)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Activates KMS client licensing on networks", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.vlmcsdEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(vlmcsdEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Text("Enable napt66 (IPv6 NAT)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Translates IPv6 local addresses after reboot", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.napt66Enable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(napt66Enable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Text("LLTD Service", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Link Layer Topology Discovery protocol daemon", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.lltdEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(lltdEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Text("ASUS Discovery Service", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allows discovery by router utilities", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.adscEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(adscEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Text("Cron Daemon (Scheduler)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allows task scheduling via crontabs", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.crondEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(crondEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Hardware Watchdog Timer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Automatically reboots router on CPU freeze", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.watchdogCpu,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(watchdogCpu = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            // 7. Cron Scheduler Tasks Card
                            if (uiState.config.crondEnable) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Scheduler tasks (Crontab)", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                        OutlinedTextField(
                                            value = uiState.config.crontabLogin,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(crontabLogin = it)) },
                                            label = { Text("Crontab Entries (one task per line)") },
                                            placeholder = { Text("*/5 * * * * /sbin/ping_watchdog.sh") },
                                            minLines = 4,
                                            maxLines = 10,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        "OpMode" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Router Operating Mode", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFF9E6))
                                            .border(1.dp, Color(0xFFFFCC00), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Warning, contentDescription = "warning", tint = Color(0xFFFF9900))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "CRITICAL: Modifying the operation mode will erase temporary states and automatically REBOOT the physical router! Ensure you are ready.",
                                                color = Color(0xFF996600),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Text("Operation Mode Selection", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    val currentMode = uiState.config.opMode
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.updateConfig(uiState.config.copy(opMode = "1")) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = currentMode == "1" || currentMode != "3",
                                            onClick = { viewModel.updateConfig(uiState.config.copy(opMode = "1")) },
                                            colors = RadioButtonDefaults.colors(selectedColor = ArcherTeal)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Wireless Radio (default) / Gateway Mode", color = Color.Black, fontSize = 14.sp)
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.updateConfig(uiState.config.copy(opMode = "3")) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = currentMode == "3",
                                            onClick = { viewModel.updateConfig(uiState.config.copy(opMode = "3")) },
                                            colors = RadioButtonDefaults.colors(selectedColor = ArcherTeal)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Access Point Mode (AP)", color = Color.Black, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        "Firmware" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Firmware Upgrade Instructions", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))
                                    Text("Follow instructions listed below:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    
                                    val steps = listOf(
                                        "1. Check if any new version of firmware is available on ASUS RT-N56U custom firmware website or here: https://github.com/aljayvee/CleanPadavan-Newifi3",
                                        "2. Download a proper version to your local machine.",
                                        "3. Specify the path and name of the downloaded file in the [New Firmware File].",
                                        "4. Click [Upload] to upload the file to router. Uploading process takes about 2-3 minutes.",
                                        "5. After receiving a correct firmware file, router will automatically start the upgrade process. The system reboots after the upgrading process is finished."
                                    )
                                    steps.forEach { step ->
                                        Text(step, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Firmware Upgrade Configuration", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Product ID:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp)
                                        Text(uiState.config.productId.ifEmpty { "RT-N56U" }, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Firmware Version:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp)
                                        Text(uiState.config.firmwareVersion, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("New Firmware File:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Button(
                                            onClick = { firmwarePicker.launch("*/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.DarkGray)
                                        ) {
                                            Text("Choose File (.trx)")
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = selectedFirmwareName.ifEmpty { "No file chosen" },
                                            fontSize = 13.sp,
                                            color = if (selectedFirmwareUri != null) Color.Black else Color.Gray,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val uri = selectedFirmwareUri
                                            if (uri != null) {
                                                isUploading = true
                                                uploadProgress = 0f
                                                uploadStatus = "Uploading firmware upgrade..."
                                                showUploadDialog = true
                                                coroutineScope.launch {
                                                    val part = uriToMultipartBodyPartHelper(context, uri, "file")
                                                    if (part == null) {
                                                        showUploadDialog = false
                                                        isUploading = false
                                                        Toast.makeText(context, "Failed to read firmware file.", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }
                                                    val progressJob = launch {
                                                        for (percent in 1..95) {
                                                            delay(80)
                                                            uploadProgress = percent / 100f
                                                        }
                                                    }
                                                    val success = repository.uploadFirmwareUpgrade(part)
                                                    progressJob.cancel()
                                                    uploadProgress = 1.0f
                                                    delay(500)
                                                    showUploadDialog = false
                                                    isUploading = false
                                                    if (success) {
                                                        Toast.makeText(context, "Firmware upgraded successfully! Router is rebooting... 🔄", Toast.LENGTH_LONG).show()
                                                        onNavigateBack()
                                                    } else {
                                                        Toast.makeText(context, "Firmware upgrade failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Please choose a .trx firmware file first.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = selectedFirmwareUri != null && !isUploading,
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Upload")
                                    }

                                    Button(
                                        onClick = { checkFirmwareUpgrade() },
                                        enabled = !isCheckingUpgrade,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.DarkGray),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isCheckingUpgrade) {
                                            CircularProgressIndicator(color = Color.DarkGray, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text("Check for Online Upgrades 🔄")
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Upgrade Process Notes", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Text(
                                        "Note:\n" +
                                        "For a configuration parameter existing both in the old and new firmware, its setting will be kept during the upgrade process.\n" +
                                        "In case the upgrade process fails, router enters the emergency mode automatically. The LED signals at the front panel will indicate such situation. Use the Firmware Restoration utility on the CD to do system recovery.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        "Backup" -> {
                            val modelName = uiState.config.productId.ifEmpty { "Router" }

                            fun downloadSettings() {
                                coroutineScope.launch {
                                    val body = repository.downloadSettingsBackup(modelName)
                                    if (body != null) {
                                        try {
                                            val bytes = body.bytes()
                                            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                            val file = java.io.File(downloadsDir, "Settings_${modelName}.CFG")
                                            file.writeBytes(bytes)
                                            Toast.makeText(context, "Settings backup saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to download settings backup.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            fun uploadSettingsFile() {
                                val uri = selectedSettingsUri ?: return
                                isUploading = true
                                uploadProgress = 0f
                                uploadStatus = "Uploading settings backup..."
                                showUploadDialog = true
                                coroutineScope.launch {
                                    for (i in 1..10) {
                                        delay(200)
                                        uploadProgress = i * 0.1f
                                    }
                                    val part = uriToMultipartBodyPartHelper(context, uri, "file")
                                    val success = if (part != null) repository.uploadSettingsBackup(part) else false
                                    showUploadDialog = false
                                    isUploading = false
                                    if (success) {
                                        Toast.makeText(context, "Settings restored successfully! Router is rebooting...", Toast.LENGTH_LONG).show()
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "Failed to restore settings backup.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            fun downloadStorage() {
                                coroutineScope.launch {
                                    val body = repository.downloadStorageBackup(modelName)
                                    if (body != null) {
                                        try {
                                            val bytes = body.bytes()
                                            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                            val file = java.io.File(downloadsDir, "Storage_${modelName}.TBZ")
                                            file.writeBytes(bytes)
                                            Toast.makeText(context, "Storage backup saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to download storage backup.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            fun uploadStorageFile() {
                                val uri = selectedStorageUri ?: return
                                isUploading = true
                                uploadProgress = 0f
                                uploadStatus = "Uploading storage backup..."
                                showUploadDialog = true
                                coroutineScope.launch {
                                    for (i in 1..10) {
                                        delay(200)
                                        uploadProgress = i * 0.1f
                                    }
                                    val part = uriToMultipartBodyPartHelper(context, uri, "file")
                                    val success = if (part != null) repository.uploadStorageBackup(part) else false
                                    showUploadDialog = false
                                    isUploading = false
                                    if (success) {
                                        Toast.makeText(context, "Storage restored successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to restore storage backup.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Router Settings (NVRAM)", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Button(
                                        onClick = { showResetDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                                        enabled = !isResetting,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        if (isResetting) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text("Restore Factory Default Settings ⚠️")
                                        }
                                    }

                                    Button(
                                        onClick = { downloadSettings() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Save Setting to a File 📤")
                                    }

                                    Text("Restore Settings from a File:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Button(
                                            onClick = { settingsPicker.launch("*/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.DarkGray)
                                        ) {
                                            Text("Choose File (.CFG)")
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = selectedSettingsName.ifEmpty { "No file chosen" },
                                            fontSize = 13.sp,
                                            color = if (selectedSettingsUri != null) Color.Black else Color.Gray,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (selectedSettingsUri != null) {
                                        Button(
                                            onClick = { uploadSettingsFile() },
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Text("Upload Settings File 📥")
                                        }
                                    }

                                    Text("NVRAM to Flash Memory Committing Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { nvramManualExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.nvramManual) {
                                                "0" -> "Auto Commit on apply"
                                                "1" -> "Manual Commit only"
                                                else -> "Auto Commit on apply"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = nvramManualExpanded, onDismissRequest = { nvramManualExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Auto Commit on apply") }, onClick = { viewModel.updateConfig(uiState.config.copy(nvramManual = "0")); nvramManualExpanded = false })
                                            DropdownMenuItem(text = { Text("Manual Commit only") }, onClick = { viewModel.updateConfig(uiState.config.copy(nvramManual = "1")); nvramManualExpanded = false })
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val success = repository.commitFlash("commit_nvram")
                                                if (success) {
                                                    Toast.makeText(context, "NVRAM successfully committed to flash memory! 💾", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to commit NVRAM to flash.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFD7E14)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Commit NVRAM Content to Flash Memory Now 💾")
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Router Internal Storage (/etc/storage)", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Button(
                                        onClick = { showStorageResetDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                                        enabled = !isStorageResetting,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        if (isStorageResetting) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text("Restore Factory Default Storage ⚠️")
                                        }
                                    }

                                    Button(
                                        onClick = { downloadStorage() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Save Storage to a Backup File 📤")
                                    }

                                    Text("Restore Storage from a Backup File:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Button(
                                            onClick = { storagePicker.launch("*/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.DarkGray)
                                        ) {
                                            Text("Choose File (.TBZ)")
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = selectedStorageName.ifEmpty { "No file chosen" },
                                            fontSize = 13.sp,
                                            color = if (selectedStorageUri != null) Color.Black else Color.Gray,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (selectedStorageUri != null) {
                                        Button(
                                            onClick = { uploadStorageFile() },
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Text("Upload Storage File 📥")
                                        }
                                    }

                                    Text("Save Network Traffic History to Internal Storage?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { rstatsStoredExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.rstatsStored) {
                                                "0" -> "No"
                                                "1" -> "Yes, on shutdown"
                                                "2" -> "Yes, periodically"
                                                else -> "Yes, on shutdown"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = rstatsStoredExpanded, onDismissRequest = { rstatsStoredExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No") }, onClick = { viewModel.updateConfig(uiState.config.copy(rstatsStored = "0")); rstatsStoredExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes, on shutdown") }, onClick = { viewModel.updateConfig(uiState.config.copy(rstatsStored = "1")); rstatsStoredExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes, periodically") }, onClick = { viewModel.updateConfig(uiState.config.copy(rstatsStored = "2")); rstatsStoredExpanded = false })
                                        }
                                    }

                                    Text("Save Current System Time to Internal Storage?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { stimeStoredExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.stimeStored) {
                                                "0" -> "No"
                                                "1" -> "Yes, on shutdown & periodically"
                                                else -> "Yes, on shutdown & periodically"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = stimeStoredExpanded, onDismissRequest = { stimeStoredExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No") }, onClick = { viewModel.updateConfig(uiState.config.copy(stimeStored = "0")); stimeStoredExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes, on shutdown & periodically") }, onClick = { viewModel.updateConfig(uiState.config.copy(stimeStored = "1")); stimeStoredExpanded = false })
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val success = repository.commitFlash("commit_storage")
                                                if (success) {
                                                    Toast.makeText(context, "Storage successfully committed to flash memory! 💾", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to commit storage to flash.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F42C1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Commit Internal Storage to Flash Memory Now 💾")
                                    }
                                }
                            }
                        }

                        "ButtonsLed" -> {
                            // Card 1: WPS Button Action
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Hardware WPS Button Behavior", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // Short press dropdown
                                    Text("Button Short Press (1 sec)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { wpsShortExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.btnWpsShort) {
                                                "0" -> "Disabled"
                                                "1" -> "Toggle LED status"
                                                "2" -> "Toggle Wi-Fi radios"
                                                "3" -> "Run Ez-Buttons Script"
                                                "4" -> "WPS Pairing"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = wpsShortExpanded, onDismissRequest = { wpsShortExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsShort = "0")); wpsShortExpanded = false })
                                            DropdownMenuItem(text = { Text("Toggle LED status") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsShort = "1")); wpsShortExpanded = false })
                                            DropdownMenuItem(text = { Text("Toggle Wi-Fi radios") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsShort = "2")); wpsShortExpanded = false })
                                            DropdownMenuItem(text = { Text("Run Ez-Buttons Script") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsShort = "3")); wpsShortExpanded = false })
                                            DropdownMenuItem(text = { Text("WPS Pairing") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsShort = "4")); wpsShortExpanded = false })
                                        }
                                    }

                                    // Long press dropdown
                                    Text("Button Long Press (3 sec)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { wpsLongExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.btnWpsLong) {
                                                "0" -> "Disabled"
                                                "1" -> "Toggle LED status"
                                                "2" -> "Toggle Wi-Fi radios"
                                                "3" -> "Run Ez-Buttons Script"
                                                "4" -> "WPS Pairing"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = wpsLongExpanded, onDismissRequest = { wpsLongExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsLong = "0")); wpsLongExpanded = false })
                                            DropdownMenuItem(text = { Text("Toggle LED status") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsLong = "1")); wpsLongExpanded = false })
                                            DropdownMenuItem(text = { Text("Toggle Wi-Fi radios") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsLong = "2")); wpsLongExpanded = false })
                                            DropdownMenuItem(text = { Text("Run Ez-Buttons Script") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsLong = "3")); wpsLongExpanded = false })
                                            DropdownMenuItem(text = { Text("WPS Pairing") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsLong = "4")); wpsLongExpanded = false })
                                        }
                                    }
                                }
                            }

                            // Card 2: LED Events Mappings
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Hardware LED Power Mappings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // Switch for Enable Front LED?
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Front LED?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Global switch for all status lights", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ledEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ledEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.ledEnable) {
                                        // Internet LED
                                        Text("Front LED Internet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { ledInternetExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.ledInternet) {
                                                    "0" -> "Link/Act (Normal status)"
                                                    "1" -> "Power status (Constant on)"
                                                    "2" -> "Always off"
                                                    else -> "Link/Act (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = ledInternetExpanded, onDismissRequest = { ledInternetExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Link/Act (Normal status)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledInternet = "0")); ledInternetExpanded = false })
                                                DropdownMenuItem(text = { Text("Power status (Constant on)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledInternet = "1")); ledInternetExpanded = false })
                                                DropdownMenuItem(text = { Text("Always off") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledInternet = "2")); ledInternetExpanded = false })
                                            }
                                        }

                                        // USB LED
                                        Text("Front LED USB", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { ledUsbExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.ledUsb) {
                                                    "0" -> "Link/Act (Normal status)"
                                                    "1" -> "Mount status (Constant on)"
                                                    "2" -> "Always off"
                                                    else -> "Link/Act (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = ledUsbExpanded, onDismissRequest = { ledUsbExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Link/Act (Normal status)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledUsb = "0")); ledUsbExpanded = false })
                                                DropdownMenuItem(text = { Text("Mount status (Constant on)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledUsb = "1")); ledUsbExpanded = false })
                                                DropdownMenuItem(text = { Text("Always off") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledUsb = "2")); ledUsbExpanded = false })
                                            }
                                        }

                                        // Wireless LED
                                        Text("Front LED Wireless", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { ledWifiExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.ledWifi) {
                                                    "0" -> "Link/Act (Normal status)"
                                                    "1" -> "Radio status (Constant on)"
                                                    "2" -> "Always off"
                                                    else -> "Link/Act (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = ledWifiExpanded, onDismissRequest = { ledWifiExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Link/Act (Normal status)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledWifi = "0")); ledWifiExpanded = false })
                                                DropdownMenuItem(text = { Text("Radio status (Constant on)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledWifi = "1")); ledWifiExpanded = false })
                                                DropdownMenuItem(text = { Text("Always off") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledWifi = "2")); ledWifiExpanded = false })
                                            }
                                        }

                                        // Power LED
                                        Text("Front LED Power", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { ledPowerExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.ledPower) {
                                                    "0" -> "Power status (Normal)"
                                                    "1" -> "Heartbeat (Blinking)"
                                                    "2" -> "Always off"
                                                    else -> "Normal (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = ledPowerExpanded, onDismissRequest = { ledPowerExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Power status (Normal)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledPower = "0")); ledPowerExpanded = false })
                                                DropdownMenuItem(text = { Text("Heartbeat (Blinking)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledPower = "1")); ledPowerExpanded = false })
                                                DropdownMenuItem(text = { Text("Always off") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledPower = "2")); ledPowerExpanded = false })
                                            }
                                        }

                                        // Ethernet Ports LED
                                        Text("Ethernet Ports Green LED", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { ledEthernetExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.ledEthernet) {
                                                    "0" -> "Link/Act (Normal status)"
                                                    "1" -> "Gigabit Link status speed"
                                                    "2" -> "Always off"
                                                    else -> "Link/Act (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = ledEthernetExpanded, onDismissRequest = { ledEthernetExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Link/Act (Normal status)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledEthernet = "0")); ledEthernetExpanded = false })
                                                DropdownMenuItem(text = { Text("Gigabit Link status speed") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledEthernet = "1")); ledEthernetExpanded = false })
                                                DropdownMenuItem(text = { Text("Always off") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledEthernet = "2")); ledEthernetExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Button (visible for forms that are persistent)
                    if (section != "Firmware" && section != "Backup") {
                        Button(
                            onClick = { saveConfig() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Apply Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm NVRAM Factory Reset ⚠️", fontWeight = FontWeight.Bold) },
            text = { Text("Are you absolutely sure you want to restore the router to default factory settings? All configurations, Wi-Fi networks, passwords, and custom scripts will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { triggerReset() }) {
                    Text("YES, RESET", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showStorageResetDialog) {
        AlertDialog(
            onDismissRequest = { showStorageResetDialog = false },
            title = { Text("Confirm Storage Factory Reset ⚠️", fontWeight = FontWeight.Bold) },
            text = { Text("Are you absolutely sure you want to restore the internal storage (/etc/storage) to default factory settings? This will clear all local script profiles, certs, and configurations stored inside internal flash.") },
            confirmButton = {
                TextButton(onClick = { triggerStorageReset() }) {
                    Text("YES, RESET STORAGE", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageResetDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss during active upload */ },
            title = { Text(uploadStatus, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        color = ArcherTeal,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${(uploadProgress * 100).toInt()}% uploaded", fontWeight = FontWeight.Bold, color = ArcherTeal)
                }
            },
            confirmButton = {}
        )
    }
}

// Helper methods for file uploads
fun getFileNameHelper(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) result = cursor.getString(idx)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

fun uriToMultipartBodyPartHelper(context: android.content.Context, uri: android.net.Uri, partName: String): okhttp3.MultipartBody.Part? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()
        val filename = getFileNameHelper(context, uri) ?: "upload.bin"
        val requestBody = okhttp3.RequestBody.create(
            "application/octet-stream".toMediaTypeOrNull(),
            bytes
        )
        okhttp3.MultipartBody.Part.createFormData(partName, filename, requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
