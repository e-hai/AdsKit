package com.an.ads.provider

import com.an.ads.AdProviderType

/**
 * 广告提供商配置类
 * **/
data class AdProviderConfig(
    val providerType: AdProviderType,
    val apiKey: String
)