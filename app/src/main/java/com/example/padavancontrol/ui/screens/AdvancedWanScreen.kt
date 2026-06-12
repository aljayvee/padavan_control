package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.PortForwardRule
import com.example.padavancontrol.data.models.WanConfig
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.AdvancedWanViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWanScreen(
    section: String, // t("Connection"), "IPv6", "PortForward", "DMZ", "DDNS"
    viewModel: AdvancedWanViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            t("Connection") -> "Advanced_WAN_Content.asp"
            "IPv6" -> "Advanced_IPv6_Content.asp"
            "PortForward" -> "Advanced_VirtualServer_Content.asp"
            "DMZ" -> "Advanced_Exposed_Content.asp"
            "DDNS" -> "Advanced_DDNS_Content.asp"
            else -> "Advanced_WAN_Content.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            t("Connection") -> t("Internet Connection")
            "IPv6" -> "IPv6 Protocol Stack"
            "PortForward" -> t("Port Forwarding (NAT)")
            "DMZ" -> t("DMZ Host Settings")
            "DDNS" -> t("Dynamic DNS (DDNS)")
            else -> "WAN & Internet Settings"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pagePath) {
        viewModel.loadConfig(pagePath, forceRefresh = false)
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Validation state
    var dns1Error by remember { mutableStateOf<String?>(null) }
    var dns2Error by remember { mutableStateOf<String?>(null) }
    var pppoeUserError by remember { mutableStateOf<String?>(null) }
    var pppoePassError by remember { mutableStateOf<String?>(null) }
    var dmzIpError by remember { mutableStateOf<String?>(null) }
    var ddnsUserError by remember { mutableStateOf<String?>(null) }
    var ddnsPassError by remember { mutableStateOf<String?>(null) }
    var ddnsHostError by remember { mutableStateOf<String?>(null) }

    // Dropdown States
    var protoDropdownExpanded by remember { mutableStateOf(false) }
    var hwNatDropdownExpanded by remember { mutableStateOf(false) }
    var sfeDropdownExpanded by remember { mutableStateOf(false) }
    var authModeDropdownExpanded by remember { mutableStateOf(false) }
    var stbPortDropdownExpanded by remember { mutableStateOf(false) }
    var stbIsoDropdownExpanded by remember { mutableStateOf(false) }
    var ddnsServerDropdownExpanded by remember { mutableStateOf(false) }
    var ddns2ServerDropdownExpanded by remember { mutableStateOf(false) }
    var ddnsSourceDropdownExpanded by remember { mutableStateOf(false) }
    var ddnsIpv6DropdownExpanded by remember { mutableStateOf(false) }
    var ddnsVerboseDropdownExpanded by remember { mutableStateOf(false) }
    var ipv6ProtoDropdownExpanded by remember { mutableStateOf(false) }
    var ipv6WanDhcpDropdownExpanded by remember { mutableStateOf(false) }
    var ipv6LanDhcpDropdownExpanded by remember { mutableStateOf(false) }
    var upnpProtoDropdownExpanded by remember { mutableStateOf(false) }

    // Add Forwarding Rule State
    var addForwardName by remember { mutableStateOf("") }
    var addForwardExtPort by remember { mutableStateOf("") }
    var addForwardIntIp by remember { mutableStateOf("") }
    var addForwardIntPort by remember { mutableStateOf("") }
    var addForwardProtocol by remember { mutableStateOf("TCP") }
    var addForwardDesc by remember { mutableStateOf("") }
    
    var forwardNameError by remember { mutableStateOf<String?>(null) }
    var forwardExtPortError by remember { mutableStateOf<String?>(null) }
    var forwardIntIpError by remember { mutableStateOf<String?>(null) }
    var forwardIntPortError by remember { mutableStateOf<String?>(null) }
    
    var protocolDropdownExpanded by remember { mutableStateOf(false) }

    fun loadConfig() {
        viewModel.loadConfig(pagePath, forceRefresh = false)
    }

    fun validateInputs(): Boolean {
        var isValid = true
        dns1Error = null
        dns2Error = null
        pppoeUserError = null
        pppoePassError = null
        dmzIpError = null
        ddnsUserError = null
        ddnsPassError = null
        ddnsHostError = null

        val config = uiState.config
        if (section == t("Connection")) {
            if (!config.wanDnsEnable) {
                if (config.wanDns1.isNotEmpty() && !isValidIp(config.wanDns1)) {
                    dns1Error = "Invalid DNS IP address format"
                    isValid = false
                }
                if (config.wanDns2.isNotEmpty() && !isValidIp(config.wanDns2)) {
                    dns2Error = "Invalid DNS IP address format"
                    isValid = false
                }
            }
            if (uiState.config.wanProto == "pppoe") {
                if (uiState.config.pppoeUser.isBlank()) {
                    pppoeUserError = "PPPoE username cannot be blank."
                    isValid = false
                }
                if (uiState.config.pppoePass.isBlank()) {
                    pppoePassError = "PPPoE password cannot be blank."
                    isValid = false
                }
            }
        }

        if (section == "DMZ" && uiState.config.dmzEnabled) {
            if (!isValidIp(uiState.config.dmzIp)) {
                dmzIpError = "Invalid DMZ Target IP format"
                isValid = false
            }
        }

        if (section == "DDNS" && uiState.config.ddnsEnabled) {
            if (uiState.config.ddnsUser.isBlank()) {
                ddnsUserError = "DDNS username cannot be blank."
                isValid = false
            }
            if (uiState.config.ddnsPass.isBlank()) {
                ddnsPassError = "DDNS password cannot be blank."
                isValid = false
            }
            if (uiState.config.ddnsHostName.isBlank()) {
                ddnsHostError = "DDNS hostname cannot be blank."
                isValid = false
            }
        }

        return isValid
    }

fun saveConfig() {
        if (!validateInputs()) {
            Toast.makeText(context, "Please fix form errors first.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.saveConfig(pagePath)
    }

    fun addPortForward() {
        forwardNameError = null
        forwardExtPortError = null
        forwardIntIpError = null
        forwardIntPortError = null
        
        var ok = true
        if (addForwardName.isBlank()) {
            forwardNameError = "Rule name cannot be blank."
            ok = false
        }
        if (!isValidPortRange(addForwardExtPort)) {
            forwardExtPortError = "Invalid external port or range (e.g. 80, 80:90)"
            ok = false
        }
        if (!isValidIp(addForwardIntIp)) {
            forwardIntIpError = "Invalid internal client IP format"
            ok = false
        }
        if (addForwardIntPort.isNotEmpty() && !isValidPort(addForwardIntPort)) {
            forwardIntPortError = "Invalid internal port number (1-65535)"
            ok = false
        }

        if (!ok) return

        val newRule = PortForwardRule(
            name = addForwardName,
            extPort = addForwardExtPort,
            intIp = addForwardIntIp,
            intPort = addForwardIntPort.ifEmpty { addForwardExtPort },
            protocol = addForwardProtocol,
            desc = addForwardDesc
        )
        viewModel.addPortForwardRule(newRule, pagePath)
        addForwardName = ""
        addForwardExtPort = ""
        addForwardIntIp = ""
        addForwardIntPort = ""
        addForwardDesc = ""
    }

fun deletePortForward(index: Int) {
        viewModel.deletePortForwardRule(index, pagePath)
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
                        IconButton(onClick = { viewModel.loadConfig(pagePath, forceRefresh = true) }) {
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
            val animatedProgress by animateFloatAsState(
                targetValue = uiState.config.loadProgress,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing),
                label = "loadProgressAnimation"
            )

            if (uiState.isLoading) {
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
                        text = uiState.config.loadStatus.ifEmpty { "Fetching WAN configuration..." },
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (uiState.errorMessage != null) {
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
                        onClick = { viewModel.loadConfig(pagePath, forceRefresh = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // Category Panel info
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
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "info",
                                    tint = ArcherTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WAN Settings Manager",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Manage dynamic IP routing, WAN gateways, dynamic DNS configurations, and NAT forwarding.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Form Sections
                    when (section) {
                        t("Connection") -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("WAN Connection settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    // Connection Protocol Selector
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.wanProto) {
                                                "dhcp" -> "Dynamic IP (DHCP)"
                                                "static" -> "Static IP"
                                                "pppoe" -> "PPPoE (DSL)"
                                                "pptp" -> "PPTP"
                                                "l2tp" -> "L2TP"
                                                else -> uiState.config.wanProto
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Connection Type") },
                                            trailingIcon = {
                                                IconButton(onClick = { protoDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = protoDropdownExpanded,
                                            onDismissRequest = { protoDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Dynamic IP (DHCP)") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(wanProto = "dhcp")); protoDropdownExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Static IP") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(wanProto = "static")); protoDropdownExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("PPPoE (DSL)") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(wanProto = "pppoe")); protoDropdownExpanded = false }
                                            )
                                        }
                                    }

                                    // Hardware Offload NAT
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.hwNatMode) {
                                                "0" -> "Offload TCP for LAN"
                                                "1" -> "Offload TCP for LAN/WLAN"
                                                "3" -> "Offload TCP/UDP for LAN"
                                                "4" -> "Offload TCP/UDP for LAN/WLAN"
                                                "2" -> "Disable (Slow)"
                                                else -> uiState.config.hwNatMode
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Hardware Offload NAT/Routing IPv4") },
                                            trailingIcon = {
                                                IconButton(onClick = { hwNatDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = hwNatDropdownExpanded,
                                            onDismissRequest = { hwNatDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(text = { Text("Offload TCP for LAN") }, onClick = { viewModel.updateConfig(uiState.config.copy(hwNatMode = "0")); hwNatDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Offload TCP for LAN/WLAN") }, onClick = { viewModel.updateConfig(uiState.config.copy(hwNatMode = "1")); hwNatDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Offload TCP/UDP for LAN") }, onClick = { viewModel.updateConfig(uiState.config.copy(hwNatMode = "3")); hwNatDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Offload TCP/UDP for LAN/WLAN") }, onClick = { viewModel.updateConfig(uiState.config.copy(hwNatMode = "4")); hwNatDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Disable (Slow)") }, onClick = { viewModel.updateConfig(uiState.config.copy(hwNatMode = "2")); hwNatDropdownExpanded = false })
                                        }
                                    }

                                    // SFE Enable
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.sfeEnable) {
                                                "0" -> "Disable"
                                                "1" -> "Enable for IPv4/IPv6"
                                                "2" -> "Enable for IPv4/IPv6 and WiFi"
                                                else -> uiState.config.sfeEnable
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Shortcut Forwarding Engine (SFE)") },
                                            trailingIcon = {
                                                IconButton(onClick = { sfeDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = sfeDropdownExpanded,
                                            onDismissRequest = { sfeDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(text = { Text("Disable") }, onClick = { viewModel.updateConfig(uiState.config.copy(sfeEnable = "0")); sfeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Enable for IPv4/IPv6") }, onClick = { viewModel.updateConfig(uiState.config.copy(sfeEnable = "1")); sfeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Enable for IPv4/IPv6 and WiFi") }, onClick = { viewModel.updateConfig(uiState.config.copy(sfeEnable = "2")); sfeDropdownExpanded = false })
                                        }
                                    }

                                    // ARP Ping Switch
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ARP Ping Alive of Remote Gateway", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Check gateway connectivity via ARP", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.gwArpPing,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(gwArpPing = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.wanProto == "pppoe") {
                                        OutlinedTextField(
                                            value = uiState.config.pppoeUser,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pppoeUser = it)) },
                                            label = { Text("PPPoE Username") },
                                            isError = pppoeUserError != null,
                                            supportingText = pppoeUserError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pppoePass,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pppoePass = it)) },
                                            label = { Text("PPPoE Password") },
                                            isError = pppoePassError != null,
                                            supportingText = pppoePassError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pppoeMtu.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pppoeMtu = it.toIntOrNull() ?: 1492)) },
                                            label = { Text("MTU (bytes)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pppoeMru.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pppoeMru = it.toIntOrNull() ?: 1492)) },
                                            label = { Text("MRU (bytes)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    }

                                    Text("WAN DNS Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Get DNS Server Automatically", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Fetch DNS configurations from ISP", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.wanDnsEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(wanDnsEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (!uiState.config.wanDnsEnable) {
                                        OutlinedTextField(
                                            value = uiState.config.wanDns1,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(wanDns1 = it)) },
                                            label = { Text("Primary DNS") },
                                            isError = dns1Error != null,
                                            supportingText = dns1Error?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.wanDns2,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(wanDns2 = it)) },
                                            label = { Text("Secondary DNS") },
                                            isError = dns2Error != null,
                                            supportingText = dns2Error?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    }

                                    Text("Special Requirements from ISP", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                    // Authentication Mode
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.wanAuthMode) {
                                                "0" -> "None"
                                                "1" -> "ISP KABiNET"
                                                "2" -> "802.1x EAP-MD5"
                                                "3" -> "802.1x EAP-TTLS/PAP"
                                                "4" -> "802.1x EAP-TTLS/CHAP"
                                                "5" -> "802.1x EAP-TTLS/MSCHAP"
                                                "6" -> "802.1x EAP-TTLS/MSCHAPv2"
                                                "7" -> "802.1x EAP-PEAP/MSCHAPv2"
                                                else -> uiState.config.wanAuthMode
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Authentication") },
                                            trailingIcon = {
                                                IconButton(onClick = { authModeDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = authModeDropdownExpanded,
                                            onDismissRequest = { authModeDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(text = { Text("None") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "0")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("ISP KABiNET") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "1")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("802.1x EAP-MD5") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "2")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("802.1x EAP-TTLS/PAP") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "3")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("802.1x EAP-TTLS/CHAP") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "4")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("802.1x EAP-TTLS/MSCHAP") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "5")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("802.1x EAP-TTLS/MSCHAPv2") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "6")); authModeDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("802.1x EAP-PEAP/MSCHAPv2") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanAuthMode = "7")); authModeDropdownExpanded = false })
                                        }
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.wanHostname,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(wanHostname = it)) },
                                        label = { Text("Hostname") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.wanVci,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(wanVci = it)) },
                                        label = { Text("Vendor Class Identifier") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.wanHwaddr,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(wanHwaddr = it)) },
                                        label = { Text(t("MAC Address")) },
                                        placeholder = { Text("e.g. AA:BB:CC:DD:EE:FF") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Don't Decrement TTL after Routing", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Prevent downstream TTL reduction", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.wanTtlFix,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(wanTtlFix = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    OutlinedTextField(
                                        value = uiState.config.wanTtlValue,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(wanTtlValue = it)) },
                                        label = { Text("TTL Value") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Text("Ports Isolation and VLAN Filtering", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                    // IPTV STB Port
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.wanStbPort) {
                                                "0" -> t("No")
                                                "1" -> "LAN1"
                                                "2" -> "LAN2"
                                                "3" -> "LAN3"
                                                "4" -> "LAN4"
                                                "5" -> "LAN3 & LAN4"
                                                "6" -> "LAN1 & LAN2"
                                                "7" -> "LAN1 & LAN2 & LAN3"
                                                else -> uiState.config.wanStbPort
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Choose IPTV STB Port") },
                                            trailingIcon = {
                                                IconButton(onClick = { stbPortDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = stbPortDropdownExpanded,
                                            onDismissRequest = { stbPortDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "0")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN1") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "1")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN2") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "2")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN3") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "3")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN4") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "4")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN3 & LAN4") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "5")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN1 & LAN2") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "6")); stbPortDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("LAN1 & LAN2 & LAN3") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbPort = "7")); stbPortDropdownExpanded = false })
                                        }
                                    }

                                    // IPTV STB Ports Isolation
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.wanStbIso) {
                                                "0" -> t("No")
                                                "1" -> "STB Port Isolated from WAN"
                                                "2" -> "STB Port Isolated from WAN & LAN"
                                                else -> uiState.config.wanStbIso
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("IPTV STB Ports Isolation") },
                                            trailingIcon = {
                                                IconButton(onClick = { stbIsoDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = stbIsoDropdownExpanded,
                                            onDismissRequest = { stbIsoDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbIso = "0")); stbIsoDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("STB Port Isolated from WAN") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbIso = "1")); stbIsoDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("STB Port Isolated from WAN & LAN") }, onClick = { viewModel.updateConfig(uiState.config.copy(wanStbIso = "2")); stbIsoDropdownExpanded = false })
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("VLAN Tagged Traffic Filter?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Enable CPU VLAN tagging and filtering", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.vlanFilter,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(vlanFilter = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.vlanFilter) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = uiState.config.vlanVidCpu,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(vlanVidCpu = it)) },
                                                label = { Text("VLAN CPU Internet (VID)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            OutlinedTextField(
                                                value = uiState.config.vlanPriCpu,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(vlanPriCpu = it)) },
                                                label = { Text("PRIO") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.width(80.dp)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = uiState.config.vlanVidIptv,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(vlanVidIptv = it)) },
                                                label = { Text("VLAN CPU IPTV (VID)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            OutlinedTextField(
                                                value = uiState.config.vlanPriIptv,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(vlanPriIptv = it)) },
                                                label = { Text("PRIO") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.width(80.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "IPv6" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("IPv6 Connection Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = when (uiState.config.ipv6Proto) {
                                                "" -> t("Disabled")
                                                "static" -> "Native Static IPv6"
                                                "dhcp6" -> "Native DHCPv6"
                                                "6in4" -> "Tunnel 6in4"
                                                "6to4" -> "Tunnel 6to4"
                                                "6rd" -> "Tunnel 6rd"
                                                else -> uiState.config.ipv6Proto
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("IPv6 Service Type") },
                                            trailingIcon = {
                                                IconButton(onClick = { ipv6ProtoDropdownExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        DropdownMenu(
                                            expanded = ipv6ProtoDropdownExpanded,
                                            onDismissRequest = { ipv6ProtoDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(text = { Text(t("Disabled")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "")); ipv6ProtoDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Native DHCPv6") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "dhcp6")); ipv6ProtoDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Native Static IPv6") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "static")); ipv6ProtoDropdownExpanded = false })
                                            DropdownMenuItem(text = { Text("Tunnel 6to4") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "6to4")); ipv6ProtoDropdownExpanded = false })
                                        }
                                    }

                                    val isIpv6On = uiState.config.ipv6Proto.isNotEmpty()
                                    
                                    if (isIpv6On) {
                                        if (uiState.config.ipv6Proto == "dhcp6") {
                                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                                OutlinedTextField(
                                                    value = when (uiState.config.ipv6WanDhcp) {
                                                        "0" -> "Stateless: RA"
                                                        "1" -> "Stateful: DHCPv6 IA-NA"
                                                        "2" -> "Stateless & Stateful: Both"
                                                        else -> uiState.config.ipv6WanDhcp
                                                    },
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Get WAN IPv6 Address From Source") },
                                                    trailingIcon = {
                                                        IconButton(onClick = { ipv6WanDhcpDropdownExpanded = true }) {
                                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                DropdownMenu(
                                                    expanded = ipv6WanDhcpDropdownExpanded,
                                                    onDismissRequest = { ipv6WanDhcpDropdownExpanded = false }
                                                ) {
                                                    DropdownMenuItem(text = { Text("Stateless: RA") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6WanDhcp = "0")); ipv6WanDhcpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("Stateful: DHCPv6 IA-NA") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6WanDhcp = "1")); ipv6WanDhcpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("Stateless & Stateful: Both") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6WanDhcp = "2")); ipv6WanDhcpDropdownExpanded = false })
                                                }
                                            }
                                        }

                                        Text("WAN DNSv6 Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Get DNSv6 Servers Automatically?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Fetch IPv6 DNS from ISP dynamically", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.ipv6DnsAuto,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ipv6DnsAuto = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }

                                        if (!uiState.config.ipv6DnsAuto) {
                                            OutlinedTextField(
                                                value = uiState.config.ipv6Dns1,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(ipv6Dns1 = it)) },
                                                label = { Text("Primary DNSv6 Server") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )
                                            OutlinedTextField(
                                                value = uiState.config.ipv6Dns2,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(ipv6Dns2 = it)) },
                                                label = { Text("Secondary DNSv6 Server") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )
                                            OutlinedTextField(
                                                value = uiState.config.ipv6Dns3,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(ipv6Dns3 = it)) },
                                                label = { Text("Tertiary DNSv6 Server") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )
                                        }

                                        Text("LAN IPv6 Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Get LAN IPv6 Address via DHCPv6 IA-PD?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Prefix delegation dynamic addressing", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.ipv6LanAuto,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ipv6LanAuto = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Enable LAN Router Advertisement?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Send RA packets to clients", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.ipv6LanRadv,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ipv6LanRadv = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }

                                        if (uiState.config.ipv6LanRadv) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                                OutlinedTextField(
                                                    value = when (uiState.config.ipv6LanDhcp) {
                                                        "0" -> t("No")
                                                        "1" -> "Stateless (*)"
                                                        "2" -> "Stateful"
                                                        "3" -> "Stateless & Stateful"
                                                        else -> uiState.config.ipv6LanDhcp
                                                    },
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Enable LAN DHCPv6 Server?") },
                                                    trailingIcon = {
                                                        IconButton(onClick = { ipv6LanDhcpDropdownExpanded = true }) {
                                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                DropdownMenu(
                                                    expanded = ipv6LanDhcpDropdownExpanded,
                                                    onDismissRequest = { ipv6LanDhcpDropdownExpanded = false }
                                                ) {
                                                    DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6LanDhcp = "0")); ipv6LanDhcpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("Stateless (*)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6LanDhcp = "1")); ipv6LanDhcpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("Stateful") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6LanDhcp = "2")); ipv6LanDhcpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("Stateless & Stateful") }, onClick = { viewModel.updateConfig(uiState.config.copy(ipv6LanDhcp = "3")); ipv6LanDhcpDropdownExpanded = false })
                                                }
                                            }

                                            val isStateful = uiState.config.ipv6LanDhcp == "2" || uiState.config.ipv6LanDhcp == "3"
                                            if (isStateful) {
                                                fun decToHex(decStr: String): String {
                                                    val intVal = decStr.toIntOrNull() ?: return ""
                                                    return intVal.toString(16)
                                                }
                                                fun hexToDec(hexStr: String): String {
                                                    val cleanHex = hexStr.trim().replace(":", "")
                                                    if (cleanHex.isEmpty()) return ""
                                                    val intVal = cleanHex.toIntOrNull(16) ?: return "1"
                                                    return intVal.toString()
                                                }

                                                var sfpsHex by remember(uiState.config.ipv6LanSfps) {
                                                    mutableStateOf(decToHex(uiState.config.ipv6LanSfps))
                                                }
                                                var sfpeHex by remember(uiState.config.ipv6LanSfpe) {
                                                    mutableStateOf(decToHex(uiState.config.ipv6LanSfpe))
                                                }

                                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                    OutlinedTextField(
                                                        value = sfpsHex,
                                                        onValueChange = {
                                                            sfpsHex = it
                                                            val dec = hexToDec(it)
                                                            if (dec.isNotEmpty()) {
                                                                viewModel.updateConfig(uiState.config.copy(ipv6LanSfps = dec))
                                                            }
                                                        },
                                                        label = { Text("Stateful Pool Start (hex)") },
                                                        placeholder = { Text("e.g. 2") },
                                                        prefix = { Text("::") },
                                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = sfpeHex,
                                                        onValueChange = {
                                                            sfpeHex = it
                                                            val dec = hexToDec(it)
                                                            if (dec.isNotEmpty()) {
                                                                viewModel.updateConfig(uiState.config.copy(ipv6LanSfpe = dec))
                                                            }
                                                        },
                                                        label = { Text("Stateful Pool End (hex)") },
                                                        placeholder = { Text("e.g. fffe") },
                                                        prefix = { Text("::") },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }

                                                OutlinedTextField(
                                                    value = uiState.config.ipv6LanSflt,
                                                    onValueChange = { viewModel.updateConfig(uiState.config.copy(ipv6LanSflt = it)) },
                                                    label = { Text("DHCP Lease Time (sec)") },
                                                    supportingText = { Text("[120..604800]") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "PortForward" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Port Forwarding (NAT) Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Text("Enable UPnP", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allow automated port bindings from local apps", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.upnpEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(upnpEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.upnpEnabled) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = when (uiState.config.upnpProto) {
                                                    "0" -> "UPnP (*)"
                                                    "1" -> "NAT-PMP & PCP"
                                                    "2" -> "UPnP & NAT-PMP & PCP"
                                                    else -> uiState.config.upnpProto
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Support Protocols") },
                                                trailingIcon = {
                                                    IconButton(onClick = { upnpProtoDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = upnpProtoDropdownExpanded,
                                                onDismissRequest = { upnpProtoDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text("UPnP (*)") }, onClick = { viewModel.updateConfig(uiState.config.copy(upnpProto = "0")); upnpProtoDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("NAT-PMP & PCP") }, onClick = { viewModel.updateConfig(uiState.config.copy(upnpProto = "1")); upnpProtoDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("UPnP & NAT-PMP & PCP") }, onClick = { viewModel.updateConfig(uiState.config.copy(upnpProto = "2")); upnpProtoDropdownExpanded = false })
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Column {
                                                Text("Enable UPnP Security Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Secure dynamic port binding requests", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.upnpSecure,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(upnpSecure = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = uiState.config.upnpEportMin,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(upnpEportMin = it)) },
                                                label = { Text("External Port Min") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            OutlinedTextField(
                                                value = uiState.config.upnpEportMax,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(upnpEportMax = it)) },
                                                label = { Text("External Port Max") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = uiState.config.upnpIportMin,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(upnpIportMin = it)) },
                                                label = { Text("Internal Port Min") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            OutlinedTextField(
                                                value = uiState.config.upnpIportMax,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(upnpIportMax = it)) },
                                                label = { Text("Internal Port Max") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = uiState.config.upnpCleanInt,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(upnpCleanInt = it)) },
                                            label = { Text("Autoclean Rules Interval (sec)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.upnpCleanMin,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(upnpCleanMin = it)) },
                                            label = { Text("Minimal Rules Before Autoclean") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Enable Manual Port Forwarding", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Define custom manual IP/port bindings", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.portForwardEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(portForwardEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            if (uiState.config.portForwardEnabled) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Add Forwarding Rule", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                        
                                        OutlinedTextField(
                                            value = addForwardName,
                                            onValueChange = { addForwardName = it },
                                            label = { Text("Service Name") },
                                            isError = forwardNameError != null,
                                            supportingText = forwardNameError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = addForwardExtPort,
                                            onValueChange = { addForwardExtPort = it },
                                            label = { Text("External Port (e.g. 80, 8080:8090)") },
                                            isError = forwardExtPortError != null,
                                            supportingText = forwardExtPortError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = addForwardIntIp,
                                            onValueChange = { addForwardIntIp = it },
                                            label = { Text("Internal IP Address") },
                                            isError = forwardIntIpError != null,
                                            supportingText = forwardIntIpError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = addForwardIntPort,
                                            onValueChange = { addForwardIntPort = it },
                                            label = { Text("Internal Port (Optional)") },
                                            isError = forwardIntPortError != null,
                                            supportingText = forwardIntPortError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = addForwardProtocol,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Protocol") },
                                                trailingIcon = {
                                                    IconButton(onClick = { protocolDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = protocolDropdownExpanded,
                                                onDismissRequest = { protocolDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("TCP") },
                                                    onClick = { addForwardProtocol = "TCP"; protocolDropdownExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("UDP") },
                                                    onClick = { addForwardProtocol = "UDP"; protocolDropdownExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("TCP & UDP (BOTH)") },
                                                    onClick = { addForwardProtocol = "BOTH"; protocolDropdownExpanded = false }
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = addForwardDesc,
                                            onValueChange = { addForwardDesc = it },
                                            label = { Text("Description") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        Button(
                                            onClick = { addPortForward() },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add NAT Rule")
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
                                        Text("Current Forwarding Rules (${uiState.config.portForwardRules.size})", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))
                                        
                                        if (uiState.config.portForwardRules.isEmpty()) {
                                            Text("No manual rules active.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                        } else {
                                            uiState.config.portForwardRules.forEachIndexed { index, rule ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFFAFAFA))
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(rule.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                        Text("Ext Port: ${rule.extPort} → ${rule.intIp}:${rule.intPort}", fontSize = 12.sp, color = Color.DarkGray)
                                                        Text("Protocol: ${rule.protocol} | Desc: ${rule.desc}", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                    IconButton(onClick = { deletePortForward(index) }) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "DMZ" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("DMZ Host settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable DMZ Host", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Expose a local host fully to the WAN", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.dmzEnabled,
                                            onCheckedChange = {
                                                val newEnabled = it
                                                viewModel.updateConfig(
                                                    uiState.config.copy(
                                                        dmzEnabled = newEnabled,
                                                        dmzIp = if (newEnabled) uiState.config.dmzIp else ""
                                                    )
                                                )
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.dmzEnabled) {
                                        OutlinedTextField(
                                            value = uiState.config.dmzIp,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dmzIp = it)) },
                                            label = { Text("DMZ Host IP Address") },
                                            isError = dmzIpError != null,
                                            supportingText = dmzIpError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Starcraft (Battle.Net)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Enable Battle.Net connection sharing", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.dmzSpBattle,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(dmzSpBattle = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }

                        "DDNS" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Primary Dynamic DNS configuration", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Dynamic DNS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Bind WAN IP address to dynamic hostname", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.ddnsEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(ddnsEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.ddnsEnabled) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = uiState.config.ddnsServer,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("DDNS Provider") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsServerDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsServerDropdownExpanded,
                                                onDismissRequest = { ddnsServerDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text("no-ip.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "WWW.NO-IP.COM")); ddnsServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("dyndns.org") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "WWW.DYNDNS.ORG")); ddnsServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("dnsomatic.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "WWW.DNSOMATIC.COM")); ddnsServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("domains.google.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "DOMAINS.GOOGLE.COM")); ddnsServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("custom (http basic auth)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "CUSTOM")); ddnsServerDropdownExpanded = false })
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.config.ddnsHostName,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ddnsHostName = it)) },
                                            label = { Text("Dynamic Hostname 1") },
                                            isError = ddnsHostError != null,
                                            supportingText = ddnsHostError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.ddnsHostName2,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ddnsHostName2 = it)) },
                                            label = { Text("Dynamic Hostname 2 (Optional)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.ddnsHostName3,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ddnsHostName3 = it)) },
                                            label = { Text("Dynamic Hostname 3 (Optional)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.ddnsUser,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ddnsUser = it)) },
                                            label = { Text("DDNS Username / Email") },
                                            isError = ddnsUserError != null,
                                            supportingText = ddnsUserError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.ddnsPass,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ddnsPass = it)) },
                                            label = { Text("DDNS Password / Key") },
                                            isError = ddnsPassError != null,
                                            supportingText = ddnsPassError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        var ddnsSslDropdownExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsSsl) {
                                                    "0" -> t("No")
                                                    "1" -> t("Yes (*)")
                                                    else -> uiState.config.ddnsSsl
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Use Secure HTTPS Connection?") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsSslDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsSslDropdownExpanded,
                                                onDismissRequest = { ddnsSslDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsSsl = "0")); ddnsSslDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("Yes (*)")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsSsl = "1")); ddnsSslDropdownExpanded = false })
                                            }
                                        }

                                        var ddnsWildcardDropdownExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsWildcard) {
                                                    "0" -> t("No")
                                                    "1" -> t("Yes")
                                                    else -> uiState.config.ddnsWildcard
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Enable Wildcard?") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsWildcardDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsWildcardDropdownExpanded,
                                                onDismissRequest = { ddnsWildcardDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsWildcard = "0")); ddnsWildcardDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("Yes")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsWildcard = "1")); ddnsWildcardDropdownExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.config.ddnsEnabled) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Secondary DDNS Service (Optional)", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = if (uiState.config.ddns2Server.isEmpty()) t("Disabled") else uiState.config.ddns2Server,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("DDNS Provider") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddns2ServerDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddns2ServerDropdownExpanded,
                                                onDismissRequest = { ddns2ServerDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text(t("Disabled")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Server = "")); ddns2ServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("no-ip.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Server = "WWW.NO-IP.COM")); ddns2ServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("dyndns.org") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Server = "WWW.DYNDNS.ORG")); ddns2ServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("dnsomatic.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Server = "WWW.DNSOMATIC.COM")); ddns2ServerDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("domains.google.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Server = "DOMAINS.GOOGLE.COM")); ddns2ServerDropdownExpanded = false })
                                            }
                                        }

                                        if (uiState.config.ddns2Server.isNotEmpty()) {
                                            OutlinedTextField(
                                                value = uiState.config.ddns2HostName,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(ddns2HostName = it)) },
                                                label = { Text("Dynamic Hostname") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )

                                            OutlinedTextField(
                                                value = uiState.config.ddns2User,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(ddns2User = it)) },
                                                label = { Text("DDNS Username / Email") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )

                                            OutlinedTextField(
                                                value = uiState.config.ddns2Pass,
                                                onValueChange = { viewModel.updateConfig(uiState.config.copy(ddns2Pass = it)) },
                                                label = { Text("DDNS Password / Key") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )

                                            var ddns2SslDropdownExpanded by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                OutlinedTextField(
                                                    value = when (uiState.config.ddns2Ssl) {
                                                        "0" -> t("No")
                                                        "1" -> t("Yes (*)")
                                                        else -> uiState.config.ddns2Ssl
                                                    },
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Use Secure HTTPS Connection?") },
                                                    trailingIcon = {
                                                        IconButton(onClick = { ddns2SslDropdownExpanded = true }) {
                                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                DropdownMenu(
                                                    expanded = ddns2SslDropdownExpanded,
                                                    onDismissRequest = { ddns2SslDropdownExpanded = false }
                                                ) {
                                                    DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Ssl = "0")); ddns2SslDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("Yes (*)")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Ssl = "1")); ddns2SslDropdownExpanded = false })
                                                }
                                            }

                                            var ddns2WildcardDropdownExpanded by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedTextField(
                                                    value = when (uiState.config.ddns2Wildcard) {
                                                        "0" -> t("No")
                                                        "1" -> t("Yes")
                                                        else -> uiState.config.ddns2Wildcard
                                                    },
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Enable Wildcard?") },
                                                    trailingIcon = {
                                                        IconButton(onClick = { ddns2WildcardDropdownExpanded = true }) {
                                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                DropdownMenu(
                                                    expanded = ddns2WildcardDropdownExpanded,
                                                    onDismissRequest = { ddns2WildcardDropdownExpanded = false }
                                                ) {
                                                    DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Wildcard = "0")); ddns2WildcardDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("Yes")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2Wildcard = "1")); ddns2WildcardDropdownExpanded = false })
                                                }
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
                                        Text("Common DDNS Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsSource) {
                                                    "0" -> "External IP address"
                                                    "1" -> "WAN IP address"
                                                    "2" -> "MAN IP address"
                                                    else -> uiState.config.ddnsSource
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("My Internet IPv4 Address Source") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsSourceDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsSourceDropdownExpanded,
                                                onDismissRequest = { ddnsSourceDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text("External IP address") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsSource = "0")); ddnsSourceDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("WAN IP address") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsSource = "1")); ddnsSourceDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("MAN IP address") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsSource = "2")); ddnsSourceDropdownExpanded = false })
                                            }
                                        }

                                        val isExtIpSource = uiState.config.ddnsSource == "0"
                                        if (isExtIpSource) {
                                            var ddnsCheckIpDropdownExpanded by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                OutlinedTextField(
                                                    value = when (uiState.config.ddnsCheckIp) {
                                                        "0" -> "Default"
                                                        "1" -> "checkip.dyndns.org"
                                                        "5" -> "myip.dnsomatic.com"
                                                        "6" -> "ip1.dynupdate.no-ip.com"
                                                        "22" -> "api.ipify.org"
                                                        else -> "checkip (Code ${uiState.config.ddnsCheckIp})"
                                                    },
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Server to Autodetect External IP") },
                                                    trailingIcon = {
                                                        IconButton(onClick = { ddnsCheckIpDropdownExpanded = true }) {
                                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                DropdownMenu(
                                                    expanded = ddnsCheckIpDropdownExpanded,
                                                    onDismissRequest = { ddnsCheckIpDropdownExpanded = false }
                                                ) {
                                                    DropdownMenuItem(text = { Text("Default") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsCheckIp = "0")); ddnsCheckIpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("checkip.dyndns.org") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsCheckIp = "1")); ddnsCheckIpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("myip.dnsomatic.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsCheckIp = "5")); ddnsCheckIpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("ip1.dynupdate.no-ip.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsCheckIp = "6")); ddnsCheckIpDropdownExpanded = false })
                                                    DropdownMenuItem(text = { Text("api.ipify.org") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsCheckIp = "22")); ddnsCheckIpDropdownExpanded = false })
                                                }
                                            }

                                            if (uiState.config.ddns2Server.isNotEmpty()) {
                                                var ddns2CheckIpDropdownExpanded by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                    OutlinedTextField(
                                                        value = when (uiState.config.ddns2CheckIp) {
                                                            "0" -> "Default"
                                                            "1" -> "checkip.dyndns.org"
                                                            "5" -> "myip.dnsomatic.com"
                                                            "6" -> "ip1.dynupdate.no-ip.com"
                                                            "22" -> "api.ipify.org"
                                                            else -> "checkip (Code ${uiState.config.ddns2CheckIp})"
                                                        },
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = { Text("Server to Autodetect External IP (Second DDNS)") },
                                                        trailingIcon = {
                                                            IconButton(onClick = { ddns2CheckIpDropdownExpanded = true }) {
                                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    DropdownMenu(
                                                        expanded = ddns2CheckIpDropdownExpanded,
                                                        onDismissRequest = { ddns2CheckIpDropdownExpanded = false }
                                                    ) {
                                                        DropdownMenuItem(text = { Text("Default") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2CheckIp = "0")); ddns2CheckIpDropdownExpanded = false })
                                                        DropdownMenuItem(text = { Text("checkip.dyndns.org") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2CheckIp = "1")); ddns2CheckIpDropdownExpanded = false })
                                                        DropdownMenuItem(text = { Text("myip.dnsomatic.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2CheckIp = "5")); ddns2CheckIpDropdownExpanded = false })
                                                        DropdownMenuItem(text = { Text("ip1.dynupdate.no-ip.com") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2CheckIp = "6")); ddns2CheckIpDropdownExpanded = false })
                                                        DropdownMenuItem(text = { Text("api.ipify.org") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddns2CheckIp = "22")); ddns2CheckIpDropdownExpanded = false })
                                                    }
                                                }
                                            }
                                        }

                                        var ddnsPeriodDropdownExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsPeriod) {
                                                    "0" -> "10 mins"
                                                    "1" -> "1 hour"
                                                    "2" -> "2 hours"
                                                    "3" -> "3 hours"
                                                    "6" -> t("6 hours")
                                                    "12" -> t("12 hours")
                                                    "24" -> t("1 day (*)")
                                                    "48" -> t("2 days")
                                                    "72" -> t("3 days")
                                                    else -> "${uiState.config.ddnsPeriod} hours"
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("DDNS Update Period") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsPeriodDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsPeriodDropdownExpanded,
                                                onDismissRequest = { ddnsPeriodDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text("10 mins") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "0")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("1 hour") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "1")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("2 hours") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "2")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("3 hours") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "3")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("6 hours")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "6")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("12 hours")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "12")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("1 day (*)")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "24")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("2 days")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "48")); ddnsPeriodDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("3 days")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsPeriod = "72")); ddnsPeriodDropdownExpanded = false })
                                            }
                                        }

                                        var ddnsForcedDropdownExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsForced) {
                                                    "7" -> "7 days"
                                                    "10" -> "10 days (*)"
                                                    "20" -> "20 days"
                                                    "30" -> "30 days"
                                                    else -> "${uiState.config.ddnsForced} days"
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("DDNS Forced Update Period") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsForcedDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsForcedDropdownExpanded,
                                                onDismissRequest = { ddnsForcedDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text("7 days") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsForced = "7")); ddnsForcedDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("10 days (*)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsForced = "10")); ddnsForcedDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("20 days") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsForced = "20")); ddnsForcedDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("30 days") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsForced = "30")); ddnsForcedDropdownExpanded = false })
                                            }
                                        }

                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsIpv6) {
                                                    "0" -> t("No")
                                                    "1" -> t("Yes")
                                                    else -> uiState.config.ddnsIpv6
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Allow IPv6") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsIpv6DropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsIpv6DropdownExpanded,
                                                onDismissRequest = { ddnsIpv6DropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsIpv6 = "0")); ddnsIpv6DropdownExpanded = false })
                                                DropdownMenuItem(text = { Text(t("Yes")) }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsIpv6 = "1")); ddnsIpv6DropdownExpanded = false })
                                            }
                                        }

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = when (uiState.config.ddnsVerbose) {
                                                    "0" -> "0 (Quiet)"
                                                    "1" -> "1 (Default)"
                                                    "2" -> "2 (Verbose)"
                                                    else -> uiState.config.ddnsVerbose
                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Syslog Verbose Level") },
                                                trailingIcon = {
                                                    IconButton(onClick = { ddnsVerboseDropdownExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = ddnsVerboseDropdownExpanded,
                                                onDismissRequest = { ddnsVerboseDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(text = { Text("0 (Quiet)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsVerbose = "0")); ddnsVerboseDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("1 (Default)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsVerbose = "1")); ddnsVerboseDropdownExpanded = false })
                                                DropdownMenuItem(text = { Text("2 (Verbose)") }, onClick = { viewModel.updateConfig(uiState.config.copy(ddnsVerbose = "2")); ddnsVerboseDropdownExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Button
                    if (section != "PortForward") {
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
    }
}


private fun isValidPort(port: String): Boolean {
    val num = port.toIntOrNull()
    return num != null && num >= 1 && num <= 65535
}

private fun isValidPortRange(port: String): Boolean {
    if (port.contains(":")) {
        val parts = port.split(":")
        if (parts.size == 2) {
            val start = parts[0].toIntOrNull()
            val end = parts[1].toIntOrNull()
            return start != null && end != null && start in 1..65535 && end in 1..65535 && start <= end
        }
        return false
    }
    return isValidPort(port)
}
