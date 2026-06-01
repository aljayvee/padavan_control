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
            cpuTemp = "52°C",
            cpuUsage = "12%",
            memUsage = "45%",
            uptime = "2 days, 4 hours",
            activeConn = "150",
            wifi2gState = "ON",
            wifi5gState = "ON",
            firmware = "3.4.3.9-099"
        )
        val wanStatus = WanStatus(
            status = "Connected",
            connectionType = "PPPoE",
            ipAddress = "120.24.53.111",
            subnetMask = "255.255.255.255",
            gateway = "120.24.53.1",
            dns1 = "8.8.8.8",
            dns2 = "8.8.4.4"
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
                    onNavigateToLogViewer = {}
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
