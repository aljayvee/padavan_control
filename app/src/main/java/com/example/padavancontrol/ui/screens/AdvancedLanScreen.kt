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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.LanConfig
import com.example.padavancontrol.data.models.StaticLease
import com.example.padavancontrol.data.models.StaticRoute
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.AdvancedLanViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedLanScreen(
    section: String, // "IP", "DHCP", "Route", "IPTV", "Switch", "WOL"
    viewModel: AdvancedLanViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            "IP" -> "Advanced_LAN_Content.asp"
            "DHCP" -> "Advanced_DHCP_Content.asp"
            "IPTV" -> "Advanced_IPTV_Content.asp"
            "Route" -> "Advanced_GWStaticRoute_Content.asp"
            "Switch" -> "Advanced_Switch_Content.asp"
            "WOL" -> "Advanced_WOL_Content.asp"
            else -> "Advanced_LAN_Content.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            "IP" -> "LAN IP & Subnet"
            "DHCP" -> "DHCP Server Configuration"
            "IPTV" -> "IPTV Settings"
            "Route" -> "LAN Static Routes"
            "Switch" -> "Ethernet Switch Controls"
            "WOL" -> "Wake-on-LAN Trigger"
            else -> "LAN Advanced Settings"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Validation state
    var ipError by remember { mutableStateOf<String?>(null) }
    var maskError by remember { mutableStateOf<String?>(null) }
    var gatewayError by remember { mutableStateOf<String?>(null) }
    var dhcpStartError by remember { mutableStateOf<String?>(null) }
    var dhcpEndError by remember { mutableStateOf<String?>(null) }
    var leaseError by remember { mutableStateOf<String?>(null) }

    // WOL state
    var wolTargetMac by remember { mutableStateOf("") }
    var wolMacError by remember { mutableStateOf<String?>(null) }

    // Add Lease State
    var addLeaseMac by remember { mutableStateOf("") }
    var addLeaseIp by remember { mutableStateOf("") }
    var addLeaseName by remember { mutableStateOf("") }
    var addLeaseMacError by remember { mutableStateOf<String?>(null) }
    var addLeaseIpError by remember { mutableStateOf<String?>(null) }

    // Add Route State
    var addRouteDest by remember { mutableStateOf("") }
    var addRouteMask by remember { mutableStateOf("") }
    var addRouteGateway by remember { mutableStateOf("") }
    var addRouteMetric by remember { mutableStateOf("1") }
    var addRouteIf by remember { mutableStateOf("LAN") }
    var routeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = section) {
        viewModel.loadConfig(pagePath)
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.config) {
        if (section == "WOL" && wolTargetMac.isEmpty() && uiState.config.staticLeases.isNotEmpty()) {
            wolTargetMac = uiState.config.staticLeases.first().mac
        }
    }

    fun validateInputs(): Boolean {
        var isValid = true
        ipError = null
        maskError = null
        gatewayError = null
        dhcpStartError = null
        dhcpEndError = null
        leaseError = null

        val config = uiState.config
        if (section == "IP") {
            if (!isValidIp(config.lanIpAddr)) {
                ipError = "Invalid LAN IP format (e.g. 192.168.2.1)"
                isValid = false
            }
            if (!isValidIp(config.lanNetmask)) {
                maskError = "Invalid subnet mask format"
                isValid = false
            }
            if (config.lanGateway.isNotEmpty() && !isValidIp(config.lanGateway)) {
                gatewayError = "Invalid gateway IP format"
                isValid = false
            }
        }

        if (section == "DHCP" && config.dhcpEnabled) {
            if (!isValidIp(config.dhcpStart)) {
                dhcpStartError = "Invalid start IP pool format"
                isValid = false
            }
            if (!isValidIp(config.dhcpEnd)) {
                dhcpEndError = "Invalid end IP pool format"
                isValid = false
            }
            if (config.dhcpLease < 120L || config.dhcpLease > 604800L) {
                leaseError = "Lease time must be between 120 and 604800 seconds"
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

    fun addLease() {
        addLeaseMacError = null
        addLeaseIpError = null
        var ok = true
        if (!isValidMac(addLeaseMac)) {
            addLeaseMacError = "Invalid MAC address format (e.g., AA:BB:CC:DD:EE:FF)"
            ok = false
        }
        if (!isValidIp(addLeaseIp)) {
            addLeaseIpError = "Invalid IP address format"
            ok = false
        }
        if (addLeaseName.isBlank()) {
            addLeaseName = "Device"
        }

        if (!ok) return

        viewModel.addStaticLease(addLeaseMac, addLeaseIp, addLeaseName, pagePath)
        addLeaseMac = ""
        addLeaseIp = ""
        addLeaseName = ""
    }

    fun deleteLease(index: Int) {
        viewModel.deleteStaticLease(index, pagePath)
    }

    fun wakeDevice() {
        wolMacError = null
        if (!isValidMac(wolTargetMac)) {
            wolMacError = "Invalid MAC format"
            return
        }
        viewModel.sendWolPacket(wolTargetMac)
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
                        IconButton(onClick = { viewModel.loadConfig(pagePath) }) {
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
                        text = uiState.loadStatus.ifEmpty { "Fetching LAN configuration..." },
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
                    // Glassmorphic Category Panel
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
                                    text = "Padavan Subnet Settings Manager",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Highly stable Archer teal form validations linked instantly to flash NVRAM configuration blocks.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Form Fields by Section
                    when (section) {
                        "IP" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("IP Subnet settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    OutlinedTextField(
                                        value = uiState.config.lanIpAddr,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(lanIpAddr = it)) },
                                        label = { Text("LAN IP Address") },
                                        isError = ipError != null,
                                        supportingText = ipError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.lanNetmask,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(lanNetmask = it)) },
                                        label = { Text("Subnet Mask") },
                                        isError = maskError != null,
                                        supportingText = maskError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.lanGateway,
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(lanGateway = it)) },
                                        label = { Text("Gateway (Optional)") },
                                        isError = gatewayError != null,
                                        supportingText = gatewayError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Spanning Tree Protocol (STP)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Prevent network loops in bridge", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.lanStp,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(lanStp = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }

                        "DHCP" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("DHCP server", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable DHCP Server", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Assign dynamic IPs to local clients", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.dhcpEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(dhcpEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.dhcpEnabled) {
                                        OutlinedTextField(
                                            value = uiState.config.dhcpDomain,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpDomain = it)) },
                                            label = { Text("Local Domain Name") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpStart,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpStart = it)) },
                                            label = { Text("Start IP Address Pool") },
                                            isError = dhcpStartError != null,
                                            supportingText = dhcpStartError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpEnd,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpEnd = it)) },
                                            label = { Text("End IP Address Pool") },
                                            isError = dhcpEndError != null,
                                            supportingText = dhcpEndError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpLease.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpLease = it.toLongOrNull() ?: 86400L)) },
                                            label = { Text("Lease Time (seconds)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            isError = leaseError != null,
                                            supportingText = leaseError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpGateway,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpGateway = it)) },
                                            label = { Text("Default Gateway (Optional)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpDns1,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpDns1 = it)) },
                                            label = { Text("Primary DNS Server") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpDns2,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpDns2 = it)) },
                                            label = { Text("Secondary DNS Server") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpDns3,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpDns3 = it)) },
                                            label = { Text("Tertiary DNS Server (Optional)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpDnsv6,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpDnsv6 = it)) },
                                            label = { Text("IPv6 DNS Server (Optional)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dhcpWins,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dhcpWins = it)) },
                                            label = { Text("WINS Server (Optional)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        // Verbose dropdown
                                        var verboseExpanded by remember { mutableStateOf(false) }
                                        val verboseOptions = listOf(
                                            0 to t("Disabled"),
                                            1 to "DHCPv4",
                                            2 to "DHCPv6",
                                            3 to "DHCPv4 + DHCPv6"
                                        )
                                        val selectedVerboseText = verboseOptions.find { it.first == uiState.config.dhcpVerbose }?.second ?: t("Disabled")

                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            OutlinedTextField(
                                                value = selectedVerboseText,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("DHCP Server Verbose Logging") },
                                                trailingIcon = {
                                                    IconButton(onClick = { verboseExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            DropdownMenu(
                                                expanded = verboseExpanded,
                                                onDismissRequest = { verboseExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.9f)
                                            ) {
                                                verboseOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.second) },
                                                        onClick = {
                                                            viewModel.updateConfig(uiState.config.copy(dhcpVerbose = option.first))
                                                            verboseExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Text("Custom Configurations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

                                        OutlinedTextField(
                                            value = uiState.config.dnsmasqDnsmasqConf,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dnsmasqDnsmasqConf = it)) },
                                            label = { Text("dnsmasq.conf Custom Configuration") },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                            minLines = 3,
                                            maxLines = 8,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dnsmasqDhcpConf,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dnsmasqDhcpConf = it)) },
                                            label = { Text("dhcp.conf Custom Configuration") },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                            minLines = 3,
                                            maxLines = 8,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.dnsmasqHosts,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(dnsmasqHosts = it)) },
                                            label = { Text("hosts Custom Configuration") },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                            minLines = 3,
                                            maxLines = 8,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        ) {
                                            Column {
                                                Text("Enable Static IP Assignment", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Bind client MACs to fixed local IPs", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.dhcpStaticEnabled,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(dhcpStaticEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text("Enable Static ARP Binding", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Secure networking using ARP bindings", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.dhcpStaticArp,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(dhcpStaticArp = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.config.dhcpEnabled && uiState.config.dhcpStaticEnabled) {
                                // Add lease Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Add Static Assignment rule", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                        
                                        OutlinedTextField(
                                            value = addLeaseMac,
                                            onValueChange = { addLeaseMac = it },
                                            label = { Text(t("MAC Address")) },
                                            isError = addLeaseMacError != null,
                                            supportingText = addLeaseMacError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = addLeaseIp,
                                            onValueChange = { addLeaseIp = it },
                                            label = { Text("Static IP Address") },
                                            isError = addLeaseIpError != null,
                                            supportingText = addLeaseIpError?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = addLeaseName,
                                            onValueChange = { addLeaseName = it },
                                            label = { Text(t("Device Name")) },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        Button(
                                            onClick = { addLease() },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Lease Rule")
                                        }
                                    }
                                }

                                // Leases list
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Current Static Assignments (${uiState.config.staticLeases.size})", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))
                                        
                                        if (uiState.config.staticLeases.isEmpty()) {
                                            Text("No assignments active.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                        } else {
                                            uiState.config.staticLeases.forEachIndexed { index, lease ->
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
                                                        Text(lease.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                        Text("IP: ${lease.ip}", fontSize = 12.sp, color = Color.DarkGray)
                                                        Text("MAC: ${lease.mac}", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                    IconButton(onClick = { deleteLease(index) }) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "IPTV" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("IPTV & Multicast Configuration", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Multicast Routing (IGMP Proxy)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Allow IGMP multicast traffic to LAN", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.mrEnable,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(mrEnable = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    // Force IGMP dropdown
                                    var igmpExpanded by remember { mutableStateOf(false) }
                                    val igmpOptions = listOf(
                                        0 to "No (*)",
                                        1 to "IGMPv1",
                                        2 to "IGMPv2"
                                    )
                                    val selectedIgmpText = igmpOptions.find { it.first == uiState.config.forceIgmp }?.second ?: "No (*)"

                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = selectedIgmpText,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Force IGMP Protocol Version") },
                                            trailingIcon = {
                                                IconButton(onClick = { igmpExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DropdownMenu(
                                            expanded = igmpExpanded,
                                            onDismissRequest = { igmpExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            igmpOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.second) },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(forceIgmp = option.first))
                                                        igmpExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Text("IPTV Proxy Servers", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                                    OutlinedTextField(
                                        value = uiState.config.udpxyPort.toString(),
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(udpxyPort = it.toIntOrNull() ?: 0)) },
                                        label = { Text("UDP Multicast to HTTP Proxy Port (0 to disable)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = uiState.config.xupnpdPort.toString(),
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(xupnpdPort = it.toIntOrNull() ?: 0)) },
                                        label = { Text("eXtensible UPnP agent (xUPNPd) Web Port (0 to disable)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    if (uiState.config.udpxyPort > 0 && uiState.config.xupnpdPort > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Column {
                                                Text("Use udpxy proxy inside xupnpd", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Bridge multicast feeds through proxy", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Switch(
                                                checked = uiState.config.xupnpdUdpxy,
                                                onCheckedChange = { viewModel.updateConfig(uiState.config.copy(xupnpdUdpxy = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                            )
                                        }
                                    }

                                    Text("Multicast Traffic - IGMP/MLD Snooping", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable IGMP/MLD Snooping", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Optimize switch traffic using snooping", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.igmpSnoop,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(igmpSnoop = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    // M2U Switch dropdown
                                    var m2uExpanded by remember { mutableStateOf(false) }
                                    val m2uOptions = listOf(
                                        0 to "Disable",
                                        1 to "Multicast to Unicast",
                                        2 to "HW IGMP/MLD snooping (*)"
                                    )
                                    val selectedM2uText = m2uOptions.find { it.first == uiState.config.etherM2u }?.second ?: "HW IGMP/MLD snooping (*)"

                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        OutlinedTextField(
                                            value = selectedM2uText,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("M2U - Ethernet Switch") },
                                            trailingIcon = {
                                                IconButton(onClick = { m2uExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DropdownMenu(
                                            expanded = m2uExpanded,
                                            onDismissRequest = { m2uExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            m2uOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.second) },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(etherM2u = option.first))
                                                        m2uExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // M2U 2.4G dropdown
                                    var m2u2gExpanded by remember { mutableStateOf(false) }
                                    val m2u2gOptions = listOf(
                                        0 to "Disable",
                                        1 to "Multicast to Unicast (*)"
                                    )
                                    val selectedM2u2gText = m2u2gOptions.find { it.first == uiState.config.rtIgmpSnEnable }?.second ?: "Multicast to Unicast (*)"

                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        OutlinedTextField(
                                            value = selectedM2u2gText,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("M2U - Wireless 2.4GHz") },
                                            trailingIcon = {
                                                IconButton(onClick = { m2u2gExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DropdownMenu(
                                            expanded = m2u2gExpanded,
                                            onDismissRequest = { m2u2gExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            m2u2gOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.second) },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(rtIgmpSnEnable = option.first))
                                                        m2u2gExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // M2U 5G dropdown
                                    var m2u5gExpanded by remember { mutableStateOf(false) }
                                    val m2u5gOptions = listOf(
                                        0 to "Disable",
                                        1 to "Multicast to Unicast (*)"
                                    )
                                    val selectedM2u5gText = m2u5gOptions.find { it.first == uiState.config.wlIgmpSnEnable }?.second ?: "Multicast to Unicast (*)"

                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        OutlinedTextField(
                                            value = selectedM2u5gText,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("M2U - Wireless 5GHz") },
                                            trailingIcon = {
                                                IconButton(onClick = { m2u5gExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DropdownMenu(
                                            expanded = m2u5gExpanded,
                                            onDismissRequest = { m2u5gExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            m2u5gOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.second) },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(wlIgmpSnEnable = option.first))
                                                        m2u5gExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Text("Network storm constraint on Ethernet Ports", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                                    OutlinedTextField(
                                        value = uiState.config.controlrateBroadcast.toString(),
                                        onValueChange = { viewModel.updateConfig(uiState.config.copy(controlrateBroadcast = it.toIntOrNull() ?: 0)) },
                                        label = { Text("Broadcast Storm Control (Mbps, 0 to disable)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "Route" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Static routing", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Use DHCP Routes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Obtain routes automatically from upstream DHCP", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.useDhcpRoutes,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(useDhcpRoutes = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Enable Static Routes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Define custom packet routing bounds", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.routeEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(routeEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            if (uiState.config.routeEnabled) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Current Routing Rules (${uiState.config.staticRoutes.size})", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))
                                        
                                        if (uiState.config.staticRoutes.isEmpty()) {
                                            Text("No custom routes defined.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                        } else {
                                            uiState.config.staticRoutes.forEachIndexed { index, route ->
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
                                                        Text("Dest: ${route.destIp}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                        Text("Mask: ${route.netmask}", fontSize = 12.sp, color = Color.DarkGray)
                                                        Text("GW: ${route.gateway} | Metric: ${route.metric} | IF: ${route.interfaceName}", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                    IconButton(onClick = { viewModel.deleteStaticRoute(index, "Advanced_GWStaticRoute_Content.asp") }) {
                                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Route", tint = Color(0xFFE53935))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Add Route Form
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Add Static Route", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                        
                                        OutlinedTextField(
                                            value = addRouteDest,
                                            onValueChange = { addRouteDest = it },
                                            label = { Text("Network/Host IP") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                        
                                        OutlinedTextField(
                                            value = addRouteMask,
                                            onValueChange = { addRouteMask = it },
                                            label = { Text("Netmask") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                        
                                        OutlinedTextField(
                                            value = addRouteGateway,
                                            onValueChange = { addRouteGateway = it },
                                            label = { Text("Gateway") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                        
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = addRouteMetric,
                                                onValueChange = { addRouteMetric = it },
                                                label = { Text("Metric") },
                                                modifier = Modifier.weight(1f)
                                            )
                                            var routeIfExpanded by remember { mutableStateOf(false) }
                                            val routeIfOptions = listOf("LAN", "MAN", "WAN")
                                            Box(modifier = Modifier.weight(1f)) {
                                                OutlinedTextField(
                                                    value = addRouteIf,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Interface") },
                                                    trailingIcon = {
                                                        IconButton(onClick = { routeIfExpanded = true }) {
                                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                DropdownMenu(
                                                    expanded = routeIfExpanded,
                                                    onDismissRequest = { routeIfExpanded = false }
                                                ) {
                                                    routeIfOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option) },
                                                            onClick = {
                                                                addRouteIf = option
                                                                routeIfExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Button(
                                            onClick = { 
                                                val metricInt = addRouteMetric.toIntOrNull() ?: 1
                                                viewModel.addStaticRoute(addRouteDest, addRouteMask, addRouteGateway, metricInt, addRouteIf, "Advanced_GWStaticRoute_Content.asp")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                            enabled = !uiState.isSaving && addRouteDest.isNotBlank()
                                        ) {
                                            if (uiState.isSaving) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                            } else {
                                                Text("Add Route")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Switch" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Switch base settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Green Ethernet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Reduce power consumption dynamically", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.greenEthernet,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(greenEthernet = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Energy Efficient Ethernet (EEE)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("802.3az stateful ethernet power saving", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.eeeEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(eeeEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    // Jumbo frame dropdown
                                    var jumboExpanded by remember { mutableStateOf(false) }
                                    val jumboOptions = listOf(
                                        0 to "Up to 1536 bytes",
                                        1 to "Up to 16000 bytes"
                                    )
                                    val selectedJumboText = jumboOptions.find { it.first == uiState.config.etherJumbo }?.second ?: "Up to 1536 bytes"

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedJumboText,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Accept Jumbo Frames Between Ports") },
                                            trailingIcon = {
                                                IconButton(onClick = { jumboExpanded = true }) {
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DropdownMenu(
                                            expanded = jumboExpanded,
                                            onDismissRequest = { jumboExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            jumboOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.second) },
                                                    onClick = {
                                                        viewModel.updateConfig(uiState.config.copy(etherJumbo = option.first))
                                                        jumboExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Port configuration list
                            uiState.config.portsConfig.forEach { portConfig ->
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
                                            Text(
                                                text = "Port ${portConfig.portName}",
                                                fontWeight = FontWeight.Bold,
                                                color = ArcherTeal,
                                                fontSize = 15.sp
                                            )
                                            
                                            // Link State Badge
                                            val badgeColor = when {
                                                portConfig.linkState.contains("No link", ignoreCase = true) -> Color.Gray
                                                portConfig.linkState.contains("1000", ignoreCase = true) -> Color(0xFF4CAF50)
                                                else -> Color(0xFFFF9800)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(badgeColor.copy(alpha = 0.1f))
                                                    .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = portConfig.linkState,
                                                    color = badgeColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Flow Control Dropdown
                                        var flowExpanded by remember { mutableStateOf(false) }
                                        val flowOptions = listOf(
                                            0 to "TX/RX",
                                            1 to "TX (Asymmetric Pause)",
                                            2 to t("Disabled")
                                        )
                                        val selectedFlowText = flowOptions.find { it.first == portConfig.flowControl }?.second ?: "TX/RX"

                                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            OutlinedTextField(
                                                value = selectedFlowText,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Flow Control") },
                                                trailingIcon = {
                                                    IconButton(onClick = { flowExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            DropdownMenu(
                                                expanded = flowExpanded,
                                                onDismissRequest = { flowExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.9f)
                                            ) {
                                                flowOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.second) },
                                                        onClick = {
                                                            val updatedList = uiState.config.portsConfig.map {
                                                                if (it.portName == portConfig.portName) it.copy(flowControl = option.first) else it
                                                            }
                                                            viewModel.updateConfig(uiState.config.copy(portsConfig = updatedList))
                                                            flowExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Speed & Duplex Dropdown
                                        var speedExpanded by remember { mutableStateOf(false) }
                                        val speedOptions = listOf(
                                            0 to "Auto",
                                            1 to "1000 Mbps, Full Duplex: [AN]",
                                            2 to "100 Mbps, Full Duplex: [AN]",
                                            3 to "100 Mbps, Half Duplex: [AN]",
                                            4 to "10 Mbps, Full Duplex: [AN]",
                                            5 to "10 Mbps, Half Duplex: [AN]",
                                            6 to "100 Mbps, Full Duplex: [Force]",
                                            7 to "100 Mbps, Half Duplex: [Force]",
                                            8 to "10 Mbps, Full Duplex: [Force]",
                                            9 to "10 Mbps, Half Duplex: [Force]",
                                            15 to "Power Off"
                                        )
                                        val selectedSpeedText = speedOptions.find { it.first == portConfig.speedDuplex }?.second ?: "Auto"

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = selectedSpeedText,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Speed and Duplex") },
                                                trailingIcon = {
                                                    IconButton(onClick = { speedExpanded = true }) {
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            DropdownMenu(
                                                expanded = speedExpanded,
                                                onDismissRequest = { speedExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.9f)
                                            ) {
                                                speedOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.second) },
                                                        onClick = {
                                                            val updatedList = uiState.config.portsConfig.map {
                                                                if (it.portName == portConfig.portName) it.copy(speedDuplex = option.first) else it
                                                            }
                                                            viewModel.updateConfig(uiState.config.copy(portsConfig = updatedList))
                                                            speedExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "WOL" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(t("Wake-on-LAN client"), fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    OutlinedTextField(
                                        value = wolTargetMac,
                                        onValueChange = { wolTargetMac = it },
                                        label = { Text("Target MAC Address") },
                                        isError = wolMacError != null,
                                        supportingText = wolMacError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )

                                    Button(
                                        onClick = { wakeDevice() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        enabled = !uiState.isSaving
                                    ) {
                                        if (uiState.isSaving) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                        } else {
                                            Text("Send Magic Packet ⚡")
                                        }
                                    }
                                }
                            }

                            // Table Connected Device Lists
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Connected & DHCP Lease Devices", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    if (uiState.config.wolDevices.isEmpty()) {
                                        Text("No connected devices discovered.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                                    } else {
                                        uiState.config.wolDevices.forEach { device ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFAFAFA))
                                                    .clickable { wolTargetMac = device.mac }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text("MAC: ${device.mac}", fontSize = 11.sp, color = Color.DarkGray)
                                                    if (device.vendor.isNotEmpty()) {
                                                        Text("Vendor: ${device.vendor}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                }
                                                
                                                Button(
                                                    onClick = { viewModel.sendWolPacket(device.mac) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Wake ⚡", fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Button (for persistent forms)
                    if (section != "WOL" && section != "Route") {
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

fun isValidIp(ip: String): Boolean {
    val pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()
    return ip.matches(pattern)
}

fun isValidMac(mac: String): Boolean {
    val pattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
    return mac.matches(pattern)
}
