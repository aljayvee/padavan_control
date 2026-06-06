package com.example.padavancontrol

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Login : NavKey
@Serializable
data object Dashboard : NavKey
@Serializable
data object Devices : NavKey
@Serializable
data object Traffic : NavKey
@Serializable
data object Settings : NavKey
@Serializable
data object ShellConsole : NavKey
@Serializable
data object LogViewer : NavKey

// Advanced Settings Hub
@Serializable
data object AdvancedHub : NavKey

// Wireless 2.4GHz
@Serializable
data object Wifi2gGeneral : NavKey
@Serializable
data object Wifi2gGuest : NavKey
@Serializable
data object Wifi2gBridge : NavKey
@Serializable
data object Wifi2gMacFilter : NavKey
@Serializable
data object Wifi2gRadius : NavKey
@Serializable
data object Wifi2gProfessional : NavKey

// Wireless 5GHz
@Serializable
data object Wifi5gGeneral : NavKey
@Serializable
data object Wifi5gGuest : NavKey
@Serializable
data object Wifi5gBridge : NavKey
@Serializable
data object Wifi5gMacFilter : NavKey
@Serializable
data object Wifi5gRadius : NavKey
@Serializable
data object Wifi5gProfessional : NavKey

// LAN
@Serializable
data object LanIp : NavKey
@Serializable
data object LanDhcp : NavKey
@Serializable
data object LanRoute : NavKey
@Serializable
data object LanIptv : NavKey
@Serializable
data object LanSwitch : NavKey
@Serializable
data object LanWol : NavKey

// WAN
@Serializable
data object WanConnection : NavKey
@Serializable
data object WanIpv6 : NavKey
@Serializable
data object WanPortForward : NavKey
@Serializable
data object WanDmz : NavKey
@Serializable
data object WanDdns : NavKey

// Firewall
@Serializable
data object FirewallGeneral : NavKey
@Serializable
data object FirewallNetfilter : NavKey
@Serializable
data object FirewallUrlFilter : NavKey
@Serializable
data object FirewallMacFilter : NavKey
@Serializable
data object FirewallServicesFilter : NavKey
@Serializable
data object FirewallDnsIpsetFilter : NavKey

// USB Application
@Serializable
data object UsbCommon : NavKey
@Serializable
data object UsbSamba : NavKey
@Serializable
data object UsbFtp : NavKey
@Serializable
data object UsbModem : NavKey
@Serializable
data object UsbPrinter : NavKey

// Administration
@Serializable
data object AdminSystem : NavKey
@Serializable
data object AdminServices : NavKey
@Serializable
data object AdminOpMode : NavKey
@Serializable
data object AdminFirmware : NavKey
@Serializable
data object AdminSettingsBackup : NavKey
@Serializable
data object AdminConsole : NavKey
@Serializable
data object TtydWebShell : NavKey
@Serializable
data object AdminButtonsLed : NavKey

// Customization
@Serializable
data object CustomScripts : NavKey
@Serializable
data object CustomDetector : NavKey
