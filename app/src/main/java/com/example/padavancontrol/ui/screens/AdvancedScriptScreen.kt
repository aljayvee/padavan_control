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

    // Active script dialog editor states
    var editingScriptKey by remember { mutableStateOf<String?>(null) } // "Init", "Start", "WanUp", "WanDown", "Shutdown", "Firewall", "EzButton"
    var editingScriptContent by remember { mutableStateOf("") }

    // Watchdog drop down states
    var actionExpanded by remember { mutableStateOf(false) }
    var pollModeExpanded by remember { mutableStateOf(false) }

    // Validation error states
    var host1Error by remember { mutableStateOf<String?>(null) }
    var periodError by remember { mutableStateOf<String?>(null) }
    var failPeriodError by remember { mutableStateOf<String?>(null) }
    var timeoutError by remember { mutableStateOf<String?>(null) }

    fun loadConfig() {
        viewModel.loadConfig(pagePath)
    }

    LaunchedEffect(key1 = section) {
        loadConfig()
    }

    fun validateInputs(): Boolean {
        var isValid = true
        host1Error = null
        periodError = null
        failPeriodError = null
        timeoutError = null

        if (section == "Detector" && uiState.config.pingPollMode != "0") {
            if (uiState.config.pingHost1.isEmpty()) {
                host1Error = "Host 1 cannot be empty."
                isValid = false
            }
            if (uiState.config.pingIntervalSuccess < 5 || uiState.config.pingIntervalSuccess > 3600) {
                periodError = "Interval must be between 5 and 3600 seconds."
                isValid = false
            }
            if (uiState.config.pingIntervalFail < 5 || uiState.config.pingIntervalFail > 3600) {
                failPeriodError = "Interval must be between 5 and 3600 seconds."
                isValid = false
            }
            if (uiState.config.pingTimeout < 1 || uiState.config.pingTimeout > 60) {
                timeoutError = "Timeout must be between 1 and 60 seconds."
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
                            Text("Custom User Scripts", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                            val scriptItems = listOf(
                                Triple("Init", "Run Before Router Initialized", "scripts.init_script.sh"),
                                Triple("Start", "Run After Router Started", "scripts.start_script.sh"),
                                Triple("Shutdown", "Run Before Router Shutdown", "scripts.shutdown_script.sh"),
                                Triple("WanUpDown", "Run After WAN Up/Down Events", "scripts.post_wan_script.sh / post_wandn_script.sh"),
                                Triple("Firewall", "Run After Firewall Rules Restarted", "scripts.post_iptables_script.sh"),
                                Triple("EzButton", "Run On Press WPS/FN Ez-Buttons", "scripts.ez_buttons_script.sh")
                            )

                            scriptItems.forEach { (key, label, filename) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clickable {
                                            if (key == "WanUpDown") {
                                                editingScriptKey = "WanUp"
                                                editingScriptContent = uiState.config.scriptWanUp
                                            } else {
                                                editingScriptKey = key
                                                editingScriptContent = when (key) {
                                                    "Init" -> uiState.config.scriptInit
                                                    "Start" -> uiState.config.scriptStart
                                                    "Shutdown" -> uiState.config.scriptShutdown
                                                    "Firewall" -> uiState.config.scriptIpRules
                                                    "EzButton" -> uiState.config.scriptEzButton
                                                    else -> ""
                                                }
                                            }
                                        }
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE0F2F1)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$", color = ArcherTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                                            Text(filename, color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Edit Script",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        "Detector" -> {
                            // Watchdog Mode Card
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

                                    Text("Internet Detector Poll Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable { pollModeExpanded = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val desc = when (uiState.config.pingPollMode) {
                                                "0" -> "Disabled"
                                                "1" -> "Ping (ICMP Echo)"
                                                "2" -> "TCP Connection check"
                                                else -> "Disabled"
                                            }
                                            Text(desc, color = Color.Black, fontSize = 14.sp)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                        DropdownMenu(expanded = pollModeExpanded, onDismissRequest = { pollModeExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Disabled") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingPollMode = "0", pingEnabled = false)); pollModeExpanded = false })
                                            DropdownMenuItem(text = { Text("Ping (ICMP Echo)") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingPollMode = "1", pingEnabled = true)); pollModeExpanded = false })
                                            DropdownMenuItem(text = { Text("TCP Connection check") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingPollMode = "2", pingEnabled = true)); pollModeExpanded = false })
                                        }
                                    }

                                    if (uiState.config.pingPollMode != "0") {
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
                                                    "2" -> "Execute custom failure script"
                                                    else -> "Restart WAN (0)"
                                                }
                                                Text(desc, color = Color.Black, fontSize = 14.sp)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                            DropdownMenu(expanded = actionExpanded, onDismissRequest = { actionExpanded = false }) {
                                                DropdownMenuItem(text = { Text("Restart WAN connection link") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingAction = "0")); actionExpanded = false })
                                                DropdownMenuItem(text = { Text("Reboot Router") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingAction = "1")); actionExpanded = false })
                                                DropdownMenuItem(text = { Text("Execute custom failure script") }, onClick = { viewModel.updateConfig(uiState.config.copy(pingAction = "2")); actionExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.config.pingPollMode != "0") {
                                // List of Internet Hosts Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("List of Internet Hosts for Check Connection", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))
                                        Text("Provide target IP addresses or domain names. For TCP Connection mode, add port numbers (e.g. 1.1.1.1:53).", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp))

                                        OutlinedTextField(
                                            value = uiState.config.pingHost1,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost1 = it)) },
                                            label = { Text("Remote Server Address and Port 1") },
                                            placeholder = { Text("8.8.8.8") },
                                            isError = host1Error != null,
                                            supportingText = host1Error?.let { { Text(it) } },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingHost2,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost2 = it)) },
                                            label = { Text("Remote Server Address and Port 2") },
                                            placeholder = { Text("114.114.114.114") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingHost3,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost3 = it)) },
                                            label = { Text("Remote Server Address and Port 3") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingHost4,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost4 = it)) },
                                            label = { Text("Remote Server Address and Port 4") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingHost5,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost5 = it)) },
                                            label = { Text("Remote Server Address and Port 5") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingHost6,
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingHost6 = it)) },
                                            label = { Text("Remote Server Address and Port 6") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                // Polling settings Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Internet Hosts Polling Settings", fontWeight = FontWeight.Bold, color = ArcherTeal, modifier = Modifier.padding(bottom = 16.dp))

                                        OutlinedTextField(
                                            value = uiState.config.pingIntervalSuccess.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingIntervalSuccess = it.toIntOrNull() ?: 30, pingPeriod = it.toIntOrNull() ?: 30)) },
                                            label = { Text("Poll Interval After Connection Success (s)") },
                                            isError = periodError != null,
                                            supportingText = periodError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingIntervalFail.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingIntervalFail = it.toIntOrNull() ?: 10)) },
                                            label = { Text("Poll Interval After Connection Failed (s)") },
                                            isError = failPeriodError != null,
                                            supportingText = failPeriodError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = uiState.config.pingTimeout.toString(),
                                            onValueChange = { viewModel.updateConfig(uiState.config.copy(pingTimeout = it.toIntOrNull() ?: 5)) },
                                            label = { Text("TCP Connection Timeout (s)") },
                                            isError = timeoutError != null,
                                            supportingText = timeoutError?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Script editor dialog overlay
                    if (editingScriptKey != null) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { editingScriptKey = null }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                                    .padding(16.dp)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    val titleText = when (editingScriptKey) {
                                        "Init" -> "Before Router Initialized"
                                        "Start" -> "After Router Started"
                                        "Shutdown" -> "Before Router Shutdown"
                                        "WanUp" -> "After WAN Up Event"
                                        "WanDown" -> "After WAN Down Event"
                                        "Firewall" -> "After Firewall Rules Restarted"
                                        "EzButton" -> "On Press WPS/FN Ez-Buttons"
                                        else -> "Script Editor"
                                    }
                                    
                                    val descText = when (editingScriptKey) {
                                        "Init" -> "Executes before any network modules or services are started."
                                        "Start" -> "Executes immediately after kernel boot finish."
                                        "Shutdown" -> "Executes before system restarts or powers down."
                                        "WanUp" -> "Executes once WAN dial up completes successfully."
                                        "WanDown" -> "Executes once WAN dials down or connection fails."
                                        "Firewall" -> "Executes after iptables rules are flushed/reapplied."
                                        "EzButton" -> "Executes when physical WPS or Ez-button is pressed."
                                        else -> ""
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(titleText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ArcherTeal)
                                        if (editingScriptKey == "WanUp" || editingScriptKey == "WanDown") {
                                            Row(
                                                modifier = Modifier
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                                    .clip(RoundedCornerShape(6.dp))
                                            ) {
                                                Text(
                                                    "WAN Up",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (editingScriptKey == "WanUp") Color.White else Color.Gray,
                                                    modifier = Modifier
                                                        .background(if (editingScriptKey == "WanUp") ArcherTeal else Color.Transparent)
                                                        .clickable {
                                                            editingScriptKey = "WanUp"
                                                            editingScriptContent = uiState.config.scriptWanUp
                                                        }
                                                        .padding(6.dp)
                                                )
                                                Text(
                                                    "WAN Down",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (editingScriptKey == "WanDown") Color.White else Color.Gray,
                                                    modifier = Modifier
                                                        .background(if (editingScriptKey == "WanDown") ArcherTeal else Color.Transparent)
                                                        .clickable {
                                                            editingScriptKey = "WanDown"
                                                            editingScriptContent = uiState.config.scriptWanDown
                                                        }
                                                        .padding(6.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(descText, fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = editingScriptContent,
                                        onValueChange = { editingScriptContent = it },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        placeholder = { Text("#!/bin/sh\n# Enter custom actions here...") }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { editingScriptKey = null },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Cancel", color = Color.Black)
                                        }
                                        Button(
                                            onClick = {
                                                val updated = when (editingScriptKey) {
                                                    "Init" -> uiState.config.copy(scriptInit = editingScriptContent)
                                                    "Start" -> uiState.config.copy(scriptStart = editingScriptContent, scriptStartup = editingScriptContent)
                                                    "Shutdown" -> uiState.config.copy(scriptShutdown = editingScriptContent)
                                                    "WanUp" -> uiState.config.copy(scriptWanUp = editingScriptContent)
                                                    "WanDown" -> uiState.config.copy(scriptWanDown = editingScriptContent)
                                                    "Firewall" -> uiState.config.copy(scriptIpRules = editingScriptContent)
                                                    "EzButton" -> uiState.config.copy(scriptEzButton = editingScriptContent)
                                                    else -> uiState.config
                                                }
                                                viewModel.updateConfig(updated)
                                                editingScriptKey = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                                        ) {
                                            Text("Done", color = Color.White)
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
