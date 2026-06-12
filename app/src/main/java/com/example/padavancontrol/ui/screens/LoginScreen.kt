package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padavancontrol.theme.ArcherTeal
import com.example.padavancontrol.ui.viewmodels.LoginEvent
import com.example.padavancontrol.ui.viewmodels.LoginViewModel
import com.example.padavancontrol.ui.viewmodels.LoginStep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> onLoginSuccess()
                is LoginEvent.ShowError -> {
                    // Handled locally via uiState.errorMessage
                }
            }
        }
    }

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == LoginStep.SCANNING) {
            viewModel.discoverRouter(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ArcherTeal.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐",
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = t("PADAVAN CONTROL"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = t("Secure Companion App for newifi D2"),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    when (uiState.currentStep) {
                        LoginStep.SCANNING -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RadarScannerAnimation(
                                    modifier = Modifier.size(140.dp),
                                    scannerColor = ArcherTeal
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = t("Wifi Scanning"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.scanStatusMessage ?: t("Initializing router discovery..."),
                                    fontSize = 13.sp,
                                    color = ArcherTeal,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                // Clickable discovered routers list
                                if (uiState.discoveredRouters.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = t("Tap a router to connect:"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    uiState.discoveredRouters.forEach { ip ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { viewModel.selectRouter(ip) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ArcherTeal.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("📡", fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = t("Padavan Router"),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = ip,
                                                        fontSize = 12.sp,
                                                        color = ArcherTeal,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = t("Select"),
                                                    tint = ArcherTeal
                                                )
                                            }
                                        }
                                    }
                                }

                                uiState.errorMessage?.let { error ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                TextButton(onClick = { viewModel.updateStep(LoginStep.MANUAL_IP) }) {
                                    Text(t("Set IP Manually"), color = ArcherTeal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        LoginStep.MANUAL_IP -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = t("Router IP Address"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.ipAddress,
                                    onValueChange = { viewModel.updateIpAddress(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("192.168.2.2") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                                )

                                uiState.errorMessage?.let { error ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { viewModel.updateStep(LoginStep.SCANNING) }) {
                                        Text(t("Scan Network"), color = ArcherTeal)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Button(
                                        onClick = { viewModel.testManualIp(uiState.ipAddress) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !uiState.isLoading
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(t("NEXT"), color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        LoginStep.CREDENTIALS -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = t("Username"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.username,
                                    onValueChange = { viewModel.updateUsername(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = ArcherTeal
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = t("Password"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.password,
                                    onValueChange = { viewModel.updatePassword(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = ArcherTeal
                                        )
                                    },
                                    trailingIcon = {
                                        TextButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                            Text(if (uiState.passwordVisible) t("HIDE") else t("SHOW"), color = ArcherTeal)
                                        }
                                    }
                                )

                                uiState.errorMessage?.let {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { viewModel.login() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ArcherTeal),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !uiState.isLoading
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(
                                            text = t("CONNECT"),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarScannerAnimation(
    modifier: Modifier = Modifier,
    scannerColor: Color = ArcherTeal
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Rotation of the sweeping line
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing alpha for the waves
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    // Pulsing radius scale
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2

        // Draw background concentric grid circles
        drawCircle(
            color = scannerColor.copy(alpha = 0.08f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = scannerColor.copy(alpha = 0.08f),
            radius = maxRadius * 0.66f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = scannerColor.copy(alpha = 0.08f),
            radius = maxRadius * 0.33f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw expanding pulsing wave
        drawCircle(
            color = scannerColor.copy(alpha = waveAlpha * 0.15f),
            radius = maxRadius * waveScale,
            center = center
        )
        drawCircle(
            color = scannerColor.copy(alpha = waveAlpha),
            radius = maxRadius * waveScale,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw sweeping radar line and gradient trail
        rotate(degrees = rotation, pivot = center) {
            val sweepGradient = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    scannerColor.copy(alpha = 0.01f),
                    scannerColor.copy(alpha = 0.35f),
                    scannerColor
                ),
                center = center
            )
            drawCircle(
                brush = sweepGradient,
                radius = maxRadius,
                center = center
            )
            drawLine(
                color = scannerColor,
                start = center,
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
