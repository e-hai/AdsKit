package com.an.ads

import android.content.Context
import com.an.ads.provider.AdProviderAdapter
import com.an.ads.provider.AdProviderAdapterFactory
import com.an.ads.provider.AdProviderConfig


object AdManager {

    private val adProviderAdapters: MutableMap<AdProviderType, AdProviderAdapter> = mutableMapOf()

    /**
     * 初始化所有广告提供商适配器
     * **/
    fun initialize(context: Context) {
        val providerConfigs = listOf(
            AdProviderConfig(AdProviderType.ADMOB, "admob key"),
            AdProviderConfig(AdProviderType.APPLOVIN, "applovin key")
        )
        for (config in providerConfigs) {
            val adapter = AdProviderAdapterFactory.create(context, config)
            adapter.initialize(config)
            adProviderAdapters[config.providerType] = adapter
        }
    }

    /**
     * 加载广告数据，成功加载后生成广告实体类AdEntity
     * **/
    fun loadAd(request: AdPlacementRequest, adListener: AdListener) {
        val adapter = adProviderAdapters[request.providerType]
        if (null == adapter) {
            adListener.onAdFailedToLoad("No adapter found for provider: ${request.providerType}")
        } else {
            AdPlacementCall.build(request, adListener, adapter).loadAd()
        }
    }

}
