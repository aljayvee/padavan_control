package com.example.padavancontrol.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.TrafficViewModel
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficScreen(
    viewModel: TrafficViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traffic Monitor", fontWeight = FontWeight.Bold) },
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SpeedMetricCard(
                    title = "Download Speed",
                    speed = String.format(Locale.US, "%.1f", uiState.currentDownloadSpeed),
                    unit = "Mbps",
                    color = ArcherTeal,
                    modifier = Modifier.weight(1f)
                )

                SpeedMetricCard(
                    title = "Upload Speed",
                    speed = String.format(Locale.US, "%.1f", uiState.currentUploadSpeed),
                    unit = "Mbps",
                    color = Color(0xFF9B59B6),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                        text = "Real-Time Bandwidth Activity",
                        fontSize = 14.sp,
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

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Transferred",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dlGB = uiState.totalDownloadBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val ulGB = uiState.totalUploadBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Download:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            String.format(Locale.US, "%.2f GB", dlGB),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcherTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Upload:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            String.format(Locale.US, "%.2f GB", ulGB),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9B59B6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedMetricCard(
    title: String,
    speed: String,
    unit: String,
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = speed, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
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
            10f,
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
                        ArcherTeal.copy(alpha = 0.3f),
                        ArcherTeal.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = dlPath,
                color = ArcherTeal,
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
                        Color(0xFF9B59B6).copy(alpha = 0.2f),
                        Color(0xFF9B59B6).copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = ulPath,
                color = Color(0xFF9B59B6),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
