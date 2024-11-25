package com.kit.ads.provider

import android.content.Context
import com.kit.ads.provider.admob.AdmobProviderAdapter
import com.kit.ads.provider.applovin.ApplovinProviderAdapter


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

