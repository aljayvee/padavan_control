package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.ShellConsoleViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShellConsoleScreen(
    viewModel: ShellConsoleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val terminalScrollState = rememberScrollState()

    // Automatically scroll to the end of the terminal when logs update
    LaunchedEffect(uiState.terminalOutput) {
        terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("Developer Command Shell"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = t("Back"),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Retro Terminal Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A)) // Tailwind Slate-900 slate black
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(terminalScrollState)
                ) {
                    Text(
                        text = uiState.terminalOutput,
                        color = Color(0xFF10B981), // Emerald green
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    if (uiState.isExecuting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t("Executing remote diagnostic..."),
                                color = Color(0xFF10B981),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Shortcut Chips Area
            Column {
                Text(
                    text = t("Quick Diagnostic Shortcuts"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.shortcutChips.forEach { chipCmd ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.runCommand(chipCmd) },
                            label = { Text(chipCmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = InputChipDefaults.inputChipColors(
                                labelColor = ArcherTeal,
                                containerColor = ArcherTeal.copy(alpha = 0.08f)
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                selected = false,
                                enabled = true,
                                borderColor = ArcherTeal.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Command input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.commandInput,
                    onValueChange = { viewModel.updateCommandInput(it) },
                    placeholder = { Text(t("Enter shell command..."), fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcherTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { viewModel.runCommand(uiState.commandInput) })
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { viewModel.runCommand(uiState.commandInput) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (uiState.commandInput.trim().isNotEmpty() && !uiState.isExecuting) ArcherTeal else MaterialTheme.colorScheme.surfaceVariant),
                    enabled = uiState.commandInput.trim().isNotEmpty() && !uiState.isExecuting
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = t("Run"),
                        tint = if (uiState.commandInput.trim().isNotEmpty() && !uiState.isExecuting) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
