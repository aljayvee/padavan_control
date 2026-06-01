package com.example.padavancontrol.ui

import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.SettingsDataStore
import com.example.padavancontrol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val credentialStore: CredentialStore = mock(CredentialStore::class.java)
    private val settingsDataStore: SettingsDataStore = mock(SettingsDataStore::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(credentialStore.getRouterIp()).thenReturn("192.168.2.2")
        whenever(credentialStore.isRememberCredentials()).thenReturn(true)
        whenever(credentialStore.isUseBiometric()).thenReturn(false)
        whenever(credentialStore.getUsername()).thenReturn("admin")
        whenever(credentialStore.getPassword()).thenReturn("password")

        whenever(settingsDataStore.isMonitoringEnabled()).thenReturn(flowOf(false))
        whenever(settingsDataStore.getMonitorIntervalMinutes()).thenReturn(flowOf(15))
        whenever(settingsDataStore.isNotifyWanDisconnect()).thenReturn(flowOf(true))
        whenever(settingsDataStore.isNotifyNewDevice()).thenReturn(flowOf(true))
        whenever(settingsDataStore.isNotifyHighTemp()).thenReturn(flowOf(true))
        whenever(settingsDataStore.getHighTempThreshold()).thenReturn(flowOf(75))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(credentialStore, settingsDataStore)

        val state = viewModel.uiState.value
        assertEquals("192.168.2.2", state.routerIp)
        assertTrue(state.rememberMe)
        assertFalse(state.useBiometric)
    }

    @Test
    fun testDebouncedRouterIpSave() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(credentialStore, settingsDataStore)

        // Type IP
        viewModel.updateRouterIp("192.168.2.1")
        assertEquals("192.168.2.1", viewModel.uiState.value.routerIp)

        // Advance 300ms, should not save yet
        testDispatcher.scheduler.advanceTimeBy(300)
        verify(credentialStore, never()).saveRouterIp(anyString())

        // Type another IP, resetting the debounce
        viewModel.updateRouterIp("192.168.2.3")
        assertEquals("192.168.2.3", viewModel.uiState.value.routerIp)

        // Advance 300ms, should not save yet
        testDispatcher.scheduler.advanceTimeBy(300)
        verify(credentialStore, never()).saveRouterIp(anyString())

        // Advance another 250ms (total 550ms since last change), should save the final IP
        testDispatcher.scheduler.advanceTimeBy(250)
        verify(credentialStore, times(1)).saveRouterIp("192.168.2.3")
    }

    @Test
    fun testUpdateRememberMe() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(credentialStore, settingsDataStore)

        viewModel.updateRememberMe(false)
        assertFalse(viewModel.uiState.value.rememberMe)

        verify(credentialStore).saveCredentials(
            eq("admin"),
            eq("192.168.2.2"),
            eq("password"),
            eq(false),
            eq(false)
        )
    }
}
