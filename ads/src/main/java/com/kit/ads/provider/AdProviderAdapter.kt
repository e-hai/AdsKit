package com.kit.ads.provider

import android.app.Activity
import android.app.Application
import android.view.ViewGroup
import com.kit.ads.AdPlacement

/**
 * 广告提供商适配器接口
 * 该接口用于定义与广告提供商的交互方法，广告SDK适配器需要实现该接口来处理广告加载、展示、调试等功能。
 * 每个广告SDK提供的适配器实现该接口，以统一的方式进行广告的加载和展示。
 */
interface AdProviderAdapter {

    /**
     * 初始化广告提供商
     * 该方法用于初始化广告SDK，传入的配置会根据广告SDK的要求进行初始化。
     *
     * @param context 应用的上下文，通常为 Application 对象。
     * @param config 广告提供商的配置对象，包含广告SDK的相关配置信息（如API key、广告位ID等）。
     * @param listener 初始化完成后的回调，`success` 为 true 表示初始化成功，false 表示初始化失败。
     */
    fun initialize(
        context: Application,
        config: AdProviderConfig,
        listener: (success: Boolean) -> Unit
    )

    /**
     * 加载广告
     * 该方法用于加载广告资源，在广告加载过程中会触发 `ProviderListener` 提供的回调方法。
     *
     * @param activity 当前的 Activity，通常用于广告SDK与 UI 交互时的上下文。
     * @param request 广告位请求对象，包含广告的配置和广告位标识等信息。
     * @param listener 广告加载过程中触发的回调接口，用于通知广告加载的各个阶段（加载开始、加载成功、加载失败等）。
     */
    fun loadAd(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    )

    /**
     * 展示广告
     * 该方法用于展示已经加载好的广告，广告展示时会触发 `ProviderListener` 提供的回调方法。
     *
     * @param activity 当前的 Activity，通常用于广告SDK与 UI 交互时的上下文。
     * @param container 广告展示的容器视图，广告会被展示在这个容器中。
     * @param request 广告位请求对象，包含广告的配置和广告位标识等信息。
     * @param ad 已加载的广告对象，通常是在 `loadAd` 中成功加载后的广告对象。
     * @param listener 广告展示过程中触发的回调接口，用于通知广告展示的各个阶段（广告展示、点击、关闭等）。
     */
    fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdPlacement,
        ad: Any,
        listener: ProviderListener
    )

    /**
     * 打开调试模式
     * 该方法用于在调试时打开广告SDK的调试功能，通常用于打印日志或查看广告请求的详细信息。
     *
     * @param activity 当前的 Activity，调试模式通常与 UI 相关，用于展示调试信息。
     */
    fun openDebug(activity: Activity)
}
