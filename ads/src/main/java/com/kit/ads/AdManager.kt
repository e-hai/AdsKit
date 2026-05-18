package com.kit.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.kit.ads.event.AdEventObserver
import com.kit.ads.event.AdEventObserverManager
import com.kit.ads.event.InitFailure
import com.kit.ads.event.InitSuccess
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.AdProviderAdapterFactory
import com.kit.ads.provider.AdProviderConfig


object AdManager {
    // 标签，用于日志输出
    const val TAG = "AdManager"

    // 存储唯一的广告提供商适配器
    private var adProviderAdapter: AdProviderAdapter? = null

    // 事件观察者管理器，用于管理和通知广告事件
    private val observerManager = AdEventObserverManager()

    /**
     * 初始化广告提供商适配器
     * @param context 应用的上下文
     * @param config 广告提供商的配置
     */
    fun initialize(context: Application, config: AdProviderConfig) {
        val providerType = config.providerType
        val adapter = AdProviderAdapterFactory.create(providerType)

        // 初始化广告适配器，完成后通知观察者
        adapter.initialize(context, config) { success ->
            val eventType =
                if (success) InitSuccess(providerType)
                else InitFailure(providerType)
            observerManager.notifyObservers(eventType)
        }

        // 保存唯一的适配器
        adProviderAdapter = adapter
    }

    /**
     * 开启调试模式，显示广告SDK的调试信息
     * @param activity 当前Activity
     */
    fun openDebug(activity: Activity) {
        adProviderAdapter?.openDebug(activity)
            ?: Log.e(TAG, "No adapter initialized. Please call initialize first.")
    }


    /**
     * 加载广告数据
     * @param context 上下文
     * @param placement 广告触发点请求
     * @param adListener 广告回调监听器
     */
    fun loadAd(context: Context, placement: AdPlacement, adListener: AdListener) {
        executeAdPlacement(context, placement, adListener)
    }

    /**
     * 构建并执行广告加载
     * @param context 上下文
     * @param placement 广告触发点请求
     * @param adListener 广告回调监听器
     */
    private fun executeAdPlacement(
        context: Context,
        placement: AdPlacement,
        adListener: AdListener
    ) {
        val adapter = adProviderAdapter

        if (adapter == null) {
            // 如果没有初始化适配器，调用失败回调
            adListener.onAdFailedToLoad("AdManager not initialized. No adapter found.")
        } else {
            // 使用当前已初始化的适配器执行广告加载
            AdPlacementExecutor.build(placement, adapter, observerManager)
                .loadAd(context, adListener)
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
