package com.kit.ads

import android.app.Activity
import android.view.ViewGroup
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.ProviderListener


/**
 * 广告实体类
 * 负责广告显示
 */
class AdEntity(
    private val providerAdapter: AdProviderAdapter,  // 用于处理广告展示的执行器
    private val placement: AdPlacement,  // 广告触发点请求配置
    private val providerListener: ProviderListener,  // 广告事件监听器
    private val ad: Any  // 已加载的广告对象，具体类型依赖于广告提供商
) {

    /**
     * 展示广告
     *
     * 此方法负责展示广告，开发者可以在应用的某个界面调用该方法来展示已加载的广告。
     *
     * @param activity 当前的 Activity，用于广告展示时与用户交互。
     * @param container 广告展示的容器视图，广告将被展示在这个视图中。
     */
    fun show(
        activity: Activity,  // 当前的 Activity，通常用于广告SDK与 UI 交互时的上下文
        container: ViewGroup  // 广告展示的容器，广告会被添加到此容器中
    ) {
        providerAdapter.showAd(activity, container, placement, ad, providerListener)  // 使用适配器展示广告
    }

}
