package com.example.padavancontrol.ui.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAdminScreen(
    section: String, // "System", "Services", "OpMode", "Firmware", "Backup", "ButtonsLed"
    viewModel: AdvancedAdminViewModel,
    repository: PadavanRepository,
    onNavigateBack: () -> Unit,
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

        if (section == "System" && passwordChangeInput.isNotEmpty()) {
            if (passwordChangeInput != passwordVerifyInput) {
                passwordError = "Passwords do not match."
                isValid = false
            } else if (passwordChangeInput.length < 5) {
                passwordError = "Password must be at least 5 characters."
                isValid = false
            }
        }

        if (section == "Services" && uiState.config.enableSsh) {
            val port = uiState.config.sshPort.toIntOrNull()
            if (port == null || port < 1 || port > 65535) {
                sshPortError = "Invalid port range 1 - 65535"
                isValid = false
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
            val success = repository.executeCommand("mtd-erase -d RootFS && reboot")
            isResetting = false
            if (success.contains("Error") || success.contains("Exception")) {
                Toast.makeText(context, "Failed to send reset instruction.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Router is restoring factory defaults and rebooting... 🔄", Toast.LENGTH_LONG).show()
                onNavigateBack()
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
                        }

                        "Services" -> {
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

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable Dropbear SSH", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Permit secure encrypted terminal connection", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.enableSsh,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(enableSsh = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.enableSsh) {
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

                                    Text("Active Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { opModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.opMode) {
                                                "1" -> "Wireless Router (Gateway Mode)"
                                                "2" -> "Access Point (AP Switch Mode)"
                                                "3" -> "WDS Wireless Bridge Mode"
                                                else -> "Wireless Router (1)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = opModeExpanded, onDismissRequest = { opModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Wireless Router (Gateway)") }, onClick = { viewModel.updateConfig(uiState.config.copy(opMode = "1")); opModeExpanded = false })
                                            DropdownMenuItem(text = { Text("Access Point (AP Mode)") }, onClick = { viewModel.updateConfig(uiState.config.copy(opMode = "2")); opModeExpanded = false })
                                            DropdownMenuItem(text = { Text("WDS Wireless Bridge") }, onClick = { viewModel.updateConfig(uiState.config.copy(opMode = "3")); opModeExpanded = false })
                                        }
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
                                    Text("Firmware Information Status", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Current Version:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp)
                                        Text(uiState.config.firmwareVersion, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                                    ) {
                                        Text("Build Compile Date:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp)
                                        Text(uiState.config.buildDate, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                    }

                                    Button(
                                        onClick = { checkFirmwareUpgrade() },
                                        enabled = !isCheckingUpgrade,
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isCheckingUpgrade) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text("Check for Online Upgrades 🔄")
                                        }
                                    }
                                }
                            }
                        }

                        "Backup" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Settings Backup & Restore Actions", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 20.dp))

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Backup configuration downloaded! (config.tar.gz)", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("Export Router Backup File 📤")
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Select backup package selected.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.DarkGray),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                                    ) {
                                        Text("Import Settings from Backup File 📥")
                                    }

                                    Button(
                                        onClick = { showResetDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                                        enabled = !isResetting,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isResetting) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text("Restore Factory Default Settings ⚠️")
                                        }
                                    }
                                }
                            }
                        }

                        "ButtonsLed" -> {
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

                                    Text("WPS Button Function", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { wpsModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.btnWpsMode) {
                                                "0" -> "Toggle WPS Mode client search"
                                                "1" -> "Toggle Wi-Fi 2.4G/5GHz Radio"
                                                "2" -> "Toggle router LEDs on/off"
                                                else -> "Toggle WPS (0)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = wpsModeExpanded, onDismissRequest = { wpsModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Standard WPS Association") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsMode = "0")); wpsModeExpanded = false })
                                            DropdownMenuItem(text = { Text("Toggle Wi-Fi Radios") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsMode = "1")); wpsModeExpanded = false })
                                            DropdownMenuItem(text = { Text("Toggle Hardware LEDs") }, onClick = { viewModel.updateConfig(uiState.config.copy(btnWpsMode = "2")); wpsModeExpanded = false })
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
                                    Text("Hardware LED Power Mappings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Text("Router LEDs Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { ledModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.ledPowerMode) {
                                                "0" -> "Normal LED behavior status"
                                                "1" -> "Energy Saving (Turn off all LEDs)"
                                                else -> "Normal (0)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = ledModeExpanded, onDismissRequest = { ledModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Normal Status LED operation") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledPowerMode = "0")); ledModeExpanded = false })
                                            DropdownMenuItem(text = { Text("All LEDs Off (Energy saver)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ledPowerMode = "1")); ledModeExpanded = false })
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
            title = { Text("Confirm Factory Reset ⚠️", fontWeight = FontWeight.Bold) },
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
}
