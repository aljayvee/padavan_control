package com.example.padavancontrol.ui

import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.LanConfig
import com.example.padavancontrol.data.models.ScriptConfig
import com.example.padavancontrol.ui.viewmodels.AdvancedFirewallViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

/**
 * Kotlin-safe wrapper for Mockito's any() matcher.
 * Mockito's any() returns null, which crashes with Kotlin non-null params.
 * This uses a cast to convince the Kotlin compiler it's non-null.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> anyNonNull(): T = org.mockito.ArgumentMatchers.any<T>() as T

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedFirewallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PadavanRepository = mock(PadavanRepository::class.java)
    private var viewModel: AdvancedFirewallViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AdvancedFirewallViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testParseDomainsFromDnsmasq() = runTest(testDispatcher) {
        val mockDnsmasq = """
            # Some other configs
            dhcp-range=192.168.2.10,192.168.2.200,24h
            
            # --- Padavan Control App Domains Start ---
            address=/pornhub.com/0.0.0.0
            address=/pornhub.com/::
            address=/example.com/0.0.0.0
            address=/example.com/::
            local=/use-application-dns.net/
            # --- Padavan Control App Domains End ---
        """.trimIndent()

        val mockLanConfig = LanConfig(
            lanIpAddr = "192.168.2.1",
            lanNetmask = "255.255.255.0",
            dhcpEnabled = true,
            dhcpStart = "10",
            dhcpEnd = "200",
            dhcpLease = 86400L,
            dhcpVerbose = 1,
            dnsmasqDnsmasqConf = mockDnsmasq
        )

        val mockScriptConfig = ScriptConfig(scriptIpRules = "# --- Padavan Control App DoH Block Start ---")

        whenever(repository.getLanConfig("Advanced_DHCP_Content.asp")).thenReturn(flowOf(Result.success(mockLanConfig)))
        whenever(repository.getScriptConfig("Advanced_Scripts_Content.asp")).thenReturn(flowOf(Result.success(mockScriptConfig)))

        viewModel?.loadConfig("Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        val state = viewModel?.uiState?.value
        assertEquals(2, state?.blockedDomains?.size)
        assertEquals("pornhub.com", state?.blockedDomains?.get(0))
        assertEquals("example.com", state?.blockedDomains?.get(1))
    }

    @Test
    fun testSaveDnsIpsetConfig() = runTest(testDispatcher) {
        val initialDnsmasq = """
            # --- Padavan Control App Domains Start ---
            address=/oldblock.com/0.0.0.0
            address=/oldblock.com/::
            # --- Padavan Control App Domains End ---
        """.trimIndent()

        val mockLanConfig = LanConfig(
            lanIpAddr = "192.168.2.1",
            lanNetmask = "255.255.255.0",
            dhcpEnabled = true,
            dhcpStart = "10",
            dhcpEnd = "200",
            dhcpLease = 86400L,
            dhcpVerbose = 1,
            dnsmasqDnsmasqConf = initialDnsmasq
        )

        val initialScripts = """
            # Some startup firewall rules
            iptables -P INPUT ACCEPT
        """.trimIndent()

        val mockScriptConfig = ScriptConfig(
            scriptIpRules = initialScripts
        )

        // Track captured arguments via doAnswer to avoid Kotlin non-null captor NPE
        var capturedLanConfig: LanConfig? = null
        var capturedScriptConfig: ScriptConfig? = null

        whenever(repository.getLanConfig("Advanced_DHCP_Content.asp")).thenReturn(flowOf(Result.success(mockLanConfig)))
        whenever(repository.getScriptConfig("Advanced_Scripts_Content.asp")).thenReturn(flowOf(Result.success(mockScriptConfig)))

        // Use doAnswer to capture args (avoids ArgumentCaptor null issue with Kotlin)
        doAnswer { invocation ->
            capturedLanConfig = invocation.getArgument<LanConfig>(1)
            true
        }.`when`(repository).saveLanConfig(anyString(), anyNonNull())

        doAnswer { invocation ->
            capturedScriptConfig = invocation.getArgument<ScriptConfig>(1)
            true
        }.`when`(repository).saveScriptConfig(anyString(), anyNonNull())

        whenever(repository.commitFlash()).thenReturn(true)
        whenever(repository.executeCommand(anyString())).thenReturn("Success")

        // Load config first to set initial state
        viewModel?.loadConfig("Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        // Call addDomainBlock which triggers saveDnsIpsetConfig
        viewModel?.addDomainBlock("pornhub.com", "Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        // Assertions for dnsmasq conf
        val savedDnsmasq = capturedLanConfig?.dnsmasqDnsmasqConf
        assertTrue("Expected saved dnsmasq conf to not be null", savedDnsmasq != null)
        assertTrue("Expected oldblock.com sinkhole rule", savedDnsmasq!!.contains("address=/oldblock.com/0.0.0.0"))
        assertTrue("Expected pornhub.com sinkhole rule", savedDnsmasq.contains("address=/pornhub.com/0.0.0.0"))
        assertTrue("Expected DoH canary block", savedDnsmasq.contains("local=/use-application-dns.net/"))

        // Assertions for script
        val savedScripts = capturedScriptConfig?.scriptIpRules
        assertTrue("Expected saved script to not be null", savedScripts != null)
        assertFalse("Expected DNS hijack rule to be removed", savedScripts!!.contains("iptables -t nat -C PREROUTING -i br0 -p udp --dport 53 -j REDIRECT --to-ports 53"))

        verify(repository).commitFlash()
        verify(repository).executeCommand("restart_firewall; restart_dhcpd")
    }

    @Test
    fun testParseDomainsEmpty() = runTest(testDispatcher) {
        // Test that empty/no-markers dnsmasq conf produces empty domain list
        val mockDnsmasq = """
            dhcp-range=192.168.2.10,192.168.2.200,24h
        """.trimIndent()

        val mockLanConfig = LanConfig(
            lanIpAddr = "192.168.2.1",
            lanNetmask = "255.255.255.0",
            dhcpEnabled = true,
            dhcpStart = "10",
            dhcpEnd = "200",
            dhcpLease = 86400L,
            dhcpVerbose = 1,
            dnsmasqDnsmasqConf = mockDnsmasq
        )

        val mockScriptConfig = ScriptConfig(scriptIpRules = "# --- Padavan Control App DoH Block Start ---")

        whenever(repository.getLanConfig("Advanced_DHCP_Content.asp")).thenReturn(flowOf(Result.success(mockLanConfig)))
        whenever(repository.getScriptConfig("Advanced_Scripts_Content.asp")).thenReturn(flowOf(Result.success(mockScriptConfig)))

        viewModel?.loadConfig("Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        val state = viewModel?.uiState?.value
        assertEquals(0, state?.blockedDomains?.size)
    }

    @Test
    fun testDuplicateDomainNotAdded() = runTest(testDispatcher) {
        val mockDnsmasq = """
            # --- Padavan Control App Domains Start ---
            address=/pornhub.com/0.0.0.0
            address=/pornhub.com/::
            # --- Padavan Control App Domains End ---
        """.trimIndent()

        val mockLanConfig = LanConfig(
            lanIpAddr = "192.168.2.1",
            lanNetmask = "255.255.255.0",
            dhcpEnabled = true,
            dhcpStart = "10",
            dhcpEnd = "200",
            dhcpLease = 86400L,
            dhcpVerbose = 1,
            dnsmasqDnsmasqConf = mockDnsmasq
        )

        val mockScriptConfig = ScriptConfig(scriptIpRules = "# --- Padavan Control App DoH Block Start ---")

        whenever(repository.getLanConfig("Advanced_DHCP_Content.asp")).thenReturn(flowOf(Result.success(mockLanConfig)))
        whenever(repository.getScriptConfig("Advanced_Scripts_Content.asp")).thenReturn(flowOf(Result.success(mockScriptConfig)))

        viewModel?.loadConfig("Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        // Try adding duplicate — should be a no-op (addDomainBlock returns early)
        viewModel?.addDomainBlock("pornhub.com", "Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        val state = viewModel?.uiState?.value
        // Should still be 1, not 2
        assertEquals(1, state?.blockedDomains?.size)
    }

    @Test
    fun testLoadConfigFailure() = runTest(testDispatcher) {
        val errorMsg = "Connection refused"
        whenever(repository.getLanConfig("Advanced_DHCP_Content.asp")).thenReturn(flowOf(Result.failure(RuntimeException(errorMsg))))

        viewModel?.loadConfig("Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        val state = viewModel?.uiState?.value
        assertEquals(false, state?.isLoading)
        assertEquals(errorMsg, state?.errorMessage)
    }

    @Test
    fun testToggleDoHBlock() = runTest(testDispatcher) {
        val mockLanConfig = LanConfig(dnsmasqDnsmasqConf = "")
        val mockScriptConfig = ScriptConfig(scriptIpRules = "")

        var capturedScriptConfig: ScriptConfig? = null

        whenever(repository.getLanConfig("Advanced_DHCP_Content.asp")).thenReturn(flowOf(Result.success(mockLanConfig)))
        whenever(repository.getScriptConfig("Advanced_Scripts_Content.asp")).thenReturn(flowOf(Result.success(mockScriptConfig)))

        doAnswer { invocation ->
            capturedScriptConfig = invocation.getArgument<ScriptConfig>(1)
            true
        }.`when`(repository).saveScriptConfig(anyString(), anyNonNull())
        whenever(repository.saveLanConfig(anyString(), anyNonNull())).thenReturn(true)
        whenever(repository.commitFlash()).thenReturn(true)
        whenever(repository.executeCommand(anyString())).thenReturn("Success")

        // Initial load
        viewModel?.loadConfig("Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        // Toggle feature
        viewModel?.toggleBlockPublicDoh(true, "Advanced_DHCP_Content.asp")
        advanceUntilIdle()

        // Verify state is updated
        assertEquals(true, viewModel?.uiState?.value?.blockPublicDoh)

        // Verify script config contains the DoH block rules
        val savedScripts = capturedScriptConfig?.scriptIpRules
        assertTrue(savedScripts != null)
        assertTrue("Expected DoH Block Start Marker", savedScripts!!.contains("# --- Padavan Control App DoH Block Start ---"))
        assertTrue("Expected 8.8.8.8 to be blocked", savedScripts.contains("iptables -C FORWARD -d 8.8.8.8 -p tcp --dport 443 -j DROP"))
        assertTrue("Expected port 853 to be blocked", savedScripts.contains("iptables -C FORWARD -p tcp --dport 853 -j DROP"))
    }
}
