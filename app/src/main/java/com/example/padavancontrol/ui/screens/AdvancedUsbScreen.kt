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
import com.example.padavancontrol.ui.viewmodels.AdvancedUsbViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.UsbShareConfig
import com.example.padavancontrol.theme.ArcherTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedUsbScreen(
    section: String, // "Common", "Samba", "FTP", "Modem", "Printer"
    viewModel: AdvancedUsbViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            "Common" -> "Advanced_AiDisk_others.asp"
            "Samba" -> "Advanced_AiDisk_samba.asp"
            "FTP" -> "Advanced_AiDisk_ftp.asp"
            "Modem" -> "Advanced_Modem_others.asp"
            "Printer" -> "Advanced_Printer_others.asp"
            else -> "Advanced_AiDisk_others.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            "Common" -> "USB - Common Settings"
            "Samba" -> "USB - Samba Server Share"
            "FTP" -> "USB - FTP Server Share"
            "Modem" -> "USB - 3G/4G USB Modem"
            "Printer" -> "USB - LPR Printer Share"
            else -> "USB Applications"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Dropdown selectors states
    var spinExpanded by remember { mutableStateOf(false) }
    var optwExpanded by remember { mutableStateOf(false) }
    var sambaModeExpanded by remember { mutableStateOf(false) }
    var ftpModeExpanded by remember { mutableStateOf(false) }
    var rawdExpanded by remember { mutableStateOf(false) }
    var modemTypeExpanded by remember { mutableStateOf(false) }
    var modemNetsExpanded by remember { mutableStateOf(false) }
    var modemNodeExpanded by remember { mutableStateOf(false) }

    // Password visibility state
    var showModemPassword by remember { mutableStateOf(false) }

    // Validation errors state
    var maxUserError by remember { mutableStateOf<String?>(null) }
    var ftpPminError by remember { mutableStateOf<String?>(null) }
    var ftpPmaxError by remember { mutableStateOf<String?>(null) }
    var modemMtuError by remember { mutableStateOf<String?>(null) }

    fun loadConfig() {
        viewModel.loadConfig(pagePath)
    }

    LaunchedEffect(key1 = section) {
        loadConfig()
    }

    fun validateInputs(): Boolean {
        var isValid = true
        maxUserError = null
        ftpPminError = null
        ftpPmaxError = null
        modemMtuError = null

        if (section == "Common") {
            val maxUsr = uiState.config.stMaxUser.toIntOrNull()
            if (maxUsr == null || maxUsr < 1 || maxUsr > 50) {
                maxUserError = "Max users must be between 1 and 50"
                isValid = false
            }
        }

        if (section == "FTP" && uiState.config.enableFtp) {
            val pmin = uiState.config.ftpPmin.toIntOrNull()
            val pmax = uiState.config.ftpPmax.toIntOrNull()
            if (pmin == null || pmin < 1024 || pmin > 65535) {
                ftpPminError = "Min port range is 1024 - 65535"
                isValid = false
            }
            if (pmax == null || pmax < 1024 || pmax > 65535 || (pmin != null && pmax < pmin)) {
                ftpPmaxError = "Max port must be valid and higher than min port"
                isValid = false
            }
        }

        if (section == "Modem" && uiState.config.modemRule) {
            if (uiState.config.modemMtu < 576 || uiState.config.modemMtu > 1500) {
                modemMtuError = "MTU size must be between 576 and 1500 bytes"
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
        viewModel.saveConfig(pagePath)
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
                        text = uiState.loadStatus.ifEmpty { "Fetching USB configuration..." },
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
                    // Category Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = ArcherTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "USB Device Services Portal",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Administer your router USB connected peripherals, network storage nodes, FTP shares, NDIS LTE modems, and raw LPR network printing.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Render sections
                    when (section) {
                        "Common" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("USB Hub Physical Hardware Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(0.8f)) {
                                            Text("Force USB 2.0 (Disable USB 3.0)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Solves Wi-Fi 2.4GHz interference due to USB 3.0 shielding leaks", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.usb3Disable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(usb3Disable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    // HDD Spindown dropdown
                                    Text("HDD Standby Spindown Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { spinExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.hddSpindown) {
                                                "0" -> "Disabled"
                                                "300" -> "5 Minutes"
                                                "600" -> "10 Minutes"
                                                "900" -> "15 Minutes"
                                                "1800" -> "30 Minutes"
                                                "3600" -> "1 Hour"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = spinExpanded, onDismissRequest = { spinExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled / Keep Active") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddSpindown = "0")); spinExpanded = false })
                                            DropdownMenuItem(text = { Text("5 Minutes") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddSpindown = "300")); spinExpanded = false })
                                            DropdownMenuItem(text = { Text("10 Minutes") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddSpindown = "600")); spinExpanded = false })
                                            DropdownMenuItem(text = { Text("15 Minutes") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddSpindown = "900")); spinExpanded = false })
                                            DropdownMenuItem(text = { Text("30 Minutes") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddSpindown = "1800")); spinExpanded = false })
                                            DropdownMenuItem(text = { Text("1 Hour") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddSpindown = "3600")); spinExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Disable HDD APM Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Overrides strict storage sleep settings", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.hddApmOff,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(hddApmOff = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Automated Storage Integrity Check", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Runs fsck tool automatically at dynamic mount", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.achkEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(achkEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
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
                                    Text("Entware & Memory Mounts", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // Optware/Entware mount
                                    Text("External Package Partition Mount", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { optwExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.optwEnable) {
                                                "0" -> "Disabled"
                                                "1" -> "Optware Subsystem"
                                                "2" -> "Entware Subsystem"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = optwExpanded, onDismissRequest = { optwExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(optwEnable = "0")); optwExpanded = false })
                                            DropdownMenuItem(text = { Text("Optware Partition Mount (/opt)") }, onClick = { viewModel.updateConfig(uiState.config.copy(optwEnable = "1")); optwExpanded = false })
                                            DropdownMenuItem(text = { Text("Entware Package Subsystem (Recommended)") }, onClick = { viewModel.updateConfig(uiState.config.copy(optwEnable = "2")); optwExpanded = false })
                                        }
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.stMaxUser,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(stMaxUser = it)) },
                                        label = { Text("Max Concurrent Connections Limit") },
                                        isError = maxUserError != null,
                                        supportingText = maxUserError?.let { { Text(it) } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "Samba" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Samba Storage Server Setting", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Samba LAN Server", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Windows Network Neighborhood Share (SMB)", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.enableSamba,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(enableSamba = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.sambaWorkgroup,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(sambaWorkgroup = it)) },
                                        label = { Text("SMB Workgroup Name") },
                                        placeholder = { Text("WORKGROUP") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    // Samba protocol security mode
                                    Text("Samba Protocols Security Level", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { sambaModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.sambaMode) {
                                                "0" -> "Anonymous Share (All permitted)"
                                                "1" -> "User Authentication Mode"
                                                else -> "User Authentication Mode (1)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = sambaModeExpanded, onDismissRequest = { sambaModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Anonymous Mode (No accounts required)") }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaMode = "0")); sambaModeExpanded = false })
                                            DropdownMenuItem(text = { Text("User Mode (Authenticated accounts logins)") }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaMode = "1")); sambaModeExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Samba Local Master Browser", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Acts as active LAN domain resolver node", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.sambaLmb == "1",
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(sambaLmb = if (it) "1" else "0")) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Force Fast Path IO Acceleration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Maximizes read/write transfer throughput", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.sambaFp == "1",
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(sambaFp = if (it) "1" else "0")) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }

                        "FTP" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("FTP Server Sharing Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable FTP Share Service", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Enables vsftpd active daemon", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.enableFtp,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(enableFtp = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Text("FTP Authentication Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { ftpModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.ftpMode) {
                                                "0" -> "Anonymous Login Mode"
                                                "1" -> "Accounts Authentication Mode"
                                                else -> "Accounts Mode (1)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = ftpModeExpanded, onDismissRequest = { ftpModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Anonymous Mode (Read access)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ftpMode = "0")); ftpModeExpanded = false })
                                            DropdownMenuItem(text = { Text("User Mode (Authenticated admin logins)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ftpMode = "1")); ftpModeExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable FTP Log Tracker", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Records transaction activity into system log", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ftpLog,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ftpLog = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.ftpPmin,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(ftpPmin = it)) },
                                        label = { Text("Min Passive Port") },
                                        isError = ftpPminError != null,
                                        supportingText = ftpPminError?.let { { Text(it) } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.ftpPmax,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(ftpPmax = it)) },
                                        label = { Text("Max Passive Port") },
                                        isError = ftpPmaxError != null,
                                        supportingText = ftpPmaxError?.let { { Text(it) } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.ftpAnmr,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(ftpAnmr = it)) },
                                        label = { Text("Anonymous Download Speed Limit (KB/s)") },
                                        placeholder = { Text("0 (Unlimited)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "Modem" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("3G/4G USB Modem Dial Connection", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable USB Modem Interface", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Triggers modem fallback connection", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.modemRule,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(modemRule = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.modemRule) {
                                        // Modem Type dropdown
                                        Text("Modem Driver Protocol Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { modemTypeExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.modemType) {
                                                    "0" -> "Legacy PPP Dial Mode"
                                                    "1" -> "NDIS QMI HSPA"
                                                    "3" -> "NDIS LTE (High Speed)"
                                                    else -> "NDIS Mode (3)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = modemTypeExpanded, onDismissRequest = { modemTypeExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Legacy Dial Mode (PPP)") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemType = "0")); modemTypeExpanded = false })
                                                DropdownMenuItem(text = { Text("NDIS QMI HSPA driver") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemType = "1")); modemTypeExpanded = false })
                                                DropdownMenuItem(text = { Text("NDIS Direct LTE driver (4G speed)") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemType = "3")); modemTypeExpanded = false })
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.config.modemCountry,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemCountry = it)) },
                                            label = { Text("ISP Region / Country") },
                                            placeholder = { Text("China") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.modemIsp,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemIsp = it)) },
                                            label = { Text("ISP Operator / Carrier Name") },
                                            placeholder = { Text("China Telecom") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.modemApn,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemApn = it)) },
                                            label = { Text("APN Access Point Name") },
                                            placeholder = { Text("ctnet") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.modemPin,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemPin = it)) },
                                            label = { Text("SIM Card PIN Code (If locked)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.modemDialnum,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemDialnum = it)) },
                                            label = { Text("Dial Number String") },
                                            placeholder = { Text("*99#") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.modemUser,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemUser = it)) },
                                            label = { Text("Dial Username (If required)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.modemPass,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemPass = it)) },
                                            label = { Text("Dial Password") },
                                            visualTransformation = if (showModemPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                val text = if (showModemPassword) "Hide" else "Show"
                                                Text(
                                                    text = text,
                                                    color = ArcherTeal,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.clickable { showModemPassword = !showModemPassword }
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        // Network Mode dropdown
                                        Text("Network Generation Modes Selection", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { modemNetsExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.modemNets) {
                                                    "0" -> "Automatic selection"
                                                    "1" -> "4G LTE Only"
                                                    "2" -> "3G Only"
                                                    else -> "Automatic"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = modemNetsExpanded, onDismissRequest = { modemNetsExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Automatic Selection") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemNets = "0")); modemNetsExpanded = false })
                                                DropdownMenuItem(text = { Text("4G LTE Only") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemNets = "1")); modemNetsExpanded = false })
                                                DropdownMenuItem(text = { Text("3G WCDMA/CDMA Only") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemNets = "2")); modemNetsExpanded = false })
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.config.modemMtu.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemMtu = it.toIntOrNull() ?: 1500)) },
                                            label = { Text("Modem Connection MTU size (bytes)") },
                                            isError = modemMtuError != null,
                                            supportingText = modemMtuError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Column {
                                                Text("Obtain DNS Automatically", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Pulls DNS parameters from carrier dial lease", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.modemDnsAuto,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(modemDnsAuto = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }

                                        if (!uiState.config.modemDnsAuto) {
                                            OutlinedTextField(
                                                value = uiState.config.wanDns1,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(wanDns1 = it)) },
                                                label = { Text("Override DNS Server 1") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            )

                                            OutlinedTextField(
                                                value = uiState.config.wanDns2,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(wanDns2 = it)) },
                                                label = { Text("Override DNS Server 2") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            )

                                            OutlinedTextField(
                                                value = uiState.config.wanDns3,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(wanDns3 = it)) },
                                                label = { Text("Override DNS Server 3") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                            )
                                        }

                                        // Modem USB port Node dropdown
                                        Text("USB Port Node Selector", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { modemNodeExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.modemNode) {
                                                    "0" -> "Automatic Selection"
                                                    "1" -> "Upper USB Port (Node 1)"
                                                    "2" -> "Lower USB Port (Node 2)"
                                                    else -> "Automatic"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = modemNodeExpanded, onDismissRequest = { modemNodeExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Automatic Selection") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemNode = "0")); modemNodeExpanded = false })
                                                DropdownMenuItem(text = { Text("Upper USB Port (Node 1)") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemNode = "1")); modemNodeExpanded = false })
                                                DropdownMenuItem(text = { Text("Lower USB Port (Node 2)") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemNode = "2")); modemNodeExpanded = false })
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.config.modemCmd,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(modemCmd = it)) },
                                            label = { Text("Custom AT Initialisation Command") },
                                            placeholder = { Text("AT+CGDCONT=1,\"IP\",\"apn\"") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text("Bypass ZeroCD USB Storage", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Force eject virtual CD drive to reveal modem interface", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.modemZcd == "1",
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(modemZcd = if (it) "1" else "0")) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "Printer" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("LPR / RAW Network Printer Sharing", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // rawd dropdown
                                    Text("RAW Print Daemon", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { rawdExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.rawdEnable) {
                                                "0" -> "Disabled"
                                                "1" -> "RAW TCP server enabled"
                                                "2" -> "RAW Bidirectional connection enabled"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = rawdExpanded, onDismissRequest = { rawdExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(rawdEnable = "0")); rawdExpanded = false })
                                            DropdownMenuItem(text = { Text("RAW Server (JetDirect Port 9100)") }, onClick = { viewModel.updateConfig(uiState.config.copy(rawdEnable = "1")); rawdExpanded = false })
                                            DropdownMenuItem(text = { Text("RAW Bidirectional Server Mode") }, onClick = { viewModel.updateConfig(uiState.config.copy(rawdEnable = "2")); rawdExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable LPR/LPD Spool Daemon", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Standard Unix Line Printer Remote Daemon", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.lprdEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(lprdEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Enable ASUS U2EC Utility Connector", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Connector software for ASUS specialized print utilities", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.u2ecEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(u2ecEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Button
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
