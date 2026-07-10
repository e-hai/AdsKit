package com.kit.ads

/**
 * 广告监听器接口
 *
 * 该接口定义了广告加载、展示、点击、奖励等一系列广告事件的回调方法。
 * 开发者可以通过实现该接口，监听广告生命周期中的不同阶段，并在相应的阶段执行自定义操作。
 */
interface AdsListener {

    /**
     * 广告加载完成时触发
     *
     * 在广告成功加载后会触发该回调方法，开发者可以在此时获取到加载完成的广告实体（AdsEntity）。
     *
     * @param ad 加载完成的广告实体
     */
    fun onAdLoaded(ad: AdsEntity)

    /**
     * 广告加载失败时触发
     *
     * 当广告加载失败时会触发该回调方法，开发者可以在此处理加载失败的逻辑，如重试加载或显示错误信息。
     *
     * @param error 错误信息，描述加载失败的原因
     */
    fun onAdFailedToLoad(error: String)

    /**
     * 广告加载失败时触发（含错误码）
     *
     * 当广告加载失败时触发，相比 [onAdFailedToLoad] 多提供了 errorCode，可用于区分不同的失败原因。
     * 默认实现直接委派给 [onAdFailedToLoad]，因此不重写此方法也能保持向后兼容。
     *
     * @param error 错误信息，描述加载失败的原因
     * @param errorCode 错误码，用于区分失败类型（如 PROVIDER_MISMATCH、STATE_IDLE、SDK 原始错误码等）
     */
    fun onAdFailedToLoad(error: String, errorCode: String?) {
        onAdFailedToLoad(error)
    }

    /**
     * 广告展示时触发
     *
     * 当广告成功展示给用户时，会触发该回调方法。开发者可以在此处理广告展示后的逻辑，例如统计展示次数。
     */
    fun onAdShown()

    /**
     * 广告点击时触发
     *
     * 当用户点击广告时，会触发该回调方法。开发者可以在此处理广告点击事件，如打开目标链接等操作。
     */
    fun onAdClicked()

    /**
     * 广告付费事件触发时调用（无收益明细，保持向后兼容）
     */
    fun onAdPaidEvent()

    /**
     * 广告付费事件触发时调用（含收益明细）
     *
     * 默认委派到无参 [onAdPaidEvent]，旧代码无需改动。
     */
    fun onAdPaidEvent(paid: AdsPaidEvent) {
        onAdPaidEvent()
    }

    /**
     * 用户通过广告获得奖励时触发
     *
     * 当用户观看广告并获得奖励（如积分、虚拟物品等）时，会触发该回调方法。
     * 开发者可以在此处理奖励逻辑，如增加游戏积分、虚拟物品等。
     */
    fun onAdUserEarnedReward()

    /**
     * 广告关闭时触发
     *
     * 当广告关闭时会触发该回调方法，开发者可以在此进行广告关闭后的处理逻辑，例如恢复应用界面等。
     */
    fun onAdClosed()

    /**
     * 广告开始加载时触发
     *
     * 当广告开始加载时会触发此回调方法，开发者可以在此处理加载开始时的逻辑（如显示加载动画）。
     * 此回调对应 [ProviderListener.onAdStartedToLoad]，通过 [AdsLoadHandler] 转发至此接口。
     */
    fun onAdStartedToLoad() = Unit

    /**
     * 广告展示失败时触发
     *
     * 广告已加载成功，但在 [AdsEntity.show] 阶段失败时触发（如激励广告过期、重复展示等）。
     * 默认空实现，保持向后兼容。错误码可能带 `DISPLAY_` 前缀（AppLovin）。
     *
     * @param error 错误描述
     * @param errorCode 错误码，可选
     */
    fun onAdFailedToShow(error: String, errorCode: String? = null) = Unit
}

/**
 * 广告回调抽象类
 *
 * 该抽象类提供了 `AdsListener` 接口的默认实现，开发者可以继承该类并重写需要的回调方法。
 * 如果开发者只关心部分广告事件（例如，只关心广告加载完成或广告点击），可以继承该类并覆盖相应的方法，避免实现不需要的回调。
 */
abstract class AdCallback : AdsListener {

    override fun onAdLoaded(ad: AdsEntity) {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdFailedToLoad(error: String) {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdFailedToLoad(error: String, errorCode: String?) {
        super.onAdFailedToLoad(error, errorCode)
    }

    override fun onAdShown() {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdClicked() {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdPaidEvent() {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdPaidEvent(paid: AdsPaidEvent) {
        super.onAdPaidEvent(paid)
    }

    override fun onAdUserEarnedReward() {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdClosed() {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdFailedToShow(error: String, errorCode: String?) {
        // 默认实现，子类可以选择重写该方法
    }
}
