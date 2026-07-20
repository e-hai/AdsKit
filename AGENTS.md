# AdsKit – AGENTS.md

This file is for AI agents (Codex, Copilot, etc.) to quickly understand the project structure, conventions, and how to make safe edits.

## What This Project Is

An Android ad mediation SDK (`:ads-core` + optional provider modules) plus a sample app (`:sample`). The SDK wraps AdMob and AppLovin SDKs under a single `AdsManager` facade so that the host app only imports one dependency and uses one API.

**Key design decision**: single-adapter mode – `AdsManager` initializes exactly one `AdsProviderAdapter` at a time. Switching providers requires reinitializing via `AdsManager.initialize()`, which destroys the old adapter first. Provider implementations are loaded via Java SPI — only modules on the classpath are available at runtime.

## Project Layout

```
AdsKit/
├── ads-core/                     # Core SDK (namespace: com.kit.ads) — AdsManager, public API, UMP
│   ├── build.gradle.kts
│   └── src/main/java/com/kit/ads/
│       ├── AdsManager.kt              # [Entry Point] Singleton facade with init state machine
│       ├── AdsRequest.kt              # Ad request data class
│       ├── AdsLoadHandler.kt          # [internal] Load+callback flow, main-thread dispatch
│       ├── AdsEntity.kt               # Loaded ad wrapper with show() and destroy()
│       ├── AdsListener.kt             # Callback interface + AdCallback base class
│       ├── AdsType.kt                 # BANNER / SPLASH / REWARDED / INTERSTITIAL / NATIVE / MREC
│       ├── AdsLogger.kt               # Log utility wrapping android.util.Log, runtime toggle
│       ├── AdsDebug.kt                # Shared debug flag for provider modules
│       ├── ump/UMP.kt                 # Google UMP consent (shared by AdMob / AppLovin)
│       └── provider/
│           ├── AdsProviderAdapter.kt  # [Interface] All adapters implement this
│           ├── AdsProviderAdapterFactory.kt  # SPI factory (internal)
│           ├── AdsProviderAdapterRegistrar.kt  # SPI registration interface
│           ├── AdsProviderConfig.kt   # Config data class
│           ├── AdsProviderType.kt     # ADMOB / APPLOVIN enum
│           └── AdsProviderListener.kt # Internal callback interface
├── ads-admob/                    # AdMob provider (namespace: com.kit.ads.provider.admob)
│   ├── build.gradle.kts
│   └── src/main/java/com/kit/ads/provider/admob/AdmobProviderAdapter.kt
├── ads-applovin/                 # AppLovin provider (namespace: com.kit.ads.provider.applovin)
│   ├── build.gradle.kts
│   └── src/main/java/com/kit/ads/provider/applovin/ApplovinProviderAdapter.kt
├── ads/                          # Full bundle aggregator (publishes artifact `AdsKit`)
│   └── build.gradle.kts          # api(:ads-core, :ads-admob, :ads-applovin)
├── sample/                       # Sample app (namespace: com.kit.ads.sample)
│   ├── build.gradle.kts          # Reads admob.app.id / applovin.sdk.key / ad units from local.properties
│   └── src/main/java/com/kit/ads/sample/
│       ├── App.kt            # Application class showing initialization
│       ├── MainActivity.kt   # Demo UI in Jetpack Compose
│       └── SampleConfig.kt   # Keys from BuildConfig; ad unit IDs for demos
├── local.properties          # sdk.dir + optional admob.app.id / applovin.sdk.key / ad unit IDs (gitignored)
├── build.gradle.kts          # Root build file (plugin aliases only)
├── settings.gradle.kts       # Includes :ads-core, :ads-admob, :ads-applovin, :ads, :sample
├── gradle/
│   ├── libs.versions.toml    # Version catalog (AGP, SDK, mediation deps)
│   ├── artifact-size-limits.properties  # CI AAR/APK size gates
│   └── wrapper/
├── scripts/
│   └── check-artifact-sizes.sh   # Verify publish artifacts vs size limits
├── gradle.properties
├── jitpack.yml
├── README.md
├── AGENTS.md                 # This file
└── gradlew                   # macOS sandbox helper for GRADLE_USER_HOME (see Environment Constraints)
```

## Module Dependency Options

Host apps choose artifacts based on which providers they need. **Public API is unchanged** — always `com.kit.ads.AdsManager`.

| Use case | JitPack dependency | Runtime switch |
|----------|-------------------|----------------|
| AdMob only | `AdsKit-admob` | ADMOB only |
| AppLovin only | `AdsKit-applovin` | APPLOVIN only |
| Dynamic switch | `AdsKit-admob` + `AdsKit-applovin` (or `AdsKit` full) | ADMOB ↔ APPLOVIN via `initialize()` |
| Backward compatible | `AdsKit` (full bundle) | Same as today |

