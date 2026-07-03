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
│       ├── AdsRequest.kt              # Ad request data class
│       ├── AdsLoadHandler.kt          # Coordinates load+callback flow, dispatches to main thread
│       ├── AdsEntity.kt               # Loaded ad wrapper with show() and destroy()
│       ├── AdsListener.kt             # Callback interface + AdCallback base class
│       ├── AdsType.kt                 # BANNER / SPLASH / REWARDED enum
│       ├── AdsLogger.kt               # Log utility wrapping android.util.Log, runtime toggle
│       ├── provider/
│       │   ├── AdsProviderAdapter.kt  # [Interface] All adapters implement this
│       │   ├── AdsProviderAdapterFactory.kt  # Factory (internal)
│       │   ├── AdsProviderConfig.kt   # Config data class
│       │   ├── AdsProviderType.kt     # ADMOB / APPLOVIN enum
│       │   ├── AdsProviderListener.kt # Internal callback interface
│       │   ├── admob/AdmobProviderAdapter.kt  # AdMob implementation
│       │   └── applovin/ApplovinProviderAdapter.kt  # AppLovin implementation
│       └── ump/UMP.kt               # Google UMP consent flow
├── app/                      # Sample app (namespace: com.kit.ads.sample)
│   ├── build.gradle
│   └── src/main/java/com/kit/ads/sample/
│       ├── App.kt            # Application class showing initialization
│       └── MainActivity.kt   # Demo UI in Jetpack Compose
├── build.gradle              # Root build file
├── settings.gradle           # Includes :app and :ads
├── README.md
├── AGENTS.md                 # This file
└── gradlew                   # Automatically sets GRADLE_USER_HOME for sandbox safety
```

## API Reference

All public types are under `com.kit.ads`. Import paths shown inline.

### 1. Module Dependency

The host app depends on the `:ads` module. Typical setup:

```kotlin
// settings.gradle
include ':ads'
project(':ads').projectDir = file('ads')

// app/build.gradle
dependencies {
    implementation project(':ads')
}
```

No extra Gradle plugin or manifest configuration is required — the SDK bundles everything internally.

### 2. AdsProviderConfig — Provider Configuration

> **package**: `com.kit.ads.provider`

```kotlin
data class AdsProviderConfig(
    val providerType: AdsProviderType,
    val apiKey: String             // AdMob: App ID  (e.g. "ca-app-pub-3940256099942544~3347511713")
                                   // AppLovin: SDK key from MAX dashboard
)

enum class AdsProviderType { ADMOB, APPLOVIN }
```

### 3. AdsRequest — Ad Placement Request

> **package**: `com.kit.ads`

```kotlin
data class AdsRequest(
    val triggerId: String,          // Developer-defined scene identifier, e.g. "home_banner"
    val adUnitId: String,           // Ad unit ID from the ad platform
    val adType: AdsType,            // BANNER / SPLASH / REWARDED
    val providerType: AdsProviderType   // Must match the initialized provider
)
```

`triggerId` doubles as the preload cache key. Use unique values per scene.

### 4. AdsManager.initialize() — SDK Initialization

```kotlin
package com.kit.ads

fun AdsManager.initialize(
    context: Application,
    config: AdsProviderConfig,
    onResult: ((success: Boolean) -> Unit)? = null   // Called when init completes
)
```

- **Single-adapter mode**: one provider at a time.
- Reinitialising with the same provider is silently skipped.
- Switching providers destroys the previous adapter before creating a new one.
- Concurrent calls are ignored.
- All `loadAd()` / `preloadAd()` calls before `onResult(true)` fail with `STATE_INITIALIZING`.

```kotlin
// Application.onCreate()
val config = AdsProviderConfig(AdsProviderType.ADMOB, "ca-app-pub-3940256099942544~3347511713")
AdsManager.initialize(this, config) { success ->
    Log.d("App", "Ads init: $success")
}
```

### 5. AdsManager.loadAd() — Load & Show

```kotlin
fun AdsManager.loadAd(
    context: Context,
    request: AdsRequest,
    adListener: AdsListener       // Callbacks dispatched on main thread
)
```

Preconditions verified before loading:
- State must be `READY`, otherwise fails with `STATE_INITIALIZING` / `STATE_IDLE` / `STATE_FAILED`.
- `request.providerType` must match the initialized provider, otherwise `PROVIDER_MISMATCH`.
- If a preloaded ad exists for `request.triggerId`, it is returned immediately via `onAdLoaded()` without a network request.

```kotlin
val request = AdsRequest(
    "home_banner",
    "ca-app-pub-3940256099942544/9214589741",
    AdsType.BANNER,
    AdsProviderType.ADMOB
)

AdsManager.loadAd(this, request, object : AdCallback() {
    override fun onAdLoaded(ad: AdsEntity) {
        ad.show(this@MainActivity, bannerContainer)
    }
    override fun onAdFailedToLoad(error: String, errorCode: String?) {
        Log.e("Ad", "failed: $error ($errorCode)")
    }
})
```

### 6. AdsManager.preloadAd() — Preload (Fire & Forget)

```kotlin
fun AdsManager.preloadAd(
    context: Context,
    request: AdsRequest
)
```

- Loads the ad silently and caches it by `request.triggerId`.
- Does **not** invoke any public callbacks. Log output is visible in Logcat under tag `AdsKit`.
- A subsequent `loadAd()` with the same `triggerId` returns the cached ad immediately.
- Cache is single-use — consumed on the first `loadAd()` call, or cleared on `AdsManager.destroy()`.
- Same state / provider validation as `loadAd()`; failures are silently logged.

```kotlin
// Preload early, e.g. in Application.onCreate()
val request = AdsRequest("home_banner", "ca-app-pub-xxx/9214589741", AdsType.BANNER, AdsProviderType.ADMOB)
AdsManager.preloadAd(this, request)

