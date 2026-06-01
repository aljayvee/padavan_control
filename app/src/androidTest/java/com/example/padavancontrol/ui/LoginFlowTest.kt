package com.example.padavancontrol.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.padavancontrol.ui.screens.LoginScreen
import com.example.padavancontrol.ui.viewmodels.LoginUiState
import com.example.padavancontrol.ui.viewmodels.LoginViewModel
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
class LoginFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLoginScreenDisplayAndInput() {
        val viewModel = mock(LoginViewModel::class.java)

        val stateFlow = MutableStateFlow(
            LoginUiState(
                ipAddress = "192.168.2.2",
                username = "admin",
                password = "admin_password"
            )
        )
        whenever(viewModel.uiState).thenReturn(stateFlow)
        whenever(viewModel.events).thenReturn(MutableSharedFlow())

        composeTestRule.setContent {
            PadavanControlTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {}
                )
            }
        }

        // Verify title & prefilled texts exist
        composeTestRule.onNodeWithText("PADAVAN CONTROL").assertExists()
        composeTestRule.onNodeWithText("Router IP Address").assertExists()
        composeTestRule.onNodeWithText("Username").assertExists()
        composeTestRule.onNodeWithText("Password").assertExists()
        composeTestRule.onNodeWithText("CONNECT & LOGIN").assertExists()

        // Test clicking login triggers the viewmodel
        composeTestRule.onNodeWithText("CONNECT & LOGIN").performClick()
        verify(viewModel).login()
    }
}
