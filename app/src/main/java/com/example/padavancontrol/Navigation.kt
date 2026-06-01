package com.example.padavancontrol

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.DefaultPadavanRepository
import com.example.padavancontrol.data.SettingsDataStore
import com.example.padavancontrol.ui.components.BreadcrumbBar
import androidx.compose.foundation.layout.Column
import com.example.padavancontrol.ui.screens.DashboardScreen
import com.example.padavancontrol.ui.screens.DevicesScreen
import com.example.padavancontrol.ui.screens.LoginScreen
import com.example.padavancontrol.ui.screens.SettingsScreen
import com.example.padavancontrol.ui.screens.TrafficScreen
import com.example.padavancontrol.ui.screens.ShellConsoleScreen
import com.example.padavancontrol.ui.screens.LogViewerScreen
import com.example.padavancontrol.ui.screens.AdvancedSidebarContent
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.example.padavancontrol.ui.screens.AdvancedSettingFormPlaceholderScreen
import com.example.padavancontrol.ui.screens.AdvancedWirelessScreen
import com.example.padavancontrol.ui.screens.AdvancedLanScreen
import com.example.padavancontrol.ui.screens.AdvancedWanScreen
import com.example.padavancontrol.ui.screens.AdvancedFirewallScreen
import com.example.padavancontrol.ui.screens.AdvancedUsbScreen
import com.example.padavancontrol.ui.screens.AdvancedAdminScreen
import com.example.padavancontrol.ui.screens.AdvancedScriptScreen
import com.example.padavancontrol.ui.viewmodels.DashboardViewModel
import com.example.padavancontrol.ui.viewmodels.LoginViewModel
import com.example.padavancontrol.ui.viewmodels.SettingsViewModel
import com.example.padavancontrol.ui.viewmodels.DevicesViewModel
import com.example.padavancontrol.ui.viewmodels.TrafficViewModel
import com.example.padavancontrol.ui.viewmodels.ShellConsoleViewModel
import com.example.padavancontrol.ui.viewmodels.LogViewerViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedWirelessViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedLanViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedWanViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedFirewallViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedUsbViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedAdminViewModel
import com.example.padavancontrol.ui.viewmodels.AdvancedScriptViewModel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.padavancontrol.ui.screens.SplashLoadingScreen
import com.example.padavancontrol.network.RetrofitClient

sealed interface AppInitState {
    object Loading : AppInitState
    data class Success(
        val credentialStore: CredentialStore,
        val repository: DefaultPadavanRepository,
        val settingsDataStore: SettingsDataStore
    ) : AppInitState
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    var initState by remember { mutableStateOf<AppInitState>(AppInitState.Loading) }

    LaunchedEffect(context) {
        withContext(Dispatchers.IO) {
            val credentialStore = CredentialStore(context)
            val repository = DefaultPadavanRepository(credentialStore)
            val settingsDataStore = SettingsDataStore(context)
            initState = AppInitState.Success(credentialStore, repository, settingsDataStore)
        }
    }

    when (val state = initState) {
        is AppInitState.Loading -> {
            SplashLoadingScreen()
        }
        is AppInitState.Success -> {
            MainNavigationContent(
                credentialStore = state.credentialStore,
                repository = state.repository,
                settingsDataStore = state.settingsDataStore
            )
        }
    }
}

