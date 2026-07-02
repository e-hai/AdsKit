# AdsKit – AGENTS.md

This file is for AI agents (Codex, Copilot, etc.) to quickly understand the project structure, conventions, and how to make safe edits.

## What This Project Is

An Android ad mediation SDK module (`:ads`) plus a sample app (`:app`). The SDK wraps AdMob and AppLovin SDKs under a single `AdsManager` facade so that the host app only imports one dependency and uses one API.

**Key design decision**: single-adapter mode – `AdsManager` initializes exactly one `AdsProviderAdapter` at a time. Switching providers requires reinitializing via `AdsManager.initialize()`, which destroys the old adapter first.

## Project Layout

```
AdsKit/
├── ads/                     # SDK library module (namespace: com.kit.ads)
│   ├── build.gradle
│   └── src/main/java/com/kit/ads/
│       ├── AdsManager.kt              # [Entry Point] Singleton facade with init state machine
│       ├── AdsRequest.kt              # Ad request data class (triggerId, adUnitId, adType, providerType)
│       ├── AdsLoadHandler.kt          # Coordinates load+callback flow, dispatches to main thread
│       ├── AdsEntity.kt               # Loaded ad wrapper with show() and destroy()
│       ├── AdsListener.kt             # Callback interface + AdCallback base class
│       ├── AdsType.kt                 # BANNER / SPLASH / REWARDED enum
│       ├── AdsLogger.kt               # Log utility wrapping android.util.Log, runtime toggle
│       ├── provider/
│       │   ├── AdsProviderAdapter.kt  # [Interface] All adapters implement this
│       │   ├── AdsProviderAdapterFactory.kt  # Factory (internal)
│       │   ├── AdsProviderConfig.kt   # Config data class (providerType, apiKey)
│       │   ├── AdsProviderType.kt     # ADMOB / APPLOVIN enum
│       │   ├── AdsProviderListener.kt # Internal callback interface (includes onAdFailedToShow)
│       │   ├── admob/AdmobProviderAdapter.kt  # AdMob implementation
│       │   └── applovin/ApplovinProviderAdapter.kt  # AppLovin implementation
│       └── ump/UMP.kt               # Google UMP consent flow
├── app/                      # Sample app (namespace: com.kit.ads.sample)
│   ├── build.gradle
│   └── src/main/java/com/kit/ads/sample/
│       ├── App.kt            # Application class showing initialization
│       └── MainActivity.kt   # Demo UI for BANNER/SPLASH/REWARDED loading
├── build.gradle              # Root build file
├── settings.gradle           # Includes :app and :ads
├── README.md
├── AGENTS.md                 # This file
└── gradlew                   # Automatically sets GRADLE_USER_HOME for sandbox safety
```

## Core Data Flow

- **Initialization**: `AdsManager.initialize(context, AdsProviderConfig, onResult?)` → creates provider adapter by type via `AdsProviderAdapterFactory`, drives state machine (IDLE → INITIALIZING → READY/FAILED). Stale-callback guard via initToken.
- **Load**: `AdsManager.loadAd(context, AdsRequest, AdsListener)` → checks init state & provider type match, creates `AdsLoadHandler`, loads via provider adapter. Public callbacks dispatched to main thread.
- **Preload**: `AdsManager.preloadAd(context, AdsRequest)` → loads and caches the ad by `AdsRequest.triggerId`. Subsequent `loadAd()` with the same `triggerId` returns the cached ad immediately without network request.
- **Show**: `AdsEntity.show(activity, container)` → delegates to `AdsProviderAdapter.showAd()`. Full-screen types (REWARDED/SPLASH) ignore container semantics.
- **Cleanup**: `AdsEntity.destroy()` → calls `AdsProviderAdapter.destroyAd(ad)`. `AdsManager.destroy()` → calls `AdsProviderAdapter.destroy()` and resets state to IDLE.
- **Callbacks**: Provider callbacks mapped in `AdsLoadHandler` into `AdsListener` callbacks. All public callbacks are posted to the main thread.

## Show API Semantics

- `AdsEntity.show(activity, container)` is intentionally unified for all ad types.
- `container` is **mandatory to pass**, and is **meaningful only for BANNER** ads.
- For `AdsType.REWARDED` and `AdsType.SPLASH`, the concrete adapter currently ignores container semantics; passing a valid `ViewGroup` is sufficient to satisfy API shape.

## Init State Machine & Guards

- `AdsManager` maintains an internal state machine: `IDLE → INITIALIZING → READY | FAILED`.
- `initialize()` calls are gated: concurrent initialize is ignored; repeated initialize to the same provider is skipped; switching providers destroys the old adapter first.
- `loadAd()` validates: if state is not READY, fails fast with `STATE_INITIALIZING` / `STATE_IDLE` / `STATE_FAILED` error code. If `AdsRequest.providerType` mismatches the initialized provider, fails fast with `PROVIDER_MISMATCH`.
- `preloadAd()` passes through the same validation as `loadAd()`. A preloaded ad survives until consumed by `loadAd()` or cleared by `destroy()`.
- All immediate failure paths are dispatched to the main thread.

