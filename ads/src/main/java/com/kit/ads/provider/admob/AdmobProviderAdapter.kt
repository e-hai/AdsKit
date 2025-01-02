package com.kit.ads.provider.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.kit.ads.AdManager.TAG
import com.kit.ads.AdPlacement
import com.kit.ads.provider.AdProviderAdapter
import com.kit.ads.provider.AdProviderConfig
import com.kit.ads.AdType
import com.kit.ads.BuildConfig
import com.kit.ads.provider.ProviderListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


internal class AdmobProviderAdapter : AdProviderAdapter {

    private var isInitialized = false
    private val pendingActions: MutableList<Runnable> = mutableListOf()

    override fun initialize(
        context: Application,
        config: AdProviderConfig,
        listener: (success: Boolean) -> Unit
    ) {
        try {
            //初始化完成或30秒超时会走该回调
            MobileAds.initialize(context) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d(
                        TAG, String.format(
                            "Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status!!.description, status.latency
                        )
                    )
                }
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

    /**
     * 检查 logcat 输出，以查找如下所示的消息，其中显示您的设备 ID 以及如何将其添加为测试设备：
     *
     * I/Ads: Use RequestConfiguration.Builder.setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
     * to get test ads on this device."
     * 将测试设备 ID 复制到剪贴板。
     * **/
    override fun openDebug(activity: Activity) {
        val testDeviceIds = listOf("9204E5C14728DD6DD82CE0A440ED41F5")
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.openAdInspector(activity) { error ->
            // Error will be non-null if ad inspector closed due to an error.
            Log.d(TAG, "Admob openAdInspector=${error?.message}")
        }
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

    private fun executePendingActions() {
        for (action in pendingActions) {
            action.run()
        }
        pendingActions.clear()
    }

    /**
     * Admob的Banner广告是没有关闭按钮的
     * **/
    private fun loadBanner(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {

        val ad = AdView(activity).apply {
            adUnitId = if (BuildConfig.DEBUG) DEBUG_BANNER_AD_UNIT_ID else request.adUnitId
            // Stretch to the width of the screen for banners to be fully functional
            // 宽充满屏幕，高度50dp为最理想比例
            val adWidth = defaultBannerAdWidth(activity)
            Log.d(TAG, "广告宽=$adWidth")
            val adSize = AdSize.getInlineAdaptiveBannerAdSize(adWidth, 50)
            setAdSize(adSize)
        }

        ad.adListener = object : AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
                listener.onAdClicked()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
                Log.d(TAG, "从广告展开页面返回会被回调，并不是广告被关闭")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Code to be executed when an ad request fails.
                listener.onAdFailedToLoad(adError.message)
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
                listener.onAdShown()
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                listener.onAdLoaded(ad)
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.d(TAG, "广告被点击后，跳转到广告展开页面")
            }
        }
        ad.onPaidEventListener = OnPaidEventListener { paid ->
            Log.d(
                TAG,
                "收益=${paid.valueMicros}-${paid.precisionType}-${paid.currencyCode}"
            )
            listener.onAdPaidEvent()
        }
        listener.onAdStartedToLoad()
        ad.loadAd(AdRequest.Builder().build())
    }

    private fun defaultBannerAdWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        return (adWidthPixels / density).toInt()
    }

    private fun loadRewarded(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {
        val adUnitId =
            if (BuildConfig.DEBUG) DEBUG_REWARDED_AD_UNIT_ID else request.adUnitId
        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener.onAdFailedToLoad(adError.message)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            listener.onAdClicked()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            listener.onAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.d(TAG, "广告全屏展示失败")
                        }

                        override fun onAdImpression() {
                            listener.onAdShown()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "广告全屏展示成功")
                        }
                    }
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        Log.d(
                            TAG,
                            "收益=${paid.valueMicros}-${paid.precisionType}-${paid.currencyCode}"
                        )
                        listener.onAdPaidEvent()
                    }
                    listener.onAdLoaded(ad)
                }
            })
    }


    private fun loadSplash(
        activity: Activity,
        request: AdPlacement,
        listener: ProviderListener
    ) {
        val adUnitId = if (BuildConfig.DEBUG) DEBUG_OPEN_AD_UNIT_ID else request.adUnitId
        AppOpenAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            listener.onAdClicked()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            listener.onAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.d(TAG, "广告全屏展示失败")
                        }

                        override fun onAdImpression() {
                            listener.onAdShown()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "广告全屏展示成功")
                        }
                    }
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        Log.d(
                            TAG,
                            "收益=${paid.valueMicros}-${paid.precisionType}-${paid.currencyCode}"
                        )
                        listener.onAdPaidEvent()
                    }
                    listener.onAdLoaded(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    listener.onAdFailedToLoad(loadAdError.message)
                }
            })
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdPlacement,
        ad: Any,
        listener: ProviderListener
    ) {
        when (request.adType) {
            AdType.SPLASH -> showSplash(activity, ad as AppOpenAd)
            AdType.BANNER -> showBanner(container, ad as AdView)
            AdType.REWARDED -> showRewarded(activity, ad as RewardedAd, listener)
        }
    }


    private fun showBanner(container: ViewGroup, ad: AdView) {
        if (ad.parent != null) {
            (ad.parent as ViewGroup).removeView(ad)
        }
        container.addView(ad)
    }


    private fun showRewarded(activity: Activity, ad: RewardedAd, listener: ProviderListener) {
        ad.show(activity) { rewardItem ->
            Log.d(
                TAG,
                "reward=${rewardItem.amount}-${rewardItem.type}"
            )
            listener.onAdUserEarnedReward()
        }
    }

    private fun showSplash(activity: Activity, ad: AppOpenAd) {
        ad.show(activity)
    }


    companion object {

        private const val DEBUG_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val DEBUG_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val DEBUG_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

}