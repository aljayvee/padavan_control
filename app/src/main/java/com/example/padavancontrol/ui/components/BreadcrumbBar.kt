package com.example.padavancontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.padavancontrol.*

@Composable
fun BreadcrumbBar(
    backStack: List<NavKey>,
    onNavigateToKey: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show breadcrumbs if we are deep inside navigation (size > 1)
    if (backStack.size <= 1) return

    val currentKey = backStack.lastOrNull()
    if (currentKey == null || 
        currentKey == Login || 
        currentKey == Dashboard || 
        currentKey == Devices || 
        currentKey == Traffic || 
        currentKey == Settings) {
        return
    }

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        backStack.forEachIndexed { index, navKey ->
            val label = getLabelForKey(navKey)
            val isLast = index == backStack.size - 1

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLast) Color(0xFF333333) else Color(0xFF008080), // ArcherTeal-ish
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = !isLast) {
                            onNavigateToKey(navKey)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (!isLast) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "separator",
                        tint = Color.Gray,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getLabelForKey(key: NavKey): String {
    return when (key) {
        is Login -> "Login"
        is Dashboard -> "Home"
        is Devices -> "Devices"
        is Traffic -> "Traffic"
        is Settings -> "Settings"
        is ShellConsole -> "Console"
        is LogViewer -> "Syslog"
        is AdvancedHub -> "Advanced"
        
        // Wireless 2.4G
        is Wifi2gGeneral -> "2.4G General"
        is Wifi2gGuest -> "2.4G Guest"
        is Wifi2gBridge -> "2.4G Bridge"
        is Wifi2gMacFilter -> "2.4G MAC Filter"
        is Wifi2gRadius -> "2.4G RADIUS"
        is Wifi2gProfessional -> "2.4G Professional"

        // Wireless 5G
        is Wifi5gGeneral -> "5G General"
        is Wifi5gGuest -> "5G Guest"
        is Wifi5gBridge -> "5G Bridge"
        is Wifi5gMacFilter -> "5G MAC Filter"
        is Wifi5gRadius -> "5G RADIUS"
        is Wifi5gProfessional -> "5G Professional"

        // LAN
        is LanIp -> "LAN IP"
        is LanDhcp -> "LAN DHCP"
        is LanRoute -> "Static Route"
        is LanIptv -> "IPTV"
        is LanSwitch -> "Switch"
        is LanWol -> "WOL"

        // WAN
        is WanConnection -> "WAN Connection"
        is WanIpv6 -> "WAN IPv6"
        is WanPortForward -> "Port Forwarding"
        is WanDmz -> "DMZ"
        is WanDdns -> "DDNS"

        // Firewall
        is FirewallGeneral -> "Firewall Security"
        is FirewallNetfilter -> "Netfilter SPI"
        is FirewallUrlFilter -> "URL Filter"
        is FirewallMacFilter -> "MAC Filter"
        is FirewallServicesFilter -> "Services Filter"

        // USB
        is UsbCommon -> "USB Settings"
        is UsbSamba -> "Samba Share"
        is UsbFtp -> "FTP Share"
        is UsbModem -> "USB Modem"
        is UsbPrinter -> "Printer Share"

        // Admin
        is AdminSystem -> "System Settings"
        is AdminServices -> "Services"
        is AdminOpMode -> "Operation Mode"
        is AdminFirmware -> "Firmware Upgrade"
        is AdminSettingsBackup -> "Backup Settings"
        is AdminConsole -> "Console"
        is AdminButtonsLed -> "Buttons & LEDs"

        // Customization
        is CustomScripts -> "Startup Scripts"
        is CustomDetector -> "Internet Watchdog"

        else -> "Category"
    }
}
