package com.kit.ads.provider

/**
 * SPI hook for optional provider modules (ads-admob, ads-applovin).
 * Each provider module ships a META-INF/services entry pointing to its implementation.
 */
interface AdsProviderAdapterRegistrar {
    val providerType: AdsProviderType
    fun create(): AdsProviderAdapter
}
