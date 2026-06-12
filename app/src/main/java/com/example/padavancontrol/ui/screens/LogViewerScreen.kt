package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
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
    val lazyListState = rememberLazyListState()

    // Automatically scroll to the end of the logs when log line count changes
    val filteredLinesCount = uiState.filteredLogLines.size
    LaunchedEffect(filteredLinesCount) {
        if (filteredLinesCount > 0) {
            lazyListState.animateScrollToItem(filteredLinesCount - 1)
        }
    }

    // Toast Effect
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LogViewerEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("System logs"), fontWeight = FontWeight.Bold) },
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
                            contentDescription = t("Clear Logs"),
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
                label = { Text(t("Search / filter logs")) },
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
                if (uiState.isLoading && uiState.rawLogsLength == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ArcherTeal)
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.filteredLogLines.isEmpty()) {
                            item {
                                Text(
                                    text = t("No log entries matches your search query."),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            items(
                                count = uiState.filteredLogLines.size,
                                key = { index -> index }
                            ) { index ->
                                val line = uiState.filteredLogLines[index]
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
