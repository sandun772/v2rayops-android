# V2RayOPS Android Client — Architecture & Technical Guide

Native commercial-grade Android VPN client for the **V2RayOPS** edge server management platform.

---

## 1. Architectural Highlights

- **Language & UI**: 100% Kotlin + Jetpack Compose (Material3 + Cyber Ops Design Tokens).
- **Architecture**: Clean Architecture + MVVM + Unidirectional Data Flow (UDF).
- **Asynchronous Operations**: Kotlin Coroutines + StateFlow / SharedFlow.
- **Dependency Injection**: Google Dagger Hilt.
- **Networking**: Retrofit 2 + OkHttp 4 + Moshi JSON Serialization.
- **Local Storage**: Jetpack DataStore (User Preferences) + Android Keystore EncryptedSharedPreferences (Secure Credentials).
- **VPN Engine (Phase 2 Completed)**: Native **Xray Core** integration via `AndroidLibXrayLite` (`libgojni.so` + `libv2ray.aar`).
- **TUN Tunneling**: Android `VpnService` Layer 3 TUN interface (`172.19.0.1/30`, capturing `0.0.0.0/0`) linked directly into Xray core loop.
- **Real-Time Telemetry**: Real outbound upload/download speed and session usage queried directly from Xray core via `queryAllOutboundTrafficStats()`.
- **Website OAuth & Deep Linking**: Web sign-in / registration via browser redirecting seamlessly back to `v2rayops://auth/callback?code=...`.
- **Brand Fidelity**: Official V2RayOPS vector and trimmed branding, cyber-tech dark theme, zero-emoji policy (Remix Icons).

---

## 2. Package & Module Structure

```
com.v2rayops.client
├── V2RayOpsApp.kt                       # Application entry point, Hilt initialization, Xray Core environment setup
├── MainActivity.kt                      # Single activity hosting Jetpack Compose Navigation & Deep Link receiver
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
│   │   ├── V2RayOpsClientApi.kt         # Matches /api/client/profile, /api/client/subscription, /api/client/servers
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
│   ├── AuthViewModel.kt                 # StateFlow-driven login & code verification state machine
│   └── ui/LoginScreen.kt                # Cyber login UI with Customer Code input and "Sign In via Website" OAuth button
├── ui/                                  # Presentation UI layer
│   ├── dashboard/                       # DashboardScreen, ConnectionButton, StatsCard, ActiveServerCard
│   ├── servers/                         # ServerSelectionScreen, ServerListItem
│   ├── splash/                          # SplashScreen & session routing
│   └── navigation/                      # AppNavGraph, Screen destinations & deep link router
├── vpn/                                 # VPN tunneling layer
│   ├── V2RayOpsVpnService.kt            # Android VpnService foreground service establishing Layer 3 TUN interface
│   ├── VpnManager.kt                    # VPN lifecycle state machine, real-time speed polling (1s interval)
│   └── VpnServiceController.kt          # Contextual VpnService intent dispatcher
├── xray/                                # Xray Core native engine integration (Phase 2)
│   ├── XrayEngine.kt                    # Clean contract for Xray Core lifecycle and metrics
│   ├── XrayEngineImpl.kt                # Production implementation wrapping Libv2ray and CoreController
│   └── XrayConfigGenerator.kt           # Generates compliant Xray JSON config with outbound stats & policy
└── network/                             # Network intelligence & diagnostics
    ├── NetworkMonitor.kt                # Reactive network connectivity StateFlow
    └── LatencyTester.kt                 # TCP socket latency & native Xray measureOutboundDelay
```

---

## 3. Website OAuth & Deep-Link Authentication Flow

1. User opens the V2RayOPS Android app and taps **"SIGN IN VIA WEBSITE"**.
2. The app launches the web browser pointing to:
   ```
   https://v2rayops.vpnxhub.com/sign-in?from=/auth/mobile-callback
   ```
3. Existing or new users authenticate via Google.
4. The backend automatically registers the user, provisions their trial/active customer code, and redirects them to:
   ```
   /auth/mobile-callback
   ```
5. The callback page automatically triggers the mobile deep link:
   ```
   v2rayops://auth/callback?code=V2R-XXXXXX
   ```
6. Android launches `MainActivity`, which extracts the customer code, populates `AuthViewModel`, validates credentials against the API, and smoothly enters the Dashboard.

---

## 4. Building the Application

### Prerequisites:
- Java JDK 17 (Temurin recommended)
- Android SDK (API 34 / Build Tools 34.0.0)

### Run Unit Tests:
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK:
```bash
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```
