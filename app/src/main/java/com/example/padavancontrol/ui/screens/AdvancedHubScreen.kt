package com.example.padavancontrol.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.padavancontrol.AdminButtonsLed
import com.example.padavancontrol.AdminConsole
import com.example.padavancontrol.AdminFirmware
import com.example.padavancontrol.AdminOpMode
import com.example.padavancontrol.AdminServices
import com.example.padavancontrol.AdminSettingsBackup
import com.example.padavancontrol.AdminSystem
import com.example.padavancontrol.CustomDetector
import com.example.padavancontrol.CustomScripts
import com.example.padavancontrol.FirewallGeneral
import com.example.padavancontrol.FirewallMacFilter
import com.example.padavancontrol.FirewallNetfilter
import com.example.padavancontrol.FirewallServicesFilter
import com.example.padavancontrol.FirewallUrlFilter
import com.example.padavancontrol.LanDhcp
import com.example.padavancontrol.LanIp
import com.example.padavancontrol.LanIptv
import com.example.padavancontrol.LanRoute
import com.example.padavancontrol.LanSwitch
import com.example.padavancontrol.LanWol
import com.example.padavancontrol.UsbCommon
import com.example.padavancontrol.UsbFtp
import com.example.padavancontrol.UsbModem
import com.example.padavancontrol.UsbPrinter
import com.example.padavancontrol.UsbSamba
import com.example.padavancontrol.WanDdns
import com.example.padavancontrol.WanDmz
import com.example.padavancontrol.WanConnection
import com.example.padavancontrol.WanIpv6
import com.example.padavancontrol.WanPortForward
import com.example.padavancontrol.Wifi2gBridge
import com.example.padavancontrol.Wifi2gGeneral
import com.example.padavancontrol.Wifi2gGuest
import com.example.padavancontrol.Wifi2gMacFilter
import com.example.padavancontrol.Wifi2gProfessional
import com.example.padavancontrol.Wifi2gRadius
import com.example.padavancontrol.Wifi5gBridge
import com.example.padavancontrol.Wifi5gGeneral
import com.example.padavancontrol.Wifi5gGuest
import com.example.padavancontrol.Wifi5gMacFilter
import com.example.padavancontrol.Wifi5gProfessional
import com.example.padavancontrol.Wifi5gRadius
import com.example.padavancontrol.theme.ArcherTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPage: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    // Accordion expand states
    val expandedStates = remember {
        mutableStateMapOf(
            "wireless2g" to false,
            "wireless5g" to false,
            "lan" to false,
            "wan" to false,
            "firewall" to false,
            "usb" to false,
            "admin" to false,
            "custom" to false
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArcherTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Sub-system Configurations",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category 1: Wireless 2.4GHz
            AccordionCategory(
                title = "Wireless 2.4GHz",
                subtitle = "SSIDs, security, and radio limits",
                isExpanded = expandedStates["wireless2g"] == true,
                onToggle = { expandedStates["wireless2g"] = !(expandedStates["wireless2g"] ?: false) },
                items = listOf(
                    "General Settings" to Wifi2gGeneral,
                    "Guest AP" to Wifi2gGuest,
                    "Bridge (WDS)" to Wifi2gBridge,
                    "Wireless MAC Filter" to Wifi2gMacFilter,
                    "RADIUS Settings" to Wifi2gRadius,
                    "Professional Parameters" to Wifi2gProfessional
                ),
                onItemClick = onNavigateToPage
            )

            // Category 2: Wireless 5GHz
            AccordionCategory(
                title = "Wireless 5GHz",
                subtitle = "High-speed 5G channels and security",
                isExpanded = expandedStates["wireless5g"] == true,
                onToggle = { expandedStates["wireless5g"] = !(expandedStates["wireless5g"] ?: false) },
                items = listOf(
                    "General Settings" to Wifi5gGeneral,
                    "Guest AP" to Wifi5gGuest,
                    "Bridge (WDS)" to Wifi5gBridge,
                    "Wireless MAC Filter" to Wifi5gMacFilter,
                    "RADIUS Settings" to Wifi5gRadius,
                    "Professional Parameters" to Wifi5gProfessional
                ),
                onItemClick = onNavigateToPage
            )

            // Category 3: LAN
            AccordionCategory(
                title = "Local Network (LAN)",
                subtitle = "Subnet routing, DHCP lease and IPTV",
                isExpanded = expandedStates["lan"] == true,
                onToggle = { expandedStates["lan"] = !(expandedStates["lan"] ?: false) },
                items = listOf(
                    "LAN IP & Netmask" to LanIp,
                    "DHCP Server Pool" to LanDhcp,
                    "Static Routes" to LanRoute,
                    "IPTV Setup" to LanIptv,
                    "Ethernet Switch Config" to LanSwitch,
                    "Wake-on-LAN client" to LanWol
                ),
                onItemClick = onNavigateToPage
            )

            // Category 4: WAN
            AccordionCategory(
                title = "Internet Gateway (WAN)",
                subtitle = "WAN, IPv6 settings, DMZ & DDNS",
                isExpanded = expandedStates["wan"] == true,
                onToggle = { expandedStates["wan"] = !(expandedStates["wan"] ?: false) },
                items = listOf(
                    "Internet Connection" to WanConnection,
                    "IPv6 Protocol stack" to WanIpv6,
                    "Port Forwarding (NAT)" to WanPortForward,
                    "DMZ Host Settings" to WanDmz,
                    "Dynamic DNS (DDNS)" to WanDdns
                ),
                onItemClick = onNavigateToPage
            )

            // Category 5: Firewall
            AccordionCategory(
                title = "Security & Firewall",
                subtitle = "Packet filters, MAC / URL blocklists",
                isExpanded = expandedStates["firewall"] == true,
                onToggle = { expandedStates["firewall"] = !(expandedStates["firewall"] ?: false) },
                items = listOf(
                    "General Security" to FirewallGeneral,
                    "Netfilter SPI rules" to FirewallNetfilter,
                    "URL Web Filter" to FirewallUrlFilter,
                    "Hardware MAC Filter" to FirewallMacFilter,
                    "Network Services Filter" to FirewallServicesFilter
                ),
                onItemClick = onNavigateToPage
            )

            // Category 6: USB Application
            AccordionCategory(
                title = "USB Applications",
                subtitle = "Samba share, FTP share, modems & printer",
                isExpanded = expandedStates["usb"] == true,
                onToggle = { expandedStates["usb"] = !(expandedStates["usb"] ?: false) },
                items = listOf(
                    "Common Setting" to UsbCommon,
                    "Samba Server Share" to UsbSamba,
                    "FTP Accounts Share" to UsbFtp,
                    "3G/4G USB Modem" to UsbModem,
                    "LPR Printer Share" to UsbPrinter
                ),
                onItemClick = onNavigateToPage
            )

            // Category 7: Administration
            AccordionCategory(
                title = "System Administration",
                subtitle = "Firmware, password, logs & op modes",
                isExpanded = expandedStates["admin"] == true,
                onToggle = { expandedStates["admin"] = !(expandedStates["admin"] ?: false) },
                items = listOf(
                    "System Settings" to AdminSystem,
                    "SSH / Telnet Services" to AdminServices,
                    "Operation Mode" to AdminOpMode,
                    "Firmware Upgrade" to AdminFirmware,
                    "Settings Backup & Flash" to AdminSettingsBackup,
                    "Shell Web Console" to AdminConsole,
                    "Hardware Buttons / LEDs" to AdminButtonsLed
                ),
                onItemClick = onNavigateToPage
            )

            // Category 8: Customization Scripts
            AccordionCategory(
                title = "Hacker Customizations",
                subtitle = "Startup script files and ping watchdog",
                isExpanded = expandedStates["custom"] == true,
                onToggle = { expandedStates["custom"] = !(expandedStates["custom"] ?: false) },
                items = listOf(
                    "Bash Startup Scripts" to CustomScripts,
                    "Internet Detector" to CustomDetector
                ),
                onItemClick = onNavigateToPage
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AccordionCategory(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    items: List<Pair<String, NavKey>>,
    onItemClick: (NavKey) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = ArcherTeal,
                    modifier = Modifier.rotate(rotation)
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(bottom = 8.dp)
                ) {
                    items.forEach { (name, route) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(route) }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ArcherTeal)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
