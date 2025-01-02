package com.kit.ads.provider.applovin

import android.app.Activity
import android.app.Application
import android.view.ViewGroup
import android.widget.FrameLayout
import com.kit.ads.AdType
import com.kit.ads.BuildConfig
import com.kit.ads.AdPlacement
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.AdProviderConfig
import com.kit.ads.provider.ProviderListener
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.applovin.sdk.AppLovinSdkUtils
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.Collections

internal class ApplovinProviderAdapter : AdProviderAdapter {

    private var isInitialized = false
    private val pendingActions: MutableList<Runnable> = mutableListOf()

    override fun initialize(
        context: Application,
        config: AdProviderConfig,
        listener: (success: Boolean) -> Unit
    ) {
        try {
            // Enable test mode by default for the current device. Cannot be run on the main thread.
            val currentGaid = AdvertisingIdClient.getAdvertisingIdInfo(context).id

            // Create the initialization configuration
            val initConfig = AppLovinSdkInitializationConfiguration.builder(config.apiKey, context)
                .setTestDeviceAdvertisingIds(Collections.singletonList(currentGaid))
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .build()

            // Initialize the SDK with the configuration
            val appLovinSdk = AppLovinSdk.getInstance(context)
            appLovinSdk.settings.setVerboseLogging(BuildConfig.DEBUG)
            appLovinSdk.initialize(initConfig) {
                // Start loading ads
                isInitialized = true
                listener.invoke(true)
                executePendingActions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
            listener.invoke(false)
            pendingActions.clear()
        }
    }

    private fun executePendingActions() {
        for (action in pendingActions) {
            action.run()
        }
        pendingActions.clear()
    }

    override fun openDebug(activity: Activity) {
        AppLovinSdk.getInstance(activity).showMediationDebugger()
    }

    override fun loadAd(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {
        if (!isInitialized) {
            pendingActions.add(
                Runnable {
                    loadAd(activity, request, listener)
                }
            )
            return
        }
        when (request.adType) {
            AdType.BANNER -> loadBanner(activity, request, listener)
            AdType.SPLASH -> loadSplash(activity, request, listener)
            AdType.REWARDED -> loadRewarded(activity, request, listener)
        }
    }

    private fun loadRewarded(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {
        val ad = MaxRewardedAd.getInstance(request.adUnitId, activity)
        ad.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
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
                listener.onAdFailedToLoad(error.message)
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                listener.onAdFailedToLoad(error.message)
            }

            override fun onUserRewarded(maxAd: MaxAd, maxReward: MaxReward) {
                listener.onAdUserEarnedReward()
            }
        })
        ad.setRevenueListener {
            listener.onAdPaidEvent()
        }
        ad.loadAd()
    }

    private fun loadSplash(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {
        val ad = MaxAppOpenAd(request.adUnitId, activity)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
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
                listener.onAdFailedToLoad(error.message)
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
            }
        })
        ad.setRevenueListener {
            listener.onAdPaidEvent()
        }
        ad.loadAd()
    }

    private fun loadBanner(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {
        val ad = MaxAdView(request.adUnitId, activity)
        ad.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(maxAd: MaxAd) {
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
                listener.onAdFailedToLoad(error.message)
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                listener.onAdFailedToLoad(error.message)
            }

            override fun onAdExpanded(maxAd: MaxAd) {
            }

            override fun onAdCollapsed(maxAd: MaxAd) {
            }
        })
        ad.setRevenueListener {
            listener.onAdPaidEvent()
        }

        // Stretch to the width of the screen for banners to be fully functional
        // 宽充满屏幕，高度50dp为最理想比例
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val heightPx = AppLovinSdkUtils.dpToPx(activity, 50)
        ad.layoutParams = FrameLayout.LayoutParams(width, heightPx)
        ad.loadAd()
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdPlacement,
        ad: Any,
        listener: ProviderListener
    ) {
        when (request.adType) {
            AdType.SPLASH -> showSplash(ad as MaxAppOpenAd)
            AdType.BANNER -> showBanner(container, ad as MaxAdView)
            AdType.REWARDED -> showRewarded(activity, ad as MaxRewardedAd)
        }
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
}