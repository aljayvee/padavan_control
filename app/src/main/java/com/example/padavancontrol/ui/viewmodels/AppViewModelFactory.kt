package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.SettingsDataStore

/**
 * A centralized, lifecycle-aware Factory that handles dependency injection
 * for all ViewModels in the application. This ensures that ViewModels are
 * properly managed by the ViewModelStore and their onCleared() is called.
 */
class AppViewModelFactory(
    private val repository: PadavanRepository,
    private val credentialStore: CredentialStore,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(repository, credentialStore) as T
            
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository, credentialStore) as T
            
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(credentialStore, settingsDataStore) as T
            
            modelClass.isAssignableFrom(DevicesViewModel::class.java) ->
                DevicesViewModel(repository) as T
            
            modelClass.isAssignableFrom(TrafficViewModel::class.java) ->
                TrafficViewModel(repository) as T
            
            modelClass.isAssignableFrom(ShellConsoleViewModel::class.java) ->
                ShellConsoleViewModel(repository) as T
            
            modelClass.isAssignableFrom(LogViewerViewModel::class.java) ->
                LogViewerViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedWirelessViewModel::class.java) ->
                AdvancedWirelessViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedLanViewModel::class.java) ->
                AdvancedLanViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedWanViewModel::class.java) ->
                AdvancedWanViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedFirewallViewModel::class.java) ->
                AdvancedFirewallViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedUsbViewModel::class.java) ->
                AdvancedUsbViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedAdminViewModel::class.java) ->
                AdvancedAdminViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedScriptViewModel::class.java) ->
                AdvancedScriptViewModel(repository) as T
            
            modelClass.isAssignableFrom(AdvancedVpnViewModel::class.java) ->
                AdvancedVpnViewModel(repository) as T
            
            modelClass.isAssignableFrom(HardwareInfoViewModel::class.java) ->
                HardwareInfoViewModel(repository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
