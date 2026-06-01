package com.example.padavancontrol.ui

import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.ui.viewmodels.DevicesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PadavanRepository = mock(PadavanRepository::class.java)
    private var viewModel: DevicesViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getLanClients()).thenReturn(flowOf(Result.success(
            listOf(
                LanClient(hostname = "iPhone", ipAddress = "192.168.2.10", macAddress = "AA:BB:CC:DD:EE:FF", connectionType = "5G", isOnline = true),
                LanClient(hostname = "PC", ipAddress = "192.168.2.11", macAddress = "11:22:33:44:55:66", connectionType = "LAN", isOnline = false)
            )
        )))
    }

    @After
    fun tearDown() {
        viewModel?.onCleared()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndPolling() = runTest(testDispatcher) {
        val vm = DevicesViewModel(repository)
        viewModel = vm

        // Force polling loop to execute once
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.clients.size)
        assertEquals("iPhone", state.clients[0].hostname)
        assertTrue(state.clients[0].isOnline)
        assertFalse(state.clients[1].isOnline)

        vm.onCleared()
    }

    @Test
    fun testUpdateSearchQuery() = runTest(testDispatcher) {
        val vm = DevicesViewModel(repository)
        viewModel = vm
        vm.updateSearchQuery("iPhone")
        assertEquals("iPhone", vm.uiState.value.searchQuery)

        vm.onCleared()
    }
}
