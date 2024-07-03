package com.an.ads.provider

import android.app.Activity
import android.app.Application
import android.view.ViewGroup
import com.an.ads.placement.AdPlacementRequest

/**
 *广告提供商适配器接口
 * **/
interface AdProviderAdapter {

    fun initialize(
        context: Application,
        config: AdProviderConfig,
        listener: (success: Boolean) -> Unit
    )

    fun openDebug(activity: Activity)

    fun loadAd(
        activity: Activity,
        request: AdPlacementRequest,
        listener: ProviderListener
    )

    fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdPlacementRequest,
        ad: Any,
        listener: ProviderListener
    )
}