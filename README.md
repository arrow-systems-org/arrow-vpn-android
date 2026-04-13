# Arrow VPN (Android)

Arrow VPN is an Android app built with Kotlin + Jetpack Compose.  
It includes login/session handling, server selection with latency checks, VPN/proxy mode UI, app settings (auto-connect, kill switch), and a foreground VPN service scaffold ready for backend/runtime integration.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **State management:** `ViewModel` + `StateFlow`/`SharedFlow`
- **Async:** Kotlin Coroutines
- **Persistence:** SharedPreferences (`AppPreferences`)
- **Networking:** `HttpURLConnection` (custom API client)
- **Build:** Gradle Kotlin DSL

## Requirements

- Android Studio (latest stable recommended)
- JDK 11
- Android SDK configured

App module settings:
- `minSdk = 24`
- `targetSdk = 36`
- `compileSdk = 36`

## Quick Start

1. Open project in Android Studio.
2. Sync Gradle.
3. Select a device/emulator.
4. Run the `app` configuration.

## Useful CLI Commands

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/org/arrowx/vpn/
│   ├── MainActivity.kt                  # App entry point; binds view models and effects
│   ├── ArrowVpnService.kt               # Foreground VPN service (runtime scaffold)
│   ├── data/
│   │   ├── local/AppPreferences.kt      # Credentials/settings/server cache persistence
│   │   ├── remote/ArrowApiClient.kt     # Backend HTTP requests + response parsing
│   │   └── repository/
│   │       ├── ArrowRepository.kt       # Data contract used by view models
│   │       └── ArrowRepositoryImpl.kt   # Startup/login/cache/ping orchestration
│   ├── domain/model/                    # Core app models and helpers
│   ├── presentation/
│   │   ├── splash/                      # Splash loading flow
│   │   ├── main/                        # Main screen state + behavior
│   │   └── components/                  # Reusable Compose components
│   └── ui/theme/                        # Material theme and color scheme
└── res/
    ├── values/strings.xml               # Centralized user-facing strings
    └── values/colors.xml                # Centralized color resources
```

## Architecture Overview

### UI Flow

1. `MainActivity` launches Compose content.
2. `SplashViewModel` preloads startup data (cache-first, online sync when possible).
3. `MainViewModel` receives startup data and drives the main UI.
4. UI emits events to `MainViewModel`.
5. `MainViewModel` emits side effects (`MainUiEffect`) consumed by `MainActivity` for:
   - VPN permission requests
   - start/stop service calls
   - clipboard and toasts

### Data Flow

`Presentation` → `ArrowRepository` → (`ArrowApiClient` + `AppPreferences`)

- `ArrowApiClient`: login + latency checks
- `AppPreferences`: stores credentials, server list, selected server, and settings
- `ArrowRepositoryImpl`: merges cache and network strategy for startup/login behavior

## Current Backend Contract (Implemented)

### Login request

`POST {BASE_URL}/api/login`

Request body:
- `uuid: String`
- `password: String`

### Expected login response fields

- `valido: Boolean`
- `msg: String`
- `servidores: Object`
  - each key = server id
  - each value should include at least:
    - `vless: String`
    - optional metadata used by UI:
      - `nombre`
      - `codigo_pais` / `country_code` / `pais` / `bandera`

Mapped domain model:
- `ServerNode(id, name, countryCode, vlessConfig)`

## Feature Notes

- **Kill switch behavior (UI logic):**
  - blocks manual disconnect while enabled
  - attempts reconnection when VPN stops unexpectedly
- **Auto-connect behavior:**
  - can trigger startup connection attempts when eligible
- **Server latency:**
  - measured via socket connect timing to host/port in VLESS URI
- **Flags:**
  - uses country-code-to-emoji logic (`CountryFlag.kt`), not remote images