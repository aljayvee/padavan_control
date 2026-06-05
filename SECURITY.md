# Security Policy

Security is a core priority for the Padavan Control project. Because this application manages home router gateway credentials and network settings, we take all potential security issues seriously. 

This document details supported versions, how to report vulnerabilities, and security practices built into the application.

---

## Supported Versions

Only the latest release version on the main branch is actively supported with security updates. 

We recommend always running the latest version of the app to ensure your router credentials and connection configurations remain protected by the latest Android security patches.

| Version | Supported |
| --- | --- |
| 1.0.x | Active Support |
| < 1.0.0 | Unsupported |

---

## Reporting a Vulnerability

If you discover a security vulnerability, please do not disclose it publicly (such as via GitHub Issues or public pull requests). Doing so could expose users' home networks to exploitation before a fix is deployed.

Instead, please report security vulnerabilities by emailing the maintainer directly.

### Security Contact
Please send vulnerability details to: aljayveeversola@gmail.com

### What to Include in Your Report
To help us investigate and patch the issue quickly, please include:
* A detailed description of the vulnerability.
* Steps to reproduce the issue (proof-of-concept code or configuration details where applicable).
* The version of the app and the version of the Android OS on which the vulnerability was found.
* Any potential impact or vectors of exploit you have identified.

### Response Timeline
* **Acknowledgment**: You will receive an initial email acknowledgment of your report within 48 hours.
* **Investigation**: We will investigate the issue and determine the appropriate fix.
* **Fix & Release**: We aim to release a patch for critical security issues within 7 to 14 days of receipt.
* **Disclosure**: A coordinated public disclosure will be made after a fixed version has been published and users have had time to update.

---

## Application Security Architecture

The app includes several built-in security features designed to minimize the risk of credential leakage and unauthorized access:

### 1. Hardware-Backed Encryption
The application stores router administration credentials using Android's `EncryptedSharedPreferences`. 
* The system utilizes a 256-bit AES Master Key stored securely in the device's system Keystore (utilizing hardware-backed storage like a Trusted Execution Environment/TEE or StrongBox where available).
* Key aliases are encrypted with AES-256-SIV and values are encrypted with AES-256-GCM.
* Credential data is never saved in plaintext to disk.

### 2. Auto-Logout Interceptor
The OkHttp connection client utilizes a basic authentication interceptor. If the router responds to any query with an HTTP `401 Unauthorized` status code (indicating credentials have changed or expired), the app immediately:
* Clears all credentials from active memory and encrypted storage.
* Resets the navigation backstack.
* Redirects the user to the login screen, halting all background repository polling processes to prevent brute-forcing or leaking outdated credentials.

### 3. Self-Signed SSL Contexts
Because custom Padavan firmware routers deploy with self-signed SSL/TLS certificates by default, the app configures a custom socket trust manager to permit encrypted HTTPS communication. 
* Traffic sent over HTTPS remains encrypted to prevent packet sniffing.
* Users are strongly advised to only access their routers over trusted local subnets or secure VPNs to mitigate potential Man-in-the-Middle (MitM) attacks.
