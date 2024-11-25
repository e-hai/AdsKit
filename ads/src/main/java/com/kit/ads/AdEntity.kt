package com.kit.ads

import android.app.Activity
import android.view.ViewGroup
import com.kit.ads.placement.AdPlacementCall


/**
 * 广告实体类
 * **/
class AdEntity(
    private val call: AdPlacementCall,
    private val ad: Any
) {
    fun show(
        activity: Activity,
        container: ViewGroup
    ) {
        call.showAd(activity, container, ad)
    }
}