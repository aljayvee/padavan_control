package com.example.padavancontrol.ui.screens

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
    section: String, // "Connection", "IPv6", "PortForward", "DMZ", "DDNS"
    viewModel: AdvancedWanViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            "Connection" -> "Advanced_WAN_Content.asp"
            "IPv6" -> "Advanced_IPv6_Content.asp"
            "PortForward" -> "Advanced_VirtualServer_Content.asp"
            "DMZ" -> "Advanced_VirtualServer_Content.asp"
            "DDNS" -> "Advanced_DDNS_Content.asp"
            else -> "Advanced_WAN_Content.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            "Connection" -> "Internet Connection"
            "IPv6" -> "IPv6 Protocol Stack"
            "PortForward" -> "Port Forwarding (NAT)"
            "DMZ" -> "DMZ Host Settings"
            "DDNS" -> "Dynamic DNS (DDNS)"
            else -> "WAN & Internet Settings"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    var ddnsServerDropdownExpanded by remember { mutableStateOf(false) }
    var ipv6ProtoDropdownExpanded by remember { mutableStateOf(false) }

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
        viewModel.loadConfig(pagePath)
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
        if (section == "Connection") {
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
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = ArcherTeal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching WAN configuration...", color = Color.Gray, fontSize = 14.sp)
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
                        onClick = { viewModel.loadConfig(pagePath) },
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
                        "Connection" -> {
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

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
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
                                            modifier = Modifier.fillMaxWidth()
                                        )
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
                                    Text("IPv6 Protocol settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = when (uiState.config.ipv6Proto) {
                                                "" -> "Disabled"
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
                                            DropdownMenuItem(
                                                text = { Text("Disabled") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "")); ipv6ProtoDropdownExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Native DHCPv6") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "dhcp6")); ipv6ProtoDropdownExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Native Static IPv6") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "static")); ipv6ProtoDropdownExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Tunnel 6to4") },
                                                onClick = { viewModel.updateConfig(uiState.config.copy(ipv6Proto = "6to4")); ipv6ProtoDropdownExpanded = false }
                                            )
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
                                    Text("Port Forwarding (NAT) global", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
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
                                // Add Forwarding Rule
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
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(dmzEnabled = it)) },
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
                                    Text("Dynamic DNS configuration", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
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
                                        // Server Provider Selector
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
                                                DropdownMenuItem(
                                                    text = { Text("no-ip.com") },
                                                    onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "WWW.NO-IP.COM")); ddnsServerDropdownExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("dyndns.org") },
                                                    onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "WWW.DYNDNS.ORG")); ddnsServerDropdownExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("dnsomatic.com") },
                                                    onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "WWW.DNSOMATIC.COM")); ddnsServerDropdownExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("domains.google.com") },
                                                    onClick = { viewModel.updateConfig(uiState.config.copy(ddnsServer = "DOMAINS.GOOGLE.COM")); ddnsServerDropdownExpanded = false }
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.config.ddnsHostName,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(ddnsHostName = it)) },
                                            label = { Text("Dynamic Hostname") },
                                            isError = ddnsHostError != null,
                                            supportingText = ddnsHostError?.let { { Text(it) } },
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
                                            modifier = Modifier.fillMaxWidth()
                                        )
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
