package com.kit.ads.provider

/**
 * 广告提供商配置类
 * **/
data class AdProviderConfig(
    val providerType: AdProviderType,
    val apiKey: String
)