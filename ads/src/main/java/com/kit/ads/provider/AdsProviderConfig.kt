package com.kit.ads.provider

/**
 * 广告提供商配置类
 * **/
data class AdsProviderConfig(
    val providerType: AdsProviderType,
    val apiKey: String
)