# V2RayOPS Android Client — Architecture & Technical Guide

Native commercial-grade Android VPN client for the **V2RayOPS** edge server management platform.

---

## 1. Architectural Highlights (Phase 1 — Foundation)

- **Language & UI**: 100% Kotlin + Jetpack Compose (Material3 + Cyber Ops Design Tokens).
- **Architecture**: Clean Architecture + MVVM + Unidirectional Data Flow (UDF).
- **Asynchronous Operations**: Kotlin Coroutines + StateFlow / SharedFlow.
- **Dependency Injection**: Google Dagger Hilt.
- **Networking**: Retrofit 2 + OkHttp 4 + Moshi JSON Serialization.
- **Local Storage**: Jetpack DataStore (User Preferences) + Android Keystore EncryptedSharedPreferences (Secure Credentials).
- **VPN Engine Abstraction**: Android `VpnService` lifecycle controller + Xray Core Engine abstraction.
- **Strict Anti-Mocking Rule**: Does **not** fake active tunnels or pretend Xray binaries are operational before Phase 2 compilation.

---

## 2. Package & Module Structure

```
com.v2rayops.client
├── V2RayOpsApp.kt                       # Application entry point, Hilt initialization, AutoConnect observer
├── MainActivity.kt                      # Single activity hosting Jetpack Compose Navigation
├── core/                                # Shared core utilities
│   ├── dispatchers/                     # Coroutine dispatchers provider (Main, IO, Default)
│   ├── error/                           # Domain AppError hierarchy
│   ├── logger/                          # Safe logging utility (strictly redacting credentials)
│   └── result/                          # AppResult<T> functional result wrappers
├── designsystem/                        # Cyber Ops design tokens & UI components
│   ├── components/                      # CyberButton, CyberCard, CyberTextField, StatusBadge, TopAppBar, SpeedIndicator
│   └── theme/                           # Color, Theme, Type, Shape (Cyber Ops Dark Palette)
├── data/                                # Data layer
│   ├── api/                             # Retrofit API interface & typed DTO models matching Next.js backend
│   │   ├── V2RayOpsClientApi.kt         # Matches /api/client/profile, /api/client/subscription, /api/client/servers, etc.
│   │   └── dto/                         # ProfileDto, SubscriptionDto, ServerDto, HeartbeatDto, SupportDto
│   ├── repository/                      # Repository implementations
│   │   ├── AuthRepositoryImpl.kt        # Authentication & Customer Code session logic
│   │   ├── ServerRepositoryImpl.kt      # Edge nodes fetching & live TCP latency measurement
│   │   ├── SubscriptionRepositoryImpl.kt# Authorized configs sync & telemetry heartbeat
│   │   └── SettingsRepositoryImpl.kt    # Reactive settings persistence
│   └── storage/                         # Local persistence
│       ├── SecureCredentialStore.kt     # Keystore-backed AES256 encrypted credential storage
│       └── UserPreferencesDataStore.kt  # Jetpack DataStore preferences
├── domain/                              # Domain business logic & contracts
│   ├── model/                           # UserProfile, ServerNode, VpnConfig, ConnectionState, UserSettings
│   ├── repository/                      # AuthRepository, ServerRepository, SubscriptionRepository, SettingsRepository
│   └── usecase/                         # LoginWithCodeUseCase, GetProfileUseCase, SyncServersUseCase, etc.
├── auth/                                # Authentication feature
│   ├── AuthState.kt                     # UI state for customer code binding
│   ├── AuthViewModel.kt                 # Login business logic and validation
│   └── ui/LoginScreen.kt                # Cyber Ops dark-themed authorization screen
├── vpn/                                 # VPN tunneling layer
│   ├── V2RayOpsVpnService.kt            # Android VpnService implementation & notification channel
│   ├── VpnServiceController.kt          # VpnService.prepare() & system intent dispatcher
│   └── VpnManager.kt                    # Connection state machine & traffic coordinator
├── xray/                                # Xray Core integration
│   ├── XrayEngine.kt                    # Engine abstraction contract & lifecycle states
│   ├── XrayEngineStub.kt                # Foundation stub reporting BinaryPending without faking tunnel
│   └── XrayConfigGenerator.kt           # Generates standard Xray JSON (VLESS Reality, inbounds, routing)
├── network/                             # Network intelligence
│   ├── NetworkMonitor.kt                # ConnectivityManager flow (Wi-Fi, Cellular, Ethernet)
│   └── LatencyTester.kt                 # Direct TCP socket ping measurement
├── dns/                                 # DNS diagnostics
│   └── DnsDiagnostics.kt                # Low-level DNS resolution & timing checks
├── profiles/                            # Authorized profile parsing & validation
│   ├── ProfileParser.kt                 # VLESS Reality, VMess, Trojan URI parser
│   └── AuthorizedProfileValidator.kt    # Enforces V2RayOPS server authorization, rejects rogue configs
├── autoconnect/                         # Smart auto-connect
│   └── AutoConnectManager.kt            # Reconnect on network recovery, offline pause
├── settings/                            # Application configuration
│   ├── SettingsViewModel.kt             # Settings mutation handlers
│   └── ui/SettingsScreen.kt             # Kill Switch, Bypass Sri Lanka routing, Auto-connect toggles
└── ui/                                  # Presentation layer
    ├── navigation/                      # Screen routes & AppNavGraph
    ├── splash/                          # SplashScreen & SplashViewModel
    ├── dashboard/                       # DashboardScreen, ConnectionButton, StatsCard, ActiveServerCard
    └── servers/                         # ServerSelectionScreen, ServerListItem
```