```kotlin
// AdMob only
implementation("com.github.AdvertisingKit:AdsKit-admob:<version>")

// AppLovin only
implementation("com.github.AdvertisingKit:AdsKit-applovin:<version>")

// Dynamic switch (both providers on classpath)
implementation("com.github.AdvertisingKit:AdsKit-admob:<version>")
implementation("com.github.AdvertisingKit:AdsKit-applovin:<version>")

// Full bundle (backward compatible, one line)
implementation("com.github.AdvertisingKit:AdsKit:<version>")
```

Monorepo local development:

```kotlin
// Full bundle (sample default)
implementation(project(":ads"))

// AdMob only
implementation(project(":ads-admob"))  // transitively includes :ads-core

// Dynamic switch
implementation(project(":ads-admob"))
implementation(project(":ads-applovin"))
```

If a provider module is missing from the classpath, `initialize()` fails with `onResult(false)` and logs a clear message. `loadAd()` before init still returns `STATE_IDLE` / `STATE_FAILED` as before.

## Build Toolchain

All Gradle scripts use **Kotlin DSL** (`.gradle.kts`). Shared versions live in `gradle/libs.versions.toml`.

| Tool | Version |
|------|---------|
| Gradle | 9.6.0 |
| Android Gradle Plugin (AGP) | 9.2.1 |
| Kotlin (Compose plugin on `:sample`) | 2.4.0 |
| JDK | 17 |
| SDK release version | `sdkVersion` in `gradle/libs.versions.toml` (currently `1.0.0`) |
| minSdk / compileSdk / targetSdk | 24 / 37 / 36 |

**AGP 9 built-in Kotlin** — do **not** apply `org.jetbrains.kotlin.android`. Kotlin compilation is provided by AGP. Only `:sample` applies `org.jetbrains.kotlin.plugin.compose` for Jetpack Compose.

**`:ads-core` module specifics**:
- `buildFeatures { buildConfig = true }` — required because SDK code uses `BuildConfig.DEBUG`.
- `maven-publish` — publishes artifact `AdsKit-core`.
- Provider adapters register via `META-INF/services/com.kit.ads.provider.AdsProviderAdapterRegistrar`.

**`:ads` (full bundle) specifics**:
- No source code — re-exports `api(project(...))` for all provider modules.
- `maven-publish` — publishes artifact `AdsKit` (backward compatible default).
- Local release AAR copied to `ads-v<sdkVersion>.aar`.

## API Reference

All public types are under `com.kit.ads`. Import paths shown inline.

### 1. Module Dependency

See **Module Dependency Options** above for JitPack coordinates. For embedding the monorepo:

```kotlin
// settings.gradle.kts
include(":ads-core")
include(":ads-admob")
include(":ads-applovin")
include(":ads")

// app/build.gradle.kts — full bundle (default)
dependencies {
    implementation(project(":ads"))
}
```

No extra Gradle plugin or manifest configuration is required — provider SDKs are bundled by the chosen artifact.

### 2. AdsProviderConfig — Provider Configuration

> **package**: `com.kit.ads.provider`

```kotlin
data class AdsProviderConfig(
    val providerType: AdsProviderType,
    val apiKey: String, // AdMob: App ID; AppLovin: SDK key
)

enum class AdsProviderType { ADMOB, APPLOVIN }
```

### 3. AdsRequest — Ad Placement Request

> **package**: `com.kit.ads`

```kotlin
data class AdsRequest(
    val triggerId: String,          // Developer-defined scene identifier, e.g. "home_banner"
    val adUnitId: String,           // Ad unit ID from the ad platform
    val adType: AdsType,            // BANNER / SPLASH / REWARDED / INTERSTITIAL / NATIVE / MREC
    val providerType: AdsProviderType, // Must match the initialized provider
    val preloadTtlMs: Long? = null, // null → DEFAULT_PRELOAD_TTL_MS (55m); <=0 → never expire
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

fun AdsManager.getInitState(): AdsInitState
fun AdsManager.getInitializedProviderType(): AdsProviderType?
```

