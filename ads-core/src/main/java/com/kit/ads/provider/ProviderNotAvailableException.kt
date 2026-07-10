package com.kit.ads.provider

class ProviderNotAvailableException(
    val providerType: AdsProviderType,
) : IllegalStateException(
    "Provider $providerType is not on the classpath. " +
        "Add the matching AdsKit provider artifact (AdsKit-admob or AdsKit-applovin)."
)
