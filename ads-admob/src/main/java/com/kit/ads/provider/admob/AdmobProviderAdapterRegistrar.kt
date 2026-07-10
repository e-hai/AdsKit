package com.kit.ads.provider.admob

import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderAdapterRegistrar
import com.kit.ads.provider.AdsProviderType

class AdmobProviderAdapterRegistrar : AdsProviderAdapterRegistrar {
    override val providerType: AdsProviderType = AdsProviderType.ADMOB

    override fun create(): AdsProviderAdapter = AdmobProviderAdapter()
}
