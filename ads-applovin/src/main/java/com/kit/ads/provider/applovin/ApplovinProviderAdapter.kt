package com.kit.ads.provider.applovin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.applovin.sdk.AppLovinSdkUtils
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.kit.ads.AdsDebug
import com.kit.ads.AdsLogger
import com.kit.ads.AdsPaidEvent
import com.kit.ads.AdsRequest
import com.kit.ads.AdsType
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderConfig
import com.kit.ads.provider.AdsProviderListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "AdsKit-AppLovin"

private fun MaxAd.toPaidEvent(): AdsPaidEvent {
    // MAX revenue is in USD; convert to micros for a unified AdsPaidEvent unit.
    val micros = (revenue * 1_000_000.0).toLong()
    return AdsPaidEvent(
        valueMicros = micros,
        currencyCode = "USD",
        precision = revenuePrecision.orEmpty(),
    )
}

internal class ApplovinProviderAdapter : AdsProviderAdapter {

    private val initExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { r -> Thread(r, "AdsKit-AppLovin-Init") }

    override fun initialize(
        context: Application,
        config: AdsProviderConfig,
        listener: (success: Boolean) -> Unit
    ) {
        // AdvertisingIdClient 禁止在主线程调用；GAID 仅用于测试设备标记，失败不影响正式初始化
        initExecutor.execute {
            try {
                val testDeviceIds = mutableListOf<String>()
                try {
                    AdvertisingIdClient.getAdvertisingIdInfo(context).id
                        ?.takeIf { it.isNotBlank() }
                        ?.let { testDeviceIds.add(it) }
                } catch (e: Exception) {
                    AdsLogger.w(TAG, "Failed to read GAID for test devices: ${e.message}")
                }

                val initConfigBuilder = AppLovinSdkInitializationConfiguration.builder(config.apiKey)
                    .setMediationProvider(AppLovinMediationProvider.MAX)
                if (testDeviceIds.isNotEmpty()) {
                    initConfigBuilder.setTestDeviceAdvertisingIds(testDeviceIds)
                }
                val initConfig = initConfigBuilder.build()

                val appLovinSdk = AppLovinSdk.getInstance(context)
                appLovinSdk.settings.setVerboseLogging(AdsDebug.isEnabled)
                appLovinSdk.initialize(initConfig) {
                    AdsLogger.d(TAG, "AppLovin SDK initialized")
                    listener.invoke(true)
                }
            } catch (e: Exception) {
                AdsLogger.e(TAG, "initialize failed error=${e.message}", e)
                listener.invoke(false)
            }
        }
    }

    override fun destroy() {
        initExecutor.shutdownNow()
    }

    override fun openDebug(activity: Activity) {
        AppLovinSdk.getInstance(activity).showMediationDebugger()
    }

    override fun loadAd(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        when (request.adType) {
            AdsType.BANNER -> loadBanner(context, request, listener)
            AdsType.SPLASH -> loadSplash(context, request, listener)
            AdsType.REWARDED -> loadRewarded(context, request, listener)
            AdsType.INTERSTITIAL -> loadInterstitial(context, request, listener)
            AdsType.NATIVE -> loadNative(context, request, listener)
            AdsType.MREC -> loadMrec(context, request, listener)
        }
    }

    private fun loadRewarded(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val ad = MaxRewardedAd.getInstance(request.adUnitId)
        ad.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                AdsLogger.d(TAG, "激励广告加载成功")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                AdsLogger.d(TAG, "激励广告展示成功")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                AdsLogger.d(TAG, "激励广告关闭")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                AdsLogger.d(TAG, "激励广告被点击")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                AdsLogger.e(TAG, "onAdLoadFailed type=rewarded error=${error.message} errorCode=${error.code}")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.e(TAG, "onAdDisplayFailed type=rewarded error=${error.message} errorCode=DISPLAY_${error.code}")
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }

