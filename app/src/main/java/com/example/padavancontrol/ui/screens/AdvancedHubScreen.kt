package com.example.padavancontrol.ui.screens

import com.example.padavancontrol.data.t

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.padavancontrol.FirewallDnsIpsetFilter
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
import com.example.padavancontrol.AdvancedVpn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSidebarContent(
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
            "custom" to false,
            "vpn" to false
        )
    }

    ModalDrawerSheet(
        modifier = modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Sidebar Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text("📡", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = t("ADVANCED SETTINGS"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcherTeal
                    )
                    Text(
                        text = t("Router Configuration"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Category 1: Wireless 2.4GHz
            AccordionCategory(
                title = t("Wireless 2.4GHz"),
                subtitle = t("SSIDs, security, and radio limits"),
                isExpanded = expandedStates["wireless2g"] == true,
                onToggle = { expandedStates["wireless2g"] = !(expandedStates["wireless2g"] ?: false) },
                items = listOf(
                    t("General Settings") to Wifi2gGeneral,
                    t("Guest AP") to Wifi2gGuest,
                    t("Bridge (WDS)") to Wifi2gBridge,
                    t("Wireless MAC Filter") to Wifi2gMacFilter,
                    t("RADIUS Settings") to Wifi2gRadius,
                    t("Professional Parameters") to Wifi2gProfessional
                ),
                onItemClick = onNavigateToPage
            )

            // Category 2: Wireless 5GHz
            AccordionCategory(
                title = t("Wireless 5GHz"),
                subtitle = t("High-speed 5G channels and security"),
                isExpanded = expandedStates["wireless5g"] == true,
                onToggle = { expandedStates["wireless5g"] = !(expandedStates["wireless5g"] ?: false) },
                items = listOf(
                    t("General Settings") to Wifi5gGeneral,
                    t("Guest AP") to Wifi5gGuest,
                    t("Bridge (WDS)") to Wifi5gBridge,
                    t("Wireless MAC Filter") to Wifi5gMacFilter,
                    t("RADIUS Settings") to Wifi5gRadius,
                    t("Professional Parameters") to Wifi5gProfessional
                ),
                onItemClick = onNavigateToPage
            )

            // Category 3: Local Network (LAN)
            AccordionCategory(
                title = t("Local Network (LAN)"),
                subtitle = t("Subnet routing, DHCP lease and IPTV"),
                isExpanded = expandedStates["lan"] == true,
                onToggle = { expandedStates["lan"] = !(expandedStates["lan"] ?: false) },
                items = listOf(
                    t("LAN IP & Netmask") to LanIp,
                    t("DHCP Server Pool") to LanDhcp,
                    t("Static Routes") to LanRoute,
                    t("IPTV Setup") to LanIptv,
                    t("Ethernet Switch Config") to LanSwitch,
                    t("Wake-on-LAN client") to LanWol
                ),
                onItemClick = onNavigateToPage
            )

            // Category 4: Internet Gateway (WAN)
            AccordionCategory(
                title = t("Internet Gateway (WAN)"),
                subtitle = t("WAN, IPv6 settings, DMZ & DDNS"),
                isExpanded = expandedStates["wan"] == true,
                onToggle = { expandedStates["wan"] = !(expandedStates["wan"] ?: false) },
                items = listOf(
                    t("Internet Connection") to WanConnection,
                    t("IPv6 Protocol stack") to WanIpv6,
                    t("Port Forwarding (NAT)") to WanPortForward,
                    t("DMZ Host Settings") to WanDmz,
                    t("Dynamic DNS (DDNS)") to WanDdns
                ),
                onItemClick = onNavigateToPage
            )

            // Category 5: Security & Firewall
            AccordionCategory(
                title = t("Security & Firewall"),
                subtitle = t("Packet filters, MAC / URL blocklists"),
                isExpanded = expandedStates["firewall"] == true,
                onToggle = { expandedStates["firewall"] = !(expandedStates["firewall"] ?: false) },
                items = listOf(
                    t("General Security") to FirewallGeneral,
                    t("Netfilter SPI rules") to FirewallNetfilter,
                    t("URL Web Filter") to FirewallUrlFilter,
                    t("Hardware MAC Filter") to FirewallMacFilter,
                    t("Network Services Filter") to FirewallServicesFilter,
                    t("DNS ipset Blacklist") to FirewallDnsIpsetFilter
                ),
                onItemClick = onNavigateToPage
            )

            // Category 6: USB Applications
            AccordionCategory(
                title = t("USB Applications"),
                subtitle = t("Samba share, FTP share, modems & printer"),
                isExpanded = expandedStates["usb"] == true,
                onToggle = { expandedStates["usb"] = !(expandedStates["usb"] ?: false) },
                items = listOf(
                    t("Common Setting") to UsbCommon,
                    t("Samba Server Share") to UsbSamba,
                    t("FTP Accounts Share") to UsbFtp,
                    t("3G/4G USB Modem") to UsbModem,
                    t("LPR Printer Share") to UsbPrinter
                ),
                onItemClick = onNavigateToPage
            )

            // Category 7: System Administration
            AccordionCategory(
                title = t("System Administration"),
                subtitle = t("Firmware, password, logs & op modes"),
                isExpanded = expandedStates["admin"] == true,
                onToggle = { expandedStates["admin"] = !(expandedStates["admin"] ?: false) },
                items = listOf(
                    t("System Settings") to AdminSystem,
                    t("SSH / Telnet Services") to AdminServices,
                    t("Operation Mode") to AdminOpMode,
                    t("Firmware Upgrade") to AdminFirmware,
                    t("Settings Backup & Flash") to AdminSettingsBackup,
                    t("Shell Web Console") to AdminConsole,
                    t("Hardware Buttons / LEDs") to AdminButtonsLed
                ),
                onItemClick = onNavigateToPage
            )

            // Category 8: Hacker Customizations
            AccordionCategory(
                title = t("Hacker Customizations"),
                subtitle = t("Startup script files and ping watchdog"),
                isExpanded = expandedStates["custom"] == true,
                onToggle = { expandedStates["custom"] = !(expandedStates["custom"] ?: false) },
                items = listOf(
                    t("Bash Startup Scripts") to CustomScripts,
                    t("Internet Detector") to CustomDetector
                ),
                onItemClick = onNavigateToPage
            )

            // Category 9: Virtual Private Network (VPN)
            AccordionCategory(
                title = t("Virtual Private Network (VPN)"),
                subtitle = t("VPN client configurations"),
                isExpanded = expandedStates["vpn"] == true,
                onToggle = { expandedStates["vpn"] = !(expandedStates["vpn"] ?: false) },
                items = listOf(
                    t("VPN Client Settings") to AdvancedVpn
                ),
                onItemClick = onNavigateToPage
            )

            Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        .padding(bottom = 6.dp)
                ) {
                    items.forEach { (name, route) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(route) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(ArcherTeal)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = name,
                                fontSize = 12.sp,
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
