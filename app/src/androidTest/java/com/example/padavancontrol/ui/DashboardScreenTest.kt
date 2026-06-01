package com.example.padavancontrol.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.padavancontrol.ui.screens.DashboardScreen
import com.example.padavancontrol.ui.viewmodels.DashboardUiState
import com.example.padavancontrol.ui.viewmodels.DashboardViewModel
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import com.example.padavancontrol.theme.PadavanControlTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardDisplayAndInteraction() {
        val viewModel = mock(DashboardViewModel::class.java)

        val systemStatus = SystemStatus(
            cpuUsage = 12,
            ramTotal = 512 * 1024 * 1024L,
            ramUsed = 230 * 1024 * 1024L,
            uptime = 187200L,
            cpuTemp = 52f
        )
        val wanStatus = WanStatus(
            isConnected = true,
            wanIp = "120.24.53.111",
            gateway = "120.24.53.1",
            dns1 = "8.8.8.8",
            dns2 = "8.8.4.4",
            connectionType = "PPPoE",
            uptime = 187200L
        )

        val stateFlow = MutableStateFlow(
            DashboardUiState(
                routerIp = "192.168.2.1",
                systemStatus = systemStatus,
                wanStatus = wanStatus,
                lanLinks = emptyList(),
                isRefreshing = false
            )
        )

        whenever(viewModel.uiState).thenReturn(stateFlow)
        whenever(viewModel.events).thenReturn(MutableSharedFlow())

        composeTestRule.setContent {
            PadavanControlTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {},
                    onNavigateToShellConsole = {},
                    onNavigateToLogViewer = {},
                    onMenuClick = {}
                )
            }
        }

        // Verify elements exist
        composeTestRule.onNodeWithText("newifi D2").assertExists()
        composeTestRule.onNodeWithText("192.168.2.1").assertExists()
        composeTestRule.onNodeWithText("Internet Connected").assertExists()
        composeTestRule.onNodeWithText("IP: 120.24.53.111 • Type: PPPoE").assertExists()

        // Verify refresh button click
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        verify(viewModel).refresh()
    }
}
