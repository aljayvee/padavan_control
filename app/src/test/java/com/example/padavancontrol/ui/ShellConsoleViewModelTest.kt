package com.example.padavancontrol.ui

import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.ui.viewmodels.ShellConsoleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ShellConsoleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PadavanRepository = mock(PadavanRepository::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateCommandInput() = runTest(testDispatcher) {
        val viewModel = ShellConsoleViewModel(repository)
        viewModel.updateCommandInput("ping 8.8.8.8")
        assertEquals("ping 8.8.8.8", viewModel.uiState.value.commandInput)
    }

    @Test
    fun testRunCommand() = runTest(testDispatcher) {
        doReturn("PING 8.8.8.8 (8.8.8.8): 56 data bytes\n64 bytes from 8.8.8.8")
            .`when`(repository).executeCommand(anyString())

        val viewModel = ShellConsoleViewModel(repository)
        viewModel.runCommand("ping 8.8.8.8")

        assertTrue(viewModel.uiState.value.isExecuting)
        assertEquals("", viewModel.uiState.value.commandInput)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isExecuting)
        assertTrue(viewModel.uiState.value.terminalOutput.contains("64 bytes from 8.8.8.8"))
    }
}
