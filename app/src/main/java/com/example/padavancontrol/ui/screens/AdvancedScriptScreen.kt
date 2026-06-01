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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.AdvancedScriptViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.ScriptConfig
import com.example.padavancontrol.theme.ArcherTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScriptScreen(
    section: String, // "Scripts", "Detector"
    viewModel: AdvancedScriptViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagePath = remember(section) {
        when (section) {
            "Scripts" -> "Advanced_Scripts_Content.asp"
            "Detector" -> "Advanced_InetDetect_Content.asp"
            else -> "Advanced_Scripts_Content.asp"
        }
    }

    val title = remember(section) {
        when (section) {
            "Scripts" -> "Custom Startup Bash Scripts"
            "Detector" -> "Internet Watchdog Detector"
            else -> "Customization"
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Scripts UI active sub-tab selector
    var activeScriptTab by remember { mutableStateOf("Startup") } // "Startup", "WanUp", "WanDown", "Shutdown", "Firewall"

    // Watchdog drop down
    var actionExpanded by remember { mutableStateOf(false) }

    // Validation error states
    var host1Error by remember { mutableStateOf<String?>(null) }
    var host2Error by remember { mutableStateOf<String?>(null) }
    var periodError by remember { mutableStateOf<String?>(null) }

    fun loadConfig() {
        viewModel.loadConfig(pagePath)
    }

    LaunchedEffect(key1 = section) {
        loadConfig()
    }

    fun validateInputs(): Boolean {
        var isValid = true
        host1Error = null
        host2Error = null
        periodError = null

        if (section == "Detector" && uiState.config.pingEnabled) {
            if (uiState.config.pingHost1.isEmpty()) {
                host1Error = "Host cannot be empty."
                isValid = false
            }
            if (uiState.config.pingHost2.isEmpty()) {
                host2Error = "Host cannot be empty."
                isValid = false
            }
            if (uiState.config.pingPeriod < 5 || uiState.config.pingPeriod > 3600) {
                periodError = "Interval must be between 5 and 3600 seconds."
                isValid = false
            }
        }

        return isValid
    }

    fun saveConfig() {
        if (!validateInputs()) {
            Toast.makeText(context, "Please fix verification errors.", Toast.LENGTH_SHORT).show()
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
                    Text("Error loading scripts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        text = uiState.loadStatus.ifEmpty { "Fetching customized scripts..." },
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
                    // Category Info Panel
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
                                    text = "System Customization Core",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Adjust custom startup shells or ping watchdog properties. These bash script instructions run automatically at specific hardware cycles.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Render sections
                    when (section) {
                        "Scripts" -> {
                            // Sub-tabs segment switcher
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Startup", "WanUp", "WanDown").forEach { tab ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (activeScriptTab == tab) ArcherTeal else Color.Transparent)
                                                    .clickable { activeScriptTab = tab }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tab,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (activeScriptTab == tab) Color.White else Color.Gray
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Shutdown", "Firewall").forEach { tab ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (activeScriptTab == tab) ArcherTeal else Color.Transparent)
                                                    .clickable { activeScriptTab = tab }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tab,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (activeScriptTab == tab) Color.White else Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Active editor card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val activeText = when (activeScriptTab) {
                                        "Startup" -> uiState.config.scriptStartup
                                        "WanUp" -> uiState.config.scriptWanUp
                                        "WanDown" -> uiState.config.scriptWanDown
                                        "Shutdown" -> uiState.config.scriptShutdown
                                        "Firewall" -> uiState.config.scriptIpRules
                                        else -> ""
                                    }
                                    val desc = when (activeScriptTab) {
                                        "Startup" -> "Startup Shell - executed immediately after kernel boot finish."
                                        "WanUp" -> "Post-WAN UP - executed once external internet dials up."
                                        "WanDown" -> "Post-WAN Down / Button script - triggered on carrier cut."
                                        "Shutdown" -> "Shutdown Script - executed before system restarts or powers down."
                                        "Firewall" -> "Firewall Rules - executed when Netfilter iptables load."
                                        else -> ""
                                    }

                                    Text(activeScriptTab + " Bash Commands", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 6.dp))
                                    Text(desc, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp))

                                     OutlinedTextField(
                                         value = activeText,
                                         onValueChange = { txt ->
                                             val updated = when (activeScriptTab) {
                                                 "Startup" -> uiState.config.copy(scriptStartup = txt)
                                                 "WanUp" -> uiState.config.copy(scriptWanUp = txt)
                                                 "WanDown" -> uiState.config.copy(scriptWanDown = txt)
                                                 "Shutdown" -> uiState.config.copy(scriptShutdown = txt)
                                                 "Firewall" -> uiState.config.copy(scriptIpRules = txt)
                                                 else -> uiState.config
                                             }
                                             viewModel.updateConfig(updated)
                                         },
                                         label = { Text("Code shell (sh)") },
                                        placeholder = { Text("#!/bin/sh\n# type your scripts here...") },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                    )
                                }
                            }
                        }

                        "Detector" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Internet Watchdog Setup", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text("Enable Watchdog Ping Check", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Monitors network link status dynamically", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = uiState.config.pingEnabled,
                                            onCheckedChange = { viewModel.updateConfig(uiState.config.copy(pingEnabled = it)) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ArcherTeal, checkedTrackColor = ArcherTeal.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (uiState.config.pingEnabled) {
                                        OutlinedTextField(
                                            value = uiState.config.pingHost1,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost1 = it)) },
                                            label = { Text("Primary Target IP / Domain") },
                                            placeholder = { Text("8.8.8.8") },
                                            isError = host1Error != null,
                                            supportingText = host1Error?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingHost2,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost2 = it)) },
                                            label = { Text("Backup Target IP / Domain") },
                                            placeholder = { Text("114.114.114.114") },
                                            isError = host2Error != null,
                                            supportingText = host2Error?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                         OutlinedTextField(
                                             value = uiState.config.pingPeriod.toString(),
                                             onValueChange = { viewModel.updateConfig(uiState.config.copy(pingPeriod = it.toIntOrNull() ?: 30)) },
                                             label = { Text("Ping Check Interval Rate (seconds)") },
                                            isError = periodError != null,
                                            supportingText = periodError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )

                                        // watchdog lost action dropdown
                                        Text("Action on Network Failure", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { actionExpanded = true }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val desc = when (uiState.config.pingAction) {
                                                    "0" -> "Restart WAN connection link"
                                                    "1" -> "Reboot physical router"
                                                    "2" -> "Execute custom events script"
                                                    else -> "Restart WAN (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = actionExpanded, onDismissRequest = { actionExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Restart WAN Connection link") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingAction = "0")); actionExpanded = false })
                                                DropdownMenuItem(text = { Text("Reboot Router") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingAction = "1")); actionExpanded = false })
                                                DropdownMenuItem(text = { Text("Run custom failure scripts") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingAction = "2")); actionExpanded = false })
                                            }
                                        }
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
