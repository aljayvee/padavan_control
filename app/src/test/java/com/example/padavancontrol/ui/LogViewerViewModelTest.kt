package com.example.padavancontrol.ui

import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.LogViewerEvent
import com.example.padavancontrol.ui.viewmodels.LogViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
import org.mockito.Mockito.doReturn
import org.mockito.ArgumentMatchers.anyString

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PadavanRepository = mock(PadavanRepository::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getSystemLogs()).thenReturn(flowOf(Result.success(
            "Jun  1 00:00:01 newifi3 dnsmasq[1234]: query[A] google.com\nJun  1 00:00:02 newifi3 dnsmasq[1234]: reply google.com"
        )))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndLogsFetch() = runTest(testDispatcher) {
        val viewModel = LogViewerViewModel(repository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.rawLogs.contains("dnsmasq[1234]"))
    }

    @Test
    fun testUpdateFilterQuery() = runTest(testDispatcher) {
        val viewModel = LogViewerViewModel(repository)
        viewModel.updateFilterQuery("google")
        assertEquals("google", viewModel.uiState.value.filterQuery)
    }

    @Test
    fun testClearLogs() = runTest(testDispatcher) {
        doReturn(true).`when`(repository).clearSystemLogs()

        val viewModel = LogViewerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val events = mutableListOf<LogViewerEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.clearLogs()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Syslog is currently empty.", viewModel.uiState.value.rawLogs)
        assertEquals(1, events.size)
        assertEquals("System syslog cleared", (events[0] as LogViewerEvent.ShowToast).message)

        job.cancel()
    }
}
