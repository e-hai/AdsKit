package com.kit.ads.provider

/**
 * 广告提供商配置类
 *
 * @param providerType 广告提供商类型
 * @param apiKey AdMob App ID 或 AppLovin SDK Key
 */
data class AdsProviderConfig(
    val providerType: AdsProviderType,
    val apiKey: String,
)
