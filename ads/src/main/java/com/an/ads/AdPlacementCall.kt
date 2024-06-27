package com.an.ads

import android.app.Activity
import android.view.ViewGroup
import com.an.ads.provider.AdProviderAdapter
import com.an.ads.provider.ProviderEventListener
import com.an.ads.provider.ProviderRewardListener

/**
 * 广告位加载、展示的执行者
 * **/
class AdPlacementCall private constructor(
    private val request: AdPlacementRequest,
    private val listener: AdListener,
    private val providerAdapter: AdProviderAdapter
) {


    fun loadAd() {
        providerAdapter.loadAd(request, object : ProviderEventListener {
            override fun onAdLoaded(ad: Any) {
                listener.onAdLoaded(AdEntity(this@AdPlacementCall, ad))
            }

            override fun onAdFailedToLoad(error: String) {
                listener.onAdFailedToLoad(error)
            }

            override fun onAdShown() {
                listener.onAdShown()
            }

            override fun onAdClicked() {
                listener.onAdClicked()
            }

            override fun onAdPaidEvent() {
                listener.onAdPaidEvent()
            }

            override fun onAdClosed() {
                listener.onAdClosed()
            }
        })
    }

    fun showAd(
        activity: Activity,
        container: ViewGroup,
        ad: Any
    ) {
        providerAdapter.showAd(activity, container, request, ad, object : ProviderRewardListener {
            override fun onAdUserEarnedReward() {
                listener.onAdUserEarnedReward()
            }
        })
    }

    companion object {
        fun build(
            request: AdPlacementRequest,
            listener: AdListener,
            providerAdapter: AdProviderAdapter
        ): AdPlacementCall {
            return AdPlacementCall(request, listener, providerAdapter)
        }
    }
}