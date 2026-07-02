# AdsManager SDK

该项目提供了一个广告管理模块，旨在统一管理不同广告 SDK 的加载、展示、事件回调等操作。通过该模块，开发者可以集成和管理广告提供商（如 AdMob、AppLovin 等），并统一处理广告生命周期中的各类事件。

## 目录结构

- **`AdsManager`**：核心广告管理单例类，负责初始化广告提供商、加载广告、展示广告、资源销毁等功能，内部维护初始化状态机。
- **`AdsEntity`**：封装已加载的广告对象，提供 `show()` 和 `destroy()` 接口。
- **`AdsListener`**：广告回调接口，用于接收广告的加载、展示、点击等事件。
- **`AdCallback`**：`AdsListener` 的抽象实现类，提供空默认实现，开发者可继承并只覆写需要的方法。
- **`AdsRequest`**：广告请求数据类，定义广告触发点标识、广告单元 ID、广告类型和提供商类型。
- **`AdsLoadHandler`**：广告加载协调器，负责将提供商的回调映射到公开的 `AdsListener`，并确保主线程派发。
- **`AdsType`**：广告类型枚举（`BANNER` / `SPLASH` / `REWARDED`）。
- **`AdsLogger`**：日志工具类，封装 `android.util.Log`，支持运行时开关。
- **`AdsProviderAdapter`**：广告提供商适配器接口，所有广告 SDK 的适配器都需要实现该接口。
- **`AdsProviderAdapterFactory`**：适配器工厂（`internal`），根据 `AdsProviderType` 创建对应的适配器实例。
- **`AdsProviderConfig`**：广告提供商配置数据类，包含 `providerType` 和 `apiKey`。
- **`AdsProviderType`**：广告提供商类型枚举（`ADMOB` / `APPLOVIN`）。
- **`AdsProviderListener`**：内部回调接口，包含加载、展示、关闭等事件，额外提供 `onAdFailedToShow` 用于展示阶段失败。
- **`UMP`**：Google UMP（User Messaging Platform）隐私同意流程封装。

## 核心功能

### 1. 广告管理

`AdsManager` 是广告管理模块的核心单例类，内部维护初始化状态机（IDLE → INITIALIZING → READY/FAILED），提供了广告 SDK 的初始化、广告的预加载与展示、资源销毁等功能。主要方法如下：

- **`initialize(context: Application, config: AdsProviderConfig, onResult: ((Boolean) -> Unit)? = null)`**：初始化广告提供商适配器，通过外部传入配置初始化指定的广告 SDK。**单适配器模式**：每次运行仅初始化一个广告提供商。切换提供商时自动销毁旧适配器。
- **`destroy()`**：销毁当前适配器，释放所有资源（含 SDK 级别状态），回到 IDLE 状态，可重新 `initialize`。
- **`openDebug(activity: Activity)`**：开启当前已初始化广告提供商的调试模式，用于展示广告 SDK 的调试信息。
- **`loadAd(context: Context, request: AdsRequest, adListener: AdsListener)`**：加载广告。调用前会校验初始化状态和提供商类型是否匹配，不符合则快速失败并附上标准化错误码。
- **`preloadAd(context: Context, request: AdsRequest)`**：预加载广告，按 `triggerId` 缓存在 `AdsManager` 内部。后续调用 `loadAd()` 使用相同 `triggerId` 时，优先从缓存返回，无需网络请求。

### 2. 广告回调

`AdsListener` 和 `AdCallback` 提供了广告回调接口，所有回调均在主线程派发。

```
onAdStartedToLoad()                      // 广告开始加载（默认空实现）
onAdLoaded(ad: AdsEntity)               // 广告加载成功
onAdFailedToLoad(error: String)         // 广告加载失败（旧版单参数，保持向后兼容）
onAdFailedToLoad(error: String, errorCode: String?)  // 广告加载失败（含错误码）
onAdShown()                             // 广告展示
onAdClicked()                           // 广告被点击
onAdPaidEvent()                         // 广告付费事件
onAdUserEarnedReward()                  // 用户获得奖励（激励广告）
onAdClosed()                            // 广告关闭
```

