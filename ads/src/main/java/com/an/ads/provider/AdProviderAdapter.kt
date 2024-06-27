package com.an.ads.provider

import android.app.Activity
import android.view.ViewGroup
import com.an.ads.AdEntity
import com.an.ads.AdPlacementCall
import com.an.ads.AdPlacementRequest

/**
 *广告提供商适配器接口
 * **/
interface AdProviderAdapter {

    fun initialize(config: AdProviderConfig)

    fun loadAd(
        request: AdPlacementRequest,
        listener: ProviderEventListener
    )

    fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdPlacementRequest,
        ad: Any,
        listener: ProviderRewardListener
    )
}