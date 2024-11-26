package com.kit.ads

/**
 * 广告监听器接口
 *
 * 该接口定义了广告加载、展示、点击、奖励等一系列广告事件的回调方法。
 * 开发者可以通过实现该接口，监听广告生命周期中的不同阶段，并在相应的阶段执行自定义操作。
 */
interface AdListener {

    /**
     * 广告加载完成时触发
     *
     * 在广告成功加载后会触发该回调方法，开发者可以在此时获取到加载完成的广告实体（AdEntity）。
     *
     * @param ad 加载完成的广告实体
     */
    fun onAdLoaded(ad: AdEntity)

    /**
     * 广告加载失败时触发
     *
     * 当广告加载失败时会触发该回调方法，开发者可以在此处理加载失败的逻辑，如重试加载或显示错误信息。
     *
     * @param error 错误信息，描述加载失败的原因
     */
    fun onAdFailedToLoad(error: String)

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
     * 广告付费事件触发时调用
     *
     * 当广告提供商报告广告收入或收益事件时，会触发该回调方法。开发者可以在此记录广告的收益事件或进行相应的逻辑处理。
     */
    fun onAdPaidEvent()

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
}

/**
 * 广告回调抽象类
 *
 * 该抽象类提供了 `AdListener` 接口的默认实现，开发者可以继承该类并重写需要的回调方法。
 * 如果开发者只关心部分广告事件（例如，只关心广告加载完成或广告点击），可以继承该类并覆盖相应的方法，避免实现不需要的回调。
 */
abstract class AdCallback : AdListener {

    override fun onAdLoaded(ad: AdEntity) {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdFailedToLoad(error: String) {
        // 默认实现，子类可以选择重写该方法
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

    override fun onAdUserEarnedReward() {
        // 默认实现，子类可以选择重写该方法
    }

    override fun onAdClosed() {
        // 默认实现，子类可以选择重写该方法
    }
}