### 3. 广告请求 (AdsRequest)

广告的加载和展示基于 **广告请求（AdsRequest）** 数据类：

```kotlin
data class AdsRequest(
    val triggerId: String,      // 广告触发点标识
    val adUnitId: String,       // 广告单元 ID
    val adType: AdsType,        // 广告类型（BANNER / SPLASH / REWARDED）
    val providerType: AdsProviderType,  // 提供商类型（ADMOB / APPLOVIN）
)
```

`triggerId` 由开发者自定义，用于区分不同的广告展示场景（如首页横幅、插屏、开屏等）。

### 4. 广告提供商适配

`AdsProviderAdapter` 是广告提供商适配器接口，负责与不同广告 SDK 的交互：

```kotlin
interface AdsProviderAdapter {
    fun initialize(context: Application, config: AdsProviderConfig, listener: (Boolean) -> Unit)
    fun loadAd(context: Context, request: AdsRequest, listener: AdsProviderListener)
    fun showAd(activity: Activity, container: ViewGroup, request: AdsRequest, ad: Any, listener: AdsProviderListener)
    fun openDebug(activity: Activity)
    fun destroyAd(ad: Any)   // 默认空实现
    fun destroy()            // 默认空实现
}
```

### 5. 错误码说明

| 错误码 | 说明 |
|--------|------|
| `STATE_INITIALIZING` | `loadAd()` 在初始化过程中被调用 |
| `STATE_IDLE` | `loadAd()` 在未调用 `initialize()` 时被调用 |
| `STATE_FAILED` | `loadAd()` 在初始化失败后被调用 |
| `PROVIDER_MISMATCH` | `AdsRequest.providerType` 与已初始化的提供商类型不一致 |
| `DISPLAY_*` | AppLovin 展示阶段错误，如 `DISPLAY_AD_ALREADY_SHOWN` |
| SDK 原生码 | AdMob / AppLovin 框架原始错误码透传 |

### 6. 日志规范

所有 SDK 内部日志统一使用 `AdsKit-{类名}` TAG 前缀，通过 `AdsLogger` 输出：

| TAG | 来源 |
|-----|------|
| `AdsKit` | `AdsManager.kt` |
| `AdsKit-AdsLoadHandler` | `AdsLoadHandler.kt` |
| `AdsKit-AdsEntity` | `AdsEntity.kt` |
| `AdsKit-Admob` | `AdmobProviderAdapter.kt` |
| `AdsKit-AppLovin` | `ApplovinProviderAdapter.kt` |
| `AdsKit-UMP` | `UMP.kt` |

Logcat 中：
- 过滤 `AdsKit` 可看到 **所有** SDK 日志（子串匹配覆盖 `AdsKit-*` 类）。
- 过滤 `AdsKit-Admob` 只看 AdMob 适配器日志。

日志采用 `key=value` 紧凑格式，例如：
```
AdsKit-AdsLoadHandler: onAdLoaded id=home_banner unit=ca-app-pub-xxx/9214589741 type=BANNER provider=ADMOB
```

## 使用示例

### 1. 初始化广告 SDK

在应用的 `Application` 类中，调用 `AdsManager.initialize()` 来初始化指定的广告 SDK。

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AdsProviderConfig(
            AdsProviderType.ADMOB,
            "ca-app-pub-3940256099942544~3347511713"  // Google 测试 App ID
        )
        AdsManager.initialize(this, config)
    }
}
```

### 2. 加载和展示广告

```kotlin
val request = AdsRequest(
    triggerId = "home_banner",
    adUnitId = "ca-app-pub-3940256099942544/9214589741",  // Google 测试广告单元
    adType = AdsType.BANNER,
    providerType = AdsProviderType.ADMOB
)

