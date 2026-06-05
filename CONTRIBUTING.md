# Contributing to Padavan Control

Thank you for your interest in contributing to Padavan Control! This project is a native Android companion application designed to monitor and manage Padavan-based routers (such as the Newifi3 D2) securely and efficiently.

This guide outlines our development workflow, coding standards, security practices, and testing requirements to help you get started.

---

## Table of Contents
1. Development Environment Setup
2. Code Style and Architecture
3. Security Best Practices
4. Router Settings and NVRAM Persistence
5. Testing Requirements
6. Submitting Changes

---

## Development Environment Setup

To get started with development, follow these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/aljayvee/padavan_control.git
   ```
2. Open the project in Android Studio (Jellyfish or newer recommended).
3. Ensure you have JDK 17 installed and configured as your Gradle JDK in:
   * Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK -> JDK 17
4. Enable Developer Options and USB Debugging on your Android test device (Android 8.0 / API 26 or newer).
5. Build the project locally:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Code Style and Architecture

This project is built using modern Android standards:
* **UI Framework**: Jetpack Compose
* **State Management**: MVVM (Model-View-ViewModel) utilizing Kotlin Flows and Coroutines
* **Network Stack**: Retrofit2 with OkHttp3 client interceptors
* **Dependency Injection**: Simple constructor-based instantiations managed within the main Navigation container.

### Coding Guidelines
* **Kotlin**: Follow the official Kotlin coding conventions.
* **Jetpack Compose**:
  * Use stable, immutable data models for Compose states.
  * Keep Composable functions focused, reusable, and side-effect free.
  * Always use `LaunchedEffect` or `remember` to handle state transitions and async initialization safely without blocking the main UI thread.
* **Threading**: Heavy disk operations (e.g., Keystore/Preferences access) and network requests must be explicitly dispatched on `Dispatchers.IO` using Kotlin Coroutines. Never block the main thread.

---

## Security Best Practices

Because this application manages gateway and firewall infrastructure, security is our highest priority. Contributors must adhere to the following rules:

### 1. Hardcoding Credentials
* **Never** hardcode IP addresses, administrative usernames, passwords, or tokens in source code, configuration files, or unit tests.
* Utilize the mock utilities in test classes to write functional tests without exposing production router details.

### 2. Storing Secrets
* All administrative credentials must be stored using `EncryptedSharedPreferences` via the `CredentialStore` wrapper class.
* The preferences storage is backed by the Android Keystore system with hardware-backed AES-256 encryption.
* Handle keystore desynchronizations (`AEADBadTagException`) gracefully to clear the corrupt local file instead of causing an application crash.

### 3. Dynamic Authentication and Interceptors
* Outgoing requests must utilize OkHttp interceptors (such as `BasicAuthInterceptor`) to inject HTTP Basic Authentication headers dynamically.
* If a request fails with an HTTP `401 Unauthorized` response code, it must be intercepted, and the application must trigger an auto-logout flow to clear active memory caches and redirect the user safely back to the Login screen.

---

## Router Settings and NVRAM Persistence

When configuring settings on Padavan-based routers, keep in mind how the router's memory works:
* Standard POST requests to `apply.cgi` apply settings to the running volatile configuration. If the router reboots, these changes are lost.
* To make changes permanent, you must commit them to the physical flash memory.
* When adding or modifying repository configuration methods in `PadavanRepository`, always route requests through the `applyAndCommitSettings()` helper method. This method automatically executes a `commit_nvram` action (`CommitFlash`) immediately after updating settings, ensuring settings persist across router reboots.

---

## Testing Requirements

All modifications to the parser, repository, or viewmodels must be verified by tests:
* **Unit Tests**: Place in the `app/src/test/` directory. Use mock HTTP responses to verify HTML parser outputs in `PadavanResponseParserTest`.
* **Instrumented Tests**: Place in the `app/src/androidTest/` directory. Use Espresso and Mockito Android dependencies.
* Before submitting a pull request, ensure all tests compile and pass successfully:
  ```bash
  ./gradlew testDebugUnitTest
  ```

---

## Submitting Changes

1. Create a descriptive feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Implement your changes, ensuring code matches style standards and contains no debugging logs or temporary print statements.
3. Verify that your code compiles locally.
4. Ensure the CodeQL security scanning action passes. The workflow is configured to use manual compilation:
   ```bash
   ./gradlew compileDebugKotlin compileDebugJavaWithJavac --no-daemon
   ```
5. Commit your changes with clear, structured commit messages:
   ```bash
   git commit -m "Brief description of the fix or feature"
   ```
6. Push your branch and open a Pull Request against the `main` branch. Provide a summary of the changes and references to any issues resolved.