@Composable
fun MainNavigationContent(
    credentialStore: CredentialStore,
    repository: DefaultPadavanRepository,
    settingsDataStore: SettingsDataStore
) {
    val context = LocalContext.current

    val loginViewModel = remember(repository, credentialStore) {
        LoginViewModel(repository, credentialStore)
    }
    val dashboardViewModel = remember(repository, credentialStore) {
        DashboardViewModel(repository, credentialStore)
    }
    val settingsViewModel = remember(credentialStore, settingsDataStore) {
        SettingsViewModel(credentialStore, settingsDataStore)
    }
    val devicesViewModel = remember(repository) {
        DevicesViewModel(repository)
    }
    val trafficViewModel = remember(repository) {
        TrafficViewModel(repository)
    }
    val shellConsoleViewModel = remember(repository) {
        ShellConsoleViewModel(repository)
    }
    val logViewerViewModel = remember(repository) {
        LogViewerViewModel(repository)
    }

    val advancedWirelessViewModel = remember(repository) {
        AdvancedWirelessViewModel(repository)
    }
    val advancedLanViewModel = remember(repository) {
        AdvancedLanViewModel(repository)
    }
    val advancedWanViewModel = remember(repository) {
        AdvancedWanViewModel(repository)
    }
    val advancedFirewallViewModel = remember(repository) {
        AdvancedFirewallViewModel(repository)
    }
    val advancedUsbViewModel = remember(repository) {
        AdvancedUsbViewModel(repository)
    }
    val advancedAdminViewModel = remember(repository) {
        AdvancedAdminViewModel(repository)
    }
    val advancedScriptViewModel = remember(repository) {
        AdvancedScriptViewModel(repository)
    }

    val startKey =
        if (credentialStore.isRememberCredentials() && credentialStore.getPassword().isNotEmpty()) {
            Dashboard
        } else {
            Login
        }

    val backStack = rememberNavBackStack(startKey)
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(credentialStore, backStack) {
        RetrofitClient.unauthorizedEvents.collect {
            credentialStore.clearCredentials()
            backStack.clear()
            backStack.add(Login)
        }
    }

    val showBottomBar = currentKey == Dashboard || currentKey == Devices || currentKey == Traffic || currentKey == Settings

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBottomBar,
        drawerContent = {
            if (showBottomBar) {
                AdvancedSidebarContent(
                    onNavigateToPage = { pageKey ->
                        scope.launch { drawerState.close() }
                        backStack.add(pageKey)
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentKey == Dashboard,
                        onClick = {
                            if (currentKey != Dashboard) {
                                backStack.clear()
                                backStack.add(Dashboard)
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentKey == Devices,
                        onClick = {
                            if (currentKey != Devices) {
                                backStack.add(Devices)
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Devices") },
                        label = { Text("Devices") }
                    )
                    NavigationBarItem(
                        selected = currentKey == Traffic,
                        onClick = {
                            if (currentKey != Traffic) {
                                backStack.add(Traffic)
                            }
                        },
                        icon = { Icon(Icons.Filled.Info, contentDescription = "Traffic") },
                        label = { Text("Traffic") }
                    )
                    NavigationBarItem(
                        selected = currentKey == Settings,
                        onClick = {
                            if (currentKey != Settings) {
                                backStack.add(Settings)
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            BreadcrumbBar(
                backStack = backStack,
                onNavigateToKey = { key ->
                    val idx = backStack.indexOf(key)
                    if (idx >= 0) {
                        while (backStack.size > idx + 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                }
            )
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                modifier = Modifier.weight(1f),
                entryProvider = androidx.navigation3.runtime.entryProvider {
                entry<Login> {
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = {
                            backStack.clear()
                            backStack.add(Dashboard)
                        },
                        modifier = Modifier
                    )
                }
                entry<Dashboard> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToSettings = {
                            backStack.add(Settings)
                        },
                        onNavigateToShellConsole = {
                            backStack.add(ShellConsole)
                        },
                        onNavigateToLogViewer = {
                            backStack.add(LogViewer)
                        },
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                    )
                }
                entry<Devices> {
                    DevicesScreen(
                        viewModel = devicesViewModel,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                    )
                }
                entry<Traffic> {
                    TrafficScreen(
                        viewModel = trafficViewModel,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onLogout = {
                            credentialStore.clearCredentials()
                            backStack.clear()
                            backStack.add(Login)
                        },
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                    )
                }
                entry<ShellConsole> {
                    ShellConsoleScreen(
                        viewModel = shellConsoleViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        modifier = Modifier
                    )
                }
                entry<LogViewer> {
                    LogViewerScreen(
                        viewModel = logViewerViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        modifier = Modifier
                    )
                }
                
                // Wireless 2.4GHz
                entry<Wifi2gGeneral> { AdvancedWirelessScreen(is5GHz = false, section = "General", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi2gGuest> { AdvancedWirelessScreen(is5GHz = false, section = "Guest", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi2gBridge> { AdvancedWirelessScreen(is5GHz = false, section = "Bridge", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi2gMacFilter> { AdvancedWirelessScreen(is5GHz = false, section = "MAC Filter", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi2gRadius> { AdvancedWirelessScreen(is5GHz = false, section = "RADIUS", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi2gProfessional> { AdvancedWirelessScreen(is5GHz = false, section = "Professional", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // Wireless 5GHz
                entry<Wifi5gGeneral> { AdvancedWirelessScreen(is5GHz = true, section = "General", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi5gGuest> { AdvancedWirelessScreen(is5GHz = true, section = "Guest", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi5gBridge> { AdvancedWirelessScreen(is5GHz = true, section = "Bridge", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi5gMacFilter> { AdvancedWirelessScreen(is5GHz = true, section = "MAC Filter", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi5gRadius> { AdvancedWirelessScreen(is5GHz = true, section = "RADIUS", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<Wifi5gProfessional> { AdvancedWirelessScreen(is5GHz = true, section = "Professional", viewModel = advancedWirelessViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // LAN
                entry<LanIp> { AdvancedLanScreen("IP", advancedLanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<LanDhcp> { AdvancedLanScreen("DHCP", advancedLanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<LanRoute> { AdvancedLanScreen("Route", advancedLanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<LanIptv> { AdvancedLanScreen("IPTV", advancedLanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<LanSwitch> { AdvancedLanScreen("Switch", advancedLanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<LanWol> { AdvancedLanScreen("WOL", advancedLanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // WAN
                entry<WanConnection> { AdvancedWanScreen("Connection", advancedWanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<WanIpv6> { AdvancedWanScreen("IPv6", advancedWanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<WanPortForward> { AdvancedWanScreen("PortForward", advancedWanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<WanDmz> { AdvancedWanScreen("DMZ", advancedWanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<WanDdns> { AdvancedWanScreen("DDNS", advancedWanViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // Firewall
                entry<FirewallGeneral> { AdvancedFirewallScreen("General", viewModel = advancedFirewallViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<FirewallNetfilter> { AdvancedFirewallScreen("Netfilter", viewModel = advancedFirewallViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<FirewallUrlFilter> { AdvancedFirewallScreen("URLFilter", viewModel = advancedFirewallViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<FirewallMacFilter> { AdvancedFirewallScreen("MACFilter", viewModel = advancedFirewallViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<FirewallServicesFilter> { AdvancedFirewallScreen("ServicesFilter", viewModel = advancedFirewallViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // USB Application
                entry<UsbCommon> { AdvancedUsbScreen("Common", viewModel = advancedUsbViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<UsbSamba> { AdvancedUsbScreen("Samba", viewModel = advancedUsbViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<UsbFtp> { AdvancedUsbScreen("FTP", viewModel = advancedUsbViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<UsbModem> { AdvancedUsbScreen("Modem", viewModel = advancedUsbViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<UsbPrinter> { AdvancedUsbScreen("Printer", viewModel = advancedUsbViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // Administration
                entry<AdminSystem> { AdvancedAdminScreen("System", viewModel = advancedAdminViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<AdminServices> { AdvancedAdminScreen("Services", viewModel = advancedAdminViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<AdminOpMode> { AdvancedAdminScreen("OpMode", viewModel = advancedAdminViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<AdminFirmware> { AdvancedAdminScreen("Firmware", viewModel = advancedAdminViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<AdminSettingsBackup> { AdvancedAdminScreen("Backup", viewModel = advancedAdminViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<AdminConsole> { ShellConsoleScreen(viewModel = shellConsoleViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<AdminButtonsLed> { AdvancedAdminScreen("ButtonsLed", viewModel = advancedAdminViewModel, repository = repository, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                
                // Customization
                entry<CustomScripts> { AdvancedScriptScreen("Scripts", viewModel = advancedScriptViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
                entry<CustomDetector> { AdvancedScriptScreen("Detector", viewModel = advancedScriptViewModel, onNavigateBack = { backStack.removeLastOrNull() }, modifier = Modifier) }
            }
        )
    }
}
}
}
