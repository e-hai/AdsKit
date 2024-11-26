# AdManager SDK

该项目提供了一个广告管理模块，旨在统一管理不同广告SDK的加载、展示、事件回调等操作。通过该模块，开发者可以轻松集成和管理多个广告提供商（如 AdMob、AppLovin 等），并统一处理广告生命周期中的各类事件。

## 目录结构

- **`AdManager`**：核心广告管理类，负责初始化广告提供商、加载广告、展示广告、注册/取消事件观察者等功能。
- **`AdEntity`**：封装了广告对象和广告触发点执行器，提供广告展示的接口。
- **`AdListener`**：广告回调接口，用于接收广告的加载、展示、点击等事件。
- **`AdCallback`**：广告回调的抽象实现类，开发者可继承该类来实现具体的广告回调。
- **`AdTriggerPointExecutor`**：广告触发点执行器，负责广告的加载与展示过程，协调广告SDK和回调。
- **`AdProviderAdapter`**：广告提供商适配器接口，所有广告SDK的适配器都需要实现该接口。
- **`ProviderListener`**：广告SDK的回调接口，用于接收广告SDK的事件。
- **`AdTriggerPoint`**：广告触发点配置，定义广告的类型、位置和相关广告提供商。
- **`AdEventObserver`**：广告事件观察者接口，用于接收广告生命周期中的各种事件通知。

## 核心功能

### 1. 广告管理

`AdManager` 是广告管理模块的核心类，提供了广告SDK的初始化、广告的加载与展示、事件的观察等功能。主要方法如下：

- **`initialize(context: Application, providerConfigs: List<AdProviderConfig>)`**：初始化广告提供商适配器。通过外部传入广告提供商的配置，自动初始化广告SDK。
- **`openDebug(activity: Activity, providerType: AdProviderType)`**：开启调试模式，用于展示广告SDK的调试信息。
- **`loadAd(activity: Activity, request: AdTriggerPoint, adListener: AdListener)`**：加载广告，并在加载成功后触发回调。
- **`registerObserver(observer: AdEventObserver)`**：注册广告事件观察者，监听广告相关事件（如加载成功、展示、点击等）。
- **`unregisterObserver(observer: AdEventObserver)`**：取消注册广告事件观察者。

### 2. 广告回调

`AdListener` 和 `AdCallback` 提供了广告回调接口，用于接收广告的各个生命周期事件。

- **`onAdLoaded(ad: AdEntity)`**：广告加载成功时触发。
- **`onAdFailedToLoad(error: String)`**：广告加载失败时触发。
- **`onAdShown()`**：广告展示时触发。
- **`onAdClicked()`**：广告被点击时触发。
- **`onAdPaidEvent()`**：广告付费事件触发时调用。
- **`onAdUserEarnedReward()`**：用户通过观看广告获得奖励时触发。
- **`onAdClosed()`**：广告被关闭时触发。

`AdCallback` 是 `AdListener` 接口的抽象实现类，开发者可以继承该类并覆盖相关方法，方便定制广告回调。

### 3. 广告触发点

广告的加载和展示是基于 **广告触发点（AdTriggerPoint）** 进行的。每个广告触发点对应一个特定的广告位置或场景（如应用的某个界面或功能模块）。

- **`AdTriggerPoint`**：包含广告的配置（如广告ID、广告类型、广告提供商等），并通过广告位标识（`triggerId`）和广告单元ID（`adUnitId`）唯一标识广告位置。
- **`AdTriggerPointExecutor`**：负责加载和展示广告，协调广告SDK和回调。

### 4. 广告事件观察

`AdEventObserverManager` 用于管理广告事件观察者，开发者可以通过观察者模式接收到广告生命周期中的各类事件通知。例如，广告加载成功、广告展示、广告点击等。

- **`registerObserver(observer: AdEventObserver)`**：注册广告事件观察者。
- **`unregisterObserver(observer: AdEventObserver)`**：取消注册广告事件观察者。
- **`notifyObservers(eventType: AdEventType)`**：通知所有已注册的观察者广告事件。

### 5. 广告提供商适配

`AdProviderAdapter` 是广告提供商适配器接口，负责与不同广告SDK的交互。每个广告SDK需要实现该接口，确保广告的加载和展示方式统一。

- **`initialize(context: Application, config: AdProviderConfig, listener: (Boolean) -> Unit)`**：初始化广告SDK，完成后通知初始化是否成功。
- **`loadAd(activity: Activity, request: AdTriggerPoint, listener: ProviderListener)`**：加载广告资源。
- **`showAd(activity: Activity, container: ViewGroup, request: AdTriggerPoint, ad: Any, listener: ProviderListener)`**：展示广告。
- **`openDebug(activity: Activity)`**：开启广告SDK调试模式。

## 使用示例

### 1. 初始化广告SDK

在应用的 `Application` 类中，调用 `AdManager` 的 `initialize` 方法来初始化广告SDK。

```
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 配置广告提供商信息
        val providerConfigs = listOf(
            AdProviderConfig(AdProviderType.ADMOB, "admob_api_key"),
            AdProviderConfig(AdProviderType.APPLOVIN, "appLovin_api_key")
        )

        // 初始化广告管理器
        AdManager.initialize(this, providerConfigs)
    }
}
```
### 2.  加载广告

在需要加载广告时，调用 AdManager.loadAd() 方法，传入广告触发点（AdTriggerPoint）和回调监听器（AdListener）。
AdTriggerPoint 包含了广告的配置信息，而 AdListener 用于接收广告的加载、展示、点击等事件回调
```
val adTriggerPoint = AdTriggerPoint(
    triggerId = "home_banner",
    adUnitId = "ad_unit_id",
    adType = AdType.BANNER,
    providerType = AdProviderType.ADMOB
)

AdManager.loadAd(this, adTriggerPoint, object : AdCallback() {
    override fun onAdLoaded(ad: AdEntity) {
        ad.show(this@MainActivity, container)  // 展示广告
    }

    override fun onAdFailedToLoad(error: String) {
        Log.e("AdManager", "广告加载失败: $error")
    }
})
```
### 3.  全局广告事件观察
通过 AdManager.registerObserver() 注册广告事件观察者，开发者可以全局监听广告生命周期中的各类事件（如广告加载、展示、点击、关闭等）。
场景：用于精准统计广告数据，上传到自己服务器等等
```
val observer = object : AdEventObserver {
    override fun onAdEvent(eventType: AdEventType) {
        when (eventType) {
            is LoadAdStart -> Log.d("AdManager", "广告开始加载")
            is LoadAdSuccess -> Log.d("AdManager", "广告加载成功")
            is LoadAdFailure -> Log.d("AdManager", "广告加载失败")
            // 其他事件...
        }
    }
}

// 注册观察者
AdManager.registerObserver(observer)
```
