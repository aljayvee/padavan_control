package com.example.padavancontrol.data.models

data class VpnConfig(
    val enable: Boolean = false,
    val type: String = "0", // 0=PPTP, 1=L2TP, 2=OpenVPN
    val peer: String = "",
    val username: String = "",
    val password: String = "",
    val authType: String = "0", // PPTP/L2TP Auth Mode: 0=Auto, 1=MS-CHAPv2, 2=CHAP, 3=PAP
    val mppe: String = "0", // PPTP/L2TP Encryption: 0=Auto, 1=MPPE-128, 2=MPPE-40, 3=No encryption
    val mtu: Int = 1450,
    val mru: Int = 1450,
    val pppdOptions: String = "",
    val firewall: String = "1", // 1=Yes (all), 3=Yes (peer only), 0=No, 2=No (and disable NAT)
    val peerDns: String = "1", // 0=No, 1=Yes (add to top), 2=Yes (replace)
    val defaultGateway: Boolean = true,
    val remoteNetwork: String = "",
    val remoteMask: String = "",
    
    // OpenVPN specific
    val ovpnPort: Int = 1194,
    val ovpnProtocol: String = "0", // 0=UDP, 1=TCP, 2=UDP6, 3=TCP6
    val ovpnMode: String = "1", // 0=TAP, 1=TUN
    val ovpnAuthType: String = "1", // 0=TLS certs, 1=TLS user/pass
    val ovpnDigest: String = "1", // 0=none, 1=SHA1, 2=SHA224, 3=SHA256, 4=SHA384, 5=SHA512
    val ovpnCipher: String = "3", // 0=none, 1=DES, 2=3DES, 3=Blowfish, 4=AES128, etc.
    val ovpnLzo: String = "2", // 0=disable, 1=adaptive, 2=enabled, 3=LZO stub
    val ovpnHmacSign: String = "0", // 0=No, 1=Yes (TLS Auth)
    val ovpnRouteOptions: String = "0", // Redirect gateway options
    val ovpnCustomConfig: String = "",
    val caCert: String = "",
    val clientCert: String = "",
    val clientKey: String = "",
    val tlsAuthKey: String = "",

    // Post script
    val postScript: String = ""
)