- **Single-adapter mode**: one provider at a time.
- Reinitialising with the same provider is skipped and invokes `onResult(true)`.
- Switching providers destroys the previous adapter, **clears the preload cache**, then creates a new one.
- Concurrent calls are ignored and invoke `onResult(false)`.
- `onResult` is always dispatched on the **main thread**.
- All `loadAd()` / `preloadAd()` calls before `onResult(true)` fail with `STATE_INITIALIZING`.
- Query state anytime via `getInitState()` / `getInitializedProviderType()`.

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
- A subsequent `loadAd()` with the same `triggerId` returns the cached ad immediately **if not expired**.
- Default TTL is `AdsManager.DEFAULT_PRELOAD_TTL_MS` (55 minutes). Override per request via `AdsRequest.preloadTtlMs` (`<= 0` = never expire).
- Cache is single-use — consumed on the first `loadAd()` call, cleared on `destroy()` / provider switch, or discarded when TTL expires (expired entity is `destroy()`ed).
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
- `container` is **mandatory** and **meaningful for BANNER / NATIVE / MREC** — attaches the ad view.
- For `REWARDED` / `SPLASH` / `INTERSTITIAL`: adapter ignores `container` semantics; any valid `ViewGroup` satisfies the API.
- `destroy()` must be called for **BANNER / NATIVE / MREC** (releases WebView / native view resources). Best called in `onDestroy()`.

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
    fun onAdPaidEvent(paid: AdsPaidEvent)              // default → onAdPaidEvent()
    fun onAdUserEarnedReward()
    fun onAdFailedToShow(error: String, errorCode: String?)  // default: no-op
}
```

- `AdCallback` is an abstract class with empty defaults for every method — override only what you need.
- All callbacks are dispatched to the **main thread**.

### 10. UMP Consent Flow

> **package**: `com.kit.ads.ump` — lives in `:ads-core` (available with any provider artifact)

```kotlin
fun UMP.start(activity: Activity, callBack: (gathered: Boolean) -> Unit)
```

- Wraps Google UMP to show privacy consent form. Ships with `user-messaging-platform` via `:ads-core`.
- **AdMob**: `initialize` → `UMP.start()` → load ads.
- **AppLovin**: `UMP.start()` → `initialize` → load ads (MAX ingests the TCF / AC strings written by UMP).
- In debug builds (`AdsDebug.isEnabled`) it uses a test device hashed ID and auto-resets consent state.
- If consent retrieval fails (network error, etc.), it falls back to checking `ConsentInformation.canRequestAds()`.

```kotlin
// AdMob
AdsManager.initialize(app, admobConfig)
UMP.start(this) { canRequestAds ->
    AdsManager.loadAd(this, request, listener)
}

