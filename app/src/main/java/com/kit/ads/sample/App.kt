package com.kit.ads.sample

import android.app.Application
import com.kit.ads.AdManager
import com.kit.ads.provider.AdProviderConfig
import com.kit.ads.provider.AdProviderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            val config = listOf(
                AdProviderConfig(
                    AdProviderType.ADMOB,
                    "ca-app-pub-3940256099942544~3347511713"
                ),
                AdProviderConfig(
                    AdProviderType.APPLOVIN,
                    "05TMDQ5tZabpXQ45_UTbmEGNUtVAzSTzT6KmWQc5_CuWdzccS4DCITZoL3yIWUG3bbq60QC_d4WF28tUC4gVTF"
                )
            )
            AdManager.initialize(this@App, config)
        }
    }
}