package com.kit.ads

import android.app.Activity
import android.util.Log
import com.kit.ads.AdManager.TAG
import com.kit.ads.event.AdClicked
import com.kit.ads.event.AdClosed
import com.kit.ads.event.AdEventObserverManager
import com.kit.ads.event.AdPaidEvent
import com.kit.ads.event.AdRewarded
import com.kit.ads.event.AdShown
import com.kit.ads.event.LoadAdFailure
import com.kit.ads.event.LoadAdStart
import com.kit.ads.event.LoadAdSuccess
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.ProviderListener

/**
 * 广告触发点执行器
 *
 * 该类用于处理广告位的加载。通过封装广告请求和适配器，协调广告的加载、展示、点击等过程。
 * 它接收广告位请求并通知外部，负责在指定广告提供商的SDK中加载。
 */
class AdPlacementExecutor private constructor(
    private val placement: AdPlacement,  // 广告触发点请求配置
    private val providerAdapter: AdProviderAdapter,  // 广告提供商适配器
    private val observerManager: AdEventObserverManager  // 广告事件观察者管理器
) {


    /**
     * 加载广告
     *
     * 调用广告提供商适配器加载广告资源，并通过 `providerListener` 监听广告加载的过程。
     *
     * @param activity 当前的 Activity，广告加载时的上下文。
     */
    fun loadAd(activity: Activity, listener: AdListener) {
        // 提供商回调监听器
        // 该监听器负责接收广告提供商SDK的各类事件回调，如广告加载、展示、点击、关闭等。
        val providerListener = object : ProviderListener {

            override fun onAdStartedToLoad() {
                Log.d(TAG, "广告开始加载")
                observerManager.notifyObservers(LoadAdStart(placement))  // 通知观察者广告开始加载
            }

            override fun onAdLoaded(ad: Any) {
                Log.d(TAG, "广告加载成功")
                val adEntity = AdEntity(providerAdapter, placement, this, ad)
                listener.onAdLoaded(adEntity)  // 通知外部广告加载成功
                observerManager.notifyObservers(LoadAdSuccess(placement))  // 通知观察者广告加载成功
            }

            override fun onAdFailedToLoad(error: String) {
                Log.d(TAG, "广告加载失败")
                listener.onAdFailedToLoad(error)  // 通知外部广告加载失败
                observerManager.notifyObservers(LoadAdFailure(placement))  // 通知观察者广告加载失败
            }

            override fun onAdShown() {
                Log.d(TAG, "广告被展示")
                listener.onAdShown()  // 通知外部广告已展示
                observerManager.notifyObservers(AdShown(placement))  // 通知观察者广告已展示
            }

            override fun onAdClicked() {
                Log.d(TAG, "广告被点击")
                listener.onAdClicked()  // 通知外部广告被点击
                observerManager.notifyObservers(AdClicked(placement))  // 通知观察者广告被点击
            }

            override fun onAdPaidEvent() {
                Log.d(TAG, "广告预计获得的收益")
                listener.onAdPaidEvent()  // 通知外部广告收益事件
                observerManager.notifyObservers(AdPaidEvent(placement))  // 通知观察者广告收益事件
            }

            override fun onAdClosed() {
                Log.d(TAG, "广告被关闭")
                listener.onAdClosed()  // 通知外部广告已关闭
                observerManager.notifyObservers(AdClosed(placement))  // 通知观察者广告已关闭
            }

            override fun onAdUserEarnedReward() {
                Log.d(TAG, "用户获得奖励")
                listener.onAdUserEarnedReward()  // 通知外部用户已获得奖励
                observerManager.notifyObservers(AdRewarded(placement))  // 通知观察者用户获得奖励
            }
        }
        providerAdapter.loadAd(activity, placement, providerListener)  // 使用适配器加载广告
    }


    companion object {
        /**
         * 构建 `AdPlacementExecutor` 实例
         *
         * 该方法用于构建 `AdPlacementExecutor` 对象，提供广告触发点请求、监听器、广告提供商适配器等参数。
         *
         * @param placement 广告触发点请求，包含广告的配置和触发点信息。
         * @param providerAdapter 广告提供商适配器，用于加载和展示广告。
         * @param eventObserverManager 广告事件观察者管理器，用于管理广告事件的观察者。
         * @return `AdPlacementExecutor` 实例。
         */
        fun build(
            placement: AdPlacement,
            providerAdapter: AdProviderAdapter,
            eventObserverManager: AdEventObserverManager
        ): AdPlacementExecutor {
            return AdPlacementExecutor(placement, providerAdapter, eventObserverManager)
        }
    }
}
