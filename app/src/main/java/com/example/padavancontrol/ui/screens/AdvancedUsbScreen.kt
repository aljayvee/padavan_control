package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.ShareNode
import com.example.padavancontrol.data.models.UsbShareConfig
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.AdvancedUsbViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedUsbScreen(
    section: String, // "Common", "Samba", "FTP", "Modem", "Printer", "Transmission", "Aria2"
    viewModel: AdvancedUsbViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagePath = remember(section) {
        when (section) {
            "Common" -> "Advanced_AiDisk_others.asp"
            "Transmission" -> "Advanced_AiDisk_others.asp"
            "Aria2" -> "Advanced_AiDisk_others.asp"
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
            "Transmission" -> "USB - Torrent Transmission"
            "Aria2" -> "USB - Aria2 Downloader"
            "Samba" -> "USB - Samba Server Share"
            "FTP" -> "USB - FTP Server Share"
            "Modem" -> "USB - 3G/4G USB Modem"
            "Printer" -> "USB - LPR Printer Share"
            else -> t("USB Applications")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Dropdown selectors states
    var usb3Expanded by remember { mutableStateOf(false) }
    var spinExpanded by remember { mutableStateOf(false) }
    var apmExpanded by remember { mutableStateOf(false) }
    var achkExpanded by remember { mutableStateOf(false) }
    var reclaimExpanded by remember { mutableStateOf(false) }
    var optwExpanded by remember { mutableStateOf(false) }
    
    var sambaModeExpanded by remember { mutableStateOf(false) }
    var sambaLmbExpanded by remember { mutableStateOf(false) }
    var sambaFpExpanded by remember { mutableStateOf(false) }
    
    var ftpModeExpanded by remember { mutableStateOf(false) }
    var ftpLogExpanded by remember { mutableStateOf(false) }
    
    var rawdExpanded by remember { mutableStateOf(false) }
    
    var modemTypeExpanded by remember { mutableStateOf(false) }
    var modemCountryExpanded by remember { mutableStateOf(false) }
    var modemIspExpanded by remember { mutableStateOf(false) }
    var modemNetsExpanded by remember { mutableStateOf(false) }
    var modemNodeExpanded by remember { mutableStateOf(false) }
    var modemZcdExpanded by remember { mutableStateOf(false) }

    // Password visibility state
    var showModemPassword by remember { mutableStateOf(false) }

    // Validation errors state
    var maxUserError by remember { mutableStateOf<String?>(null) }
    var ftpPminError by remember { mutableStateOf<String?>(null) }
    var ftpPmaxError by remember { mutableStateOf<String?>(null) }
    var modemMtuError by remember { mutableStateOf<String?>(null) }

    // Directory Tree States
    val expandedNodes = remember { mutableStateMapOf<String, List<ShareNode>>() }
    val loadingNodes = remember { mutableStateMapOf<String, Boolean>() }
    var selectedNode by remember { mutableStateOf<ShareNode?>(null) }
    var selectedPermission by remember { mutableStateOf("3") }
    var selectedAccount by remember { mutableStateOf("") }

    // Account CRUD states
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf("") }
    var accountToDelete by remember { mutableStateOf("") }

    var accountNameInput by remember { mutableStateOf("") }
    var accountPassInput by remember { mutableStateOf("") }
    var accountConfirmPassInput by remember { mutableStateOf("") }

    // Folder CRUD states
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf("") }
    var folderNameInput by remember { mutableStateOf("") }

    fun loadConfig() {
        viewModel.loadConfig(pagePath)
    }

    LaunchedEffect(section) {
        loadConfig()
        // Reset dynamic tree state when switching page
        expandedNodes.clear()
        loadingNodes.clear()
        selectedNode = null
        selectedAccount = ""
    }

    // Auto select first account if available
    LaunchedEffect(uiState.config.accounts, selectedAccount) {
        if (selectedAccount.isEmpty() && uiState.config.accounts.isNotEmpty()) {
            selectedAccount = uiState.config.accounts.first()
        }
    }

    // Load root directory tree nodes if section is Samba or FTP
    LaunchedEffect(section, uiState.isLoading) {
        if ((section == "Samba" || section == "FTP") && !uiState.isLoading && !expandedNodes.containsKey("0")) {
            loadingNodes["0"] = true
            viewModel.fetchShareTree("0") { nodes ->
                expandedNodes["0"] = nodes
                loadingNodes["0"] = false
            }
        }
    }

    fun toggleExpand(node: ShareNode) {
        if (expandedNodes.containsKey(node.id)) {
            expandedNodes.remove(node.id)
        } else {
            loadingNodes[node.id] = true
            viewModel.fetchShareTree(node.id) { subNodes ->
                expandedNodes[node.id] = subNodes
                loadingNodes[node.id] = false
            }
        }
    }

    val nodeDepth = selectedNode?.let { get_layer(it.id.substring(1)) } ?: 0
    val selectedPool = remember(selectedNode) {
        selectedNode?.let {
            val parts = it.id.split("_")
            parts.getOrNull(2) ?: ""
        } ?: ""
    }
    val selectedFolder = remember(selectedNode) {
        selectedNode?.let {
            val parts = it.id.split("_")
            parts.getOrNull(3) ?: ""
        } ?: ""
    }

    // Prefill permissions when folder or account selection updates
    val protocol = if (section == "Samba") "cifs" else "ftp"
    LaunchedEffect(selectedNode, selectedAccount) {
        if (nodeDepth == 3 && selectedAccount.isNotEmpty()) {
            val accountPerms = uiState.config.permissions[selectedAccount] ?: emptyList()
            val permObj = accountPerms.find { it.poolName == selectedPool && it.folderName == selectedFolder }
            selectedPermission = if (protocol == "cifs") {
                permObj?.cifsPermission ?: "3"
            } else {
                permObj?.ftpPermission ?: "3"
            }
        }
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
            Toast.makeText(context, t("Please fix form validation errors first."), Toast.LENGTH_SHORT).show()
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
                            contentDescription = t("Back"),
                            tint = ArcherTeal
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.isSaving) {
                        IconButton(onClick = { loadConfig() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = t("Reload"), tint = ArcherTeal)
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
                        onClick = { loadConfig() },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                    ) {
                        Text("Retry")
                    }
                }
            } else if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        color = ArcherTeal,
                        trackColor = Color(0xFFE0F2F1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                    // Portal Info Card
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

                                    // usb3Disable dropdown
                                    Text("Force USB 2.0 (Disable USB 3.0) *need reboot*", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { usb3Expanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (uiState.config.usb3Disable) "Yes (USB 2.0 Only)" else "No (USB 3.0 Enabled)", color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = usb3Expanded, onDismissRequest = { usb3Expanded = false }) {
                                            DropdownMenuItem(text = { Text("No (USB 3.0 Enabled)") }, onClick = { viewModel.updateConfig(uiState.config.copy(usb3Disable = false)); usb3Expanded = false })
                                            DropdownMenuItem(text = { Text("Yes (USB 2.0 Only)") }, onClick = { viewModel.updateConfig(uiState.config.copy(usb3Disable = true)); usb3Expanded = false })
                                        }
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
                                                "0" -> t("Disabled")
                                                "300" -> "5 Minutes"
                                                "600" -> "10 Minutes"
                                                "900" -> "15 Minutes"
                                                "1800" -> "30 Minutes"
                                                "3600" -> "1 Hour"
                                                else -> t("Disabled")
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

                                    // HDD Disable APM dropdown
                                    Text("HDD Disable APM (Head Parking)?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { apmExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (uiState.config.hddApmOff) "Yes (Disable APM)" else "No (Enable APM)", color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = apmExpanded, onDismissRequest = { apmExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No (Enable APM)") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddApmOff = false)); apmExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes (Disable APM)") }, onClick = { viewModel.updateConfig(uiState.config.copy(hddApmOff = true)); apmExpanded = false })
                                        }
                                    }

                                    // Auto Check Filesystem dropdown
                                    Text("Auto Check Filesystem on Storage Plug-In?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { achkExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (uiState.config.achkEnable) "Yes (Check filesystems)" else "No (Skip check)", color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = achkExpanded, onDismissRequest = { achkExpanded = false }) {
                                            DropdownMenuItem(text = { Text("No (Skip check)") }, onClick = { viewModel.updateConfig(uiState.config.copy(achkEnable = false)); achkExpanded = false })
                                            DropdownMenuItem(text = { Text("Yes (Check filesystems)") }, onClick = { viewModel.updateConfig(uiState.config.copy(achkEnable = true)); achkExpanded = false })
                                        }
                                    }

                                    // Automatic I/O Caches Reclaim dropdown
                                    Text("Automatic I/O RAM Caches Reclaim", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { reclaimExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.pcacheReclaim) {
                                                "0" -> t("Disabled")
                                                "1" -> "70% RAM"
                                                "2" -> "50% RAM"
                                                "3" -> "30% RAM"
                                                "4" -> "15% RAM"
                                                else -> t("Disabled")
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = reclaimExpanded, onDismissRequest = { reclaimExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("Disabled")) }, onClick = { viewModel.updateConfig(uiState.config.copy(pcacheReclaim = "0")); reclaimExpanded = false })
                                            DropdownMenuItem(text = { Text("70% RAM") }, onClick = { viewModel.updateConfig(uiState.config.copy(pcacheReclaim = "1")); reclaimExpanded = false })
                                            DropdownMenuItem(text = { Text("50% RAM") }, onClick = { viewModel.updateConfig(uiState.config.copy(pcacheReclaim = "2")); reclaimExpanded = false })
                                            DropdownMenuItem(text = { Text("30% RAM") }, onClick = { viewModel.updateConfig(uiState.config.copy(pcacheReclaim = "3")); reclaimExpanded = false })
                                            DropdownMenuItem(text = { Text("15% RAM") }, onClick = { viewModel.updateConfig(uiState.config.copy(pcacheReclaim = "4")); reclaimExpanded = false })
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
                                    Text("Entware & Memory Mounts", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // Optware/Entware mount dropdown
                                    Text("External Package Partition Mount (Optware)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
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
                                                "0" -> t("Disabled")
                                                "1" -> "Optware Subsystem"
                                                "2" -> "Entware Subsystem"
                                                else -> t("Disabled")
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = optwExpanded, onDismissRequest = { optwExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("Disabled")) }, onClick = { viewModel.updateConfig(uiState.config.copy(optwEnable = "0")); optwExpanded = false })
                                            DropdownMenuItem(text = { Text("Optware Partition Mount (/opt)") }, onClick = { viewModel.updateConfig(uiState.config.copy(optwEnable = "1")); optwExpanded = false })
                                            DropdownMenuItem(text = { Text("Entware Package Subsystem") }, onClick = { viewModel.updateConfig(uiState.config.copy(optwEnable = "2")); optwExpanded = false })
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

                        "Transmission" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Torrent Transmission BitTorrent Client", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Transmission BitTorrent Client", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Starts background transmission-daemon", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.trmdEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(trmdEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.trmdEnable) {
                                        OutlinedTextField(
                                            value = uiState.config.trmdPport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(trmdPport = it)) },
                                            label = { Text("Peer Port for Incoming Connections") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.trmdRport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(trmdRport = it)) },
                                            label = { Text("RPC Web Control Interface Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        "Aria2" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Aria2 High-Speed Download Utility", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Aria2 Download Manager", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Starts high-speed aria2c background agent", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ariaEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ariaEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.ariaEnable) {
                                        OutlinedTextField(
                                            value = uiState.config.ariaPport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ariaPport = it)) },
                                            label = { Text("Peer Port for Incoming Connections") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.ariaRport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ariaRport = it)) },
                                            label = { Text("RPC Control Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
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
                                                else -> "User Authentication Mode"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = sambaModeExpanded, onDismissRequest = { sambaModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Anonymous Mode (No accounts required)") }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaMode = "0")); sambaModeExpanded = false })
                                            DropdownMenuItem(text = { Text("User Mode (Authenticated accounts logins)") }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaMode = "1")); sambaModeExpanded = false })
                                        }
                                    }

                                    // Samba Local Master Browser dropdown
                                    Text("Samba Local Master Browser", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { sambaLmbExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (uiState.config.sambaLmb == "1") t("Yes") else t("No"), color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = sambaLmbExpanded, onDismissRequest = { sambaLmbExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaLmb = "0")); sambaLmbExpanded = false })
                                            DropdownMenuItem(text = { Text(t("Yes")) }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaLmb = "1")); sambaLmbExpanded = false })
                                        }
                                    }

                                    // Samba Fastpath dropdown
                                    Text("Force Fast Path IO Acceleration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { sambaFpExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (uiState.config.sambaFp == "1") t("Yes") else t("No"), color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = sambaFpExpanded, onDismissRequest = { sambaFpExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaFp = "0")); sambaFpExpanded = false })
                                            DropdownMenuItem(text = { Text(t("Yes")) }, onClick = { viewModel.updateConfig(uiState.config.copy(sambaFp = "1")); sambaFpExpanded = false })
                                        }
                                    }
                                }
                            }

                            if (uiState.config.enableSamba) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (uiState.config.sambaMode == "1") {
                                    AccountManagerCard(
                                        uiState = uiState,
                                        selectedAccount = selectedAccount,
                                        onSelectAccount = { selectedAccount = it },
                                        onAddClick = {
                                            accountNameInput = ""
                                            accountPassInput = ""
                                            accountConfirmPassInput = ""
                                            showAddAccountDialog = true
                                        },
                                        onEditClick = { acc ->
                                            accountToEdit = acc
                                            accountNameInput = acc
                                            accountPassInput = ""
                                            accountConfirmPassInput = ""
                                            showEditAccountDialog = true
                                        },
                                        onDeleteClick = { acc ->
                                            accountToDelete = acc
                                            showDeleteAccountDialog = true
                                        }
                                    )
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                                    ) {
                                        Text(
                                            "Anonymous Mode: User account credentials are not required. Access control and folder permissions are bypassed.",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }

                                DirectoryBrowserCard(
                                    expandedNodes = expandedNodes,
                                    loadingNodes = loadingNodes,
                                    selectedNode = selectedNode,
                                    onToggleExpand = { toggleExpand(it) },
                                    onSelect = { selectedNode = it }
                                )

                                PermissionsGridCard(
                                    selectedNode = selectedNode,
                                    nodeDepth = nodeDepth,
                                    selectedPool = selectedPool,
                                    selectedFolder = selectedFolder,
                                    selectedAccount = if (uiState.config.sambaMode == "0") "anonymous" else selectedAccount,
                                    selectedPermission = selectedPermission,
                                    protocol = protocol,
                                    onPermissionChange = { selectedPermission = it },
                                    onApplyPermissions = {
                                        viewModel.setPermission(
                                            if (uiState.config.sambaMode == "0") "anonymous" else selectedAccount,
                                            selectedPool,
                                            selectedFolder,
                                            protocol,
                                            selectedPermission,
                                            pagePath
                                        )
                                    },
                                    onAddFolder = {
                                        folderNameInput = ""
                                        showAddFolderDialog = true
                                    },
                                    onRenameFolder = {
                                        folderNameInput = selectedFolder
                                        folderToRename = selectedFolder
                                        showRenameFolderDialog = true
                                    },
                                    onDeleteFolder = {
                                        folderToDelete = selectedFolder
                                        showDeleteFolderDialog = true
                                    }
                                )
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
                                                else -> "Accounts Mode"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = ftpModeExpanded, onDismissRequest = { ftpModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Anonymous Mode (Read access)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ftpMode = "0")); ftpModeExpanded = false })
                                            DropdownMenuItem(text = { Text("User Mode (Authenticated admin logins)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ftpMode = "1")); ftpModeExpanded = false })
                                        }
                                    }

                                    // FTP Log Logger dropdown
                                    Text("Enable FTP Log Tracker", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { ftpLogExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (uiState.config.ftpLog) t("Yes") else t("No"), color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = ftpLogExpanded, onDismissRequest = { ftpLogExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ftpLog = false)); ftpLogExpanded = false })
                                            DropdownMenuItem(text = { Text(t("Yes")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ftpLog = true)); ftpLogExpanded = false })
                                        }
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

                            if (uiState.config.enableFtp) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
                                Spacer(modifier = Modifier.height(16.dp))

                                if (uiState.config.ftpMode == "1") {
                                    AccountManagerCard(
                                        uiState = uiState,
                                        selectedAccount = selectedAccount,
                                        onSelectAccount = { selectedAccount = it },
                                        onAddClick = {
                                            accountNameInput = ""
                                            accountPassInput = ""
                                            accountConfirmPassInput = ""
                                            showAddAccountDialog = true
                                        },
                                        onEditClick = { acc ->
                                            accountToEdit = acc
                                            accountNameInput = acc
                                            accountPassInput = ""
                                            accountConfirmPassInput = ""
                                            showEditAccountDialog = true
                                        },
                                        onDeleteClick = { acc ->
                                            accountToDelete = acc
                                            showDeleteAccountDialog = true
                                        }
                                    )
                                } else {
                                    // Anonymous mode: FTP sets anonymous as active account automatically
                                    LaunchedEffect(Unit) {
                                        selectedAccount = "anonymous"
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                                    ) {
                                        Text(
                                            "Anonymous FTP mode active. Directories shared are viewable by public guest anonymous accounts.",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }

                                DirectoryBrowserCard(
                                    expandedNodes = expandedNodes,
                                    loadingNodes = loadingNodes,
                                    selectedNode = selectedNode,
                                    onToggleExpand = { toggleExpand(it) },
                                    onSelect = { selectedNode = it }
                                )

                                PermissionsGridCard(
                                    selectedNode = selectedNode,
                                    nodeDepth = nodeDepth,
                                    selectedPool = selectedPool,
                                    selectedFolder = selectedFolder,
                                    selectedAccount = selectedAccount,
                                    selectedPermission = selectedPermission,
                                    protocol = protocol,
                                    onPermissionChange = { selectedPermission = it },
                                    onApplyPermissions = {
                                        viewModel.setPermission(
                                            selectedAccount,
                                            selectedPool,
                                            selectedFolder,
                                            protocol,
                                            selectedPermission,
                                            pagePath
                                        )
                                    },
                                    onAddFolder = {
                                        folderNameInput = ""
                                        showAddFolderDialog = true
                                    },
                                    onRenameFolder = {
                                        folderNameInput = selectedFolder
                                        folderToRename = selectedFolder
                                        showRenameFolderDialog = true
                                    },
                                    onDeleteFolder = {
                                        folderToDelete = selectedFolder
                                        showDeleteFolderDialog = true
                                    }
                                )
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

                                        // Location / Country Selector Dropdown
                                        val activeCountry = countryList.find { it.code == uiState.config.modemCountry } ?: countryList.last()
                                        Text("Location (ISP Country/Region)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { modemCountryExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(activeCountry.name, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = modemCountryExpanded, onDismissRequest = { modemCountryExpanded = false }) {
                                                countryList.forEach { country ->
                                                    DropdownMenuItem(
                                                        text = { Text(country.name) },
                                                        onClick = {
                                                            val firstIsp = country.isps.firstOrNull()
                                                            if (firstIsp != null) {
                                                                viewModel.updateConfig(uiState.config.copy(
                                                                    modemCountry = country.code,
                                                                    modemIsp = firstIsp.name,
                                                                    modemApn = firstIsp.apn,
                                                                    modemDialnum = firstIsp.dial,
                                                                    modemUser = firstIsp.user,
                                                                    modemPass = firstIsp.pass
                                                                ))
                                                            } else {
                                                                viewModel.updateConfig(uiState.config.copy(
                                                                    modemCountry = country.code,
                                                                    modemIsp = "",
                                                                    modemApn = "",
                                                                    modemDialnum = "",
                                                                    modemUser = "",
                                                                    modemPass = ""
                                                                ))
                                                            }
                                                            modemCountryExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // ISP Dropdown
                                        Text("ISP Carrier / Operator Selection", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { modemIspExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(uiState.config.modemIsp.ifEmpty { "other" }, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = modemIspExpanded, onDismissRequest = { modemIspExpanded = false }) {
                                                activeCountry.isps.forEach { isp ->
                                                    DropdownMenuItem(
                                                        text = { Text(isp.name) },
                                                        onClick = {
                                                            viewModel.updateConfig(uiState.config.copy(
                                                                modemIsp = isp.name,
                                                                modemApn = isp.apn,
                                                                modemDialnum = isp.dial,
                                                                modemUser = isp.user,
                                                                modemPass = isp.pass
                                                            ))
                                                            modemIspExpanded = false
                                                        }
                                                    )
                                                }
                                                // Always allow manual custom configurations
                                                DropdownMenuItem(
                                                    text = { Text("other (Manual config)") },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(
                                                            modemIsp = "",
                                                            modemApn = "",
                                                            modemDialnum = "*99#",
                                                            modemUser = "",
                                                            modemPass = ""
                                                        ))
                                                        modemIspExpanded = false
                                                    }
                                                )
                                            }
                                        }

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
                                                val text = if (showModemPassword) t("Hide") else t("Show")
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

                                        // ZeroCD switching mode dropdown
                                        Text("ZeroCD Switching Method", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { modemZcdExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.modemZcd) {
                                                    "0" -> "usb-modeswitch"
                                                    "1" -> "legacy eject"
                                                    else -> "usb-modeswitch"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = modemZcdExpanded, onDismissRequest = { modemZcdExpanded = false }) {
                                                DropdownMenuItem(text = { Text("usb-modeswitch") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemZcd = "0")); modemZcdExpanded = false })
                                                DropdownMenuItem(text = { Text("legacy eject") }, onClick = { viewModel.updateConfig(uiState.config.copy(modemZcd = "1")); modemZcdExpanded = false })
                                            }
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
                                    Text("RAW Print Daemon (JetDirect RAW Port)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
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
                                                "0" -> t("Disabled")
                                                "1" -> "RAW TCP server enabled"
                                                "2" -> "RAW Bidirectional connection enabled"
                                                else -> t("Disabled")
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = rawdExpanded, onDismissRequest = { rawdExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("Disabled")) }, onClick = { viewModel.updateConfig(uiState.config.copy(rawdEnable = "0")); rawdExpanded = false })
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
                                Icon(imageVector = Icons.Default.Check, contentDescription = t("Save"))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(t("Apply Settings"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Account CRUD dialogs
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Create User Account", color = ArcherTeal, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        label = { Text(t("Username")) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = accountPassInput,
                        onValueChange = { accountPassInput = it },
                        label = { Text(t("Password")) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = accountConfirmPassInput,
                        onValueChange = { accountConfirmPassInput = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountNameInput.isEmpty() || accountPassInput.isEmpty()) {
                            Toast.makeText(context, "Username and password cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (accountPassInput != accountConfirmPassInput) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.createAccount(accountNameInput, accountPassInput, pagePath)
                        showAddAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel", color = ArcherTeal)
                }
            }
        )
    }

    if (showEditAccountDialog) {
        AlertDialog(
            onDismissRequest = { showEditAccountDialog = false },
            title = { Text("Change Account Password", color = ArcherTeal, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Modifying password for account: $accountToEdit", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = accountPassInput,
                        onValueChange = { accountPassInput = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = accountConfirmPassInput,
                        onValueChange = { accountConfirmPassInput = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountPassInput.isEmpty()) {
                            Toast.makeText(context, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (accountPassInput != accountConfirmPassInput) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.modifyAccount(accountToEdit, accountToEdit, accountPassInput, pagePath)
                        showEditAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                ) {
                    Text(t("Save"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditAccountDialog = false }) {
                    Text("Cancel", color = ArcherTeal)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete User Account", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete user account '$accountToDelete'?", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(accountToDelete, pagePath)
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = ArcherTeal)
                }
            }
        )
    }

    // Folder CRUD dialogs
    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("Create Shared Folder", color = ArcherTeal, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Creating new folder under partition: $selectedPool", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text("Folder Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.trim().isEmpty()) {
                            Toast.makeText(context, "Folder name cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.createFolder(selectedPool, folderNameInput.trim(), pagePath)
                        showAddFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddFolderDialog = false }) {
                    Text("Cancel", color = ArcherTeal)
                }
            }
        )
    }

    if (showRenameFolderDialog) {
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Rename Shared Folder", color = ArcherTeal, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Renaming folder '$folderToRename' under partition: $selectedPool", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text("New Folder Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.trim().isEmpty()) {
                            Toast.makeText(context, "Folder name cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.modifyFolder(selectedPool, folderToRename, folderNameInput.trim(), pagePath)
                        showRenameFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancel", color = ArcherTeal)
                }
            }
        )
    }

    if (showDeleteFolderDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            title = { Text("Delete Shared Folder", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete shared folder '$folderToDelete' from partition '$selectedPool'? Warning: This will delete the shared folder configuration from Samba/FTP settings.", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(selectedPool, folderToDelete, pagePath)
                        showDeleteFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteFolderDialog = false }) {
                    Text("Cancel", color = ArcherTeal)
                }
            }
        )
    }
}

