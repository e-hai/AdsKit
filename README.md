# AdsManager SDK

该项目提供了一个广告管理模块，旨在统一管理不同广告SDK的加载、展示、事件回调等操作。通过该模块，开发者可以集成和管理广告提供商（如 AdMob、AppLovin 等），并统一处理广告生命周期中的各类事件。

## 目录结构

- **`AdsManager`**：核心广告管理类，负责初始化广告提供商、加载广告、展示广告、注册/取消事件观察者等功能。
- **`AdsEntity`**：封装了广告对象和广告触发点执行器，提供广告展示的接口。
- **`AdsListener`**：广告回调接口，用于接收广告的加载、展示、点击等事件。
- **`AdCallback`**：广告回调的抽象实现类，开发者可继承该类来实现具体的广告回调。
- **`AdPlacementExecutor`**：广告触发点执行器，负责广告的加载与展示过程，协调广告SDK和回调。
- **`AdProviderAdapter`**：广告提供商适配器接口，所有广告SDK的适配器都需要实现该接口。
- **`ProviderListener`**：广告SDK的回调接口，用于接收广告SDK的事件。
- **`AdPlacement`**：广告触发点配置，定义广告的类型、位置和相关参数。
- **`AdEventObserver`**：广告事件观察者接口，用于接收广告生命周期中的各种事件通知。

## 核心功能

### 1. 广告管理

`AdsManager` 是广告管理模块的核心类，提供了广告SDK的初始化、广告的加载与展示、事件的观察等功能。主要方法如下：

- **`initialize(context: Application, config: AdProviderConfig)`**：初始化广告提供商适配器。通过外部传入广告提供商的配置，初始化指定的广告SDK。**注意：目前 AdsManager 设计为单适配器模式，即每次运行仅初始化一个广告提供商。**
- **`openDebug(activity: Activity)`**：开启当前已初始化广告提供商的调试模式，用于展示广告SDK的调试信息。
- **`loadAd(context: Context, placement: AdPlacement, adListener: AdsListener)`**：加载广告，并在加载成功后触发回调。
- **`registerObserver(observer: AdEventObserver)`**：注册广告事件观察者，监听广告相关事件（如加载成功、展示、点击等）。
- **`unregisterObserver(observer: AdEventObserver)`**：取消注册广告事件观察者。

### 2. 广告回调

`AdsListener` 和 `AdCallback` 提供了广告回调接口，用于接收广告的各个生命周期事件。

- **`onAdLoaded(ad: AdsEntity)`**：广告加载成功时触发。
- **`onAdFailedToLoad(error: String)`**：广告加载失败时触发。
- **`onAdShown()`**：广告展示时触发。
- **`onAdClicked()`**：广告被点击时触发。
- **`onAdPaidEvent()`**：广告付费事件触发时调用。
- **`onAdUserEarnedReward()`**：用户通过观看广告获得奖励时触发。
- **`onAdClosed()`**：广告被关闭时触发。

`AdCallback` 是 `AdsListener` 接口的抽象实现类，开发者可以继承该类并覆盖相关方法，方便定制广告回调。

### 3. 广告触发点 (AdPlacement)

广告的加载和展示是基于 **广告触发点（AdPlacement）** 进行的。每个广告触发点对应一个特定的广告位置或场景。

- **`AdPlacement`**：包含广告的配置（如广告ID、广告类型等），通过广告位标识唯一标识广告位置。
- **`AdPlacementExecutor`**：负责加载和展示广告，协调广告SDK和回调。

### 4. 广告事件观察

`AdEventObserverManager` 用于管理广告事件观察者，开发者可以通过观察者模式接收到广告生命周期中的各类事件通知。

- **`registerObserver(observer: AdEventObserver)`**：注册广告事件观察者。
- **`unregisterObserver(observer: AdEventObserver)`**：取消注册广告事件观察者。
- **`notifyObservers(eventType: AdEventType)`**：通知所有已注册的观察者广告事件。

### 5. 广告提供商适配

`AdProviderAdapter` 是广告提供商适配器接口，负责与不同广告SDK的交互。

- **`initialize(context: Application, config: AdProviderConfig, listener: (Boolean) -> Unit)`**：初始化广告SDK，完成后通知初始化是否成功。
- **`loadAd(context: Context, placement: AdPlacement, listener: ProviderListener)`**：加载广告资源。
- **`showAd(activity: Activity, container: ViewGroup, placement: AdPlacement, ad: Any, listener: ProviderListener)`**：展示广告。
- **`openDebug(activity: Activity)`**：开启广告SDK调试模式。

### 6. 广告业务逻辑 (Waterfall & Bidding)

本 SDK 支持主流广告平台的 **瀑布流 (Waterfall)** 和 **应用内竞价 (In-App Bidding)** 模式：

- **瀑布流 (Waterfall)**：传统的层级变现模式。通过在广告平台后台配置不同 eCPM 的广告单元，SDK 会按照价格从高到低的顺序依次请求广告源，直到获得填充。
- **应用内竞价 (Bidding)**：现代化的实时竞价模式。广告平台会向所有参与竞价的广告源同时发起请求，各源实时出价，最高者获得展示机会。这种模式通常能显著提高填充率和收益（ARPU）。

SDK 的 `AdsManager` 抽象了这些底层的复杂交互，开发者只需配置好对应的 `adUnitId`，具体的 Bidding 或 Waterfall 策略直接在广告平台（如 AdMob 或 AppLovin MAX）的后台管理即可。

## 使用示例

### 1. 初始化广告SDK

在应用的 `Application` 类中，调用 `AdsManager` 的 `initialize` 方法来初始化指定的广告SDK。

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 配置广告提供商信息 (例如 AdMob)
        val config = AdProviderConfig(AdProviderType.ADMOB, "your_admob_app_id")

        // 初始化广告管理器 (单提供商模式)
        AdsManager.initialize(this, config)
    }
}
```

### 2. 加载广告

在需要加载广告时，调用 `AdsManager.loadAd()` 方法，传入广告触发点 (`AdPlacement`) 和回调监听器 (`AdsListener`)。

```kotlin
val placement = AdPlacement(
    triggerId = "home_banner",
    adUnitId = "your_ad_unit_id",
    adType = AdsType.BANNER,
    providerType = AdProviderType.ADMOB
)

AdsManager.loadAd(this, placement, object : AdCallback() {
    override fun onAdLoaded(ad: AdsEntity) {
        ad.show(this@MainActivity, container)  // 展示广告
    }

    override fun onAdFailedToLoad(error: String) {
        Log.e("AdsManager", "广告加载失败: $error")
    }
})
```

### 3. 全局广告事件观察

通过 `AdsManager.registerObserver()` 注册观察者，全局监听广告生命周期事件。

```kotlin
val observer = object : AdEventObserver {
    override fun onAdEvent(eventType: AdEventType) {
        when (eventType) {
            is LoadAdStart -> Log.d("AdsManager", "广告开始加载: ${eventType.placement.triggerId}")
            is LoadAdSuccess -> Log.d("AdsManager", "广告加载成功")
            is LoadAdFailure -> Log.d("AdsManager", "广告加载失败: ${eventType.errorMessage}")
            // errorCode / providerType 可选：eventType.errorCode / eventType.providerType
            // 其他事件...
        }
    }
}

// 注册观察者
AdsManager.registerObserver(observer)
```

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

该错误来自 Gradle 启动时 DefaultFileLockCommunicator 内部 new DatagramSocket(0, …) 调用。Gradle 9.x 需要用 UDP socket 做跨进程文件锁协商，这是框架启动阶段行为，**非项目代码问题**。

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
