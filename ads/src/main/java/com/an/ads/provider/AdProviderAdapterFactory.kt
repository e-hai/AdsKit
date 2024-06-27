package com.an.ads.provider

import android.content.Context
import com.an.ads.AdProviderType
import com.an.ads.provider.admob.AdmobProviderAdapter
import com.an.ads.provider.applovin.ApplovinProviderAdapter


/**
 * 广告提供商的工厂类
 * **/
internal object AdProviderAdapterFactory {
    fun create(context: Context,config: AdProviderConfig): AdProviderAdapter {
        return when (config.providerType) {
            AdProviderType.ADMOB -> AdmobProviderAdapter(context)
            AdProviderType.APPLOVIN -> ApplovinProviderAdapter(context)
        }
    }
}

