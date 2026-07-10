package com.kit.ads.sample

import com.kit.ads.AdsRequest
import com.kit.ads.AdsType
import com.kit.ads.provider.AdsProviderConfig
import com.kit.ads.provider.AdsProviderType

/**
 * Sample 广告位与 Provider 配置集中管理。
 * App ID / SDK Key 来自 local.properties（经 BuildConfig 注入，与 Manifest meta-data 一致）。
 */
object SampleConfig {

    // === Provider API Keys（BuildConfig ← local.properties）===
    val ADMOB_APP_ID: String = BuildConfig.ADMOB_APP_ID
    val APPLOVIN_SDK_KEY: String = BuildConfig.APPLOVIN_SDK_KEY

    // === triggerId：预加载缓存键，每个场景使用唯一值 ===
    object TriggerId {
        const val BANNER = "demo_banner"
        const val SPLASH = "demo_splash"
        const val REWARDED = "demo_rewarded"
        const val INTERSTITIAL = "demo_interstitial"
        const val NATIVE = "demo_native"
        const val MREC = "demo_mrec"
        const val PRELOAD_BANNER = "preload_banner"
    }

    // === Google 官方测试广告位 ===
    object AdMobUnit {
        const val BANNER = "ca-app-pub-3940256099942544/9214589741"
        const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val SPLASH = "ca-app-pub-3940256099942544/9257395921"
        const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
        const val MREC = "ca-app-pub-3940256099942544/9214589741"
    }

    // === AppLovin MAX 广告位（local.properties 或下方占位符）===
    object AppLovinUnit {
        private const val PLACEHOLDER_BANNER = "YOUR_MAX_BANNER_AD_UNIT_ID"
        private const val PLACEHOLDER_REWARDED = "YOUR_MAX_REWARDED_AD_UNIT_ID"
        private const val PLACEHOLDER_SPLASH = "YOUR_MAX_APP_OPEN_AD_UNIT_ID"
        private const val PLACEHOLDER_INTERSTITIAL = "YOUR_MAX_INTERSTITIAL_AD_UNIT_ID"
        private const val PLACEHOLDER_NATIVE = "YOUR_MAX_NATIVE_AD_UNIT_ID"
        private const val PLACEHOLDER_MREC = "YOUR_MAX_MREC_AD_UNIT_ID"

        val BANNER: String = BuildConfig.APPLOVIN_BANNER_UNIT.ifEmpty { PLACEHOLDER_BANNER }
        val REWARDED: String = BuildConfig.APPLOVIN_REWARDED_UNIT.ifEmpty { PLACEHOLDER_REWARDED }
        val SPLASH: String = BuildConfig.APPLOVIN_SPLASH_UNIT.ifEmpty { PLACEHOLDER_SPLASH }
        val INTERSTITIAL: String = BuildConfig.APPLOVIN_INTERSTITIAL_UNIT.ifEmpty { PLACEHOLDER_INTERSTITIAL }
        val NATIVE: String = BuildConfig.APPLOVIN_NATIVE_UNIT.ifEmpty { PLACEHOLDER_NATIVE }
        val MREC: String = BuildConfig.APPLOVIN_MREC_UNIT.ifEmpty { PLACEHOLDER_MREC }

        fun hasRealUnits(): Boolean =
            listOf(BANNER, REWARDED, SPLASH, INTERSTITIAL, NATIVE, MREC)
                .none { it.startsWith("YOUR_MAX_") }
    }

    fun providerConfig(provider: AdsProviderType): AdsProviderConfig = when (provider) {
        AdsProviderType.ADMOB -> AdsProviderConfig(AdsProviderType.ADMOB, ADMOB_APP_ID)
        AdsProviderType.APPLOVIN -> AdsProviderConfig(AdsProviderType.APPLOVIN, APPLOVIN_SDK_KEY)
    }

    fun request(
        provider: AdsProviderType,
        adType: AdsType,
        triggerId: String = triggerIdFor(adType),
    ): AdsRequest = AdsRequest(
        triggerId = triggerId,
        adUnitId = adUnitId(provider, adType),
        adType = adType,
        providerType = provider,
    )

    fun preloadBannerRequest(provider: AdsProviderType): AdsRequest =
        request(provider, AdsType.BANNER, TriggerId.PRELOAD_BANNER)

    private fun triggerIdFor(adType: AdsType): String = when (adType) {
        AdsType.BANNER -> TriggerId.BANNER
        AdsType.SPLASH -> TriggerId.SPLASH
        AdsType.REWARDED -> TriggerId.REWARDED
        AdsType.INTERSTITIAL -> TriggerId.INTERSTITIAL
        AdsType.NATIVE -> TriggerId.NATIVE
        AdsType.MREC -> TriggerId.MREC
    }

    private fun adUnitId(provider: AdsProviderType, adType: AdsType): String = when (provider) {
        AdsProviderType.ADMOB -> when (adType) {
            AdsType.BANNER -> AdMobUnit.BANNER
            AdsType.REWARDED -> AdMobUnit.REWARDED
            AdsType.SPLASH -> AdMobUnit.SPLASH
            AdsType.INTERSTITIAL -> AdMobUnit.INTERSTITIAL
            AdsType.NATIVE -> AdMobUnit.NATIVE
            AdsType.MREC -> AdMobUnit.MREC
        }
        AdsProviderType.APPLOVIN -> when (adType) {
            AdsType.BANNER -> AppLovinUnit.BANNER
            AdsType.REWARDED -> AppLovinUnit.REWARDED
            AdsType.SPLASH -> AppLovinUnit.SPLASH
            AdsType.INTERSTITIAL -> AppLovinUnit.INTERSTITIAL
            AdsType.NATIVE -> AppLovinUnit.NATIVE
            AdsType.MREC -> AppLovinUnit.MREC
        }
    }
}
