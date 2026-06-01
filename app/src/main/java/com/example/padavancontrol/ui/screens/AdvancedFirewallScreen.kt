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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.TopAppBarColors
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.AdvancedFirewallViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.FirewallConfig
import com.example.padavancontrol.data.models.MacFilterRule
import com.example.padavancontrol.data.models.ServiceFilterRule
import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.theme.ArcherTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFirewallScreen(
    section: String, // "General", "Netfilter", "URLFilter", "MACFilter", "ServicesFilter"
    viewModel: AdvancedFirewallViewModel,
    repository: PadavanRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            "General" -> "Advanced_BasicFirewall_Content.asp"
            "Netfilter" -> "Advanced_Netfilter_Content.asp"
            "URLFilter" -> "Advanced_URLFilter_Content.asp"
            "MACFilter" -> "Advanced_MACFilter_Content.asp"
            "ServicesFilter" -> "Advanced_Firewall_Content.asp"
            else -> "Advanced_BasicFirewall_Content.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            "General" -> "Firewall - General Security"
            "Netfilter" -> "Firewall - Netfilter SPI Limits"
            "URLFilter" -> "Firewall - URL blocked keywords"
            "MACFilter" -> "Firewall - Hardware MAC Filter"
            "ServicesFilter" -> "Firewall - Network Services Filter"
            else -> "Firewall settings"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    var clientList by remember { mutableStateOf<List<LanClient>>(emptyList()) }

    // Validation/Input states for rules
    var urlKeywordInput by remember { mutableStateOf("") }
    
    var macFilterInputMac by remember { mutableStateOf("") }
    var macFilterInputTime by remember { mutableStateOf("00002359") }
    var macFilterInputDate by remember { mutableStateOf("1111111") }
    var macMacError by remember { mutableStateOf<String?>(null) }

    var serviceFilterInputSrcIp by remember { mutableStateOf("") }
    var serviceFilterInputSrcPort by remember { mutableStateOf("") }
    var serviceFilterInputDstIp by remember { mutableStateOf("") }
    var serviceFilterInputDstPort by remember { mutableStateOf("") }
    var serviceFilterInputProto by remember { mutableStateOf("TCP") }
    var serviceFilterInputProtoNo by remember { mutableStateOf("") }
    var serviceProtoExpanded by remember { mutableStateOf(false) }

    // Dropdown list states for general values
    var logModeExpanded by remember { mutableStateOf(false) }
    var sshBfpExpanded by remember { mutableStateOf(false) }
    var natTypeExpanded by remember { mutableStateOf(false) }
    var macFilterMethodExpanded by remember { mutableStateOf(false) }
    var serviceFilterDefaultExpanded by remember { mutableStateOf(false) }

    fun loadConfig() {
        viewModel.loadConfig(pagePath)
        // Fetch active LAN clients list for MAC and Services filter helpers
        if (section == "MACFilter" || section == "ServicesFilter") {
            coroutineScope.launch {
                repository.getLanClients().collect { result ->
                    result.onSuccess { clients ->
                        clientList = clients
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = section) {
        loadConfig()
    }

    fun saveGeneralOrNetfilterConfig() {
        viewModel.saveConfig(pagePath)
    }

    // URL actions
    fun addKeyword() {
        val word = urlKeywordInput.trim()
        if (word.isEmpty()) {
            Toast.makeText(context, "Keyword cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addUrlKeyword(word, pagePath)
        urlKeywordInput = ""
    }

    fun deleteKeyword(index: Int) {
        viewModel.deleteUrlKeyword(index, pagePath)
    }

    // MAC filter actions
    fun addMacRule() {
        macMacError = null
        val mac = macFilterInputMac.trim()
        if (!isValidMac(mac)) {
            macMacError = "Invalid MAC address (e.g. AA:BB:CC:DD:EE:FF)"
            return
        }
        val time = macFilterInputTime.trim()
        val date = macFilterInputDate.trim()
        if (time.length != 8 || date.length != 7) {
            Toast.makeText(context, "Invalid date/time filter formats.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addMacFilterRule(MacFilterRule(mac, time, date), pagePath)
        macFilterInputMac = ""
    }

    fun deleteMacRule(index: Int) {
        viewModel.deleteMacFilterRule(index, pagePath)
    }

    // Service rules actions
    fun addServiceRule() {
        val srcIp = serviceFilterInputSrcIp.trim()
        val srcPort = serviceFilterInputSrcPort.trim()
        val dstIp = serviceFilterInputDstIp.trim()
        val dstPort = serviceFilterInputDstPort.trim()
        val proto = serviceFilterInputProto
        val protoNo = serviceFilterInputProtoNo.trim()

        if (srcIp.isNotEmpty() && !isValidIpOrSubnet(srcIp)) {
            Toast.makeText(context, "Invalid source IP/Subnet format", Toast.LENGTH_SHORT).show()
            return
        }
        if (dstIp.isNotEmpty() && !isValidIpOrSubnet(dstIp)) {
            Toast.makeText(context, "Invalid destination IP/Subnet format", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.addServiceFilterRule(
            ServiceFilterRule(
                srcIp = srcIp,
                srcPort = srcPort,
                dstIp = dstIp,
                dstPort = dstPort,
                proto = proto,
                protoNo = protoNo
            ),
            pagePath
        )
        
        serviceFilterInputSrcIp = ""
        serviceFilterInputSrcPort = ""
        serviceFilterInputDstIp = ""
        serviceFilterInputDstPort = ""
        serviceFilterInputProtoNo = ""
    }

    fun deleteServiceRule(index: Int) {
        viewModel.deleteServiceFilterRule(index, pagePath)
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
                        text = uiState.loadStatus.ifEmpty { "Fetching Firewall configuration..." },
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
                                    text = "Padavan Netfilter Engine Manager",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Adjust rules dynamically. Click Apply Settings to persist to router non-volatile flash storage.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Render matching content based on section
                    when (section) {
                        "General" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("General Firewall Toggles", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Firewall", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Main firewall mechanism", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.fwEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(fwEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("DoS Protection", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Mitigate Denial of Service packets", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.fwDosEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(fwDosEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("SYN Cookies", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Block SYN flooding attacks", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.fwSynCookEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(fwSynCookEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    // Log Mode Dropdown
                                    Text("Firewall Log Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { logModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(uiState.config.fwLogMode.uppercase(), color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(
                                            expanded = logModeExpanded,
                                            onDismissRequest = { logModeExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            listOf("none", "drop", "accept", "both").forEach { mode ->
                                                DropdownMenuItem(
                                                    text = { Text(mode.uppercase()) },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(fwLogMode = mode))
                                                        logModeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Respond WAN Ping", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allow ping tests from external WAN", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.miscPingEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(miscPingEnabled = it)) },
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
                                    Text("External WAN Access Rules", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    // HTTP WAN
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable Web Access (HTTP)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Web UI access from outer internet", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.miscHttpEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(miscHttpEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                    if (uiState.config.miscHttpEnabled) {
                                        OutlinedTextField(
                                            value = uiState.config.miscHttpPort,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(miscHttpPort = it)) },
                                            label = { Text("HTTP Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // HTTPS WAN
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable HTTPS Access", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Secure SSL Web administration", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.httpsWopen,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(httpsWopen = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                    if (uiState.config.httpsWopen) {
                                        OutlinedTextField(
                                            value = uiState.config.httpsWport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(httpsWport = it)) },
                                            label = { Text("HTTPS Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // SSH WAN
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable SSH Server WAN", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Secure CLI shell entry from WAN", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.sshdWopen,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(sshdWopen = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                    if (uiState.config.sshdWopen) {
                                        OutlinedTextField(
                                            value = uiState.config.sshdWport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(sshdWport = it)) },
                                            label = { Text("SSH Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        )

                                        Text("SSH Brute Force Protection", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { sshBfpExpanded = true }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when(uiState.config.sshdWbfp) {
                                                    "0" -> "Disabled"
                                                    "1" -> "Block after 5 failed logins"
                                                    "2" -> "Strict block (after 3 failures)"
                                                    else -> "Enabled (Level ${uiState.config.sshdWbfp})"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 13.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = sshBfpExpanded, onDismissRequest = { sshBfpExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(sshdWbfp = "0")); sshBfpExpanded = false })
                                                DropdownMenuItem(text = { Text("Block after 5 attempts") }, onClick = { viewModel.updateConfig(uiState.config.copy(sshdWbfp = "1")); sshBfpExpanded = false })
                                                DropdownMenuItem(text = { Text("Strict block (3 attempts)") }, onClick = { viewModel.updateConfig(uiState.config.copy(sshdWbfp = "2")); sshBfpExpanded = false })
                                            }
                                        }
                                    }

                                    // FTP WAN
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable FTP Server WAN", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("File transfer external access", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ftpdWopen,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ftpdWopen = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                    if (uiState.config.ftpdWopen) {
                                        OutlinedTextField(
                                            value = uiState.config.ftpdWport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ftpdWport = it)) },
                                            label = { Text("FTP Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // udpxy WAN
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column {
                                            Text("Enable udpxy WAN Access", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("IPTV UDP-to-HTTP multicast streaming port", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.udpxyWopen,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(udpxyWopen = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                    if (uiState.config.udpxyWopen) {
                                        OutlinedTextField(
                                            value = uiState.config.udpxyWport,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(udpxyWport = it)) },
                                            label = { Text("udpxy Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // Transmission & Aria2 WAN
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Open Transmission WAN", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allows remote Torrent panel management", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.trmdRopen,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(trmdRopen = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Open Aria2 RPC WAN", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Download controller remote command port", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ariaRopen,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ariaRopen = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }

                        "Netfilter" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Netfilter Network Address Translation", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable WAN NAT Engine", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Perform IP translation on outer packets", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.wanNatEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(wanNatEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.nfMaxConn,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(nfMaxConn = it)) },
                                        label = { Text("Maximum NAT Connections limit") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    // NAT Type Dropdown
                                    Text("NAT Translation Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { natTypeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.nfNatType) {
                                                "0" -> "Cone NAT"
                                                "1" -> "Full Cone NAT"
                                                "2" -> "Hybrid Cone / Symmetric"
                                                else -> "Hybrid (2)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = natTypeExpanded, onDismissRequest = { natTypeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Cone NAT") }, onClick = { viewModel.updateConfig(uiState.config.copy(nfNatType = "0")); natTypeExpanded = false })
                                            DropdownMenuItem(text = { Text("Full Cone NAT (Easiest for Gaming)") }, onClick = { viewModel.updateConfig(uiState.config.copy(nfNatType = "1")); natTypeExpanded = false })
                                            DropdownMenuItem(text = { Text("Hybrid Symmetric NAT (Default)") }, onClick = { viewModel.updateConfig(uiState.config.copy(nfNatType = "2")); natTypeExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("NAT Loopback (Hairpinning)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Reach inside servers using external IPs", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.nfNatLoopback,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(nfNatLoopback = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("PPPoE Passthrough", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allow LAN devices to dial PPPoE directly", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.fwPtPppoe,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(fwPtPppoe = it)) },
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
                                    Text("Application Layer Gateways (ALG)", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    OutlinedTextField(
                                        value = uiState.config.nfAlgFtp0,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(nfAlgFtp0 = it)) },
                                        label = { Text("FTP ALG Control Port 0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.nfAlgFtp1,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(nfAlgFtp1 = it)) },
                                        label = { Text("FTP ALG Control Port 1 (Optional)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("PPTP ALG Passthrough", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Switch(checked = uiState.config.nfAlgPptp, onCheckedChange = { viewModel.updateConfig(uiState.config.copy(nfAlgPptp = it)) })
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("RTSP ALG Passthrough", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Switch(checked = uiState.config.nfAlgRtsp, onCheckedChange = { viewModel.updateConfig(uiState.config.copy(nfAlgRtsp = it)) })
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text("H.323 ALG Passthrough", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Switch(checked = uiState.config.nfAlgH323, onCheckedChange = { viewModel.updateConfig(uiState.config.copy(nfAlgH323 = it)) })
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("SIP ALG (VoIP Passthrough)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Switch(checked = uiState.config.nfAlgSip, onCheckedChange = { viewModel.updateConfig(uiState.config.copy(nfAlgSip = it)) })
                                    }
                                }
                            }
                        }

                        "URLFilter" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("URL Block parameters", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable URL Keywords Filter", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Block pages containing matched strings", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.urlFilterEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(urlFilterEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.urlFilterDate,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(urlFilterDate = it)) },
                                        label = { Text("Filter Days Bitmask (Mon-Sun)") },
                                        placeholder = { Text("1111111") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.urlFilterTime,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(urlFilterTime = it)) },
                                        label = { Text("Filter Active Hours (HHMMHHMM)") },
                                        placeholder = { Text("00002359") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.urlFilterMac,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(urlFilterMac = it)) },
                                        label = { Text("Restrict Specific Client MAC (Optional)") },
                                        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Invert Keyword Match", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Block all EXCEPT keywords", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.urlFilterInvert,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(urlFilterInvert = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            // Dynamic url keywords additions
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Add Blocked URL Keyword", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = urlKeywordInput,
                                            onValueChange = { urlKeywordInput = it },
                                            label = { Text("Keyword string") },
                                            placeholder = { Text("facebook") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { addKeyword() },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(ArcherTeal, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                                        }
                                    }
                                }
                            }

                            // Keywords list
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Blocked Keywords List", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                    if (uiState.config.urlKeywords.isEmpty()) {
                                        Text("No keywords restricted yet.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                    } else {
                                        uiState.config.urlKeywords.forEachIndexed { index, word ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(word, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                IconButton(onClick = { deleteKeyword(index) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "MACFilter" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("MAC Address Policy Mode", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Text("Filtering Strategy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { macFilterMethodExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.macFilterMethod) {
                                                "0" -> "Disabled"
                                                "1" -> "Accept Association (Whitelist)"
                                                "2" -> "Reject Association (Blacklist)"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = macFilterMethodExpanded, onDismissRequest = { macFilterMethodExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(macFilterMethod = "0")); macFilterMethodExpanded = false })
                                            DropdownMenuItem(text = { Text("Accept (Only allow whitelisted MACs)") }, onClick = { viewModel.updateConfig(uiState.config.copy(macFilterMethod = "1")); macFilterMethodExpanded = false })
                                            DropdownMenuItem(text = { Text("Reject (Block blacklisted MACs)") }, onClick = { viewModel.updateConfig(uiState.config.copy(macFilterMethod = "2")); macFilterMethodExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Drop Untrusted MAC Packets", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Enforce silent drop policies", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.fwMacDrop,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(fwMacDrop = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            // Add Rule Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Add MAC Filter Rule", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                    OutlinedTextField(
                                        value = macFilterInputMac,
                                        onValueChange = { macFilterInputMac = it },
                                        label = { Text("MAC Address") },
                                        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                                        isError = macMacError != null,
                                        supportingText = macMacError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = macFilterInputTime,
                                        onValueChange = { macFilterInputTime = it },
                                        label = { Text("Time Range (HHMMHHMM)") },
                                        placeholder = { Text("00002359") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = macFilterInputDate,
                                        onValueChange = { macFilterInputDate = it },
                                        label = { Text("Days Bitmask (Mon-Sun)") },
                                        placeholder = { Text("1111111") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Button(
                                        onClick = { addMacRule() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Add Rule ➕")
                                    }
                                }
                            }

                            // Quick fill from LAN clients
                            if (clientList.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Quick Select from Active Clients", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                        clientList.forEach { client ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFAFAFA))
                                                    .clickable { macFilterInputMac = client.macAddress }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    val host = if (client.hostname.isBlank()) "Unknown Client" else client.hostname
                                                    Text(host, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text("IP: ${client.ipAddress} | MAC: ${client.macAddress}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Text("Select 🔗", fontSize = 12.sp, color = ArcherTeal, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Current rules table
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Current MAC Rules List", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                    if (uiState.config.macFilterRules.isEmpty()) {
                                        Text("No MAC filters configured yet.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                    } else {
                                        uiState.config.macFilterRules.forEachIndexed { index, rule ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("MAC: ${rule.mac}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text("Active hours: ${rule.time.take(4)}-${rule.time.drop(4)} | Days: ${rule.date}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                IconButton(onClick = { deleteMacRule(index) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "ServicesFilter" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Network Services Policy", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Network Services Filter", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Enforce TCP/UDP layer packet checks", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.fwLwEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(fwLwEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Text("Filtering Strategy Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { serviceFilterDefaultExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.filterLwDefault) {
                                                "ACCEPT" -> "Blacklist (Accept all packets except listed)"
                                                "DROP" -> "Whitelist (Block all packets except listed)"
                                                else -> "Blacklist (ACCEPT)"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = serviceFilterDefaultExpanded, onDismissRequest = { serviceFilterDefaultExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Blacklist (Default ALLOW)") }, onClick = { viewModel.updateConfig(uiState.config.copy(filterLwDefault = "ACCEPT")); serviceFilterDefaultExpanded = false })
                                            DropdownMenuItem(text = { Text("Whitelist (Default DROP)") }, onClick = { viewModel.updateConfig(uiState.config.copy(filterLwDefault = "DROP")); serviceFilterDefaultExpanded = false })
                                        }
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.filterLwDate,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(filterLwDate = it)) },
                                        label = { Text("Active Days Bitmask (Mon-Sun)") },
                                        placeholder = { Text("1111111") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.filterLwTime,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(filterLwTime = it)) },
                                        label = { Text("Active Hours (HHMMHHMM)") },
                                        placeholder = { Text("00002359") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.filterLwIcmp,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(filterLwIcmp = it)) },
                                        label = { Text("ICMP Type Filter (Optional)") },
                                        placeholder = { Text("0") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Add rule form
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Add Service Filter Rule", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                    OutlinedTextField(
                                        value = serviceFilterInputSrcIp,
                                        onValueChange = { serviceFilterInputSrcIp = it },
                                        label = { Text("Source IP / Range") },
                                        placeholder = { Text("192.168.1.50 or empty") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = serviceFilterInputSrcPort,
                                        onValueChange = { serviceFilterInputSrcPort = it },
                                        label = { Text("Source Port(s)") },
                                        placeholder = { Text("80 or 80:90") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = serviceFilterInputDstIp,
                                        onValueChange = { serviceFilterInputDstIp = it },
                                        label = { Text("Destination IP / Range") },
                                        placeholder = { Text("8.8.8.8") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = serviceFilterInputDstPort,
                                        onValueChange = { serviceFilterInputDstPort = it },
                                        label = { Text("Destination Port(s)") },
                                        placeholder = { Text("53") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    Text("Protocol Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { serviceProtoExpanded = true }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(serviceFilterInputProto, color = Color.Black, fontSize = 13.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = serviceProtoExpanded, onDismissRequest = { serviceProtoExpanded = false }) {
                                            listOf("TCP", "UDP", "ICMP", "OTHER").forEach { pr ->
                                                DropdownMenuItem(text = { Text(pr) }, onClick = { serviceFilterInputProto = pr; serviceProtoExpanded = false })
                                            }
                                        }
                                    }

                                    if (serviceFilterInputProto == "OTHER") {
                                        OutlinedTextField(
                                            value = serviceFilterInputProtoNo,
                                            onValueChange = { serviceFilterInputProtoNo = it },
                                            label = { Text("IP Protocol Number") },
                                            placeholder = { Text("47") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    Button(
                                        onClick = { addServiceRule() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Add Service Rule ➕")
                                    }
                                }
                            }

                            // Rules list
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Current Services Rules List", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))

                                    if (uiState.config.serviceFilterRules.isEmpty()) {
                                        Text("No service filters configured yet.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                    } else {
                                        uiState.config.serviceFilterRules.forEachIndexed { index, rule ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val src = if (rule.srcIp.isEmpty() && rule.srcPort.isEmpty()) "ANY" else "${rule.srcIp}:${rule.srcPort}"
                                                    val dst = if (rule.dstIp.isEmpty() && rule.dstPort.isEmpty()) "ANY" else "${rule.dstIp}:${rule.dstPort}"
                                                    val protoInfo = if (rule.proto == "OTHER") "Proto:${rule.protoNo}" else rule.proto
                                                    Text("Src: $src ➔ Dst: $dst", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                    Text("Protocol: $protoInfo", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                IconButton(onClick = { deleteServiceRule(index) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save settings button (for General & Netfilter forms, which are saved in batch)
                    if (section == "General" || section == "Netfilter" || section == "URLFilter" || section == "MACFilter" || section == "ServicesFilter") {
                        Button(
                            onClick = { saveGeneralOrNetfilterConfig() },
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
}

// Helpers
fun isValidIpOrSubnet(ip: String): Boolean {
    // Check normal IP or Subnet format like 192.168.1.0/24 or 192.168.1.10
    val normalIpPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()
    val subnetPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/([0-9]|[1-2][0-9]|3[0-2])$".toRegex()
    return ip.matches(normalIpPattern) || ip.matches(subnetPattern)
}
