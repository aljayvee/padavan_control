package com.example.padavancontrol.ui

import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.TrafficViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TrafficViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PadavanRepository = mock(PadavanRepository::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getTrafficStats()).thenReturn(flowOf(Result.success(
            mapOf("eth3" to Pair(1024L * 1024L * 5, 1024L * 1024L * 2))
        )))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndCalculations() = runTest(testDispatcher) {
        val viewModel = TrafficViewModel(repository)

        // Force polling execution
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1024L * 1024L * 5, state.totalDownloadBytes)
        assertEquals(1024L * 1024L * 2, state.totalUploadBytes)
        assertEquals(1, state.downloadHistory.size)
        assertEquals(1, state.uploadHistory.size)
    }
}
