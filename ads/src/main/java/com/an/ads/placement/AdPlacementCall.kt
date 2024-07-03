package com.an.ads.placement

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import com.an.ads.AdEntity
import com.an.ads.AdListener
import com.an.ads.AdManager.TAG
import com.an.ads.event.AdClicked
import com.an.ads.event.AdClosed
import com.an.ads.event.AdEventObserverManager
import com.an.ads.event.AdPaidEvent
import com.an.ads.event.AdRewarded
import com.an.ads.event.AdShown
import com.an.ads.event.LoadAdFailure
import com.an.ads.event.LoadAdStart
import com.an.ads.event.LoadAdSuccess
import com.an.ads.provider.AdProviderAdapter
import com.an.ads.provider.ProviderListener

/**
 * 广告位加载、展示的执行者
 * **/
class AdPlacementCall private constructor(
    private val request: AdPlacementRequest,
    private val listener: AdListener,
    private val providerAdapter: AdProviderAdapter,
    private val observerManager: AdEventObserverManager
) {

    private val providerListener = object : ProviderListener {
        override fun onAdStartedToLoad() {
            Log.d(TAG, "广告开始加载")
            observerManager.notifyObservers(LoadAdStart(request))
        }

        override fun onAdLoaded(ad: Any) {
            Log.d(TAG, "广告加载成功")
            listener.onAdLoaded(AdEntity(this@AdPlacementCall, ad))
            observerManager.notifyObservers(LoadAdSuccess(request))
        }

        override fun onAdFailedToLoad(error: String) {
            Log.d(TAG, "广告加载失败")
            listener.onAdFailedToLoad(error)
            observerManager.notifyObservers(LoadAdFailure(request))
        }

        override fun onAdShown() {
            Log.d(TAG, "广告被展示")
            listener.onAdShown()
            observerManager.notifyObservers(AdShown(request))
        }

        override fun onAdClicked() {
            Log.d(TAG, "广告被点击")
            listener.onAdClicked()
            observerManager.notifyObservers(AdClicked(request))
        }

        override fun onAdPaidEvent() {
            Log.d(TAG, "广告预计获得的收益")
            listener.onAdPaidEvent()
            observerManager.notifyObservers(AdPaidEvent(request))
        }

        override fun onAdClosed() {
            Log.d(TAG, "广告被关闭")
            listener.onAdClosed()
            observerManager.notifyObservers(AdClosed(request))
        }

        override fun onAdUserEarnedReward() {
            Log.d(TAG, "用户获得奖励")
            listener.onAdUserEarnedReward()
            observerManager.notifyObservers(AdRewarded(request))
        }
    }


    fun loadAd(activity: Activity) {
        providerAdapter.loadAd(activity, request, providerListener)
    }

    fun showAd(
        activity: Activity,
        container: ViewGroup,
        ad: Any
    ) {
        providerAdapter.showAd(activity, container, request, ad, providerListener)
    }

    companion object {
        fun build(
            request: AdPlacementRequest,
            listener: AdListener,
            providerAdapter: AdProviderAdapter,
            eventObserverManager: AdEventObserverManager
        ): AdPlacementCall {
            return AdPlacementCall(request, listener, providerAdapter, eventObserverManager)
        }
    }
}