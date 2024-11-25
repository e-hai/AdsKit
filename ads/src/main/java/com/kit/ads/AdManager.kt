package com.kit.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.kit.ads.AdListener
import com.kit.ads.event.AdEventObserver
import com.kit.ads.event.AdEventObserverManager
import com.kit.ads.event.InitFailure
import com.kit.ads.event.InitSuccess
import com.kit.ads.placement.AdPlacementCall
import com.kit.ads.placement.AdPlacementRequest
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.AdProviderAdapterFactory
import com.kit.ads.provider.AdProviderConfig
import com.kit.ads.provider.AdProviderType


object AdManager {
    const val TAG = "AdManager"


    private val adProviderAdapters: MutableMap<AdProviderType, AdProviderAdapter> = mutableMapOf()
    private val observerManager = AdEventObserverManager()

    /**
     * 初始化所有广告提供商适配器
     * **/
    fun initialize(context: Application) {
        val providerConfigs = listOf(
            AdProviderConfig(AdProviderType.ADMOB, "admob key"),
            AdProviderConfig(
                AdProviderType.APPLOVIN,
                "05TMDQ5tZabpXQ45_UTbmEGNUtVAzSTzT6KmWQc5_CuWdzccS4DCITZoL3yIWUG3bbq60QC_d4WF28tUC4gVTF"
            )
        )
        for (config in providerConfigs) {
            val providerType = config.providerType
            val adapter = AdProviderAdapterFactory.create(providerType)
            adapter.initialize(context, config) {
                val eventType = if (it) InitSuccess(providerType) else InitFailure(providerType)
                observerManager.notifyObservers(eventType)
            }
            adProviderAdapters[providerType] = adapter
        }
    }

    fun openDebug(activity: Activity, providerType: AdProviderType) {
        val adapter = adProviderAdapters[providerType]
        if (null == adapter) {
            Log.e(TAG, "No adapter found for provider: ${providerType.name}")
        } else {
            adapter.openDebug(activity)
        }
    }

    /**
     * 加载广告数据，成功加载后生成广告实体类AdEntity
     * **/
    fun loadAd(activity: Activity, request: AdPlacementRequest, adListener: AdListener) {
        val adapter = adProviderAdapters[request.providerType]
        if (null == adapter) {
            adListener.onAdFailedToLoad("No adapter found for provider: ${request.providerType}")
        } else {
            AdPlacementCall.build(request, adListener, adapter, observerManager).loadAd(activity)
        }
    }


    /**
     * 注册观察者
     **/
    fun registerObserver(observer: AdEventObserver) {
        observerManager.registerObserver(observer)
    }

    /**
     * 取消注册观察者
     **/
    fun unregisterObserver(observer: AdEventObserver) {
        observerManager.unregisterObserver(observer)
    }

}
