package com.kit.ads.provider.applovin

import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderAdapterRegistrar
import com.kit.ads.provider.AdsProviderType

class ApplovinProviderAdapterRegistrar : AdsProviderAdapterRegistrar {
    override val providerType: AdsProviderType = AdsProviderType.APPLOVIN

    override fun create(): AdsProviderAdapter = ApplovinProviderAdapter()
}
