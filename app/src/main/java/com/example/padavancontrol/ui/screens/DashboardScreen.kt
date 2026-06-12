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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.PortLink
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.theme.ArcherWarning
import com.example.padavancontrol.ui.viewmodels.DashboardEvent
import com.example.padavancontrol.ui.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToShellConsole: () -> Unit,
    onNavigateToLogViewer: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCpuDetailsModal by remember { mutableStateOf(false) }
    var showRamDetailsModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.setPollingEnabled(true)
        onDispose {
            viewModel.setPollingEnabled(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = ArcherTeal
                        )
                    }
                },
                title = {
                    Column {
                        Text("newifi D2", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            uiState.routerIp,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = ArcherTeal
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = t("Settings"),
                            tint = ArcherTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isRefreshing && uiState.systemStatus == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ArcherTeal)
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))

                // Error Alert Banner
                uiState.errorMessage?.let { errorText ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFFE74C3C), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE74C3C).copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Connection Error",
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Hardware Mismatch Warning Card
                uiState.hardwareWarning?.let { warningText ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, ArcherWarning, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ArcherWarning.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Hardware Mismatch Warning",
                                tint = ArcherWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = warningText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Internet Status Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isConnected = uiState.wanStatus?.isConnected ?: false
                        val dotColor = if (isConnected) Color(0xFF2ECC71) else Color(0xFFE74C3C)

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isConnected) t("Internet Connected") else t("No Internet Access"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${t("IP")}: ${uiState.wanStatus?.wanIp ?: "0.0.0.0"} • ${t("Type")}: ${uiState.wanStatus?.connectionType ?: "DHCP"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Physical Ethernet RJ45 Ports Panel
                if (uiState.lanLinks.isNotEmpty()) {
                    EthernetPortsPanel(ports = uiState.lanLinks)
                }

                // CPU & Memory Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CpuStatCard(
                            status = uiState.systemStatus,
                            progress = (uiState.systemStatus?.cpuUsage ?: 0) / 100f,
                            onCardClick = { showCpuDetailsModal = true },
                            modifier = Modifier.height(130.dp)
                        )
                    }

                    val totalRam = uiState.systemStatus?.ramTotal ?: 1L
                    val usedRam = uiState.systemStatus?.ramUsed ?: 0L
                    val ramPercent = (usedRam.toFloat() / totalRam.toFloat())
                    val usedMB = usedRam / (1024L * 1024L)
                    val totalMB = totalRam / (1024L * 1024L)
                    
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(
                            title = t("RAM Usage"),
                            value = "$usedMB / $totalMB MB",
                            extra = "${(ramPercent * 100).toInt()}% Used",
                            progress = ramPercent,
                            onCardClick = { showRamDetailsModal = true },
                            modifier = Modifier.height(130.dp)
                        )
                    }
                }



                // Interactive Wi-Fi Toggles Card
                WifiControlCard(
                    wifi2GEnabled = uiState.wifi2GEnabled,
                    wifi5GEnabled = uiState.wifi5GEnabled,
                    onToggle2G = { enable -> viewModel.toggleWifi2G(enable) },
                    onToggle5G = { enable -> viewModel.toggleWifi5G(enable) }
                )

                // Premium Admin Quick Actions Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = t("Administrative Actions"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ArcherTeal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Filled.Refresh,
                                label = t("Reboot"),
                                onClick = { viewModel.showRebootDialog() },
                                tint = Color(0xFFE74C3C)
                            )
                            QuickActionButton(
                                icon = Icons.Filled.PowerSettingsNew,
                                label = t("Shutdown"),
                                onClick = { viewModel.showShutdownDialog() },
                                tint = Color(0xFFE67E22)
                            )
                            QuickActionButton(
                                icon = Icons.Filled.Save,
                                label = t("Save"),
                                onClick = { viewModel.commitFlash() },
                                tint = ArcherTeal
                            )
                            QuickActionButton(
                                icon = Icons.AutoMirrored.Filled.Article,
                                label = t("Syslogs"),
                                onClick = onNavigateToLogViewer,
                                tint = ArcherTeal
                            )
                            QuickActionButton(
                                icon = Icons.Filled.Terminal,
                                label = t("Console"),
                                onClick = onNavigateToShellConsole,
                                tint = ArcherTeal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (uiState.showRebootDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRebootDialog() },
            title = { Text(t("Reboot Router")) },
            text = { Text(t("Are you sure you want to reboot the newifi D2 router? It will take about 40 seconds to power back on.")) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmReboot() }
                ) {
                    Text(t("REBOOT"), color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRebootDialog() }) {
                    Text(t("CANCEL"))
                }
            }
        )
    }

    if (uiState.showShutdownDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissShutdownDialog() },
            title = { Text(t("Power Off Router")) },
            text = { Text(t("Are you sure you want to power down the newifi D2 router? You will need to manually toggle the physical power switch on the device to start it again.")) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmShutdown() }
                ) {
                    Text(t("SHUTDOWN"), color = Color(0xFFE67E22), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissShutdownDialog() }) {
                    Text(t("CANCEL"))
                }
            }
        )
    }

    if (showCpuDetailsModal) {
        val status = uiState.systemStatus
        val busyVal = status?.cpuBusyPercent ?: 0
        val userVal = status?.cpuUserPercent ?: 0
        val sysVal = status?.cpuSysPercent ?: 0
        val sirqVal = status?.cpuSirqPercent ?: 0
        val irqVal = status?.cpuIrqPercent ?: 0
        val idleVal = status?.cpuIdlePercent ?: 99

        val days = (status?.uptime ?: 0L) / 86400
        val hours = ((status?.uptime ?: 0L) % 86400) / 3600
        val minutes = ((status?.uptime ?: 0L) % 3600) / 60
        val uptimeFormatted = "${days}d ${String.format("%02d", hours)}h ${String.format("%02d", minutes)}m"

        AlertDialog(
            onDismissRequest = { showCpuDetailsModal = false },
            title = { Text(t("CPU Performance Diagnostics"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CPU Utilization Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("busy: $busyVal%", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("idle: $idleVal%", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("user: $userVal%", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("system: $sysVal%", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("sirq: $sirqVal%", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("irq: $irqVal%", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("System Performance"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MemoryRow(t("CPU Temperature"), "${status?.cpuTemp ?: 0f}°C")
                        MemoryRow(t("Load Average"), status?.loadAvg ?: "0.00 0.00 0.00")
                        MemoryRow(t("System Uptime"), uptimeFormatted)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("Raw CPU Ticks"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArcherTeal)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MemoryRow("Total Ticks", "${status?.cpuTotal ?: 0L}")
                        MemoryRow("Busy Ticks", "${status?.cpuBusy ?: 0L}")
                        MemoryRow("User Ticks", "${status?.cpuUserTicks ?: 0L}")
                        MemoryRow("System Ticks", "${status?.cpuSysTicks ?: 0L}")
                        MemoryRow("Nice Ticks", "${status?.cpuNiceTicks ?: 0L}")
                        MemoryRow("Idle Ticks", "${status?.cpuIdleTicks ?: 0L}")
                        MemoryRow("IRQ Ticks", "${status?.cpuIrqTicks ?: 0L}")
                        MemoryRow("SoftIRQ Ticks", "${status?.cpuSirqTicks ?: 0L}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCpuDetailsModal = false }) {
                    Text(t("CLOSE"), fontWeight = FontWeight.Bold, color = ArcherTeal)
                }
            }
        )
    }

    if (showRamDetailsModal) {
        val status = uiState.systemStatus
        fun formatBytes(bytes: Long): String {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.2f MB", mb)
            } else {
                String.format(java.util.Locale.US, "%d B", bytes)
            }
        }

        AlertDialog(
            onDismissRequest = { showRamDetailsModal = false },
            title = { Text(t("Memory & Swap Utilization"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MemoryRow(t("Total Physical Memory"), formatBytes(status?.ramTotal ?: 0L))
                        MemoryRow(t("Used Memory"), formatBytes(status?.ramUsed ?: 0L))
                        MemoryRow(t("Free Memory"), formatBytes(status?.ramFree ?: 0L))
                        MemoryRow(t("Cached Memory"), formatBytes(status?.ramCached ?: 0L))
                        MemoryRow(t("Buffers Memory"), formatBytes(status?.ramBuffers ?: 0L))
                        MemoryRow(t("Swap Space"), formatBytes(status?.swapTotal ?: 0L))
                        MemoryRow(t("Swap Used"), formatBytes(status?.swapUsed ?: 0L))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRamDetailsModal = false }) {
                    Text(t("CLOSE"), fontWeight = FontWeight.Bold, color = ArcherTeal)
                }
            }
        )
    }
}

@Composable
fun CpuStatCard(
    status: SystemStatus?,
    progress: Float,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("CPU Usage"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${status?.cpuTemp ?: 0f}°C",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${status?.cpuUsage ?: 0}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ArcherTeal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${status?.cpuUsage ?: 0}% Used",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    extra: String,
    progress: Float,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ArcherTeal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = extra,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EthernetPortsPanel(ports: List<PortLink>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = t("Physical Ethernet Link Status"),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ArcherTeal
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ports.forEach { port ->
                    PortWidget(port = port)
                }
            }
        }
    }
}

@Composable
fun PortWidget(port: PortLink) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        // Physical RJ45 Port shape
        Box(
            modifier = Modifier
                .size(45.dp, 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (port.isConnected) ArcherTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = if (port.isConnected) ArcherTeal else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Link LED indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (port.isConnected) Color(0xFF2ECC71) else Color.Gray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (port.isConnected) {
                        if (port.speed.contains("1000") || port.speed.contains(t("1G"), ignoreCase = true)) t("1G") else t("100M")
                    } else t("Down"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (port.isConnected) ArcherTeal else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = port.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WifiControlCard(
    wifi2GEnabled: Boolean,
    wifi5GEnabled: Boolean,
    onToggle2G: (Boolean) -> Unit,
    onToggle5G: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = t("Wi-Fi Radios"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ArcherTeal
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("2.4 GHz", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (wifi2GEnabled) t("Active") else t("Disabled"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wifi2GEnabled,
                        onCheckedChange = onToggle2G,
                        colors = SwitchDefaults.colors(checkedTrackColor = ArcherTeal)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("5.0 GHz", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (wifi5GEnabled) t("Active") else t("Disabled"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wifi5GEnabled,
                        onCheckedChange = onToggle5G,
                        colors = SwitchDefaults.colors(checkedTrackColor = ArcherTeal)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}



@Composable
fun MemoryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
