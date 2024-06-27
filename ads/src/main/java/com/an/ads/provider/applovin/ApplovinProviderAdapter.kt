package com.an.ads.provider.applovin

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.an.ads.AdPlacementRequest
import com.an.ads.provider.AdProviderAdapter
import com.an.ads.provider.AdProviderConfig
import com.an.ads.provider.ProviderEventListener
import com.an.ads.provider.ProviderRewardListener

internal class ApplovinProviderAdapter(context: Context) : AdProviderAdapter {
    override fun initialize(config: AdProviderConfig) {
    }

    override fun loadAd(request: AdPlacementRequest, listener: ProviderEventListener) {
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdPlacementRequest,
        ad: Any,
        listener: ProviderRewardListener
    ) {
    }
}