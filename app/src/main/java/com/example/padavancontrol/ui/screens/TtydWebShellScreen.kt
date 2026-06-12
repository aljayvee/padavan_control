package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.theme.ArcherTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtydWebShellScreen(
    onNavigateBack: () -> Unit,
    repository: PadavanRepository,
    modifier: Modifier = Modifier
) {
    val routerIp = repository.getRouterIp()
    val ttydUrl = "http://$routerIp:7681"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("ttyd Web Shell")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = t("Back"),
                            tint = ArcherTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        modifier = modifier
    ) { innerPadding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(ttydUrl)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
