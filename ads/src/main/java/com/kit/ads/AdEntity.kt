package com.kit.ads

import android.app.Activity
import android.view.ViewGroup
import com.kit.ads.placement.AdTriggerPointExecutor


/**
 * 广告实体类
 *
 * 该类用于封装一个广告对象，并提供展示广告的功能。通过该类，开发者可以触发广告的展示。
 * 它持有一个广告触发点执行器 (`triggerPointExecutor`)，负责协调广告的展示过程。
 *
 * @param triggerPointExecutor 广告触发点执行器，负责广告的加载和展示操作。
 * @param ad 广告对象，表示已经加载的广告数据。
 */
class AdEntity(
    private val triggerPointExecutor: AdTriggerPointExecutor,  // 用于处理广告展示的执行器
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
        triggerPointExecutor.showAd(activity, container, ad)  // 调用执行器的展示方法来展示广告
    }
}
