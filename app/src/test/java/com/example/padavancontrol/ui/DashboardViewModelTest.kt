package com.example.padavancontrol.ui

import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.PortLink
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import com.example.padavancontrol.ui.viewmodels.DashboardEvent
import com.example.padavancontrol.ui.viewmodels.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
import org.mockito.Mockito.doReturn
import org.mockito.ArgumentMatchers.anyBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val repository: PadavanRepository = mock(PadavanRepository::class.java)
    private val credentialStore: CredentialStore = mock(CredentialStore::class.java)
    private var viewModel: DashboardViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(credentialStore.getRouterIp()).thenReturn("192.168.2.2")
        whenever(credentialStore.getHardwareModel()).thenReturn("NEWIFI3")
        whenever(repository.getSystemStatus()).thenReturn(flowOf(Result.success(
            SystemStatus(cpuUsage = 15, cpuTemp = 45f, wifiTemp = 40f, ramUsed = 50 * 1024 * 1024, ramTotal = 128 * 1024 * 1024, uptime = 3600L)
        )))
        whenever(repository.getWanStatus()).thenReturn(flowOf(Result.success(
            WanStatus(isConnected = true, wanIp = "1.2.3.4", gateway = "1.2.3.1", dns1 = "8.8.8.8", dns2 = "8.8.4.4", connectionType = "PPPoE", uptime = 1800L)
        )))
        whenever(repository.getLanLinks()).thenReturn(flowOf(Result.success(
            listOf(PortLink(name = "LAN1", isConnected = true, speed = "1G"))
        )))
    }

    @After
    fun tearDown() {
        viewModel?.onCleared()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndPolling() = runTest(testDispatcher) {
        val vm = DashboardViewModel(repository, credentialStore)
        viewModel = vm

        // Let the polling execute once
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals("192.168.2.2", state.routerIp)
        assertTrue(state.wanStatus?.isConnected == true)
        assertEquals("1.2.3.4", state.wanStatus?.wanIp)
        assertEquals(15, state.systemStatus?.cpuUsage)
        assertEquals(1, state.lanLinks.size)
        assertEquals("LAN1", state.lanLinks[0].name)

        vm.onCleared()
    }

    @Test
    fun testToggleWifi2G() = runTest(testDispatcher) {
        doReturn(true).`when`(repository).toggleWifi2G(anyBoolean())

        val vm = DashboardViewModel(repository, credentialStore)
        viewModel = vm
        testDispatcher.scheduler.runCurrent()

        vm.toggleWifi2G(false)
        assertFalse(vm.uiState.value.wifi2GEnabled)

        val events = mutableListOf<DashboardEvent>()
        val job = launch {
            vm.events.collect { events.add(it) }
        }

        testDispatcher.scheduler.runCurrent()
        assertEquals(1, events.size)
        assertTrue((events[0] as DashboardEvent.ShowToast).message.contains("toggled successfully"))

        job.cancel()
        vm.onCleared()
    }

    @Test
    fun testRebootRouter() = runTest(testDispatcher) {
        doReturn(true).`when`(repository).rebootRouter()

        val vm = DashboardViewModel(repository, credentialStore)
        viewModel = vm
        testDispatcher.scheduler.runCurrent()

        vm.showRebootDialog()
        assertTrue(vm.uiState.value.showRebootDialog)

        val events = mutableListOf<DashboardEvent>()
        val job = launch {
            vm.events.collect { events.add(it) }
        }

        vm.confirmReboot()
        assertFalse(vm.uiState.value.showRebootDialog)

        testDispatcher.scheduler.runCurrent()
        assertEquals(1, events.size)
        assertEquals("Rebooting router...", (events[0] as DashboardEvent.ShowToast).message)

        job.cancel()
        vm.onCleared()
    }
}
