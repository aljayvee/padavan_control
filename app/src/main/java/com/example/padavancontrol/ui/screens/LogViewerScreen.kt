package com.example.padavancontrol.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.LogViewerEvent
import com.example.padavancontrol.ui.viewmodels.LogViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    viewModel: LogViewerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logsScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LogViewerEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Automatically scroll to the end of the logs when fetched or filtered
    LaunchedEffect(uiState.rawLogs, uiState.filterQuery) {
        delay(100) // Small delay for layout to settle
        logsScrollState.animateScrollTo(logsScrollState.maxValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System logs", fontWeight = FontWeight.Bold) },
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
                    IconButton(
                        onClick = { viewModel.fetchLogs() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = ArcherTeal
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = MaterialTheme.colorScheme.error
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Filter input
            OutlinedTextField(
                value = uiState.filterQuery,
                onValueChange = { viewModel.updateFilterQuery(it) },
                label = { Text("Search / filter logs") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcherTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Monospaced Logs Window
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)) // Tailwind Slate-800 dark slate
                    .padding(12.dp)
            ) {
                if (uiState.isLoading && uiState.rawLogs.startsWith("Fetching")) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ArcherTeal)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(logsScrollState)
                    ) {
                        // Split logs by line and filter based on search query
                        val lines = uiState.rawLogs.split("\n")
                        val filteredLines = if (uiState.filterQuery.trim().isEmpty()) {
                            lines
                        } else {
                            lines.filter { it.contains(uiState.filterQuery, ignoreCase = true) }
                        }

                        if (filteredLines.isEmpty()) {
                            Text(
                                text = "No log entries matches your search query.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            // Highlight matches in each line
                            filteredLines.forEach { line ->
                                Text(
                                    text = buildHighlightableString(line, uiState.filterQuery),
                                    color = Color(0xFFF1F5F9), // Very light gray
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom parser to draw high-impact highlighting for query matches inside monospaced syslog lines
@Composable
fun buildHighlightableString(text: String, query: String) = buildAnnotatedString {
    if (query.isEmpty() || !text.contains(query, ignoreCase = true)) {
        append(text)
    } else {
        var startIdx = 0
        while (startIdx < text.length) {
            val idx = text.indexOf(query, startIdx, ignoreCase = true)
            if (idx == -1) {
                append(text.substring(startIdx))
                break
            } else {
                append(text.substring(startIdx, idx))
                withStyle(style = SpanStyle(background = Color(0xFFCA8A04), color = Color.White, fontWeight = FontWeight.Bold)) {
                    append(text.substring(idx, idx + query.length))
                }
                startIdx = idx + query.length
            }
        }
    }
}

// Small mock delay helper
private suspend fun delay(ms: Long) {
    kotlinx.coroutines.delay(ms)
}
