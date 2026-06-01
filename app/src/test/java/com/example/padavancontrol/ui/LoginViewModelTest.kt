package com.example.padavancontrol.ui

import android.content.Context
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.LoginStep
import com.example.padavancontrol.ui.viewmodels.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val repository = mock(PadavanRepository::class.java)
    private val credentialStore = mock(CredentialStore::class.java)
    private val context = mock(Context::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock default credentials and onboarding
        `when`(credentialStore.getRouterIp()).thenReturn("192.168.2.2")
        `when`(credentialStore.getUsername()).thenReturn("admin")
        `when`(credentialStore.getPassword()).thenReturn("")
        `when`(credentialStore.isRememberCredentials()).thenReturn(false)
        `when`(credentialStore.isOnboardingCompleted()).thenReturn(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInit_whenOnboardingNotCompleted_startsInScanningStep() {
        `when`(credentialStore.isOnboardingCompleted()).thenReturn(false)
        val viewModel = LoginViewModel(repository, credentialStore)
        assertEquals(LoginStep.SCANNING, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testInit_whenOnboardingCompleted_startsInCredentialsStep() {
        `when`(credentialStore.isOnboardingCompleted()).thenReturn(true)
        `when`(credentialStore.getRouterIp()).thenReturn("192.168.2.1")
        `when`(credentialStore.getPassword()).thenReturn("my-secret-pass")

        val viewModel = LoginViewModel(repository, credentialStore)
        assertEquals(LoginStep.CREDENTIALS, viewModel.uiState.value.currentStep)
        assertEquals("192.168.2.1", viewModel.uiState.value.ipAddress)
        assertEquals("my-secret-pass", viewModel.uiState.value.password)
    }

    @Test
    fun testSelectRouter_savesIpMarksOnboardingCompletedAndTransitions() {
        val viewModel = LoginViewModel(repository, credentialStore)
        viewModel.selectRouter("192.168.2.100")

        verify(credentialStore).saveRouterIp("192.168.2.100")
        verify(credentialStore).setOnboardingCompleted(true)
        assertEquals("192.168.2.100", viewModel.uiState.value.ipAddress)
        assertEquals(LoginStep.CREDENTIALS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testUpdateStep_changesStepSuccessfully() {
        val viewModel = LoginViewModel(repository, credentialStore)
        assertEquals(LoginStep.SCANNING, viewModel.uiState.value.currentStep)

        viewModel.updateStep(LoginStep.MANUAL_IP)
        assertEquals(LoginStep.MANUAL_IP, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testTestManualIp_withEmptyIp_setsError() {
        val viewModel = LoginViewModel(repository, credentialStore)
        viewModel.testManualIp("")
        assertEquals("IP Address is required", viewModel.uiState.value.errorMessage)
    }
}
