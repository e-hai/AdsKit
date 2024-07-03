package com.an.ads.provider

import android.content.Context
import com.an.ads.provider.admob.AdmobProviderAdapter
import com.an.ads.provider.applovin.ApplovinProviderAdapter


/**
 * 广告提供商的工厂类
 * **/
internal object AdProviderAdapterFactory {
    fun create(
        providerType: AdProviderType
    ): AdProviderAdapter {
        return when (providerType) {
            AdProviderType.ADMOB -> AdmobProviderAdapter()
            AdProviderType.APPLOVIN -> ApplovinProviderAdapter()
        }
    }
}

