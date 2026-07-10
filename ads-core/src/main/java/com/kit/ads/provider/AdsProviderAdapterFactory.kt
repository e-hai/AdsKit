package com.kit.ads.provider

import java.util.ServiceLoader

/**
 * Resolves provider adapters from optional SPI modules on the classpath.
 */
internal object AdsProviderAdapterFactory {
    private val registrars: Map<AdsProviderType, AdsProviderAdapterRegistrar> by lazy {
        ServiceLoader.load(AdsProviderAdapterRegistrar::class.java)
            .associateBy { it.providerType }
    }

    fun isAvailable(providerType: AdsProviderType): Boolean =
        registrars.containsKey(providerType)

    fun create(providerType: AdsProviderType): AdsProviderAdapter {
        val registrar = registrars[providerType]
            ?: throw ProviderNotAvailableException(providerType)
        return registrar.create()
    }
}