---

## 3. Backend Integration Matrix

The mobile client binds directly to the existing V2RayOPS backend without direct PostgreSQL connection:

| Website / API Path | Method | Mobile Feature |
| :--- | :--- | :--- |
| `https://v2rayops.vpnxhub.com/api/client/profile` | `GET` | Authenticates customer code, queries real-time PAYG balance (LKR), and active packages. |
| `https://v2rayops.vpnxhub.com/api/client/subscription` | `GET` | Synchronizes authorized VLESS Reality / Trojan / VMess node configurations. |
| `https://v2rayops.vpnxhub.com/api/client/servers` | `GET` | Retrieves active server fleet (`SG1`, etc.) for node switching & latency testing. |
| `https://v2rayops.vpnxhub.com/api/client/heartbeat` | `POST` | Reports background upload/download traffic telemetry and device metadata. |
| `https://v2rayops.vpnxhub.com/api/client/support` | `GET` | Fetches active support desks (Telegram, WhatsApp, 24/7 priority channels). |

---

## 4. How to Build & Run in Android Studio

1. **Prerequisites**:
   - JDK 17 (Eclipse Temurin 17 recommended).
   - Android Studio Hedgehog / Iguana / Jellyfish (or newer).
   - Android SDK 34 (API 34) installed.

2. **Open the Project**:
   - In Android Studio, select **Open** and point to the `android/` directory.

3. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be at `android/app/build/outputs/apk/debug/app-debug.apk`.

4. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 5. Completed vs. Unfinished Functionality (Phase 1 Status)

### ✅ Completed in Phase 1 (Foundation):
- [x] Complete Android project configuration with Gradle Version Catalog (`libs.versions.toml`).
- [x] Clean Architecture, Hilt DI, Retrofit/OkHttp, DataStore, and Android Keystore secure credentials.
- [x] Cyber Ops Dark Design System (colors, typography, shapes, and responsive components).
- [x] Full UI Screens:
  - Splash screen (with animated shield and authentication pre-check).
  - Login screen (Customer Code input, device authorization).
  - Main Dashboard (Tactile connection button, active server card, traffic speedometers, live LKR balance).
  - Server Selection screen (fleet listing with live TCP latency indicators).
  - Settings screen (Kill Switch, Bypass Sri Lanka routing, Smart Auto-Connect toggles, Unbind device).
- [x] Navigation Graph connecting all screens cleanly with Jetpack Compose Navigation.
- [x] Typed Backend API Client strictly conforming to actual Next.js backend endpoints.
- [x] VLESS Reality / Trojan / VMess URI parser.
- [x] Authorized Profile Validator (rejects unauthorized third-party configs).
- [x] Android `VpnService` implementation with foreground notification channel.
- [x] Xray Core JSON configuration generator (with inbound socks/http and routing rules).
- [x] Unit test suite covering URI parsing, authorization validation, use case execution, and config generation.

### ⏳ Scheduled for Subsequent Phases:
- [ ] **Phase 2**: Native compilation of `libxray.so` (Go mobile / gomobile bind) and linking into `XrayEngine`.
- [ ] **Phase 3**: Per-app proxy / split-tunnel package picker.
- [ ] **Phase 4**: QR Code camera scanner integration (ML Kit / CameraX) for 1-second customer code import.
- [ ] **Phase 5**: In-app PAYG top-up slip upload and instant balance refresh.
