package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t
import com.example.padavancontrol.data.models.HardwareInfoData
import com.example.padavancontrol.data.models.WirelessSectionInfo
import com.example.padavancontrol.data.models.EthernetPortInfo
import com.example.padavancontrol.data.models.ApClientConnection
import com.example.padavancontrol.data.models.StationInfo
import com.example.padavancontrol.ui.viewmodels.HardwareInfoViewModel
import com.example.padavancontrol.ui.viewmodels.HardwareInfoUiState

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.theme.ArcherTeal
import androidx.compose.ui.layout.layout

data class GlossaryTerm(
    val term: String,
    val fullName: String,
    val description: String
)

@Composable
fun getWirelessGlossary(): List<GlossaryTerm> {
    return listOf(
        GlossaryTerm("BSSID", t("Basic Service Set Identifier"), t("The unique MAC address of the connected wireless Access Point.")),
        GlossaryTerm("PhyMode", t("Physical Mode"), t("The 802.11 protocol version currently used for the connection (e.g. VHT for 802.11ac, HT for 802.11n).")),
        GlossaryTerm("BW", t("Bandwidth"), t("The width of the channel used (e.g. 20MHz, 40MHz, or 80MHz). Wider channels offer higher speeds but are more prone to interference.")),
        GlossaryTerm("MCS", t("Modulation and Coding Scheme"), t("An index representing the modulation type, coding rate, and spatial streams. Higher values indicate higher speeds.")),
        GlossaryTerm("SGI", t("Short Guard Interval"), t("A shorter delay between data symbols that increases transmission speeds by about 10%.")),
        GlossaryTerm("LDPC", t("Low-Density Parity-Check"), t("An advanced error-correcting technology that improves link stability and throughput at range.")),
        GlossaryTerm("STBC", t("Space-Time Block Coding"), t("A method of transmitting redundant copies of a data stream across multiple antennas to improve signal reliability.")),
        GlossaryTerm("TRate", t("Negotiated Transmit Rate"), t("The theoretical maximum speed of the wireless link negotiated between the router and client.")),
        GlossaryTerm("RSSI", t("Received Signal Strength Indicator"), t("The power level of the received signal in dBm. Values closer to 0 (e.g. -45 dBm) indicate a stronger signal.")),
        GlossaryTerm("AID", t("Association Identifier"), t("A temporary 16-bit number assigned by the Access Point to uniquely identify the connected client device.")),
        GlossaryTerm("PSM", t("Power Save Mode"), t("Indicates if the client device is actively transmitting (NO) or in a low-power sleep state (YES) to save battery.")),
        GlossaryTerm("MimoPS", t("MIMO Power Save"), t("Indicates if the client device disables extra antennas to save power when idle."))
    )
}

@Composable
fun getMibGlossary(): List<GlossaryTerm> {
    return listOf(
        GlossaryTerm("MIB", t("Management Information Base"), t("A structured database containing standard counters and stats for network interface monitoring.")),
        GlossaryTerm("Tx", t("Transmit"), t("Frames or bytes sent out from the router's interface.")),
        GlossaryTerm("Rx", t("Receive"), t("Frames or bytes received by the router's interface.")),
        GlossaryTerm("GoodOctets", t("Valid Bytes"), t("The total number of valid bytes (octets) transmitted or received on this port.")),
        GlossaryTerm("UcastFrames", t("Unicast Frames"), t("Data packets sent directly to a single specific device on the network.")),
        GlossaryTerm("McastFrames", t("Multicast Frames"), t("Data packets sent to a specific group of devices registered to a multicast stream.")),
        GlossaryTerm("BcastFrames", t("Broadcast Frames"), t("Data packets sent to all devices connected to the network.")),
        GlossaryTerm("DropFrames", t("Dropped Frames"), t("Packets discarded by the hardware due to memory/buffer congestion or lack of system resources.")),
        GlossaryTerm("PauseFrames", t("Pause Flow Control"), t("Special flow control frames that signal the sending end to temporarily stop transmitting data to prevent buffer overflows.")),
        GlossaryTerm("Collisions", t("Collision Frames"), t("Packet collisions detected on half-duplex links. (Should be 0 on modern full-duplex links).")),
        GlossaryTerm("CRCError", t("Cyclic Redundancy Check Errors"), t("Corrupted packets received that failed error detection, indicating cabling issues or strong interference.")),
        GlossaryTerm("FilterFrames", t("Filtered Frames"), t("Packets discarded because they were not addressed to this host or failed interface filter criteria.")),
        GlossaryTerm("AligmentError", t("Alignment Errors"), t("Packets received with an invalid number of bits (not ending on a byte boundary), indicating cabling or hardware defects."))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareInfoScreen(
    viewModel: HardwareInfoViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showGlossary by remember { mutableStateOf(false) }
    val tabs = listOf(t("Wireless Status"), t("Ethernet Ports"))

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
                title = { Text(t("Wireless & Wired Info"), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showGlossary = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Glossary",
                            tint = ArcherTeal
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ArcherTeal
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when {
                    uiState.isLoading && uiState.hardwareInfo == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ArcherTeal)
                        }
                    }
                    uiState.errorMessage != null && uiState.hardwareInfo == null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.errorMessage ?: t("Connection Error"),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal)
                            ) {
                                Text(t("Retry"))
                            }
                        }
                    }
                    else -> {
                        val data = uiState.hardwareInfo
                        if (data != null) {
                            if (selectedTab == 0) {
                                WirelessTabContent(data)
                            } else {
                                EthernetTabContent(data)
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t("No Data Available"))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlossary) {
        GlossaryDialog(onDismiss = { showGlossary = false })
    }
}

