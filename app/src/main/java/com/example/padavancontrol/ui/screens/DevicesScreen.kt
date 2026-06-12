package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.DevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedClient by remember { mutableStateOf<LanClient?>(null) }

    val filteredClients = uiState.clients.filter {
        it.hostname.contains(uiState.searchQuery, ignoreCase = true) ||
                it.ipAddress.contains(uiState.searchQuery) ||
                it.macAddress.contains(uiState.searchQuery, ignoreCase = true)
    }

    val onlineClients = filteredClients.filter { it.isOnline }
        .distinctBy { "${it.macAddress}_${it.ipAddress}_${it.connectionType}" }
    val offlineClients = filteredClients.filter { !it.isOnline }

    // Classify online clients
    val wifi5GClients = onlineClients.filter { it.connectionType == "5G" }
    val wifi2GClients = onlineClients.filter { it.connectionType == "2.4G" || it.connectionType == "Wireless" }
    val wiredClients = onlineClients.filter { it.connectionType == "Wired" }

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
                title = { Text(t("Client Status"), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                placeholder = { Text(t("Search by name, IP, or MAC...")) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = ArcherTeal
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcherTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Alert Banner
            uiState.errorMessage?.let { errorText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(1.5.dp, Color(0xFFE74C3C), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading && uiState.clients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ArcherTeal)
                }
            } else if (filteredClients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(t("No devices found"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (wifi5GClients.isNotEmpty()) {
                        item {
                            DeviceSectionHeader(title = t("5 GHz Wireless Devices"), count = wifi5GClients.size)
                        }
                        items(wifi5GClients, key = { "${it.macAddress}_${it.ipAddress}_5G" }) { client ->
                            DeviceRowItem(
                                client = client,
                                onClick = { selectedClient = client }
                            )
                        }
                    }

                    if (wifi2GClients.isNotEmpty()) {
                        item {
                            DeviceSectionHeader(title = t("2.4 GHz Wireless Devices"), count = wifi2GClients.size)
                        }
                        items(wifi2GClients, key = { "${it.macAddress}_${it.ipAddress}_2G" }) { client ->
                            DeviceRowItem(
                                client = client,
                                onClick = { selectedClient = client }
                            )
                        }
                    }

                    if (wiredClients.isNotEmpty()) {
                        item {
                            DeviceSectionHeader(title = t("LAN Cables Connected"), count = wiredClients.size)
                        }
                        items(wiredClients, key = { "${it.macAddress}_${it.ipAddress}_Wired" }) { client ->
                            DeviceRowItem(
                                client = client,
                                onClick = { selectedClient = client }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog logic
    selectedClient?.let { current ->
        val client = uiState.clients.find { it.macAddress == current.macAddress } ?: current
        val context = LocalContext.current
        val isBlocked = client.blockIndex != -1

        AlertDialog(
            onDismissRequest = { selectedClient = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val connectionIcon = when (client.connectionType) {
                        "5G", "2.4G", "Wireless" -> Icons.Default.Wifi
                        else -> Icons.Default.Computer
                    }
                    Icon(
                        imageVector = connectionIcon,
                        contentDescription = null,
                        tint = ArcherTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = client.hostname.ifEmpty { t("Device Details") },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Status row (Online/Offline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("Status"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = (if (client.isOnline) Color(0xFF2ECC71) else Color.Gray).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (client.isOnline) t("Online / Connected") else t("Offline / Disconnected"),
                                color = if (client.isOnline) Color(0xFF2ECC71) else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Connection Type Classification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("Connection"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val connectionText = when (client.connectionType) {
                            "5G" -> t("5 GHz Wireless")
                            "2.4G", "Wireless" -> t("2.4 GHz Wireless")
                            "Wired" -> t("LAN Cable")
                            else -> client.connectionType
                        }
                        Text(
                            text = t(connectionText),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // IP Address
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t("IP Address"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = client.ipAddress,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // MAC Address
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t("MAC Address"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = client.macAddress,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // RSSI (if wireless & online)
                    if (client.isOnline && (client.connectionType == "5G" || client.connectionType == "2.4G" || client.connectionType == "Wireless")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t("Signal Strength"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (client.rssi.isNotEmpty()) "${client.rssi} dBm" else "-",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                if (client.rssi.isNotEmpty()) {
                                    AnimatedWifiIcon(rssi = client.rssi.toIntOrNull() ?: -100)
                                }
                            }
                        }
                    }

                    // Access Control Status overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("Internet Access"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isBlocked) t("Blocked") else t("Allowed"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBlocked) MaterialTheme.colorScheme.error else Color(0xFF2ECC71)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            if (isBlocked) "Allowing internet access..." else "Blocking internet access...",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.toggleBlockStatus(client)
                    }
                ) {
                    Text(
                        text = if (isBlocked) t("UNBLOCK DEVICE") else t("BLOCK DEVICE"),
                        fontWeight = FontWeight.Bold,
                        color = if (isBlocked) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClient = null }) {
                    Text(t("CLOSE"), color = ArcherTeal)
                }
            }
        )
    }
}

@Composable
fun DeviceSectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ArcherTeal
        )
        Box(
            modifier = Modifier
                .background(ArcherTeal.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ArcherTeal
            )
        }
    }
}

@Composable
fun DeviceRowItem(
    client: LanClient,
    isOffline: Boolean = false,
    onClick: () -> Unit
) {
    val cardColor = if (client.blockIndex != -1) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val connectionIcon = when (client.connectionType) {
                    "5G", "2.4G", "Wireless" -> Icons.Default.Wifi
                    else -> Icons.Default.Computer
                }
                val iconColor = if (isOffline) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    ArcherTeal
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = (if (isOffline) MaterialTheme.colorScheme.surfaceVariant else ArcherTeal).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = connectionIcon,
                        contentDescription = client.connectionType,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = client.hostname.ifEmpty { t("Unknown Device") },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isOffline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (client.blockIndex != -1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = t("Blocked"),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "IP: ${client.ipAddress}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "MAC: ${client.macAddress}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (!isOffline && (client.connectionType == "5G" || client.connectionType == "2.4G" || client.connectionType == "Wireless")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (client.rssi.isNotEmpty()) {
                        Text(
                            text = "${client.rssi} dBm",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        AnimatedWifiIcon(rssi = client.rssi.toIntOrNull() ?: -100)
                    }
                }
            } else if (isOffline) {
                Text(
                    text = t("Offline / Disconnected"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AnimatedWifiIcon(rssi: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Determine signal strength icon and color based on RSSI dBm values
    val (wifiIcon, iconColor) = when {
        rssi >= -60 -> Pair(Icons.Filled.NetworkWifi, Color(0xFF4CAF50))     // Green (4 bars)
        rssi >= -70 -> Pair(Icons.Filled.NetworkWifi3Bar, Color(0xFF8BC34A)) // Light Green (3 bars)
        rssi >= -80 -> Pair(Icons.Filled.NetworkWifi2Bar, Color(0xFFFFC107)) // Yellow (2 bars)
        else -> Pair(Icons.Filled.NetworkWifi1Bar, Color(0xFFF44336))        // Red (1 bar)
    }

    Icon(
        imageVector = wifiIcon,
        contentDescription = "Wifi Signal strength $rssi dBm",
        modifier = Modifier
            .size(16.dp)
            .alpha(alpha),
        tint = iconColor
    )
}
