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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.WirelessConfig
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.AdvancedWirelessViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWirelessScreen(
    is5GHz: Boolean,
    section: String,
    viewModel: AdvancedWirelessViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(is5GHz, section) {
        when (section) {
            "General" -> if (is5GHz) "Advanced_Wireless_Content.asp" else "Advanced_Wireless2g_Content.asp"
            "Guest" -> if (is5GHz) "Advanced_WGuest_Content.asp" else "Advanced_WGuest2g_Content.asp"
            "Bridge" -> if (is5GHz) "Advanced_WMode_Content.asp" else "Advanced_WMode2g_Content.asp"
            "Professional" -> if (is5GHz) "Advanced_Wireless_Content.asp" else "Advanced_Wireless2g_Content.asp"
            "RADIUS" -> if (is5GHz) "Advanced_WSecurity_Content.asp" else "Advanced_WSecurity2g_Content.asp"
            "MAC Filter" -> if (is5GHz) "Advanced_MACFilter_Content.asp" else "Advanced_MACFilter2g_Content.asp"
            else -> if (is5GHz) "Advanced_Wireless_Content.asp" else "Advanced_Wireless2g_Content.asp"
        }
    }

    val title = remember(is5GHz, section) {
        val band = if (is5GHz) "5GHz" else "2.4GHz"
        when (section) {
            "General" -> "Wireless $band - General Settings"
            "Guest" -> "Wireless $band - Guest AP"
            "Bridge" -> "Wireless $band - Bridge (WDS)"
            "Professional" -> "Wireless $band - Professional Parameters"
            "RADIUS" -> "Wireless $band - RADIUS Settings"
            "MAC Filter" -> "Wireless $band - Wireless MAC Filter"
            else -> "Wireless $band - Settings"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showScanDialog by remember { mutableStateOf(false) }
    var scanSearchQuery by remember { mutableStateOf("") }

    // Validation State
    var ssidError by remember { mutableStateOf<String?>(null) }
    var wpaPskError by remember { mutableStateOf<String?>(null) }
    var guestSsidError by remember { mutableStateOf<String?>(null) }
    var guestWpaPskError by remember { mutableStateOf<String?>(null) }
    var staSsidError by remember { mutableStateOf<String?>(null) }
    var staWpaPskError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = pagePath) {
        viewModel.loadConfig(pagePath, is5GHz)
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Client Validation
    fun validateInputs(): Boolean {
        var isValid = true
        ssidError = null
        wpaPskError = null
        guestSsidError = null
        guestWpaPskError = null
        staSsidError = null
        staWpaPskError = null

        val config = uiState.config
        if (section == "General" || section == "Professional") {
            if (config.ssid.isBlank()) {
                ssidError = "SSID name cannot be blank."
                isValid = false
            }
            if (config.authMode.contains("psk") && (config.wpaPsk.length < 8 || config.wpaPsk.length > 64)) {
                wpaPskError = "WPA password must be between 8 and 64 characters."
                isValid = false
            }
        }

        if (section == "Guest" && config.guestEnabled) {
            if (config.guestSsid.isBlank()) {
                guestSsidError = "Guest SSID name cannot be blank."
                isValid = false
            }
            if (config.guestAuthMode.contains("psk") && (config.guestWpaPsk.length < 8 || config.guestWpaPsk.length > 64)) {
                guestWpaPskError = "Guest WPA password must be between 8 and 64 characters."
                isValid = false
            }
        }

        if (section == "Bridge" && (config.modeX == "3" || config.modeX == "4")) {
            if (config.staSsid.isBlank()) {
                staSsidError = "AP Client SSID name cannot be blank."
                isValid = false
            }
            if (config.staAuthMode.contains("psk") && (config.staWpaPsk.length < 8 || config.staWpaPsk.length > 64)) {
                staWpaPskError = "AP Client password must be between 8 and 64 characters."
                isValid = false
            }
        }

        return isValid
    }

    // Save config
    fun saveConfig() {
        if (!validateInputs()) {
            Toast.makeText(context, "Please fix form validation errors first.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.saveConfig(pagePath, is5GHz)
    }

    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            title = { Text("Site Survey - Select Uplink AP", fontWeight = FontWeight.Bold, color = ArcherTeal) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (!uiState.isScanning && uiState.scanResults.isNotEmpty()) {
                        OutlinedTextField(
                            value = scanSearchQuery,
                            onValueChange = { scanSearchQuery = it },
                            label = { Text("Search SSID...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                color = ArcherTeal,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (uiState.scanResults.isEmpty()) {
                            Text(
                                "No wireless networks found. Click rescan to retry.",
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val filteredNetworks = remember(uiState.scanResults, scanSearchQuery) {
                                uiState.scanResults.filter {
                                    it.ssid.contains(scanSearchQuery, ignoreCase = true)
                                }
                            }

                            if (filteredNetworks.isEmpty()) {
                                Text(
                                    "No networks match your search.",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredNetworks.forEach { network ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateConfig(
                                                        uiState.config.copy(
                                                            staSsid = network.ssid,
                                                            channel = network.channel,
                                                            staAuthMode = if (network.security.contains("WPA", ignoreCase = true)) "psk" else "open",
                                                            staWpaMode = if (network.security.contains("WPA2", ignoreCase = true)) "2" else "1",
                                                            staCrypto = if (network.security.contains("AES", ignoreCase = true)) "aes" else "tkip"
                                                        )
                                                    )
                                                    showScanDialog = false
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(network.ssid, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("Channel: ${network.channel} • Security: ${network.security}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${network.signal}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArcherTeal)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.scanNetworks(is5GHz) },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                ) {
                    Text("Rescan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScanDialog = false }) {
                    Text("Close")
                }
            }
        )
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
                        IconButton(onClick = { viewModel.loadConfig(pagePath, is5GHz) }) {
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
                    Text(
                        "Reading router NVRAM parameters...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
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
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connection Error",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.loadConfig(pagePath, is5GHz) },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                    ) {
                        Text("Retry Connection", color = Color.White)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual Glass Header card
                    GlassHeaderCard(is5GHz = is5GHz, section = section)

                    // Render Section Form Fields dynamically
                    when (section) {
                        "General" -> GeneralSettingsForm(
                            is5GHz = is5GHz,
                            config = uiState.config,
                            ssidError = ssidError,
                            wpaPskError = wpaPskError,
                            onConfigChange = { viewModel.updateConfig(it) }
                        )
                        "Guest" -> GuestSettingsForm(
                            config = uiState.config,
                            ssidError = guestSsidError,
                            wpaPskError = guestWpaPskError,
                            onConfigChange = { viewModel.updateConfig(it) }
                        )
                        "Bridge" -> BridgeSettingsForm(
                            is5GHz = is5GHz,
                            config = uiState.config,
                            staSsidError = staSsidError,
                            staWpaPskError = staWpaPskError,
                            onConfigChange = { viewModel.updateConfig(it) },
                            onScanClick = {
                                scanSearchQuery = ""
                                viewModel.scanNetworks(is5GHz)
                                showScanDialog = true
                            }
                        )
                        "Professional" -> ProfessionalSettingsForm(
                            config = uiState.config,
                            onConfigChange = { viewModel.updateConfig(it) }
                        )
                        else -> PlaceholderSettingsForm(title = title)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply settings button
                    Button(
                        onClick = { saveConfig() },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("COMMITTING CHANGES...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Apply", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("APPLY SETTINGS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun GlassHeaderCard(is5GHz: Boolean, section: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ArcherTeal, ArcherTeal.copy(alpha = 0.5f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (is5GHz) "5G" else "2G",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Module Focus: $section",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configuring Newifi D2 hardware interface parameters directly.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GeneralSettingsForm(
    is5GHz: Boolean,
    config: WirelessConfig,
    ssidError: String?,
    wpaPskError: String?,
    onConfigChange: (WirelessConfig) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("General Radio Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)

            // Radio Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Wireless Radio Enable", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Deactivating cuts power supply to transceiver completely.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onConfigChange(config.copy(isEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.4f))
                )
            }

            if (config.isEnabled) {
                // SSID
                OutlinedTextField(
                    value = config.ssid,
                    onValueChange = { onConfigChange(config.copy(ssid = it)) },
                    label = { Text("Wireless Network SSID") },
                    isError = ssidError != null,
                    supportingText = ssidError?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Hide SSID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hide SSID (Block Broadcast)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Stops beacon broadcasts. Clients must connect manually.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.isClosed,
                        onCheckedChange = { onConfigChange(config.copy(isClosed = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal)
                    )
                }

                // Channel Bandwidth Dropdown
                val bandwidthOptions = if (is5GHz) {
                    listOf("20 MHz" to "0", "20/40 MHz" to "1", "20/40/80 MHz" to "2", "20/40/80/160 MHz" to "3")
                } else {
                    listOf("20 MHz" to "0", "20/40 MHz" to "1")
                }
                WirelessDropdownField(
                    label = "Channel Bandwidth",
                    selectedValue = bandwidthOptions.firstOrNull { it.second == config.bandwidth }?.first ?: "20/40 MHz",
                    options = bandwidthOptions.map { it.first },
                    onSelect = { selectedName ->
                        val code = bandwidthOptions.first { it.first == selectedName }.second
                        onConfigChange(config.copy(bandwidth = code))
                    }
                )

                // Wireless Channel Dropdown
                val channelOptions = if (is5GHz) {
                    listOf("Auto" to "0", "36" to "36", "40" to "40", "44" to "44", "48" to "48", "149" to "149", "153" to "153", "157" to "157", "161" to "161", "165" to "165")
                } else {
                    listOf("Auto" to "0", "1" to "1", "2" to "2", "3" to "3", "4" to "4", "5" to "5", "6" to "6", "7" to "7", "8" to "8", "9" to "9", "10" to "10", "11" to "11", "12" to "12", "13" to "13")
                }
                WirelessDropdownField(
                    label = "Wireless Channel",
                    selectedValue = channelOptions.firstOrNull { it.second == config.channel }?.first ?: "Auto",
                    options = channelOptions.map { it.first },
                    onSelect = { selectedName ->
                        val code = channelOptions.first { it.first == selectedName }.second
                        onConfigChange(config.copy(channel = code))
                    }
                )

                // Authentication Method
                val authOptions = listOf(
                    "Open System" to "open",
                    "Shared Key" to "shared",
                    "WPA-Personal" to "psk",
                    "WPA2-Personal" to "psk",
                    "WPA-Auto-Personal" to "psk"
                )
                WirelessDropdownField(
                    label = "Authentication Method",
                    selectedValue = if (config.authMode == "psk") {
                        when (config.wpaMode) {
                            "1" -> "WPA-Personal"
                            "2" -> "WPA2-Personal"
                            else -> "WPA-Auto-Personal"
                        }
                    } else if (config.authMode == "shared") "Shared Key" else "Open System",
                    options = authOptions.map { it.first },
                    onSelect = { selectedName ->
                        val code = authOptions.first { it.first == selectedName }.second
                        val wMode = when (selectedName) {
                            "WPA-Personal" -> "1"
                            "WPA2-Personal" -> "2"
                            else -> "0"
                        }
                        onConfigChange(config.copy(authMode = code, wpaMode = wMode))
                    }
                )

                if (config.authMode == "psk") {
                    // Encryption type
                    WirelessDropdownField(
                        label = "WPA Encryption",
                        selectedValue = if (config.crypto == "aes") "AES" else "TKIP+AES",
                        options = listOf("AES", "TKIP+AES"),
                        onSelect = { onConfigChange(config.copy(crypto = if (it == "AES") "aes" else "tkip+aes")) }
                    )

                    // Pre-Shared Password
                    OutlinedTextField(
                        value = config.wpaPsk,
                        onValueChange = { onConfigChange(config.copy(wpaPsk = it)) },
                        label = { Text("WPA Pre-Shared Key") },
                        isError = wpaPskError != null,
                        supportingText = wpaPskError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Refresh else Icons.Default.Lock,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GuestSettingsForm(
    config: WirelessConfig,
    ssidError: String?,
    wpaPskError: String?,
    onConfigChange: (WirelessConfig) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Guest Network (Multi-SSID)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)

            // Guest Enable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Guest AP Isolation Enable", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Provides a separate isolated SSID for guests.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = config.guestEnabled,
                    onCheckedChange = { onConfigChange(config.copy(guestEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal)
                )
            }

            if (config.guestEnabled) {
                // Guest SSID
                OutlinedTextField(
                    value = config.guestSsid,
                    onValueChange = { onConfigChange(config.copy(guestSsid = it)) },
                    label = { Text("Guest SSID Name") },
                    isError = ssidError != null,
                    supportingText = ssidError?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Guest closed (hide broadcast)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hide SSID (Broadcast Block)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Hides the guest SSID beacon broadcast.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.guestClosed,
                        onCheckedChange = { onConfigChange(config.copy(guestClosed = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal)
                    )
                }

                // Local Lan Isolation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LAN Isolation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Stops guests from pinging LAN hosts / router admin portal.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.guestLanIsolate,
                        onCheckedChange = { onConfigChange(config.copy(guestLanIsolate = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal)
                    )
                }

                // AP isolation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Intra-AP Client Isolation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Stops guests from communicating with each other.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.guestApIsolate,
                        onCheckedChange = { onConfigChange(config.copy(guestApIsolate = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal)
                    )
                }

                // Auth
                WirelessDropdownField(
                    label = "Authentication Method",
                    selectedValue = when (config.guestAuthMode) {
                        "psk" -> if (config.guestWpaMode == "2") "WPA2-Personal" else "WPA-Personal"
                        else -> "Open System"
                    },
                    options = listOf("Open System", "WPA-Personal", "WPA2-Personal"),
                    onSelect = { selected ->
                        val code = if (selected.contains("WPA")) "psk" else "open"
                        val wMode = if (selected == "WPA2-Personal") "2" else "1"
                        onConfigChange(config.copy(guestAuthMode = code, guestWpaMode = wMode))
                    }
                )

                if (config.guestAuthMode == "psk") {
                    // Password
                    OutlinedTextField(
                        value = config.guestWpaPsk,
                        onValueChange = { onConfigChange(config.copy(guestWpaPsk = it)) },
                        label = { Text("Guest WPA Pre-Shared Key") },
                        isError = wpaPskError != null,
                        supportingText = wpaPskError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Refresh else Icons.Default.Lock,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BridgeSettingsForm(
    is5GHz: Boolean,
    config: WirelessConfig,
    staSsidError: String?,
    staWpaPskError: String?,
    onConfigChange: (WirelessConfig) -> Unit,
    onScanClick: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wireless Bridge (WDS / AP-Client)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)

            // AP Mode
            val apModes = listOf(
                "Access Point Only (Normal)" to "0",
                "WDS Lazy Mode" to "1",
                "WDS Bridge Mode" to "2",
                "AP Client Mode (Client Only)" to "3",
                "AP Client + AP Mode (Wireless Repeater)" to "4"
            )
            WirelessDropdownField(
                label = "Wireless AP Operating Mode",
                selectedValue = apModes.firstOrNull { it.second == config.modeX }?.first ?: "Access Point Only (Normal)",
                options = apModes.map { it.first },
                onSelect = { selected ->
                    val code = apModes.first { it.first == selected }.second
                    onConfigChange(config.copy(modeX = code))
                }
            )

            val mode = config.modeX
            if (mode == "1" || mode == "2") {
                // WDS configuration help card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "WDS Info", tint = ArcherTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "WDS bridge requires adding remote router's hardware MAC address in the list below.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (mode == "3" || mode == "4") {
                Text("AP Client Uplink connection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ArcherTeal)

                // WISP Mode
                WirelessDropdownField(
                    label = "AP Client Interface Role",
                    selectedValue = if (config.staWisp == "1") "WAN (Wireless ISP - Route Mode)" else "LAN Bridge (AP Bridge Mode)",
                    options = listOf("LAN Bridge (AP Bridge Mode)", "WAN (Wireless ISP - Route Mode)"),
                    onSelect = {
                        onConfigChange(config.copy(staWisp = if (it.contains("WAN")) "1" else "0"))
                    }
                )

                // Seek channel auto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto Seek Uplink Channel", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Scans and matches the host's operating channel.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.staAuto == "1",
                        onCheckedChange = { onConfigChange(config.copy(staAuto = if (it) "1" else "0")) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal)
                    )
                }

                // STA SSID with Scan Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = config.staSsid,
                        onValueChange = { onConfigChange(config.copy(staSsid = it)) },
                        label = { Text("Remote AP Host SSID") },
                        isError = staSsidError != null,
                        supportingText = staSsidError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Scan", fontWeight = FontWeight.Bold)
                    }
                }

                // STA Auth
                WirelessDropdownField(
                    label = "Uplink Authentication Mode",
                    selectedValue = when (config.staAuthMode) {
                        "psk" -> if (config.staWpaMode == "2") "WPA2-Personal" else "WPA-Personal"
                        else -> "Open System"
                    },
                    options = listOf("Open System", "WPA-Personal", "WPA2-Personal"),
                    onSelect = { selected ->
                        val code = if (selected.contains("WPA")) "psk" else "open"
                        val wMode = if (selected == "WPA2-Personal") "2" else "1"
                        onConfigChange(config.copy(staAuthMode = code, staWpaMode = wMode))
                    }
                )

                if (config.staAuthMode == "psk") {
                    // STA Crypto
                    WirelessDropdownField(
                        label = "Uplink Encryption Algorithm",
                        selectedValue = if (config.staCrypto == "aes") "AES (Recommended)" else "TKIP",
                        options = listOf("AES (Recommended)", "TKIP"),
                        onSelect = { onConfigChange(config.copy(staCrypto = if (it.contains("AES")) "aes" else "tkip")) }
                    )

                    // STA Password
                    OutlinedTextField(
                        value = config.staWpaPsk,
                        onValueChange = { onConfigChange(config.copy(staWpaPsk = it)) },
                        label = { Text("Remote AP WPA Password") },
                        isError = staWpaPskError != null,
                        supportingText = staWpaPskError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Refresh else Icons.Default.Lock,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessionalSettingsForm(
    config: WirelessConfig,
    onConfigChange: (WirelessConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Professional / Advanced parameters", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)

            // Tx Power
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transceiver Transmission Power", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${config.txPower}%", fontWeight = FontWeight.Bold, color = ArcherTeal, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = config.txPower.toFloat(),
                    onValueChange = { onConfigChange(config.copy(txPower = it.toInt())) },
                    valueRange = 0f..100f,
                    steps = 100,
                    colors = SliderDefaults.colors(thumbColor = ArcherTeal, activeTrackColor = ArcherTeal)
                )
                Text("Reducing Tx power limits range and limits interference in high density environments.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Country Code
            val countries = listOf(
                "USA (US)" to "US",
                "China (CN)" to "CN",
                "Europe (GB)" to "GB",
                "Taiwan (TW)" to "TW",
                "Japan (JP)" to "JP",
                "Russia (RU)" to "RU",
                "Australia (AU)" to "AU",
                "All Channels Debug (DB)" to "DB"
            )
            WirelessDropdownField(
                label = "Regulatory Region Code",
                selectedValue = countries.firstOrNull { it.second == config.countryCode }?.first ?: "USA (US)",
                options = countries.map { it.first },
                onSelect = { selected ->
                    val code = countries.first { it.first == selected }.second
                    onConfigChange(config.copy(countryCode = code))
                }
            )

            // Low RSSI Kick
            OutlinedTextField(
                value = if (config.kickStaRssiLow == 0) "" else config.kickStaRssiLow.toString(),
                onValueChange = {
                    val value = it.toIntOrNull() ?: 0
                    if (value in -100..0) {
                        onConfigChange(config.copy(kickStaRssiLow = value))
                    }
                },
                label = { Text("Low RSSI Roaming Kick threshold (dBm)") },
                supportingText = { Text("Range: -100 to 0. 0 = Disabled. Auto kicks low signal clients.", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Assoc Req RSSI
            OutlinedTextField(
                value = if (config.assocReqRssiThres == 0) "" else config.assocReqRssiThres.toString(),
                onValueChange = {
                    val value = it.toIntOrNull() ?: 0
                    if (value in -100..0) {
                        onConfigChange(config.copy(assocReqRssiThres = value))
                    }
                },
                label = { Text("Association Req RSSI threshold (dBm)") },
                supportingText = { Text("Range: -100 to 0. 0 = Disabled. Rejects connections under threshold.", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}

@Composable
fun PlaceholderSettingsForm(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Module Focus: RADIUS / MAC Filters", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)
            Text(
                "This module will be expanded with custom tables in Phase 3. The underlying Retrofit APIs and NVRAM binding hooks for $title are fully supported in our data layer.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Custom Slider for Material 3 slider compatibility
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    colors: androidx.compose.material3.SliderColors,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = colors,
        modifier = modifier
    )
}

@Composable
fun WirelessDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Indicator",
                    tint = ArcherTeal
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 14.sp) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
