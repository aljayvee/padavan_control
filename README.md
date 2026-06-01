# Padavan Control App - Technical Documentation and Security Overview

Welcome to the Padavan Control repository. This app is a native, modern Android companion application built in Jetpack Compose, designed to monitor and manage routers running the custom Padavan Firmware (specifically tested and optimized for the Newifi3 D2 router).

This document details the architecture, core codebase functionality, security precautions, tested hardware, and hardware-backed storage mechanisms built into the application to ensure secure, reliable administrative access.

---

## Table of Contents
1. Core Functionality
2. Tested Hardware and Kernel Versions
3. Codebase Architecture and Structure
4. Security Architecture and Precautions
   - Encrypted Credentials Storage
   - Safe Keystore Desync Recovery
   - Dynamic 401 Session Interceptor
   - TLS/HTTPS and Trust Verification
5. Settings Application and Persistence
6. Permissions and Best Practices

---

## Core Functionality

The Padavan Control app provides a mobile administration panel that interfaces directly with the router's web backend. Key features include:

* **Real-time System Monitoring**: Pulls CPU load, RAM usage, active connections, uptime, and network interface traffic statistics.
* **Client Control**: Scans connected clients, identifies current connections (wired vs. 2.4G/5G wireless), and allows blocking/unblocking specific clients by MAC address.
* **Advanced Networking Rules**: Configures Wireless settings (SSIDs, guest networks, transmission power, professional configurations), LAN configurations (DHCP servers, static routes, IPTV), and WAN configurations (PPPoE/DHCP connections, Port Forwarding, DDNS).
* **Security and Firewall**: Configures URL keywords filtering, MAC address filter lists, and service filters.
* **Developer Shell Console**: Executes shell commands and returns output dynamically from the router console.
* **Custom Scripts Manager**: View, edit, and apply startup scripts, post-WAN scripts, firewall rules, and shutdown scripts.

---

## Tested Hardware and Kernel Versions

The companion application has been compiled, tested, and verified against custom Padavan firmware builds deployed on the following hardware:

* **Tested Router Model**: Newifi3 D2 (Lenovo Newifi 3 D2 / NEWIFI3 / NEWIFI)
* **Supported Kernel Versions**:
  - **Linux Kernel 3.4 (Hanwckf baseline)**: Optimized with vb1980 KVR roaming patch (802.11k/v/r).
  - **Linux Kernel 4.4 (MeIsReallyBa baseline)**: Modern kernel version with WireGuard support and updated network stacks.

---

## Codebase Architecture and Structure

The codebase is built on Jetpack Compose for UI components, Retrofit2 with OkHttp for API requests, and Coroutines + Flow for asynchronous, non-blocking data operations.

```
PadavanControl/app/src/main/java/com/example/padavancontrol/
├── MainActivity.kt               # Main entrypoint, handles WorkManager scheduling and permissions
├── Navigation.kt                 # Navigation controller, routes between Login, Dashboard, and settings
├── NavigationKeys.kt             # Navigation keys and screen definitions
├── data/
│   ├── CredentialStore.kt        # Jetpack EncryptedSharedPreferences storage
│   ├── PadavanRepository.kt      # Main repository handling logic, caching, and background tasks
│   └── SettingsDataStore.kt      # System/App settings data store
├── network/
│   ├── BasicAuthInterceptor.kt   # Injects HTTP Basic Authentication headers dynamically
│   ├── PadavanApiService.kt      # Retrofit endpoint interfaces
│   ├── PadavanResponseParser.kt  # Custom HTML/Regex response parser
│   └── RetrofitClient.kt         # OkHttp client builder and network configurations
└── ui/
    ├── theme/                    # Material Design styles, typography, and dark/light color schemes
    ├── components/               # Shared UI elements (Breadcrumbs, forms, widgets)
    ├── screens/                  # Layouts (Dashboard, Settings, Login, and Advanced settings)
    └── viewmodels/               # State holders mapping business logic to UI screens
```

---

## Security Architecture and Precautions

Given that this application manages core home network infrastructure, security is integrated into every layer of the app.

### 1. Encrypted Credentials Storage (Jetpack Security)
To authenticate requests, the app must store the router's admin username and password. Plaintext storage is highly vulnerable to extraction attacks. 

