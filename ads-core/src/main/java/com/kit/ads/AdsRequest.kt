package com.kit.ads

import com.kit.ads.provider.AdsProviderType

/**
 * 广告请求
 *
 * 封装广告加载所需的参数：触发点标识、广告单元 ID、广告类型和提供商类型。
 * 由调用者构造后传入 [AdsManager.loadAd]。
 */
data class AdsRequest(
    /**
     * 广告触发点标识
     *
     * 唯一标识一个广告位或场景，由开发者自定义（如 "home_banner"、"splash_ad"），
     * 用于区分不同的加载请求。
     */
    val triggerId: String,

    /**
     * 广告单元ID
     *
     * 广告平台上创建的广告单元 ID。
     */
    val adUnitId: String,

    /**
     * 广告类型
     *
     * 定义广告的类型：BANNER / SPLASH / REWARDED。
     */
    val adType: AdsType,

    /**
     * 广告提供商类型
     *
     * 指定广告提供商的类型：ADMOB / APPLOVIN。
     * 必须与 [AdsManager] 当前初始化的提供商类型一致，否则会快速失败并返回 PROVIDER_MISMATCH。
     */
    val providerType: AdsProviderType,

    /**
     * 预加载缓存 TTL（毫秒）。
     *
     * - `null`：使用 [AdsManager.DEFAULT_PRELOAD_TTL_MS]
     * - `<= 0`：不过期（仅消费或 destroy / 切换 Provider 时清除）
     * - `> 0`：自缓存起超过该时长后，[AdsManager.loadAd] 会丢弃缓存并重新网络加载
     */
    val preloadTtlMs: Long? = null,
)
