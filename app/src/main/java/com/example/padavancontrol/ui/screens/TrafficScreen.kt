package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Menu
import com.example.padavancontrol.theme.ArcherTeal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.ui.viewmodels.INTERFACE_MAPPING
import com.example.padavancontrol.ui.viewmodels.TrafficViewModel
import java.util.Locale
import kotlin.math.max

// Unified Premium Palette
val TrafficDownlinkColor = Color(0xFFF39C12) // Orange
val TrafficUplinkColor = Color(0xFF2980B9)   // Blue

fun formatSpeedFromMbps(speedMbps: Double): String {
    val bps = speedMbps * 1_000_000.0
    return if (bps < 1_000_000.0) {
        String.format(Locale.US, "%.2f Kbps", bps / 1000.0)
    } else {
        String.format(Locale.US, "%.2f Mbps", speedMbps)
    }
}

fun formatBytes(bytes: Long): String {
    val d = bytes.toDouble()
    return when {
        bytes < 1024 -> String.format(Locale.US, "%d B", bytes)
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.2f KiB", d / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f MiB", d / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GiB", d / (1024.0 * 1024.0 * 1024.0))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficScreen(
    viewModel: TrafficViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val selectedDisplayName = t(INTERFACE_MAPPING[uiState.selectedInterface] ?: uiState.selectedInterface)

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
                title = { Text(t("Traffic Monitor"), fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
        ) {
            // Dropdown Selector Card
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Card(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(t("Interface Selection"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(selectedDisplayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    uiState.availableInterfaces.forEach { key ->
                        val displayName = t(INTERFACE_MAPPING[key] ?: key)
                        DropdownMenuItem(
                            text = { Text(displayName, fontWeight = if (key == uiState.selectedInterface) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.selectInterface(key)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error Alert Banner
            uiState.errorMessage?.let { errorText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
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

            // Real-time speed cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SpeedMetricCard(
                    title = t("Download Speed"),
                    speedText = formatSpeedFromMbps(uiState.currentDownloadSpeed),
                    color = TrafficDownlinkColor,
                    modifier = Modifier.weight(1f)
                )

                SpeedMetricCard(
                    title = t("Upload Speed"),
                    speedText = formatSpeedFromMbps(uiState.currentUploadSpeed),
                    color = TrafficUplinkColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = t("Real-Time Bandwidth Activity"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        RealTimeCanvasChart(
                            downloadPoints = uiState.downloadHistory,
                            uploadPoints = uiState.uploadHistory,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detailed Statistics Table Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t("Real-Time Statistics"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val activeState = uiState.interfaceStates[uiState.selectedInterface]
                    val rxCurrent = activeState?.currentRxSpeed ?: 0.0
                    val txCurrent = activeState?.currentTxSpeed ?: 0.0
                    val rxAverage = activeState?.averageRxSpeed ?: 0.0
                    val txAverage = activeState?.averageTxSpeed ?: 0.0
                    val rxMax = activeState?.maxRxSpeed ?: 0.0
                    val txMax = activeState?.maxTxSpeed ?: 0.0
                    val rxTotal = activeState?.totalRxBytes ?: 0L
                    val txTotal = activeState?.totalTxBytes ?: 0L

                    // Table Headers
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t("Direction"), modifier = Modifier.weight(1.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t("Current"), modifier = Modifier.weight(1.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                        Text(t("Average"), modifier = Modifier.weight(1.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                        Text(t("Peak"), modifier = Modifier.weight(1.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                        Text(t("Total"), modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                    }

                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    // Downlink Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(TrafficDownlinkColor, shape = RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t("Downlink"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(formatSpeedFromMbps(rxCurrent), modifier = Modifier.weight(1.3f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        Text(formatSpeedFromMbps(rxAverage), modifier = Modifier.weight(1.3f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        Text(formatSpeedFromMbps(rxMax), modifier = Modifier.weight(1.3f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        Text(formatBytes(rxTotal), modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                    }

                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))

                    // Uplink Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(TrafficUplinkColor, shape = RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t("Uplink"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(formatSpeedFromMbps(txCurrent), modifier = Modifier.weight(1.3f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        Text(formatSpeedFromMbps(txAverage), modifier = Modifier.weight(1.3f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        Text(formatSpeedFromMbps(txMax), modifier = Modifier.weight(1.3f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        Text(formatBytes(txTotal), modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SpeedMetricCard(
    title: String,
    speedText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            val parts = speedText.split(" ")
            val valStr = parts.getOrNull(0) ?: ""
            val unitStr = parts.getOrNull(1) ?: ""
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = valStr, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unitStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RealTimeCanvasChart(
    downloadPoints: List<Float>,
    uploadPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxVal = max(
            1f,
            max(downloadPoints.maxOrNull() ?: 0f, uploadPoints.maxOrNull() ?: 0f) * 1.2f
        )

        val gridCount = 4
        for (i in 0..gridCount) {
            val y = (height / gridCount) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (downloadPoints.size > 1) {
            val dlPath = Path()
            val dlFillPath = Path()

            val xInterval = width / 19f

            downloadPoints.forEachIndexed { index, point ->
                val x = index * xInterval
                val y = height - (point / maxVal) * height

                if (index == 0) {
                    dlPath.moveTo(x, y)
                    dlFillPath.moveTo(x, height)
                    dlFillPath.lineTo(x, y)
                } else {
                    dlPath.lineTo(x, y)
                    dlFillPath.lineTo(x, y)
                }

                if (index == downloadPoints.lastIndex) {
                    dlFillPath.lineTo(x, height)
                    dlFillPath.close()
                }
            }

            drawPath(
                path = dlFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TrafficDownlinkColor.copy(alpha = 0.3f),
                        TrafficDownlinkColor.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = dlPath,
                color = TrafficDownlinkColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        if (uploadPoints.size > 1) {
            val ulPath = Path()
            val ulFillPath = Path()

            val xInterval = width / 19f

            uploadPoints.forEachIndexed { index, point ->
                val x = index * xInterval
                val y = height - (point / maxVal) * height

                if (index == 0) {
                    ulPath.moveTo(x, y)
                    ulFillPath.moveTo(x, height)
                    ulFillPath.lineTo(x, y)
                } else {
                    ulPath.lineTo(x, y)
                    ulFillPath.lineTo(x, y)
                }

                if (index == uploadPoints.lastIndex) {
                    ulFillPath.lineTo(x, height)
                    ulFillPath.close()
                }
            }

            drawPath(
                path = ulFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TrafficUplinkColor.copy(alpha = 0.2f),
                        TrafficUplinkColor.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = ulPath,
                color = TrafficUplinkColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