AdsManager.loadAd(this, request, object : AdCallback() {
    override fun onAdLoaded(ad: AdsEntity) {
        ad.show(this@MainActivity, bannerContainer)
    }

    override fun onAdFailedToLoad(error: String, errorCode: String?) {
        Log.e("Ads", "加载失败: $error (code=$errorCode)")
    }
})
```

### 3. 资源销毁

```kotlin
// 销毁单个广告实体（例如在 onDestroy 中清理 Banner）
override fun onDestroy() {
    super.onDestroy()
    adEntity?.destroy()
}

// 销毁整个 AdsManager（清空适配器）
AdsManager.destroy()
```

### 4. 广告生命周期状态机

```
                    +-----------+
                    |   IDLE    |
                    +-----+-----+
                          |
                  initialize()
                          |
                    +-----v-----+
                    | INITIALIZING|
                    +-----+-----+
                     /           \
              成功 /               \ 失败
                 v                 v
          +-----------+     +-----------+
          |   READY   |     |  FAILED   |
          +-----+-----+     +-----------+
                |
          loadAd() / show()
                |
          destroy() → 回到 IDLE
```

## 预加载说明

`AdsManager.preloadAd()` 用于提前加载并缓存广告，适合在 App 启动、切换页面等时机提前发起加载请求。

```kotlin
val request = AdsRequest(
    triggerId = "home_banner",
    adUnitId = "ca-app-pub-3940256099942544/9214589741",
    adType = AdsType.BANNER,
    providerType = AdsProviderType.ADMOB
)

// 预加载
AdsManager.preloadAd(this, request)

// 后续加载 —— 有缓存直接返回，无缓存走网络
AdsManager.loadAd(this, request, object : AdCallback() {
    override fun onAdLoaded(ad: AdsEntity) {
        ad.show(this@MainActivity, container)
    }
})
```

行为规则：
- 预加载走与 `loadAd()` 相同的状态和类型校验，不合法则静默失败。
- 缓存以 `request.triggerId` 为键，消费后立即移除（单次有效）。
- `AdsManager.destroy()` 会清空所有预加载缓存。
- `loadAd()` 时若缓存不存在，自动回退到实时网络加载。

## 本地构建/运行说明（权限问题）

### 症状

`./gradlew` 在执行时若出现以下任一错误，原因均为 **运行环境禁止 UDP socket 绑定**（沙箱/网络权限限制）：

```
Could not create service of type FileLockContentionHandler ...
Caused by: java.net.SocketException: Operation not permitted
  at java.base/sun.nio.ch.Net.bind0(Native Method)
  ...
  at org.gradle.cache.internal.locklistener.DefaultFileLockCommunicator.<init>(...)
```

该错误来自 Gradle 启动时 DefaultFileLockCommunicator 内部 new DatagramSocket(0, ...) 调用。Gradle 9.x 需要用 UDP socket 做跨进程文件锁协商，这是框架启动阶段行为，**非项目代码问题**。

### 诊断

运行以下命令快速确认当前环境是否支持 UDP socket 绑定：

```bash
python3 -c "import socket; s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.bind(('127.0.0.1', 0)); print('UDP socket OK')"
```

如果抛出 PermissionError: Operation not permitted，说明当前环境禁止 socket 绑定，./gradlew 无法正常启动。

### 解决方案

- **在当前沙箱环境下**：需要提权（escalate）运行 Gradle，获得网络 socket 操作权限。沙箱环境变量 GRADLE_USER_HOME 已由 gradlew 自动保护。
- **在本地 macOS 或 CI 环境**：不存在此限制，直接运行 ./gradlew 即可。
- .lck 文件报错（如 gradle-9.4.1-bin.zip.lck）：已由 gradlew 自动处理（当 GRADLE_USER_HOME 未设置时使用 /private/tmp/gradle），无需手工干预。
