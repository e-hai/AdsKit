package com.kit.ads.provider

import com.kit.ads.provider.admob.AdmobProviderAdapter
import com.kit.ads.provider.applovin.ApplovinProviderAdapter


/**
 * 广告提供商的工厂类
 * **/
internal object AdsProviderAdapterFactory {
    fun create(
        providerType: AdsProviderType
    ): AdsProviderAdapter {
        return when (providerType) {
            AdsProviderType.ADMOB -> AdmobProviderAdapter()
            AdsProviderType.APPLOVIN -> ApplovinProviderAdapter()
        }
    }
}