## Callback Details

### AdsListener (public, main-thread)

| Method | Description |
|--------|-------------|
| `onAdLoaded(ad: AdsEntity)` | Ad loaded successfully |
| `onAdFailedToLoad(error: String)` | Legacy: error description only |
| `onAdFailedToLoad(error: String, errorCode: String?)` | Enhanced: includes error code for diagnosis |
| `onAdStartedToLoad()` | Loading began (default no-op) |
| `onAdShown()` | Ad displayed |
| `onAdClicked()` | Ad clicked |
| `onAdClosed()` | Ad dismissed |
| `onAdPaidEvent()` | Revenue event reported |
| `onAdUserEarnedReward()` | User earned reward (REWARDED) |

`AdCallback` is an abstract class with empty default implementations for all `AdsListener` methods — convenient for overriding only the methods you need.

### AdsProviderListener (internal, may be off main thread)

Same shape as `AdsListener` plus `onAdFailedToShow(ad, error, errorCode?)` for show-phase failures (default no-op). Error codes for AppLovin display failures carry a `DISPLAY_` prefix.

## Standardized Error Codes

| Code | Meaning |
|------|---------|
| `STATE_INITIALIZING` | `loadAd()` / `preloadAd()` called while `AdsManager` is still initializing |
| `STATE_IDLE` | called before any `initialize()` call |
| `STATE_FAILED` | called after initialization failed |
| `PROVIDER_MISMATCH` | `AdsRequest.providerType` != initialized provider type |
| `DISPLAY_*` | AppLovin show-phase failures (e.g. `DISPLAY_AD_ALREADY_SHOWN`) |
| SDK-specific | Raw error codes forwarded from AdMob / AppLovin |

## Logging Convention

All SDK internal logs use the `AdsKit-{ClassName}` tag pattern via `AdsLogger`:

| TAG | Source |
|-----|--------|
| `AdsKit` | `AdsManager.kt` |
| `AdsKit-AdsLoadHandler` | `AdsLoadHandler.kt` |
| `AdsKit-AdsEntity` | `AdsEntity.kt` |
| `AdsKit-Admob` | `AdmobProviderAdapter.kt` |
| `AdsKit-AppLovin` | `ApplovinProviderAdapter.kt` |
| `AdsKit-UMP` | `UMP.kt` |

In Logcat:
- Filter `AdsKit` to see **all** SDK logs (substring match covers all `AdsKit-*` tags).
- Filter `AdsKit-Admob` to isolate only AdMob adapter logs.

Log messages use a compact key=value format: `onAdLoaded id=... unit=... type=... provider=...`.

## Important Notes for Agents

1. Do not add non-requested API-level behavior changes to `AdsListener` or `AdsEntity.show` semantics.
2. When changing failure behavior, keep backward compatibility by preserving existing method signatures (e.g. the `errorCode` overload defaults to the legacy single-param version).
3. Avoid editing generated outputs under `*/build/`.
4. Existing sample app (`app/`) is a minimal demo; host integration should be validated with real devices for ad behavior.

## Implementation Caveats

- `AdsManager.initialize(...)` is asynchronous and callback-driven.
- `providerType` mismatch causes immediate callback failure before loading.
- All `AdsListener` callbacks from `AdsLoadHandler` are dispatched to the main thread.

## Environment Constraints

### Gradle UDP Socket Requirement

Gradle 9.x requires UDP socket binding at startup for cross-process file lock negotiation (`DefaultFileLockCommunicator` → `new DatagramSocket(0, ...)`). In sandboxed environments that prohibit network socket operations, `./gradlew` will fail with:

```
Could not create service of type FileLockContentionHandler ...
Caused by: java.net.SocketException: Operation not permitted
  at java.base/sun.nio.ch.Net.bind0(Native Method)
```

This is a Gradle framework bootstrap issue, not a project code issue.

**Diagnosis**: Run `python3 -c "import socket; s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.bind(('127.0.0.1', 0)); print('UDP socket OK')"` — if it throws `PermissionError`, Gradle won't work without escalated permissions.

**Workaround**: Use escalated/sandbox-exempt permissions for `./gradlew` invocations, or run in a local macOS / CI environment where socket binding is allowed.

### GRADLE_USER_HOME

The `gradlew` script automatically sets `GRADLE_USER_HOME` to `/private/tmp/gradle` when unset, avoiding `.lck` file permission errors in sandboxed environments. No manual setup needed.