@Composable
fun WirelessTabContent(data: HardwareInfoData) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WirelessBandCard(title = t("Wireless 2.4 GHz"), info = data.wireless2g)
        WirelessBandCard(title = t("Wireless 5 GHz"), info = data.wireless5g)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WirelessBandCard(title: String, info: WirelessSectionInfo) {
    var showRawLog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcherTeal
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = t("Raw Log"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = showRawLog,
                        onCheckedChange = { showRawLog = it },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Info rows
            InfoRow(label = t("MAC (AP Main)"), value = info.macApMain)
            if (info.macApClient.isNotEmpty()) {
                InfoRow(label = t("MAC (AP-Client)"), value = info.macApClient)
            }
            InfoRow(label = t("Operation Mode"), value = info.operationMode)
            InfoRow(label = t("WPHY Mode"), value = info.wphyMode)
            InfoRow(label = t("Channel Main"), value = info.channelMain.toString())

            if (showRawLog) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = t("Raw Console Dump"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        val rawListText = info.mainStationsListRaw.trim()
                        if (rawListText.isNotEmpty()) {
                            val stationsScroll = rememberScrollState()
                            Text(
                                text = rawListText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(stationsScroll)
                            )
                        } else {
                            Text(
                                text = t("No raw dump logs available"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // AP Client Connection Detail
                info.apClientConnection?.let { conn ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = t("AP-Client Connection"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ApClientConnectionCard(conn)
                }

                // AP Main Stations List (Structured for Mobile UI)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = t("AP Main Stations List"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (info.stations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        info.stations.forEach { station ->
                            StationCard(station)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = t("No stations connected"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// Station Card parsed and structured specifically for mobile UI screen constraints
@Composable
fun StationCard(station: StationInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(t("Device MAC"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(station.mac, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Surface(
                    color = when {
                        station.rssi >= -65 -> Color(0xFFD4EFDF) // Light Green
                        station.rssi >= -75 -> Color(0xFFFCF3CF) // Light Yellow
                        else -> Color(0xFFFADBD8) // Light Red
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    val textColor = when {
                        station.rssi >= -65 -> Color(0xFF196F3D)
                        station.rssi >= -75 -> Color(0xFF7D6608)
                        else -> Color(0xFF7B241C)
                    }
                    Text(
                        text = "${station.rssi} dBm",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Grid params
            Row(modifier = Modifier.fillMaxWidth()) {
                GridParam(label = "AID", value = station.aid.toString(), modifier = Modifier.weight(1f))
                GridParam(label = "PSM", value = station.psm, modifier = Modifier.weight(1f))
                GridParam(label = "MimoPS", value = station.mimoPs, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GridParam(label = "MCS", value = station.mcs.toString(), modifier = Modifier.weight(1f))
                GridParam(label = "BW", value = station.bw, modifier = Modifier.weight(1f))
                GridParam(label = "SGI", value = station.sgi, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GridParam(label = "STBC", value = station.stbc, modifier = Modifier.weight(1f))
                GridParam(label = "TRate", value = station.tRate, modifier = Modifier.weight(2f))
            }
        }
    }
}

@Composable
fun ApClientConnectionCard(conn: ApClientConnection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(t("BSSID"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(conn.bssid, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(t("RSSI"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val color = when {
                        conn.rssi >= -65 -> Color(0xFF2ECC71) // Green
                        conn.rssi >= -75 -> Color(0xFFF1C40F) // Yellow
                        else -> Color(0xFFE74C3C) // Red
                    }
                    Text("${conn.rssi} dBm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Grid parameters
            Row(modifier = Modifier.fillMaxWidth()) {
                GridParam(label = t("PhyMode"), value = conn.phyMode, modifier = Modifier.weight(1f))
                GridParam(label = t("BW"), value = conn.bw, modifier = Modifier.weight(1f))
                GridParam(label = t("MCS"), value = conn.mcs.toString(), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GridParam(label = t("SGI"), value = conn.sgi, modifier = Modifier.weight(1f))
                GridParam(label = t("LDPC"), value = conn.ldpc, modifier = Modifier.weight(1f))
                GridParam(label = t("STBC"), value = conn.stbc, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GridParam(label = t("TRate"), value = conn.tRate, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GridParam(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EthernetTabContent(data: HardwareInfoData) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.ports.forEach { port ->
            EthernetPortCard(port)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EthernetPortCard(port: EthernetPortInfo) {
    var expanded by remember { mutableStateOf(false) }
    val isConnected = !port.portLink.contains("No Link", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isConnected) Color(0xFF2ECC71) else Color(0xFF95A5A6),
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t(port.portName),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (isConnected) Color(0xFFD4EFDF) else Color(0xFFE5E7E9)
                    val badgeTextColor = if (isConnected) Color(0xFF196F3D) else Color(0xFF5D6D7E)
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isConnected) port.portLink else t("No Link"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = t("MIB Counters"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcherTeal
                )
                Spacer(modifier = Modifier.height(10.dp))

                val counters = port.mibCounters
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t("Transmit (Tx)"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MibRow("TxGoodOctets", counters.txGoodOctets)
                        MibRow("TxUcastFrames", counters.txUcastFrames)
                        MibRow("TxMcastFrames", counters.txMcastFrames)
                        MibRow("TxBcastFrames", counters.txBcastFrames)
                        MibRow("TxDropFrames", counters.txDropFrames)
                        MibRow("TxPauseFrames", counters.txPauseFrames)
                        MibRow("TxCollisions", counters.txCollisions)
                        MibRow("TxCRCError", counters.txCrcError)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t("Receive (Rx)"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MibRow("RxGoodOctets", counters.rxGoodOctets)
                        MibRow("RxUcastFrames", counters.rxUcastFrames)
                        MibRow("RxMcastFrames", counters.rxMcastFrames)
                        MibRow("RxBcastFrames", counters.rxBcastFrames)
                        MibRow("RxDropFrames", counters.rxDropFrames)
                        MibRow("RxPauseFrames", counters.rxPauseFrames)
                        MibRow("RxFilterFrames", counters.rxFilterFrames)
                        MibRow("RxCRCError", counters.rxCrcError)
                        MibRow("RxAligmentError", counters.rxAlignmentError)
                    }
                }
            }
        }
    }
}

@Composable
fun MibRow(name: String, value: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// glossary sheet dialog that explains all acronym meanings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(t("Wireless Acronyms"), t("Port & MIB Acronyms"))
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(t("Glossary of Terms"), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = ArcherTeal)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = ArcherTeal
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = t("Glossary Description"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val list = if (selectedTab == 0) getWirelessGlossary() else getMibGlossary()
                        list.forEach { item ->
                            GlossaryItemCard(item)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GlossaryItemCard(item: GlossaryTerm) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ArcherTeal.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.term,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcherTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// extension helper for scaling Compose components
fun Modifier.scale(scale: Float): Modifier = this.then(
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            (placeable.width * scale).toInt(),
            (placeable.height * scale).toInt()
        ) {
            placeable.placeRelative(0, 0)
        }
    }
)