// AppLovin
UMP.start(this) { canRequestAds ->
    AdsManager.initialize(app, AdsProviderConfig(AdsProviderType.APPLOVIN, sdkKey)) {
        // load ads
    }
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
- `container` is **mandatory to pass**, and is **meaningful for BANNER / NATIVE / MREC** ads.
- For `AdsType.REWARDED`, `AdsType.SPLASH`, and `AdsType.INTERSTITIAL`, the concrete adapter currently ignores container semantics; passing a valid `ViewGroup` is sufficient to satisfy API shape.

## Init State Machine & Guards

- `AdsManager` maintains an internal state machine: `IDLE → INITIALIZING → READY | FAILED`.
- `initialize()` calls are gated: concurrent initialize is ignored; repeated initialize to the same provider is skipped; switching providers destroys the old adapter first.
- `loadAd()` validates: if state is not READY, fails fast with `STATE_INITIALIZING` / `STATE_IDLE` / `STATE_FAILED` error code. If `AdsRequest.providerType` mismatches the initialized provider, fails fast with `PROVIDER_MISMATCH`.
- `preloadAd()` passes through the same validation as `loadAd()`. A preloaded ad survives until consumed by `loadAd()`, TTL expiry, `destroy()`, or provider switch.
- Query `getInitState()` / `getInitializedProviderType()` for host UI / gating.
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
| `onAdPaidEvent()` | Revenue event (legacy, no amount) |
| `onAdPaidEvent(paid: AdsPaidEvent)` | Revenue with `valueMicros` / `currencyCode` / `precision` (defaults to no-arg) |
| `onAdUserEarnedReward()` | User earned reward (REWARDED) |
| `onAdFailedToShow(error, errorCode?)` | Show-phase failure (default no-op); AppLovin codes may use `DISPLAY_` prefix |

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
| `AdsKit-Sample` | Sample app event log |

In Logcat:
- Filter `AdsKit` to see **all** SDK logs (substring match covers all `AdsKit-*` tags).
- Filter `AdsKit-Admob` to isolate only AdMob adapter logs.

Log messages use a compact key=value format: `onAdLoaded id=... unit=... type=... provider=...`.

Adapter-layer events (also key=value): `loadAd` / `onAdLoaded` / `onAdShown` / `onAdClicked` / `onAdClosed` / `onAdPaidEvent` / `onAdFailedToLoad` / `onAdFailedToShow` / `showAd` / `destroyAd`.

Lifecycle logs on `AdsKit` tag:
- `initialize start/complete/staleCallback` — init state machine
- `destroy` / `clearPreloadedAds` — teardown and cache eviction
- `loadAd rejected` — state or provider mismatch before network
- Failures use `AdsLogger.e` / `AdsLogger.w`; success paths use `AdsLogger.d`
- Runtime toggle: `AdsLogger.enabled` (default `BuildConfig.DEBUG` in core)
- AdMob test devices: `AdsDebug.admobTestDeviceIds` (applied at AdMob init when `AdsDebug.isEnabled`)

## Naming Conventions

Public API uses the `Ads*` prefix (`AdsManager`, `AdsEntity`, `AdsListener`). Exception: `AdCallback` (implements `AdsListener`) and `UMP` (matches Google UMP).

| Name | Notes |
|------|-------|
| `AdsType.SPLASH` | Maps to **App Open** (`AppOpenAd` / `MaxAppOpenAd`), not a generic splash image |
| `AdsEntity` | Loaded-ad handle for `show()` / `destroy()`, not a persistence entity |
| `AdmobProviderAdapter` / `ApplovinProviderAdapter` | Implementation class names (SPI); brands are AdMob / AppLovin |
| `AdsInitState` | Public init state; internal `AdsManager.InitState` is private |

Do **not** rename public types or `AdsType` enum values without a major version — JitPack consumers depend on stable names.

## Release & JitPack

### Publishing to JitPack

JitPack builds the SDK from GitHub tags/revisions and runs `publishToMavenLocal`. The `:ads` module must keep the `maven-publish` plugin configured.

**Publication config** — each publishable module has `maven-publish`:
- `AdsKit-core` — `:ads-core`
- `AdsKit-admob` — `:ads-admob` (includes `:ads-core` transitively)
- `AdsKit-applovin` — `:ads-applovin` (includes `:ads-core` transitively)
- `AdsKit` — `:ads` full bundle (all providers, backward compatible)

Common fields:
- `group` — from JitPack `-Pgroup` (e.g. `com.github.e-hai`), fallback `com.github.AdvertisingKit`
- `version` — from JitPack `-Pversion` (tag / commit hash), fallback `sdkVersion` in `gradle/libs.versions.toml`
- Component — `release` via `android.publishing { singleVariant("release") }`

**Local verification**:
```bash
./gradlew publishToMavenLocal -Pgroup=com.github.ci -Pversion=test
```

**JitPack config** — `jitpack.yml` pins JDK 17. JitPack publishes all modules with `maven-publish`.

**CI** — `.github/workflows/build.yml` runs root `publishToMavenLocal` (verifies `AdsKit-core` / `AdsKit-admob` / `AdsKit-applovin` / `AdsKit` AARs), `:ads-core:testDebugUnitTest`, `:sample:assembleDebug`, and `scripts/check-artifact-sizes.sh` (AAR/APK size gates in `gradle/artifact-size-limits.properties`).

**`:sample` dependency** — defaults to `implementation(project(":ads"))` (full bundle). To test a slim integration, swap to `project(":ads-admob")` only.
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
// settings.gradle.kts (or root build.gradle.kts)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.e-hai:AdsKit:<version>")
}
```

Replace `<version>` with a tag (e.g. `1.0.0`), commit hash, or `-SNAPSHOT`.

## Testing

`:ads-core` has Robolectric unit tests (`AdsManagerTest`, `AdsProviderAdapterFactoryTest`). Provider modules are on the test classpath for SPI wiring.

```bash
./gradlew :ads-core:testDebugUnitTest
```

## Important Notes for Agents

1. Do not add non-requested API-level behavior changes to `AdsListener` or `AdsEntity.show` semantics.
2. When changing failure behavior, keep backward compatibility by preserving existing method signatures (e.g. the `errorCode` overload defaults to the legacy single-param version).
3. Avoid editing generated outputs under `*/build/`.
4. Existing sample app (`:sample`) is a minimal demo; host integration should be validated with real devices for ad behavior.
5. When generating code for host app integration, use `AdCallback` (abstract class) instead of implementing `AdsListener` directly — it avoids forcing empty overrides.
6. **Do not add `org.jetbrains.kotlin.android`** — AGP 9 provides built-in Kotlin; applying the plugin will fail the build.
7. **Do not use legacy AGP APIs** — `libraryVariants`, `applicationVariants`, and `android.kotlinOptions {}` are incompatible with AGP 9. Use `androidComponents` Variant API for variant-level build logic.
8. **Keep `maven-publish` on `:ads`** — JitPack requires `publishToMavenLocal`; do not rename away `ads-release.aar` (copy to a versioned name instead).
9. Edit build scripts as `.gradle.kts` and prefer `gradle/libs.versions.toml` for version bumps (including `sdkVersion`).
10. **Bump release version in one place** — update `sdkVersion` in `gradle/libs.versions.toml`; Git tag should match when publishing a release.

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

The `gradlew` script sets `GRADLE_USER_HOME` to `/private/tmp/gradle` **only on macOS (Darwin)** when unset, avoiding `.lck` file permission errors in local sandboxed environments. On Linux (including JitPack CI) it uses the default `~/.gradle`. Do not force `/private/tmp/gradle` globally — it breaks Linux builds.
