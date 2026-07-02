package com.kit.ads.sample

import android.app.Application
import com.kit.ads.AdsManager
import com.kit.ads.provider.AdsProviderConfig
import com.kit.ads.provider.AdsProviderType

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        val adMobConfig =
            AdsProviderConfig(
                AdsProviderType.ADMOB,
                "ca-app-pub-3940256099942544~3347511713"
            )
        AdsManager.initialize(this@App, adMobConfig)
    }
}
