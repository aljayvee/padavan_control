package com.example.padavancontrol.ui.viewmodels

import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when` as whenever

/**
 * Aggressive regression tests for Memory Leaks identified in the SQA report.
 * Targets: Zombie ViewModels and Unbounded Coroutine Spawning.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryLeakRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PadavanRepository = mock(PadavanRepository::class.java)
    private val credentialStore: CredentialStore = mock(CredentialStore::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(credentialStore.getRouterIp()).thenReturn("192.168.1.1")
        whenever(credentialStore.getHardwareModel()).thenReturn("NEWIFI3")
        
        // Mocking flows to return empty to prevent infinite collection hangs in tests
        whenever(repository.getSystemStatus()).thenReturn(emptyFlow())
        whenever(repository.getWanStatus()).thenReturn(emptyFlow())
        whenever(repository.getLanLinks()).thenReturn(emptyFlow())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verify ViewModel polling stops immediately after onCleared is called`() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(repository, credentialStore)
        
        // Let it run for a few cycles
        advanceTimeBy(5000)
        verify(repository, atLeastOnce()).getSystemStatus()
        
        // Clear all previous call counts
        clearInvocations(repository)
        
        // Trigger onCleared (Simulating lifecycle destruction)
        // In a real app with our fix, this is called by ViewModelStore via viewModel() delegate
        viewModel.onCleared()
        
        // Advance time significantly
        advanceTimeBy(60000) // 1 minute
        
        // Verify NO more calls happen to the repository
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `verify fetchDashboardData uses structured concurrency and does not leak sub-tasks`() = runTest(testDispatcher) {
        // This is harder to test directly via mocks, but we can verify that 
        // if the parent scope is cancelled, the sub-calls are not initiated.
        
        val viewModel = DashboardViewModel(repository, credentialStore)
        viewModel.onCleared()
        
        advanceTimeBy(2000)
        
        // If onCleared is called before the first delay(1500) finishes, no calls should ever happen
        verifyNoMoreInteractions(repository)
    }
}
