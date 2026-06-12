package com.example.padavancontrol.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.t
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.AdvancedVpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedVpnScreen(
    viewModel: AdvancedVpnViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadConfig()
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(t("General"), "OpenVPN", t("Credentials & Certificates"), t("Post Script"))

    // Error states
    var peerError by remember { mutableStateOf<String?>(null) }
    var mtuError by remember { mutableStateOf<String?>(null) }
    var mruError by remember { mutableStateOf<String?>(null) }

    fun validateInputs(): Boolean {
        var isValid = true
        peerError = null
        mtuError = null
        mruError = null

        val config = uiState.config
        if (config.enable) {
            if (config.peer.isEmpty()) {
                peerError = t("VPN server address is required.")
                isValid = false
            }
            if (config.type != "2") {
                if (config.mtu < 1000 || config.mtu > 1460) {
                    mtuError = t("MTU must be between 1000 and 1460.")
                    isValid = false
                }
                if (config.mru < 1000 || config.mru > 1460) {
                    mruError = t("MRU must be between 1000 and 1460.")
                    isValid = false
                }
            }
        }
        return isValid
    }

    fun saveConfig() {
        if (!validateInputs()) {
            Toast.makeText(context, t("Please fix verification errors."), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.saveConfig()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("VPN Client Settings"), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                        IconButton(onClick = { viewModel.loadConfig() }) {
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
                    Text(t("Error loading VPN configurations"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.errorMessage ?: "", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.loadConfig() },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                    ) {
                        Text(t("Retry"))
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
                        text = uiState.loadStatus.ifEmpty { t("Fetching VPN settings...") },
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    text = t("VPN Client Settings"),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t("Configure connection to PPTP, L2TP, or OpenVPN servers. Enforces strict allowed SSL ciphers on HTTPS connections."),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Tabs
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = ArcherTeal,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = ArcherTeal
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, label ->
                            Tab(
                                selected = activeTab == index,
                                onClick = { activeTab = index },
                                text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    val config = uiState.config

                    when (activeTab) {
                        0 -> { // General Tab
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // VPN Client Enable Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(t("Enable VPN Client"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Switch(
                                            checked = config.enable,
                                            onCheckedChange = { viewModel.updateConfig(config.copy(enable = it)) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = ArcherTeal
                                            )
                                        )
                                    }

                                    AnimatedVisibility(visible = config.enable) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            // VPN Type Select
                                            var typeExpanded by remember { mutableStateOf(false) }
                                            Text(t("VPN Client Type"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                        .clickable { typeExpanded = true }
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val typeStr = when (config.type) {
                                                        "0" -> "PPTP"
                                                        "1" -> "L2TP (w/o IPSec)"
                                                        "2" -> "OpenVPN"
                                                        else -> "PPTP"
                                                    }
                                                    Text(typeStr, color = Color.Black, fontSize = 14.sp)
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                                    DropdownMenuItem(text = { Text("PPTP") }, onClick = { viewModel.updateConfig(config.copy(type = "0")); typeExpanded = false })
                                                    DropdownMenuItem(text = { Text("L2TP (w/o IPSec)") }, onClick = { viewModel.updateConfig(config.copy(type = "1")); typeExpanded = false })
                                                    DropdownMenuItem(text = { Text("OpenVPN") }, onClick = { viewModel.updateConfig(config.copy(type = "2")); typeExpanded = false })
                                                }
                                            }

                                            // Server / Host Address
                                            OutlinedTextField(
                                                value = config.peer,
                                                onValueChange = { viewModel.updateConfig(config.copy(peer = it)) },
                                                label = { Text(t("VPN Server Address (IP or Hostname)")) },
                                                isError = peerError != null,
                                                supportingText = peerError?.let { { Text(it) } },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            // Username
                                            OutlinedTextField(
                                                value = config.username,
                                                onValueChange = { viewModel.updateConfig(config.copy(username = it)) },
                                                label = { Text(t("Username")) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            // Password
                                            var passwordVisible by remember { mutableStateOf(false) }
                                            OutlinedTextField(
                                                value = config.password,
                                                onValueChange = { viewModel.updateConfig(config.copy(password = it)) },
                                                label = { Text(t("Password")) },
                                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                        val eyeIcon = if (passwordVisible) "👁" else "👁‍🗨"
                                                        Text(eyeIcon)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            if (config.type != "2") { // PPTP/L2TP Settings
                                                // Auth Type
                                                var authExpanded by remember { mutableStateOf(false) }
                                                Text(t("Authentication Mode"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                            .clickable { authExpanded = true }
                                                            .padding(14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val authStr = when (config.authType) {
                                                            "0" -> "Auto"
                                                            "1" -> "MS-CHAPv2"
                                                            "2" -> "CHAP"
                                                            "3" -> "PAP"
                                                            else -> "Auto"
                                                        }
                                                        Text(authStr, color = Color.Black, fontSize = 14.sp)
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                    DropdownMenu(expanded = authExpanded, onDismissRequest = { authExpanded = false }) {
                                                        DropdownMenuItem(text = { Text("Auto") }, onClick = { viewModel.updateConfig(config.copy(authType = "0")); authExpanded = false })
                                                        DropdownMenuItem(text = { Text("MS-CHAPv2") }, onClick = { viewModel.updateConfig(config.copy(authType = "1")); authExpanded = false })
                                                        DropdownMenuItem(text = { Text("CHAP") }, onClick = { viewModel.updateConfig(config.copy(authType = "2")); authExpanded = false })
                                                        DropdownMenuItem(text = { Text("PAP") }, onClick = { viewModel.updateConfig(config.copy(authType = "3")); authExpanded = false })
                                                    }
                                                }

                                                // MPPE Encryption
                                                var mppeExpanded by remember { mutableStateOf(false) }
                                                Text(t("MPPE Encryption"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                            .clickable { mppeExpanded = true }
                                                            .padding(14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val mppeStr = when (config.mppe) {
                                                            "0" -> "Auto"
                                                            "1" -> "MPPE-128"
                                                            "2" -> "MPPE-40"
                                                            "3" -> "No encryption"
                                                            else -> "Auto"
                                                        }
                                                        Text(mppeStr, color = Color.Black, fontSize = 14.sp)
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                    }
                                                    DropdownMenu(expanded = mppeExpanded, onDismissRequest = { mppeExpanded = false }) {
                                                        DropdownMenuItem(text = { Text("Auto") }, onClick = { viewModel.updateConfig(config.copy(mppe = "0")); mppeExpanded = false })
                                                        DropdownMenuItem(text = { Text("MPPE-128") }, onClick = { viewModel.updateConfig(config.copy(mppe = "1")); mppeExpanded = false })
                                                        DropdownMenuItem(text = { Text("MPPE-40") }, onClick = { viewModel.updateConfig(config.copy(mppe = "2")); mppeExpanded = false })
                                                        DropdownMenuItem(text = { Text("No encryption") }, onClick = { viewModel.updateConfig(config.copy(mppe = "3")); mppeExpanded = false })
                                                    }
                                                }

                                                // MTU / MRU
                                                OutlinedTextField(
                                                    value = config.mtu.toString(),
                                                    onValueChange = { viewModel.updateConfig(config.copy(mtu = it.toIntOrNull() ?: 1450)) },
                                                    label = { Text("MTU [1000..1460]") },
                                                    isError = mtuError != null,
                                                    supportingText = mtuError?.let { { Text(it) } },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = config.mru.toString(),
                                                    onValueChange = { viewModel.updateConfig(config.copy(mru = it.toIntOrNull() ?: 1450)) },
                                                    label = { Text("MRU [1000..1460]") },
                                                    isError = mruError != null,
                                                    supportingText = mruError?.let { { Text(it) } },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                // Additional PPPD
                                                OutlinedTextField(
                                                    value = config.pppdOptions,
                                                    onValueChange = { viewModel.updateConfig(config.copy(pppdOptions = it)) },
                                                    label = { Text(t("Additional pppd Options")) },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            // Firewall rule
                                            var sfwExpanded by remember { mutableStateOf(false) }
                                            Text(t("Firewall Protection"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                        .clickable { sfwExpanded = true }
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val sfwStr = when (config.firewall) {
                                                        "0" -> t("No")
                                                        "1" -> t("Yes (All traffic)")
                                                        "2" -> t("No (and disable NAT)")
                                                        "3" -> t("Yes (Peer only)")
                                                        else -> t("Yes (All traffic)")
                                                    }
                                                    Text(sfwStr, color = Color.Black, fontSize = 14.sp)
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                                DropdownMenu(expanded = sfwExpanded, onDismissRequest = { sfwExpanded = false }) {
                                                    DropdownMenuItem(text = { Text(t("Yes (All traffic)")) }, onClick = { viewModel.updateConfig(config.copy(firewall = "1")); sfwExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("Yes (Peer only)")) }, onClick = { viewModel.updateConfig(config.copy(firewall = "3")); sfwExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(config.copy(firewall = "0")); sfwExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("No (and disable NAT)")) }, onClick = { viewModel.updateConfig(config.copy(firewall = "2")); sfwExpanded = false })
                                                }
                                            }

                                            // Peer DNS
                                            var pdnsExpanded by remember { mutableStateOf(false) }
                                            Text(t("Peer DNS Configuration"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                        .clickable { pdnsExpanded = true }
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val pdnsStr = when (config.peerDns) {
                                                        "0" -> t("No")
                                                        "1" -> t("Yes (Add to top)")
                                                        "2" -> t("Yes (Replace standard DNS)")
                                                        else -> t("Yes (Add to top)")
                                                    }
                                                    Text(pdnsStr, color = Color.Black, fontSize = 14.sp)
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                                }
                                                DropdownMenu(expanded = pdnsExpanded, onDismissRequest = { pdnsExpanded = false }) {
                                                    DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(config.copy(peerDns = "0")); pdnsExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("Yes (Add to top)")) }, onClick = { viewModel.updateConfig(config.copy(peerDns = "1")); pdnsExpanded = false })
                                                    DropdownMenuItem(text = { Text(t("Yes (Replace standard DNS)")) }, onClick = { viewModel.updateConfig(config.copy(peerDns = "2")); pdnsExpanded = false })
                                                }
                                            }

                                            // Default Gateway Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(t("Set as Default Gateway"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Switch(
                                                    checked = config.defaultGateway,
                                                    onCheckedChange = { viewModel.updateConfig(config.copy(defaultGateway = it)) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = ArcherTeal
                                                    )
                                                )
                                            }

                                            if (!config.defaultGateway) { // Remote Route Subnet
                                                Text(t("Static Route Subnet for VPN Peer"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = config.remoteNetwork,
                                                        onValueChange = { viewModel.updateConfig(config.copy(remoteNetwork = it)) },
                                                        label = { Text(t("Subnet IP")) },
                                                        placeholder = { Text("10.0.0.0") },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = config.remoteMask,
                                                        onValueChange = { viewModel.updateConfig(config.copy(remoteMask = it)) },
                                                        label = { Text(t("Subnet Mask")) },
                                                        placeholder = { Text("255.255.255.0") },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> { // OpenVPN Tab
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Port
                                    OutlinedTextField(
                                        value = config.ovpnPort.toString(),
                                        onValueChange = { viewModel.updateConfig(config.copy(ovpnPort = it.toIntOrNull() ?: 1194)) },
                                        label = { Text(t("OpenVPN Server Port")) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Protocol
                                    var protExpanded by remember { mutableStateOf(false) }
                                    Text(t("Connection Protocol"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { protExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val protStr = when (config.ovpnProtocol) {
                                                "0" -> "UDP"
                                                "1" -> "TCP"
                                                "2" -> "UDP over IPv6"
                                                "3" -> "TCP over IPv6"
                                                else -> "UDP"
                                            }
                                            Text(protStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = protExpanded, onDismissRequest = { protExpanded = false }) {
                                            DropdownMenuItem(text = { Text("UDP") }, onClick = { viewModel.updateConfig(config.copy(ovpnProtocol = "0")); protExpanded = false })
                                            DropdownMenuItem(text = { Text("TCP") }, onClick = { viewModel.updateConfig(config.copy(ovpnProtocol = "1")); protExpanded = false })
                                            DropdownMenuItem(text = { Text("UDP over IPv6") }, onClick = { viewModel.updateConfig(config.copy(ovpnProtocol = "2")); protExpanded = false })
                                            DropdownMenuItem(text = { Text("TCP over IPv6") }, onClick = { viewModel.updateConfig(config.copy(ovpnProtocol = "3")); protExpanded = false })
                                        }
                                    }

                                    // Mode TUN/TAP
                                    var modeExpanded by remember { mutableStateOf(false) }
                                    Text(t("Virtual Device Interface"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { modeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val modeStr = when (config.ovpnMode) {
                                                "0" -> "TAP (Ethernet Bridge)"
                                                "1" -> "TUN (IP Tunnel)"
                                                else -> "TUN (IP Tunnel)"
                                            }
                                            Text(modeStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("TAP (Ethernet Bridge)") }, onClick = { viewModel.updateConfig(config.copy(ovpnMode = "0")); modeExpanded = false })
                                            DropdownMenuItem(text = { Text("TUN (IP Tunnel)") }, onClick = { viewModel.updateConfig(config.copy(ovpnMode = "1")); modeExpanded = false })
                                        }
                                    }

                                    // Auth type
                                    var ovpnAuthExpanded by remember { mutableStateOf(false) }
                                    Text(t("TLS Authentication Type"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { ovpnAuthExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val authStr = when (config.ovpnAuthType) {
                                                "0" -> "TLS Certificates Only"
                                                "1" -> "TLS + User/Pass Credentials"
                                                else -> "TLS + User/Pass Credentials"
                                            }
                                            Text(authStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = ovpnAuthExpanded, onDismissRequest = { ovpnAuthExpanded = false }) {
                                            DropdownMenuItem(text = { Text("TLS Certificates Only") }, onClick = { viewModel.updateConfig(config.copy(ovpnAuthType = "0")); ovpnAuthExpanded = false })
                                            DropdownMenuItem(text = { Text("TLS + User/Pass Credentials") }, onClick = { viewModel.updateConfig(config.copy(ovpnAuthType = "1")); ovpnAuthExpanded = false })
                                        }
                                    }

                                    // Digest
                                    var digestExpanded by remember { mutableStateOf(false) }
                                    Text(t("Hash Digest Algorithm"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { digestExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val digestStr = when (config.ovpnDigest) {
                                                "0" -> "MD5 (128 bit)"
                                                "1" -> "SHA1 (160 bit)"
                                                "2" -> "SHA224 (224 bit)"
                                                "3" -> "SHA256 (256 bit)"
                                                "4" -> "SHA384 (384 bit)"
                                                "5" -> "SHA512 (512 bit)"
                                                else -> "SHA1"
                                            }
                                            Text(digestStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = digestExpanded, onDismissRequest = { digestExpanded = false }) {
                                            DropdownMenuItem(text = { Text("MD5 (128 bit)") }, onClick = { viewModel.updateConfig(config.copy(ovpnDigest = "0")); digestExpanded = false })
                                            DropdownMenuItem(text = { Text("SHA1 (160 bit)") }, onClick = { viewModel.updateConfig(config.copy(ovpnDigest = "1")); digestExpanded = false })
                                            DropdownMenuItem(text = { Text("SHA224 (224 bit)") }, onClick = { viewModel.updateConfig(config.copy(ovpnDigest = "2")); digestExpanded = false })
                                            DropdownMenuItem(text = { Text("SHA256 (256 bit)") }, onClick = { viewModel.updateConfig(config.copy(ovpnDigest = "3")); digestExpanded = false })
                                            DropdownMenuItem(text = { Text("SHA384 (384 bit)") }, onClick = { viewModel.updateConfig(config.copy(ovpnDigest = "4")); digestExpanded = false })
                                            DropdownMenuItem(text = { Text("SHA512 (512 bit)") }, onClick = { viewModel.updateConfig(config.copy(ovpnDigest = "5")); digestExpanded = false })
                                        }
                                    }

                                    // Cipher
                                    var cipherExpanded by remember { mutableStateOf(false) }
                                    Text(t("Symmetric Encryption Cipher"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { cipherExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val cipherStr = when (config.ovpnCipher) {
                                                "0" -> "None"
                                                "1" -> "DES-CBC (64 bit)"
                                                "2" -> "DES-EDE-CBC (128 bit 3DES)"
                                                "3" -> "BF-CBC (128 bit Blowfish)"
                                                "4" -> "AES-128-CBC (128 bit)"
                                                "5" -> "AES-192-CBC (192 bit)"
                                                "6" -> "DES-EDE3-CBC (192 bit 3DES)"
                                                "7" -> "DESX-CBC (192 bit)"
                                                "8" -> "AES-256-CBC (256 bit)"
                                                "9" -> "CAMELLIA-128-CBC"
                                                "10" -> "CAMELLIA-192-CBC"
                                                "11" -> "CAMELLIA-256-CBC"
                                                "12" -> "AES-128-GCM"
                                                "13" -> "AES-192-GCM"
                                                "14" -> "AES-256-GCM"
                                                else -> "Blowfish"
                                            }
                                            Text(cipherStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = cipherExpanded, onDismissRequest = { cipherExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Blowfish 128 bit") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "3")); cipherExpanded = false })
                                            DropdownMenuItem(text = { Text("AES-128-CBC") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "4")); cipherExpanded = false })
                                            DropdownMenuItem(text = { Text("AES-256-CBC") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "8")); cipherExpanded = false })
                                            DropdownMenuItem(text = { Text("AES-128-GCM") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "12")); cipherExpanded = false })
                                            DropdownMenuItem(text = { Text("AES-256-GCM") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "14")); cipherExpanded = false })
                                            DropdownMenuItem(text = { Text("DES-EDE-CBC (128 bit 3DES)") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "2")); cipherExpanded = false })
                                            DropdownMenuItem(text = { Text("None") }, onClick = { viewModel.updateConfig(config.copy(ovpnCipher = "0")); cipherExpanded = false })
                                        }
                                    }

                                    // LZO
                                    var lzoExpanded by remember { mutableStateOf(false) }
                                    Text(t("LZO Compression Mode"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { lzoExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val lzoStr = when (config.ovpnLzo) {
                                                "0" -> t("Disabled")
                                                "1" -> "Adaptive Compression"
                                                "2" -> "Enabled"
                                                "3" -> "Stub Compression"
                                                else -> "Enabled"
                                            }
                                            Text(lzoStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = lzoExpanded, onDismissRequest = { lzoExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("Disabled")) }, onClick = { viewModel.updateConfig(config.copy(ovpnLzo = "0")); lzoExpanded = false })
                                            DropdownMenuItem(text = { Text("Adaptive Compression") }, onClick = { viewModel.updateConfig(config.copy(ovpnLzo = "1")); lzoExpanded = false })
                                            DropdownMenuItem(text = { Text("Enabled") }, onClick = { viewModel.updateConfig(config.copy(ovpnLzo = "2")); lzoExpanded = false })
                                            DropdownMenuItem(text = { Text("Stub Compression") }, onClick = { viewModel.updateConfig(config.copy(ovpnLzo = "3")); lzoExpanded = false })
                                        }
                                    }

                                    // HMAC TLS Auth Sign
                                    var hmacExpanded by remember { mutableStateOf(false) }
                                    Text(t("HMAC Sign (TLS Auth)"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { hmacExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val hmacStr = when (config.ovpnHmacSign) {
                                                "0" -> t("No")
                                                "1" -> t("Yes (Additional tls-auth)")
                                                else -> t("No")
                                            }
                                            Text(hmacStr, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = hmacExpanded, onDismissRequest = { hmacExpanded = false }) {
                                            DropdownMenuItem(text = { Text(t("No")) }, onClick = { viewModel.updateConfig(config.copy(ovpnHmacSign = "0")); hmacExpanded = false })
                                            DropdownMenuItem(text = { Text(t("Yes (Additional tls-auth)")) }, onClick = { viewModel.updateConfig(config.copy(ovpnHmacSign = "1")); hmacExpanded = false })
                                        }
                                    }

                                    // Custom config
                                    Text(t("Custom OpenVPN Configuration Options"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    OutlinedTextField(
                                        value = config.ovpnCustomConfig,
                                        onValueChange = { viewModel.updateConfig(config.copy(ovpnCustomConfig = it)) },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                        placeholder = { Text("# Enter extra OpenVPN options here...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    )
                                }
                            }
                        }

                        2 -> { // Certificates Tab
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Root CA
                                    Text("ca.crt (Root CA Certificate):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    OutlinedTextField(
                                        value = config.caCert,
                                        onValueChange = { viewModel.updateConfig(config.copy(caCert = it)) },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        placeholder = { Text("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                    )

                                    if (config.ovpnAuthType == "0") { // TLS certs only requires client crt/key
                                        // Client Certificate
                                        Text("client.crt (Client Certificate):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        OutlinedTextField(
                                            value = config.clientCert,
                                            onValueChange = { viewModel.updateConfig(config.copy(clientCert = it)) },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            placeholder = { Text("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        )

                                        // Client Key
                                        Text("client.key (Client Private Key):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        OutlinedTextField(
                                            value = config.clientKey,
                                            onValueChange = { viewModel.updateConfig(config.copy(clientKey = it)) },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            placeholder = { Text("-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        )
                                    }

                                    if (config.ovpnHmacSign == "1") { // TLS Auth Key
                                        // TLS Auth Key
                                        Text("ta.key (TLS Auth Key):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        OutlinedTextField(
                                            value = config.tlsAuthKey,
                                            onValueChange = { viewModel.updateConfig(config.copy(tlsAuthKey = it)) },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            placeholder = { Text("#\n# 2048 bit OpenVPN static key\n#\n-----BEGIN OpenVPN Static key V1-----\n...\n-----END OpenVPN Static key V1-----") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        )
                                    }
                                }
                            }
                        }

                        3 -> { // Post Script Tab
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(t("Post-Connection Bash Script"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(t("This script runs automatically on the router after a successful VPN connection event."), color = Color.Gray, fontSize = 11.sp)
                                    OutlinedTextField(
                                        value = config.postScript,
                                        onValueChange = { viewModel.updateConfig(config.copy(postScript = it)) },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                        placeholder = { Text("#!/bin/sh\n# Custom commands here...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                    )
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
}
