package com.kit.ads

import android.app.Activity
import android.view.ViewGroup
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderListener


/**
 * 广告实体类
 * 负责广告显示
 */
class AdsEntity(
    private val providerAdapter: AdsProviderAdapter,  // 用于处理广告展示的执行器
    private val request: AdsRequest,  // 广告触发点请求配置
    private val adsProviderListener: AdsProviderListener,  // 广告事件监听器
    private val ad: Any  // 已加载的广告对象，具体类型依赖于广告提供商
) {

    private companion object {
        private const val TAG = "AdsKit-AdsEntity"
    }


    /**
     * 展示广告
     *
     * 此方法负责展示广告，开发者可以在应用的某个界面调用该方法来展示已加载的广告。
     * `container` 在 BANNER / NATIVE / MREC 场景下用于承载广告视图；
     * 对于 REWARDED / SPLASH / INTERSTITIAL，适配器会忽略 `container` 的位置语义，
     * 只要求传入有效的 ViewGroup。
     *
     * @param activity 当前的 Activity，用于广告展示时与用户交互。
     * @param container 广告展示的容器视图，广告将被展示在这个视图中。
     */
    fun show(
        activity: Activity,  // 当前的 Activity，通常用于广告SDK与 UI 交互时的上下文
        container: ViewGroup  // 广告展示的容器，广告会被添加到此容器中
    ) {
        AdsLogger.d(
            TAG,
            "showAd id=${request.triggerId} unit=${request.adUnitId} type=${request.adType} provider=${request.providerType}",
        )
        providerAdapter.showAd(activity, container, request, ad, adsProviderListener)  // 使用适配器展示广告
    }

    /**
     * 销毁广告对象，释放资源
     *
     * 对于 Banner / MREC / Native 广告，必须调用此方法释放 View/WebView 等资源。
     * 最佳实践：在 Activity/Fragment 的 onDestroy 中调用。
     */
    fun destroy() {
        AdsLogger.d(
            TAG,
            "destroyAd id=${request.triggerId} unit=${request.adUnitId} type=${request.adType} provider=${request.providerType}",
        )
        providerAdapter.destroyAd(ad)
    }
}
