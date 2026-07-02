# AdsKit – AGENTS.md

This file is for AI agents (Codex, Copilot, etc.) to quickly understand the project structure, conventions, and how to make safe edits.

## What This Project Is

An Android ad mediation SDK module (`:ads`) plus a sample app (`:app`). The SDK wraps AdMob and AppLovin SDKs under a single `AdsManager` facade so that the host app only imports one dependency and uses one API.

**Key design decision**: single-adapter mode – `AdsManager` initializes exactly one `AdProviderAdapter` at a time. Switching providers requires a new `initialize()` call.

## Project Layout

```
AdsKit/
├── ads/                     # SDK library module (namespace: com.kit.ads)
│   ├── build.gradle
│   └── src/main/java/com/kit/ads/
│       ├── AdsManager.kt              # [Entry Point] Singleton facade
│       ├── AdsRequest.kt            # Ad placement data class
│       ├── AdsLoadHandler.kt    # Coordinates load+callback flow
│       ├── AdsEntity.kt               # Loaded ad wrapper with show()
│       ├── AdsListener.kt             # Callback interface + AdCallback base class
│       ├── AdsType.kt                 # BANNER / SPLASH / REWARDED enum
│       ├── event/
│       │   ├── AdEventObserver.kt    # Observer interface
│       │   ├── AdEventObserverManager.kt  # Observer registry + notify
│       │   └── AdEventType.kt        # Sealed event hierarchy
│       ├── provider/
│       │   ├── AdsProviderAdapter.kt  # [Interface] All adapters implement this
│       │   ├── AdsProviderAdapterFactory.kt  # Factory (internal)
│       │   ├── AdsProviderConfig.kt   # Config data class
│       │   ├── AdsProviderType.kt     # ADMOB / APPLOVIN enum
│       │   ├── AdsProviderListener.kt   # Internal callback interface
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
└── AGENTS.md                 # This file
```

## Core Data Flow

- Initialization: `AdsManager.initialize(context, AdProviderConfig)` creates provider adapter by type.
- Load: `AdsManager.loadAd(context, placement, listener)` creates `AdsLoadHandler` and loads via provider.
- Show: `AdsEntity.show(activity, container)` delegates to `AdProviderAdapter.showAd()`.
- Events: each provider callback is mapped in `AdsLoadHandler` into both public `AdsListener` and `AdEventObserverManager` events.

## Show API Semantics

- `AdsEntity.show(activity, container)` is intentionally unified for all ad types.
- `container` is **mandatory to pass**, and is **meaningful only for BANNER** ads.
- For `AdsType.REWARDED` and `AdsType.SPLASH`, the concrete adapter currently ignores container semantics; passing a valid `ViewGroup` is sufficient to satisfy API shape.

## Current Edits (In-Repo)

### 已完成
- Added resource cleanup API:
  - `AdProviderAdapter.destroyAd(ad: Any)` (default no-op)
  - `AdProviderAdapter.destroy()` (default no-op)
- Added `AdsEntity.destroy()` and `AdsManager.destroy()`.
- Added `AdsManager` initialization state machine (`IDLE/INITIALIZING/READY/FAILED`), reinitialize gating, and stale-callback guard.
- Added provider-type guard in `AdsManager.loadAd(...)`:
  - if not initialized, fail fast with clear error
  - if `placement.providerType` mismatches initialized provider, fail fast with mismatch error
- Added main-thread dispatch on public callbacks:
  - `AdsListener` / `AdEventObserver` callbacks from `AdsLoadHandler` are posted to main thread
  - immediate `AdsManager.loadAd(...)` failure paths are also posted to main thread
- Improved observer handling:
  - `AdEventObserverManager` uses thread-safe list, deduplicates register calls, adds `clear()`
  - `AdsManager.destroy()` clears observer list
- Extended `LoadAdFailure` event to carry optional `errorMessage`, `errorCode`, `providerType`.
- Fixed `onAdStartedToLoad()` missing in:
  - AdMob `loadRewarded()`, `loadSplash()`
  - AppLovin `loadBanner()`, `loadRewarded()`, `loadSplash()`
- Fixed `AdsManager.openDebug()` TOCTOU race (now guarded by `stateLock`).
- Added `AdsManager.setAdapterForTesting()` + `getInitState()` + `getInitializedProviderType()` (`@VisibleForTesting` internal helpers).
- Added `AdsManagerTest.kt` covering state transitions, provider type tracking, and destroy lifecycle.
- Updated `jvmTarget` from 1.8 to 17 in both `:ads` and `:app` modules.
- Replaced sample app fake ad unit IDs (`admob_unit_id_666`) with Google test ad unit IDs.
- Cleaned up commented-out duplicate Maven repos in `settings.gradle`.
- Enhanced error information reporting:
  - `AdsListener.onAdFailedToLoad(error, errorCode?)` — 新增 2 参数重载（含 errorCode），默认委派保持向后兼容
  - `ProviderListener.onAdFailedToShow(ad, error, errorCode?)` — 新增展示失败回调（默认空实现）
  - `AdsLoadHandler` — 将 errorCode 透传给 `AdsListener`
  - `AdsManager` 内部失败路径标准化 errorCode：`STATE_INITIALIZING` / `STATE_IDLE` / `PROVIDER_MISMATCH`
  - AppLovin 展示失败（onAdDisplayFailed）errorCode 加 `DISPLAY_` 前缀
  - AdMob 全屏展示失败（onAdFailedToShowFullScreenContent）通过 listener 上报
  - Sample app 展示加载失败时的 errorCode 日志

### Unchanged (per request)
- Kept `AdsEntity.show(activity, container)` API unchanged to avoid breaking encapsulation.

## Important Notes for Agents

1. Do not add non-requested API-level behavior changes to `AdsListener`/`AdsEntity.show` semantics.
2. When changing failure behavior, keep backward compatibility by preserving existing method signatures.
3. Avoid editing generated outputs under `*/build/`.
4. Existing sample app (`app/`) is a minimal demo; host integration should be validated with real devices for ad behavior.

## Implementation Caveats

- `AdsManager.initialize(...)` is asynchronous and callback-driven.
- `providerType` mismatch now causes immediate callback failure before loading.
- Thread-safety was improved for observer list; this should be preferred if adding event observer logic elsewhere.

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