// Later, in Activity:
AdsManager.loadAd(this, request, object : AdCallback() {
    override fun onAdLoaded(ad: AdsEntity) {
        // Returns cached ad if preloaded, otherwise fresh network load
        ad.show(this@MainActivity, container)
    }
    override fun onAdFailedToLoad(error: String, errorCode: String?) {
        Log.e("Ad", "failed: $error ($errorCode)")
    }
})
```

### 7. AdsEntity.show() / destroy() — Ad Display & Cleanup

```kotlin
class AdsEntity(...) {
    fun show(activity: Activity, container: ViewGroup)
    fun destroy()
}
```

- `show()` is unified for all ad types.
- `container` is **mandatory** and **meaningful only for BANNER** — it attaches the banner view.
- For `REWARDED` / `SPLASH`: the adapter ignores `container` semantics; any valid `ViewGroup` works.
- `destroy()` must be called for BANNER ads (releases `WebView` resources). Best called in `onDestroy()`.

### 8. AdsManager.destroy() — Full Teardown

```kotlin
fun AdsManager.destroy()
```

- Destroys the current provider adapter, releases all SDK-level resources.
- Clears the preload cache.
- Resets to `IDLE` state — can be re-initialised with `initialize()`.

### 9. AdsListener & AdCallback

```kotlin
interface AdsListener {
    fun onAdLoaded(ad: AdsEntity)
    fun onAdFailedToLoad(error: String)
    fun onAdFailedToLoad(error: String, errorCode: String?)  // default → onAdFailedToLoad(error)
    fun onAdStartedToLoad()                                   // default: no-op
    fun onAdShown()
    fun onAdClicked()
    fun onAdClosed()
    fun onAdPaidEvent()
    fun onAdUserEarnedReward()
}
```

- `AdCallback` is an abstract class with empty defaults for every method — override only what you need.
- All callbacks are dispatched to the **main thread**.

### 10. UMP Consent Flow

> **package**: `com.kit.ads.ump`

```kotlin
fun UMP.start(activity: Activity, callBack: (gathered: Boolean) -> Unit)
```

- Wraps Google UMP to show privacy consent form.
- Call in the first Activity's `onCreate()`. The callback fires with whether consent was gathered.
- In debug builds (`BuildConfig.DEBUG = true`) it uses a test device hashed ID and auto-resets consent state.
- If consent retrieval fails (network error, etc.), it falls back to checking `ConsentInformation.canRequestAds()`.

```kotlin
UMP.start(this) { canRequestAds ->
    AdsManager.loadAd(this, request, listener)   // proceed with ad load
}
```

### 11. Complete Integration Example

```kotlin
// === App.kt (Application) ===
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AdsProviderConfig(AdsProviderType.ADMOB, "ca-app-pub-3940256099942544~3347511713")
        AdsManager.initialize(this, config)
    }
}

// === MainActivity.kt ===
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bannerContainer = findViewById<ViewGroup>(R.id.banner_container)

        // Preload on startup
        AdsManager.preloadAd(this, AdsRequest(
            "home", "ca-app-pub-xxx/9214589741", AdsType.BANNER, AdsProviderType.ADMOB
        ))

        UMP.start(this) {
            // Consent gathered — load and show ad
            AdsManager.loadAd(this, AdsRequest(
                "home", "ca-app-pub-xxx/9214589741", AdsType.BANNER, AdsProviderType.ADMOB
            ), object : AdCallback() {
                override fun onAdLoaded(ad: AdsEntity) {
                    ad.show(this@MainActivity, bannerContainer)
                }
                override fun onAdFailedToLoad(error: String, errorCode: String?) {
                    Log.e("Ad", "failed: $error ($errorCode)")
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adEntity?.destroy()         // clean up banner if held
    }
}
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

## Release & JitPack

### Publishing to JitPack

JitPack builds the SDK from GitHub tags/revisions. No manual upload is needed.

1. Push all changes to GitHub:
   ```
   git push origin main
   git push origin <tag>
   ```
2. Create a tag (or use any revision):
   ```
   git tag <version>                    # e.g. 1.0.0
   git push origin <version>
   ```
3. JitPack builds automatically on push. Check status at:
   `https://jitpack.io/#/com.github.AdvertisingKit/AdsKit/<version>`

For release builds, ensure you push a tag matching the version string in the publication config.

### Consumer Dependency

Once published, the host app depends on JitPack:

```kotlin
// settings.gradle (or root build.gradle)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle
dependencies {
    implementation("com.github.AdvertisingKit:AdsKit:<version>")
}
```

Replace `<version>` with a tag (e.g. `1.0.0`), commit hash, or `-SNAPSHOT`.

## Important Notes for Agents

1. Do not add non-requested API-level behavior changes to `AdsListener` or `AdsEntity.show` semantics.
2. When changing failure behavior, keep backward compatibility by preserving existing method signatures (e.g. the `errorCode` overload defaults to the legacy single-param version).
3. Avoid editing generated outputs under `*/build/`.
4. Existing sample app (`app/`) is a minimal demo; host integration should be validated with real devices for ad behavior.
5. When generating code for host app integration, use `AdCallback` (abstract class) instead of implementing `AdsListener` directly — it avoids forcing empty overrides.

## Implementation Caveats

- `AdsManager.initialize(...)` is asynchronous and callback-driven.
- `providerType` mismatch causes immediate callback failure before loading.
- All `AdsListener` callbacks from `AdsLoadHandler` are dispatched to the main thread.
- Preloaded ad cache is single-use: consumed on first `loadAd()` call, removed on `destroy()`.

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