@Composable
private fun AccountManagerCard(
    uiState: com.example.padavancontrol.ui.viewmodels.AdvancedUsbUiState,
    selectedAccount: String,
    onSelectAccount: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("User Accounts", fontWeight = FontWeight.Bold, color = ArcherTeal, fontSize = 16.sp)
                IconButton(onClick = onAddClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account", tint = ArcherTeal)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.config.accounts.isEmpty()) {
                Text("No user accounts configured. Press '+' to add one.", color = Color.Gray, fontSize = 14.sp)
            } else {
                Column {
                    uiState.config.accounts.forEach { account ->
                        val isSelected = selectedAccount == account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ArcherTeal.copy(alpha = 0.1f) else Color.Transparent)
                                .border(1.dp, if (isSelected) ArcherTeal else Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                                .clickable { onSelectAccount(account) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) ArcherTeal else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = account,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ArcherTeal else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditClick(account) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Change Password", tint = ArcherTeal, modifier = Modifier.size(16.dp))
                                }
                                if (account != "admin") {
                                    IconButton(
                                        onClick = { onDeleteClick(account) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Account", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryBrowserCard(
    expandedNodes: Map<String, List<ShareNode>>,
    loadingNodes: Map<String, Boolean>,
    selectedNode: ShareNode?,
    onToggleExpand: (ShareNode) -> Unit,
    onSelect: (ShareNode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Directory Browser", fontWeight = FontWeight.Bold, color = ArcherTeal, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

            val rootNodes = expandedNodes["0"] ?: emptyList()
            if (rootNodes.isEmpty()) {
                val isLoadingRoot = loadingNodes["0"] ?: false
                if (isLoadingRoot) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ArcherTeal, modifier = Modifier.size(24.dp))
                    }
                } else {
                    Text("No connected storage units detected.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                Column {
                    rootNodes.forEach { node ->
                        FolderTreeNode(
                            node = node,
                            depth = 0,
                            expandedNodes = expandedNodes,
                            selectedNode = selectedNode,
                            loadingNodes = loadingNodes,
                            onToggleExpand = onToggleExpand,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTreeNode(
    node: ShareNode,
    depth: Int,
    expandedNodes: Map<String, List<ShareNode>>,
    selectedNode: ShareNode?,
    loadingNodes: Map<String, Boolean>,
    onToggleExpand: (ShareNode) -> Unit,
    onSelect: (ShareNode) -> Unit
) {
    val isExpanded = expandedNodes.containsKey(node.id)
    val isSelected = selectedNode?.id == node.id
    val isLoading = loadingNodes[node.id] ?: false

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) ArcherTeal.copy(alpha = 0.1f) else Color.Transparent)
                .clickable { onSelect(node) }
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width((depth * 12).dp))
            if (node.hasSub) {
                if (isLoading) {
                    CircularProgressIndicator(color = ArcherTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    IconButton(
                        onClick = { onToggleExpand(node) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand",
                            tint = ArcherTeal
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
            Icon(
                imageVector = when (depth) {
                    0 -> Icons.Default.Computer // disk
                    1 -> Icons.Default.Settings // partition / pool
                    else -> Icons.AutoMirrored.Filled.Article // shared folder
                },
                contentDescription = null,
                tint = if (isSelected) ArcherTeal else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ArcherTeal else Color.Black
            )
        }

        if (isExpanded) {
            val children = expandedNodes[node.id] ?: emptyList()
            children.forEach { child ->
                FolderTreeNode(
                    node = child,
                    depth = depth + 1,
                    expandedNodes = expandedNodes,
                    selectedNode = selectedNode,
                    loadingNodes = loadingNodes,
                    onToggleExpand = onToggleExpand,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun PermissionsGridCard(
    selectedNode: ShareNode?,
    nodeDepth: Int,
    selectedPool: String,
    selectedFolder: String,
    selectedAccount: String,
    selectedPermission: String,
    protocol: String,
    onPermissionChange: (String) -> Unit,
    onApplyPermissions: () -> Unit,
    onAddFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit
) {
    if (selectedNode == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Shared Directory Options", fontWeight = FontWeight.Bold, color = ArcherTeal, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

            if (nodeDepth == 2) {
                // Partition
                Text("Selected Partition: $selectedPool", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                Button(
                    onClick = onAddFolder,
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Folder")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Shared Folder")
                }
            } else if (nodeDepth == 3) {
                // Folder
                Text("Shared Folder: $selectedFolder", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Partition location: $selectedPool", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRenameFolder,
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rename", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDeleteFolder,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Folder Access Permissions ($selectedAccount)", fontWeight = FontWeight.Bold, color = ArcherTeal, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

                val permissionsOptions = if (protocol == "cifs") {
                    listOf(
                        Pair("3", "Read / Write"),
                        Pair("1", "Read Only"),
                        Pair("0", "No Access")
                    )
                } else {
                    listOf(
                        Pair("3", "Read / Write"),
                        Pair("2", "Write Only"),
                        Pair("1", "Read Only"),
                        Pair("0", "No Access")
                    )
                }

                Column {
                    permissionsOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPermissionChange(value) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPermission == value,
                                onClick = { onPermissionChange(value) },
                                colors = RadioButtonDefaults.colors(selectedColor = ArcherTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onApplyPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    enabled = selectedAccount.isNotEmpty()
                ) {
                    Text("Apply Permissions", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun get_layer(layerOrder: String): Int {
    return layerOrder.count { it == '_' }
}

// Modem location and ISP carrier database structures
private data class IspEntry(
    val name: String,
    val proto: String,
    val apn: String,
    val dial: String,
    val user: String,
    val pass: String
)

private data class CountryEntry(
    val name: String,
    val code: String,
    val isps: List<IspEntry>
)

private val countryList = listOf(
    CountryEntry("Australia", "AU", listOf(
        IspEntry("Telstra", "0", "telstra.internet", "*99#", "", ""),
        IspEntry("Optus", "0", "Internet", "*99#", "", ""),
        IspEntry("Bigpond", "0", "telstra.bigpond", "*99#", "", ""),
        IspEntry("Hutchison 3G", "0", "3netaccess", "*99***1#", "", ""),
        IspEntry("Vodafone", "0", "vfprepaymbb", "*99#", "", ""),
        IspEntry("iburst", "0", "internet", "*99***1#", "", ""),
        IspEntry("Dodo", "0", "DODOLNS1", "*99#", "", ""),
        IspEntry("Exetel", "0", "exetel1", "*99***1#", "", ""),
        IspEntry("Internode", "0", "splns333a1", "*99***1#", "", ""),
        IspEntry("Three", "0", "3netaccess", "*99***1#", "", ""),
        IspEntry("Three PrePaid", "0", "3services", "*99***1#", "", ""),
        IspEntry("TPG", "0", "internet", "*99***1#", "", ""),
        IspEntry("Virgin", "0", "VirginBroadband", "*99#", "", ""),
        IspEntry("A1", "0", "A1.net", "*99***1#", "ppp@a1plus.at", "ppp"),
        IspEntry("3", "0", "drei.at", "*99***1#", "", ""),
        IspEntry("Orange", "0", "web.one.at", "*99***1#", "web", "web"),
        IspEntry("T-Mobile Austria", "0", "gprsinternet", "*99***1#", "GPRS", ""),
        IspEntry("YESSS!", "0", "web.yesss.at", "*99***1#", "", "")
    )),
    CountryEntry("Belarus", "BY", listOf(
        IspEntry("MTS", "0", "mts", "*99#", "mts", "mts"),
        IspEntry("Velcom", "0", "vmi.velcom.by", "*99#", "", ""),
        IspEntry("Life", "0", "internet.life.com.by", "*99#", "", "")
    )),
    CountryEntry("Bosnia and Herzegovina", "BH", listOf(
        IspEntry("T3", "1", "", "#777", "t3net", "t3net")
    )),
    CountryEntry("Brazil", "BZ", listOf(
        IspEntry("Vivo", "0", "zap.vivo.com.br", "*99#", "vivo", "vivo"),
        IspEntry("Tim", "0", "tim.br", "*99#", "tim", "tim"),
        IspEntry("Oi", "0", "gprs.oi.com.br", "*99***1#", "oi", "oi"),
        IspEntry("Claro", "0", "bandalarga.claro.com.br", "*99***1#", "claro", "claro")
    )),
    CountryEntry("Bulgaria", "BUL", listOf(
        IspEntry("Globul", "0", "internet.globul.bg", "359891000", "globul", ""),
        IspEntry("Mtel", "0", "inet-gprs.mtel.bg", "359881000", "", ""),
        IspEntry("Vivacom", "0", "internet.vivatel.bg", "359871000", "vivatel", "vivatel")
    )),
    CountryEntry("Canada", "CA", listOf(
        IspEntry("Rogers", "0", "internet.com", "", "wapuser1", "wap")
    )),
    CountryEntry("China", "CN", listOf(
        IspEntry("China Unicom", "0", "3gnet", "*99#", "", ""),
        IspEntry("China Mobile", "2", "cmnet", "*99#", "net", "net"),
        IspEntry("China Telecom", "1", "ctnet", "#777", "card", "card")
    )),
    CountryEntry("Czech", "CZ", listOf(
        IspEntry("T-mobile", "0", "internet.t-mobile.cz", "*99#", "gprs", "gprs"),
        IspEntry("O2", "0", "internet", "*99#", "", ""),
        IspEntry("Vodafone", "0", "internet", "*99***1#", "", ""),
        IspEntry("Ufon", "1", "", "#777", "ufon", "ufon")
    )),
    CountryEntry("Denmark", "DK", listOf(
        IspEntry("3", "0", "data.tre.dk", "*99#", "", ""),
        IspEntry("TDC", "0", "internet", "*99#", "", ""),
        IspEntry("Orange", "0", "web.orange.dk", "*99#", "", ""),
        IspEntry("Telia", "0", "www.internet.mtelia.dk", "", "", ""),
        IspEntry("Telenor", "0", "Internet", "", "", ""),
        IspEntry("Tre", "0", "data.tre.dk", "", "", ""),
        IspEntry("Callme", "0", "websp", "", "", ""),
        IspEntry("Onfone", "0", "internet.sp.dk", "", "", ""),
        IspEntry("CBB", "0", "internet", "", "", ""),
        IspEntry("Oister", "0", "data.dk", "", "", ""),
        IspEntry("Bibob", "0", "internet.bibob.dk", "", "", ""),
        IspEntry("Happii mobil", "0", "internet", "", "", "")
    )),
    CountryEntry("Dominican Republic", "DR", listOf(
        IspEntry("Claro/Telmex", "0", "internet.ideasclaro.com.do", "*99#", "claro", "claro")
    )),
    CountryEntry("Egypt", "EG", listOf(
        IspEntry("Etisalat", "0", "internet", "*99***1#", "", "")
    )),
    CountryEntry("El Salvador", "SV", listOf(
        IspEntry("Claro/Telmex", "0", "internet.ideasclaro", "*99#", "", "")
    )),
    CountryEntry("Finland", "FI", listOf(
        IspEntry("DNA", "0", "internet", "*99#", "", ""),
        IspEntry("Saunalahti", "0", "internet.saunalahti", "*99#", "", ""),
        IspEntry("Elisa", "0", "internet", "*99#", "", ""),
        IspEntry("Sonera/TeleFinland", "0", "internet", "", "", ""),
        IspEntry("AinaCom/Sonera", "0", "internet", "", "", ""),
        IspEntry("AinaCom/DNA", "0", "internet.aina.fi", "", "", ""),
        IspEntry("TDC/DNA", "0", "inet.tdc.fi", "", "", ""),
        IspEntry("Dicame", "0", "internet.dicame.fi", "", "", "")
    )),
    CountryEntry("Germany", "DE", listOf(
        IspEntry("Vodafone", "0", "web.vodafone.de", "*99***1#", "vodafone", "nternet"),
        IspEntry("T-mobile", "0", "internet.t-mobile", "*99***1#", "tm", "tm"),
        IspEntry("E-Plus", "0", "internet.eplus.de", "*99***1#", "eplus", "gprs"),
        IspEntry("o2 Germany", "0", "internet", "*99***1#", "", ""),
        IspEntry("vistream", "0", "internet.vistream.net", "*99***1#", "WAP", "Vistream")
    )),
    CountryEntry("Hong Kong", "HK", listOf(
        IspEntry("SmarTone-Vodafone", "0", "internet", "*99***1#", "", ""),
        IspEntry("3 Hong Kong", "0", "mobile.three.com.hk", "*99***1#", "", ""),
        IspEntry("One2Free", "0", "internet", "*99***1#", "", ""),
        IspEntry("PCCW mobile", "0", "pccw", "*99#", "", ""),
        IspEntry("CSL", "0", "internet", "*99***1#", "", "")
    )),
    CountryEntry("India", "INDI", listOf(
        IspEntry("Reliance", "1", "reliance", "#777", "", ""),
        IspEntry("Tata", "1", "TATA", "#777", "internet", "internet"),
        IspEntry("MTS", "1", "", "#777", "internet@internet.mtsindia.in", "mts"),
        IspEntry("Airtel", "0", "airtelgprs.com", "*99#", "", ""),
        IspEntry("Idea", "0", "Internet", "*99#", "", ""),
        IspEntry("MTNL", "0", "gprsppsmum", "*99#", "mtnl", "mtnl123")
    )),
    CountryEntry("Indonesia", "IN", listOf(
        IspEntry("IM2", "0", "indosatm2", "*99#", "", ""),
        IspEntry("INDOSAT", "0", "indosat3g", "*99#", "indosat", "indosat"),
        IspEntry("XL", "0", "www.xlgprs.net", "*99#", "xlgprs", "proxl"),
        IspEntry("Telkomsel Flash", "0", "flash", "*99#", "", ""),
        IspEntry("3", "0", "3gprs", "*99***1#", "3gprs", "3gprs"),
        IspEntry("Axis", "0", "AXIS", "*99***1#", "axis", "123456"),
        IspEntry("Smartfren", "1", "", "#777", "smart", "smart"),
        IspEntry("Esia", "1", "", "#777", "esia", "esia"),
        IspEntry("StarOne", "1", "", "#777", "starone", "indosat"),
        IspEntry("Telkom Flexi", "1", "", "#777", "telkomnet@flexi", "telkom"),
        IspEntry("AHA", "1", "AHA", "#777", "aha@aha.co.id", "aha")
    )),
    CountryEntry("Italy", "IT", listOf(
        IspEntry("TIM", "0", "ibox.tim.it", "*99#", "", ""),
        IspEntry("Vodafone", "0", "web.omnitel.it", "", "", ""),
        IspEntry("Wind", "0", "internet.wind", "*99#", "", ""),
        IspEntry("Tre", "0", "tre.it", "*99#", "", "")
    )),
    CountryEntry("Japan", "JP", listOf(
        IspEntry("Softbank", "0", "emb.ne.jp", "*99***1#", "em", "em"),
        IspEntry("b-mobile", "0", "dm.jplat.net", "*99***1#", "bmobile@u300", "bmobile"),
        IspEntry("AU", "0", "au.NET", "*99**24#", "au@au-win.ne.jp", "au")
    )),
    CountryEntry("Malaysia", "MA", listOf(
        IspEntry("Celcom", "0", "celcom3g", "*99***1#", "", ""),
        IspEntry("Maxis", "0", "unet", "*99***1#", "maxis", "wap"),
        IspEntry("Digi", "0", "3gdgnet", "*99#", "", ""),
        IspEntry(t("Yes"), "3", "", "", "", "")
    )),
    CountryEntry("Netherland", "NE", listOf(
        IspEntry("T-Mobile", "0", "internet", "*99***1#", "", ""),
        IspEntry("KPN", "0", "internet", "*99***1#", "", ""),
        IspEntry("Telfort", "0", "internet", "*99***1#", "", ""),
        IspEntry("Vodafone", "0", "internet", "*99***1#", "", "")
    )),
    CountryEntry("New Zealand", "NZ", listOf(
        IspEntry("CHT", "0", "internet", "*99#", "", ""),
        IspEntry("Vodafone NZ", "0", "www.vodafone.net.nz", "*99#", "", ""),
        IspEntry("2 Degrees", "0", "2degrees", "*99#", "", "")
    )),
    CountryEntry("Norway", "NO", listOf(
        IspEntry("Tele2", "0", "internet.tele2.no", "*99#", "", ""),
        IspEntry("NetCom", "0", "netcom", "*99#", "", ""),
        IspEntry("Chess", "0", "netcom", "", "", ""),
        IspEntry("Telenor", "0", "telenor", "*99#", "", ""),
        IspEntry("One Call", "0", "internet", "", "", "")
    )),
    CountryEntry("Philippine", "PH", listOf(
        IspEntry("Globe Prepaid", "0", "http.globe.com.ph", "*99***1#", "", ""),
        IspEntry("Globe Postpaid", "0", "internet.globe.com.ph", "*99***1#", "", ""),
        IspEntry("Smart Bro", "0", "smartbro", "*99#", "", ""),
        IspEntry("Smart Buddy", "0", "internet", "*99#", "", ""),
        IspEntry("Sun Prepaid", "0", "minternet", "*99#", "", ""),
        IspEntry("Sun Postpaid", "0", "fbband", "*99#", "", "")
    )),
    CountryEntry("Poland", "POL", listOf(
        IspEntry("Play", "0", "internet", "*99#", "", ""),
        IspEntry("Cyfrowy Polsat", "0", "multi.internet", "*99***1#", "", ""),
        IspEntry("ERA", "0", "internet", "*99***1#", "erainternet", "erainternet"),
        IspEntry("Orange", "0", "internet", "*99#", "internet", "internet"),
        IspEntry("Plus", "0", "internet", "*99***1#", "plusgsm", "plusgsm"),
        IspEntry("Heyah", "0", "internet", "*99***1#", "heyah", "heyah")
    )),
    CountryEntry("Portugal", "PO", listOf(
        IspEntry("TMN", "0", "internet", "*99#", "", ""),
        IspEntry("Optus", "0", "myconnection", "*99#", "", "")
    )),
    CountryEntry("Romania", "RO", listOf(
        IspEntry("Vodafone", "0", "internet.vodafone.ro", "*99#", "internet.vodafone.ro", "vodafone"),
        IspEntry("Orange", "0", "internet", "*99#", "", ""),
        IspEntry("Cosmote", "0", "broadband", "*99#", "", ""),
        IspEntry("RCS-RDS", "0", "internet", "*99#", "", "")
    )),
    CountryEntry("Russia", "RU", listOf(
        IspEntry("BeeLine", "0", "internet.beeline.ru", "*99***1#", "beeline", "beeline"),
        IspEntry("Megafon", "0", "internet.nw", "*99***1#", "", ""),
        IspEntry("MTS", "0", "internet.mts.ru", "*99***1#", "", ""),
        IspEntry("TELE2", "0", "internet.tele2.ru", "*99#", "", ""),
        IspEntry("Yota", "0", "yota.ru", "*99#", "", ""),
        IspEntry("SkyLink", "1", "", "#777", "mobile", "internet")
    )),
    CountryEntry("Singapore", "SG", listOf(
        IspEntry("M1", "0", "sunsurf", "*99#", "65", "user123"),
        IspEntry("Singtel", "0", "internet", "*99#", "", ""),
        IspEntry("StarHub", "0", "shinternet", "*99#", "", "")
    )),
    CountryEntry("Slovakia", "SVK", listOf(
        IspEntry("O2", "0", "o2internet", "*99***1#", "", ""),
        IspEntry("Orange", "0", "internet", "*99#", "", ""),
        IspEntry("T-mobile", "0", "internet", "*99***1#", "", "")
    )),
    CountryEntry("South Africa", "SA", listOf(
        IspEntry("Vodacom", "0", "internet", "*99#", "", ""),
        IspEntry("MTN", "0", "internet", "*99#", "", ""),
        IspEntry("Cell-c", "0", "internet", "*99#", "", "")
    )),
    CountryEntry("Spain", "ES", listOf(
        IspEntry("Vodafone", "0", "airtelnet.es", "*99#", "vodafone", "vodafone"),
        IspEntry("Movistar", "0", "movistar.es", "*99#", "movistar", "movistar"),
        IspEntry("Simyo", "0", "gprs-service.com", "*99#", "", ""),
        IspEntry("Ono", "0", "internet.ono.com", "*99#", "", "")
    )),
    CountryEntry("Sweden", "SE", listOf(
        IspEntry("3", "0", "data.tre.se", "*99#", "", ""),
        IspEntry("Telia", "0", "online.telia.se", "*99#", "", ""),
        IspEntry("Telenor", "0", "internet.telenor.se", "*99#", "", ""),
        IspEntry("Tele2", "0", "internet.tele2.se", "*99#", "", "")
    )),
    CountryEntry("Taiwan", "TW", listOf(
        IspEntry("Far Eastern", "0", "internet", "*99#", "", ""),
        IspEntry("Chunghua Telecom", "0", "internet", "*99***1#", "", ""),
        IspEntry("Taiwan Mobile", "0", "internet", "*99#", "", "")
    )),
    CountryEntry("Thailand", "TH", listOf(
        IspEntry("AIS(TH GSM)", "0", "internet", "*99#", "", ""),
        IspEntry("DTAC", "0", "www.dtac.co.th", "*99#", "", ""),
        IspEntry("Truemove H", "0", "hinternet", "*99#", "true", "true")
    )),
    CountryEntry("Turkey", "TR", listOf(
        IspEntry("Turkcell Vinn", "0", "mgb", "*99#", "", ""),
        IspEntry("Vodafone Vodem", "0", "internet", "*99#", "", ""),
        IspEntry("Avea Jet", "0", "internet", "*99#", "", "")
    )),
    CountryEntry("UK", "UK", listOf(
        IspEntry("O2", "0", "m-bb.o2.co.uk", "*99#", "o2bb", "password"),
        IspEntry("Vodafone", "0", "PPBUNDLE.INTERNET", "*99#", "web", "web"),
        IspEntry("Orange", "0", "internetvpn", "*99#", "", "")
    )),
    CountryEntry("Ukraine", "UA", listOf(
        IspEntry("BeeLine", "0", "internet.beeline.ua", "*99#", "mobile", "internet"),
        IspEntry("MTS", "0", "internet", "*99#", "", ""),
        IspEntry("Life", "0", "Internet", "*99#", "", ""),
        IspEntry("Kyivstar Contract", "0", "www.kyivstar.net", "*99#", "", ""),
        IspEntry("Kyivstar Prepaid", "0", "www.ab.kyivstar.net", "*99#", "", ""),
        IspEntry("Kyivstar 3G", "0", "3g.kyivstar.net", "*99#", "", ""),
        IspEntry("Utel", "0", "3g.utel.ua", "*99#", "", ""),
        IspEntry("Intertelecom", "1", "", "#777", "IT", "IT")
    )),
    CountryEntry("USA", "US", listOf(
        IspEntry("T-Mobile (Internet)", "0", "internet2.voicestream.com", "*99***1#", "", ""),
        IspEntry("AT&T", "0", "broadband", "*99***1#", "", ""),
        IspEntry("Verizon", "1", "", "#777", "", ""),
        IspEntry("Sprint", "1", "", "#777", "", "")
    )),
    CountryEntry("Vietnam", "VN", listOf(
        IspEntry("Mobifone(Fast Connect)", "0", "m-wap", "*99#", "mms", "mms"),
        IspEntry("Vinaphone(ezCom)", "0", "m3-card", "*99#", "mms", "mms"),
        IspEntry("Viettel(D-Com 3G)", "0", "e-connect", "*99#", "", "")
    )),
    CountryEntry("other", "", emptyList())
)