            override fun onUserRewarded(maxAd: MaxAd, maxReward: MaxReward) {
                AdsLogger.d(TAG, "用户获得奖励: amount=${maxReward.amount}")
                listener.onAdUserEarnedReward()
            }
        })
        ad.setRevenueListener { maxAd ->
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        ad.loadAd()
    }

    private fun loadInterstitial(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        listener.onAdStartedToLoad()
        val ad = MaxInterstitialAd(request.adUnitId)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                AdsLogger.d(TAG, "插屏广告加载成功")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                AdsLogger.e(TAG, "onAdLoadFailed type=interstitial error=${error.message} errorCode=${error.code}")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.e(TAG, "onAdDisplayFailed type=interstitial error=${error.message} errorCode=DISPLAY_${error.code}")
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }
        })
        ad.setRevenueListener { maxAd ->
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        ad.loadAd()
    }

    private fun loadNative(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        listener.onAdStartedToLoad()
        val loader = MaxNativeAdLoader(request.adUnitId, context)
        loader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                if (nativeAdView != null) {
                    AdsLogger.d(TAG, "原生广告加载成功")
                    listener.onAdLoaded(nativeAdView)
                } else {
                    listener.onAdFailedToLoad("Native ad view is null", null)
                }
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                AdsLogger.e(TAG, "onAdLoadFailed type=native error=${error.message} errorCode=${error.code}")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                listener.onAdClicked()
            }
        })
        loader.setRevenueListener { maxAd ->
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        loader.loadAd()
    }

    private fun loadMrec(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        listener.onAdStartedToLoad()
        val ad = MaxAdView(request.adUnitId, MaxAdFormat.MREC, context)
        ad.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                AdsLogger.d(TAG, "MREC 广告加载成功")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                AdsLogger.e(TAG, "onAdLoadFailed type=mrec error=${error.message} errorCode=${error.code}")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.e(TAG, "onAdDisplayFailed type=mrec error=${error.message} errorCode=DISPLAY_${error.code}")
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }

            override fun onAdExpanded(maxAd: MaxAd) = Unit

            override fun onAdCollapsed(maxAd: MaxAd) = Unit
        })
        ad.setRevenueListener { maxAd ->
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        val width = AppLovinSdkUtils.dpToPx(context, 300)
        val height = AppLovinSdkUtils.dpToPx(context, 250)
        ad.layoutParams = FrameLayout.LayoutParams(width, height)
        ad.loadAd()
    }

    private fun loadSplash(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val ad = MaxAppOpenAd(request.adUnitId)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                AdsLogger.d(TAG, "开屏广告加载成功")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                AdsLogger.d(TAG, "开屏广告展示成功")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                AdsLogger.d(TAG, "开屏广告关闭")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                AdsLogger.d(TAG, "开屏广告被点击")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                AdsLogger.e(TAG, "onAdLoadFailed type=splash error=${error.message} errorCode=${error.code}")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.e(TAG, "onAdDisplayFailed type=splash error=${error.message} errorCode=DISPLAY_${error.code}")
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }
        })
        ad.setRevenueListener { maxAd ->
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        ad.loadAd()
    }

    private fun loadBanner(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val ad = MaxAdView(request.adUnitId)
        ad.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                AdsLogger.d(TAG, "横幅广告加载成功")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                AdsLogger.d(TAG, "横幅广告展示成功")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                AdsLogger.d(TAG, "横幅广告关闭")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                AdsLogger.d(TAG, "横幅广告被点击")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                AdsLogger.e(TAG, "onAdLoadFailed type=banner error=${error.message} errorCode=${error.code}")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.e(TAG, "onAdDisplayFailed type=banner error=${error.message} errorCode=DISPLAY_${error.code}")
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }

            override fun onAdExpanded(maxAd: MaxAd) {
            }

            override fun onAdCollapsed(maxAd: MaxAd) {
            }
        })
        ad.setRevenueListener { maxAd ->
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }

        // 横幅广告宽度铺满屏幕以保证正常展示
        // 宽充满屏幕，高度50dp为最理想比例
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val heightPx = AppLovinSdkUtils.dpToPx(context, 50)
        ad.layoutParams = FrameLayout.LayoutParams(width, heightPx)
        ad.loadAd()
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdsRequest,
        ad: Any,
        listener: AdsProviderListener
    ) {
        when (request.adType) {
            AdsType.SPLASH -> showSplash(ad as MaxAppOpenAd)
            AdsType.BANNER, AdsType.MREC -> showBanner(container, ad as MaxAdView)
            AdsType.NATIVE -> showNative(container, ad as MaxNativeAdView)
            AdsType.REWARDED -> showRewarded(activity, ad as MaxRewardedAd)
            AdsType.INTERSTITIAL -> showInterstitial(activity, ad as MaxInterstitialAd)
        }
    }

    private fun showInterstitial(activity: Activity, ad: MaxInterstitialAd) {
        ad.showAd(activity)
    }

    private fun showNative(container: ViewGroup, ad: MaxNativeAdView) {
        if (ad.parent != null) {
            (ad.parent as ViewGroup).removeView(ad)
        }
        container.removeAllViews()
        container.addView(ad)
    }

    private fun showRewarded(activity: Activity, maxRewardedAd: MaxRewardedAd) {
        maxRewardedAd.showAd(activity)
    }

    private fun showBanner(container: ViewGroup, ad: MaxAdView) {
        if (ad.parent != null) {
            (ad.parent as ViewGroup).removeView(ad)
        }
        container.addView(ad)
    }

    private fun showSplash(ad: MaxAppOpenAd) {
        ad.showAd()
    }

    override fun destroyAd(ad: Any) {
        when (ad) {
            is MaxAdView -> {
                ad.removeAllViews()
                ad.setListener(null)
                ad.destroy()

                AdsLogger.d(TAG, "AppLovin MaxAdView destroyed")
            }
            is MaxNativeAdView -> {
                ad.removeAllViews()
                AdsLogger.d(TAG, "AppLovin MaxNativeAdView destroyed")
            }
            // MaxRewardedAd / MaxInterstitialAd / MaxAppOpenAd 由 SDK 管理
        }
    }
}
