package com.kit.ads.provider

import com.kit.ads.AdsPaidEvent


/**
 * 用于广告SDK回调的接口，它主要用于接收来自广告提供商（如 AdMob、AppLovin 等）在广告加载、展示、点击、关闭等过程中的通知。
 * 这个接口的实现是广告提供商适配器的一部分，统一了不同广告提供商SDK回调的差异，在广告的整个生命周期中负责向 AdsRequestCall 传递不同的事件
 * **/
interface AdsProviderListener {

    /**
     * 广告开始加载时触发
     * 该回调方法在广告加载开始时被调用，通常用于显示加载进度或进行其他准备操作。
     */
    fun onAdStartedToLoad()

    /**
     * 广告加载成功时触发
     * 该回调方法在广告成功加载后被调用，通常会传递广告数据（如广告对象）。
     * 你可以在这里展示广告或执行其他相关操作。
     *
     * @param ad 加载成功的广告对象，具体类型依赖于广告提供商
     */
    fun onAdLoaded(ad: Any)

    /**
     * 广告加载失败时触发
     * 该回调方法在广告加载失败时被调用，通常用于显示错误信息或进行重试等操作。
     *
     * @param error 错误信息，描述加载失败的原因
     * @param errorCode 错误码，可选
     */
    fun onAdFailedToLoad(error: String, errorCode: String? = null)

    /**
     * 广告展示时触发
     * 该回调方法在广告成功展示给用户时被调用，通常用于更新 UI 或进行其他操作（如记录展示次数）。
     */
    fun onAdShown()

    /**
     * 广告被点击时触发
     * 该回调方法在用户点击广告时被调用，通常用于统计点击事件或执行广告点击后的动作（如打开链接）。
     */
    fun onAdClicked()

    /**
     * 广告支付事件触发时调用（无收益明细）
     */
    fun onAdPaidEvent()

    /**
     * 广告支付事件触发时调用（含收益明细）
     *
     * 默认委派到无参 [onAdPaidEvent]。
     */
    fun onAdPaidEvent(paid: AdsPaidEvent) {
        onAdPaidEvent()
    }

    /**
     * 广告被关闭时触发
     * 该回调方法在广告关闭时被调用，通常用于处理广告关闭后的逻辑（如恢复应用界面）。
     */
    fun onAdClosed()

    /**
     * 用户获得奖励时触发
     * 该回调方法在用户通过观看广告获得奖励时触发，通常用于处理奖励逻辑（如增加游戏积分、虚拟物品等）。
     */
    fun onAdUserEarnedReward()

    /**
     * 广告展示失败时触发
     *
     * 当广告已加载成功但展示时出现错误时触发（例如激励广告过期、插屏/开屏重复展示等）。
     * 注意：此回调区分于 [onAdFailedToLoad]（加载阶段失败），仅在 show 阶段失败时调用。
     *
     * @param ad 展示失败的广告对象
     * @param error 错误描述
     * @param errorCode 错误码，可选
     */
    fun onAdFailedToShow(ad: Any, error: String, errorCode: String? = null) = Unit
}