Instead, the app uses Android Jetpack Security (`androidx.security.crypto.EncryptedSharedPreferences`) to protect user data:
* **Hardware-Backed Encryption**: The app uses `MasterKey.Builder` to build a 256-bit AES Master Key. This key is generated and stored securely inside the Android system Keystore, utilizing hardware-backed storage (such as StrongBox or Trusted Execution Environment) where supported by the device.
* **Double-Encryption Layer**: 
  - Pref Keys are encrypted using AES-256-SIV.
  - Pref Values (like passwords and usernames) are encrypted using AES-256-GCM.
* **Zero-Plaintext Leakage**: The data is never written in plaintext to the application sandbox XML settings files.

### 2. Safe Keystore Desync Recovery
Android Keystore can sometimes desynchronize or throw decryption errors (e.g., if a backup is restored on a different device or after system upgrades, triggering an `AEADBadTagException` or `GeneralSecurityException`). In many apps, this causes immediate startup crashes.

Padavan Control handles this gracefully:
```kotlin
private val sharedPreferences: SharedPreferences = try {
    createEncryptedSharedPreferences(context, masterKey)
} catch (e: Exception) {
    e.printStackTrace()
    try {
        // Clear corrupt files from filesystem safely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences("secure_padavan_prefs")
        } else {
            val sharedPrefsFile = java.io.File(context.filesDir.parent, "shared_prefs/secure_padavan_prefs.xml")
            if (sharedPrefsFile.exists()) { sharedPrefsFile.delete() }
        }
    } catch (delEx: Exception) {
        delEx.printStackTrace()
    }
    // Reinitialize a fresh secure preferences storage
    createEncryptedSharedPreferences(context, masterKey)
}
```
* **Resiliency**: Instead of crashing, the app detects key corruption, deletes the unreadable encrypted files, and falls back to a clean state, prompting the user to safely re-authenticate.

### 3. Dynamic 401 Session Interceptor
If admin credentials on the router are changed elsewhere, or credentials expire, the app prevents polling loops that could spam the router:
* **Interception**: BasicAuthInterceptor intercepts all outgoing requests and appends standard HTTP Basic Auth headers. It monitors response codes.
* **Auto-Logout Event**: If the router returns an HTTP 401 Unauthorized code, the interceptor fires an event via `RetrofitClient.unauthorizedEvents` (a thread-safe SharedFlow).
* **Credentials Cleared**: The main navigation stack listens to this flow, clears the stored credentials from CredentialStore, wipes current cache contexts, resets the backstack, and immediately redirects the user to the login screen.

### 4. TLS/HTTPS and Trust Verification
* **Dynamic Protocols**: The network engine determines if the user is connecting over secure `https://` or `http://` depending on router configurations.
* **Custom SSL Context**: Because routers usually deploy with self-signed SSL certificates, standard Android WebView and OkHttp stacks reject the connection by default. Padavan Control implements a custom `X509TrustManager` inside RetrofitClient allowing administrative command flows to bypass self-signed validation while keeping traffic encrypted.
* > [!WARNING]
  > Since self-signed certificates do not verify identity against a public root certificate authority, users are advised to connect over trusted Wi-Fi networks to avoid Man-in-the-Middle (MitM) attacks.

---

## Settings Application and Persistence

Applying settings in the Padavan web backend usually places them in running volatile memory. If the router reboots without a "Flash Commit", those settings are lost.

The app secures your settings by automatically bundling updates:
```kotlin
private suspend fun applyAndCommitSettings(fields: Map<String, String>): retrofit2.Response<String> {
    val response = getService().applySettings(fields)
    if (response.isSuccessful) {
        try {
            getService().commitFlash()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return response
}
```
* Every save action in the app calls `applyAndCommitSettings()`.
* Once configurations are updated successfully (`applySettings`), the app triggers a CommitFlash operation (`commit_nvram`) to write modifications directly to the router's physical flash storage. This guarantees changes survive reboots and power outages.

---

## Permissions and Best Practices

To run securely, the application requests minimum platform permissions:
1. **Network State (`ACCESS_NETWORK_STATE` / `INTERNET`)**: Needed to communicate with the router over local subnet and cellular VPNs.
2. **Post Notifications (`POST_NOTIFICATIONS`)**: On Android 13+ (Tiramisu), requested on startup to alert the user about background monitoring alerts (e.g., client disconnects or CPU overload thresholds).
3. **Background Worker Constraints**: The WorkManager service (`RouterMonitorWorker`) runs tasks only when the device is actively connected to a network, preventing unnecessary CPU and battery drain when offline.
