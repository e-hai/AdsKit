package com.kit.ads.provider.applovin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxAdViewConfiguration
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
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

private fun MaxAd.toPaidEvent(): AdsPaidEvent {
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
        listener: (success: Boolean) -> Unit,
    ) {
        d("initialize start")
        initExecutor.execute {
            try {
                val testDeviceIds = mutableListOf<String>()
                try {
                    AdvertisingIdClient.getAdvertisingIdInfo(context).id
                        ?.takeIf { it.isNotBlank() }
                        ?.let { testDeviceIds.add(it) }
                } catch (e: Exception) {
                    w("gaid read failed error=${e.message}")
                }
                if (testDeviceIds.isNotEmpty()) {
                    d("testDeviceIds=$testDeviceIds")
                }

                val initConfigBuilder = AppLovinSdkInitializationConfiguration.builder(config.apiKey)
                    .setMediationProvider(AppLovinMediationProvider.MAX)
                if (testDeviceIds.isNotEmpty()) {
                    initConfigBuilder.setTestDeviceAdvertisingIds(testDeviceIds)
                }

                val appLovinSdk = AppLovinSdk.getInstance(context)
                appLovinSdk.settings.setVerboseLogging(AdsDebug.isEnabled)
                appLovinSdk.initialize(initConfigBuilder.build()) {
                    d("initialize complete success=true")
                    listener.invoke(true)
                }
            } catch (e: Exception) {
                e("initialize complete success=false error=${e.message}", e)
                listener.invoke(false)
            }
        }
    }

    override fun destroy() {
        d("destroy")
        initExecutor.shutdownNow()
    }

    override fun openDebug(activity: Activity) {
        d("openDebug")
        AppLovinSdk.getInstance(activity).showMediationDebugger()
    }

    override fun loadAd(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        log(request, "loadAd")
        when (request.adType) {
            AdsType.BANNER -> loadBanner(context, request, listener)
            AdsType.SPLASH -> loadSplash(request, listener)
            AdsType.REWARDED -> loadRewarded(request, listener)
            AdsType.INTERSTITIAL -> loadInterstitial(request, listener)
            AdsType.NATIVE -> loadNative( request, listener)
            AdsType.MREC -> loadMrec(context, request, listener)
        }
    }

    private fun loadRewarded(request: AdsRequest, listener: AdsProviderListener) {
        listener.onAdStartedToLoad()
        val ad = MaxRewardedAd.getInstance(request.adUnitId)
        ad.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                log(request, "onAdLoaded")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                log(request, "onAdShown")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                log(request, "onAdClosed")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                log(request, "onAdClicked")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                logLoadFailed(request, error)
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                logDisplayFailed(request, error)
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }

            override fun onUserRewarded(maxAd: MaxAd, maxReward: MaxReward) {
                log(request, "onAdUserEarnedReward", "amount=${maxReward.amount} rewardType=${maxReward.label}")
                listener.onAdUserEarnedReward()
            }
        })
        ad.setRevenueListener { maxAd ->
            logPaid(request, maxAd.toPaidEvent())
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        ad.loadAd()
    }

    private fun loadInterstitial(request: AdsRequest, listener: AdsProviderListener) {
        listener.onAdStartedToLoad()
        val ad = MaxInterstitialAd(request.adUnitId)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                log(request, "onAdLoaded")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                log(request, "onAdShown")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                log(request, "onAdClosed")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                log(request, "onAdClicked")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                logLoadFailed(request, error)
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                logDisplayFailed(request, error)
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }
        })
        ad.setRevenueListener { maxAd ->
            logPaid(request, maxAd.toPaidEvent())
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        ad.loadAd()
    }

    private fun loadNative(
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        listener.onAdStartedToLoad()
        val loader = MaxNativeAdLoader(request.adUnitId)
        loader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                if (nativeAdView != null) {
                    log(request, "onAdLoaded")
                    listener.onAdLoaded(nativeAdView)
                } else {
                    e(
                        "onAdFailedToLoad id=${request.triggerId} unit=${request.adUnitId} type=${request.adType} error=Native ad view is null",
                    )
                    listener.onAdFailedToLoad("Native ad view is null", null)
                }
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                logLoadFailed(request, error)
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                log(request, "onAdClicked")
                listener.onAdClicked()
            }
        })
        loader.setRevenueListener { maxAd ->
            logPaid(request, maxAd.toPaidEvent())
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
        val ad = MaxAdView(request.adUnitId, MaxAdFormat.MREC)
        ad.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                log(request, "onAdLoaded")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                log(request, "onAdShown")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                log(request, "onAdClosed")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                log(request, "onAdClicked")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                logLoadFailed(request, error)
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                logDisplayFailed(request, error)
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }

            override fun onAdExpanded(maxAd: MaxAd) = Unit

            override fun onAdCollapsed(maxAd: MaxAd) = Unit
        })
        ad.setRevenueListener { maxAd ->
            logPaid(request, maxAd.toPaidEvent())
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        val width = AppLovinSdkUtils.dpToPx(context, 300)
        val height = AppLovinSdkUtils.dpToPx(context, 250)
        ad.layoutParams = FrameLayout.LayoutParams(width, height)
        ad.loadAd()
    }

    private fun loadSplash(request: AdsRequest, listener: AdsProviderListener) {
        listener.onAdStartedToLoad()
        val ad = MaxAppOpenAd(request.adUnitId)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                log(request, "onAdLoaded")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                log(request, "onAdShown")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                log(request, "onAdClosed")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                log(request, "onAdClicked")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                logLoadFailed(request, error)
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                logDisplayFailed(request, error)
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }
        })
        ad.setRevenueListener { maxAd ->
            logPaid(request, maxAd.toPaidEvent())
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }
        ad.loadAd()
    }

    private fun loadBanner(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        listener.onAdStartedToLoad()
        val config = MaxAdViewConfiguration.builder()
            .setAdaptiveType(MaxAdViewConfiguration.AdaptiveType.ANCHORED)
            .build()
        val ad = MaxAdView(request.adUnitId, config)
        ad.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
                log(request, "onAdLoaded")
                listener.onAdLoaded(ad)
            }

            override fun onAdDisplayed(maxAd: MaxAd) {
                log(request, "onAdShown")
                listener.onAdShown()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                log(request, "onAdClosed")
                listener.onAdClosed()
            }

            override fun onAdClicked(maxAd: MaxAd) {
                log(request, "onAdClicked")
                listener.onAdClicked()
            }

            override fun onAdLoadFailed(msg: String, error: MaxError) {
                logLoadFailed(request, error)
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                logDisplayFailed(request, error)
                listener.onAdFailedToShow(ad, error.message, "DISPLAY_" + error.code)
            }

            override fun onAdExpanded(maxAd: MaxAd) = Unit

            override fun onAdCollapsed(maxAd: MaxAd) = Unit
        })
        ad.setRevenueListener { maxAd ->
            logPaid(request, maxAd.toPaidEvent())
            listener.onAdPaidEvent(maxAd.toPaidEvent())
        }

        // Adaptive banner: height from device (phone ~50dp, tablet ~90dp, or taller).
        val heightDp = MaxAdFormat.BANNER.getAdaptiveSize(context).height
        val heightPx = AppLovinSdkUtils.dpToPx(context, heightDp)
        d("bannerSize adaptive heightDp=$heightDp")
        ad.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            heightPx,
        )
        ad.loadAd()
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdsRequest,
        ad: Any,
        listener: AdsProviderListener,
    ) {
        log(request, "showAd")
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
                d("destroyAd type=MaxAdView")
            }
            is MaxNativeAdView -> {
                ad.removeAllViews()
                d("destroyAd type=MaxNativeAdView")
            }
        }
    }

    private companion object {
        private const val TAG = "AdsKit-AppLovin"

        fun d(msg: String) = AdsLogger.d(TAG, msg)

        fun w(msg: String) = AdsLogger.w(TAG, msg)

        fun e(msg: String, tr: Throwable? = null) {
            if (tr != null) {
                AdsLogger.e(TAG, msg, tr)
            } else {
                AdsLogger.e(TAG, msg)
            }
        }

        fun log(request: AdsRequest, event: String, extras: String = "") {
            val base =
                "$event id=${request.triggerId} unit=${request.adUnitId} type=${request.adType}"
            d(if (extras.isEmpty()) base else "$base $extras")
        }

        fun logLoadFailed(request: AdsRequest, error: MaxError) {
            e(
                "onAdFailedToLoad id=${request.triggerId} unit=${request.adUnitId} type=${request.adType} error=${error.message} errorCode=${error.code}",
            )
        }

        fun logDisplayFailed(request: AdsRequest, error: MaxError) {
            e(
                "onAdFailedToShow id=${request.triggerId} unit=${request.adUnitId} type=${request.adType} error=${error.message} errorCode=DISPLAY_${error.code}",
            )
        }

        fun logPaid(request: AdsRequest, paid: AdsPaidEvent) {
            d(
                "onAdPaidEvent id=${request.triggerId} unit=${request.adUnitId} type=${request.adType} valueMicros=${paid.valueMicros} currency=${paid.currencyCode} precision=${paid.precision}",
            )
        }
    }
}
