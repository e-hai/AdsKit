package com.kit.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.kit.ads.event.AdEventObserver
import com.kit.ads.event.AdEventObserverManager
import com.kit.ads.event.InitFailure
import com.kit.ads.event.InitSuccess
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.AdProviderAdapterFactory
import com.kit.ads.provider.AdProviderConfig
import com.kit.ads.provider.AdProviderType


object AdManager {
    // 标签，用于日志输出
    const val TAG = "AdManager"

    // 存储广告提供商适配器
    private val adProviderAdapters: MutableMap<AdProviderType, AdProviderAdapter> = mutableMapOf()

    // 事件观察者管理器，用于管理和通知广告事件
    private val observerManager = AdEventObserverManager()

    /**
     * 初始化所有广告提供商适配器
     * @param context 应用的上下文
     * @param providerConfigs 广告提供商的配置列表，外部传入
     */
    fun initialize(context: Application, providerConfigs: List<AdProviderConfig>) {
        // 遍历广告提供商配置，初始化适配器
        providerConfigs.forEach { config ->
            val providerType = config.providerType
            val adapter = AdProviderAdapterFactory.create(providerType)

            // 初始化广告适配器，完成后通知观察者
            adapter.initialize(context, config) { success ->
                val eventType =
                    if (success) InitSuccess(providerType) else InitFailure(providerType)
                observerManager.notifyObservers(eventType)
            }

            // 将适配器加入管理列表
            adProviderAdapters[providerType] = adapter
        }
    }

    /**
     * 开启调试模式，显示广告SDK的调试信息
     * @param activity 当前Activity
     * @param providerType 广告提供商类型
     */
    fun openDebug(activity: Activity, providerType: AdProviderType) {
        adProviderAdapters[providerType]?.openDebug(activity)
            ?: Log.e(TAG, "No adapter found for provider: ${providerType.name}")
    }


    /**
     * 加载广告数据
     * @param activity 当前Activity
     * @param placement 广告触发点请求
     * @param adListener 广告回调监听器
     */
    fun loadAd(activity: Activity, placement: AdPlacement, adListener: AdListener) {
        executeAdPlacement(activity, placement, adListener)
    }

    /**
     * 构建并执行广告加载
     * @param activity 当前Activity
     * @param triggerPoint 广告触发点请求
     * @param adListener 广告回调监听器
     */
    private fun executeAdPlacement(
        activity: Activity,
        placement: AdPlacement,
        adListener: AdListener
    ) {
        val adapter = adProviderAdapters[placement.providerType]

        if (adapter == null) {
            // 如果没有找到对应的适配器，调用失败回调
            adListener.onAdFailedToLoad("No adapter found for provider: ${placement.providerType}")
        } else {
            // 构建并执行广告加载
            AdPlacementExecutor.build(placement, adapter, observerManager)
                .loadAd(activity, adListener)
        }
    }

    /**
     * 注册广告事件观察者
     * 观察者会接收到广告相关事件的通知。
     * @param observer 观察者实例
     */
    fun registerObserver(observer: AdEventObserver) {
        observerManager.registerObserver(observer)
    }

    /**
     * 取消注册广告事件观察者
     * @param observer 观察者实例
     */
    fun unregisterObserver(observer: AdEventObserver) {
        observerManager.unregisterObserver(observer)
    }


}