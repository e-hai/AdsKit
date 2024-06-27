package com.an.ads.provider.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.an.ads.AdPlacementRequest
import com.an.ads.provider.AdProviderAdapter
import com.an.ads.provider.AdProviderConfig
import com.an.ads.AdType
import com.an.ads.BuildConfig
import com.an.ads.provider.ProviderEventListener
import com.an.ads.provider.ProviderRewardListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


internal class AdmobProviderAdapter(private val context: Context) : AdProviderAdapter {

    private var isInitialized = false
    private val pendingActions: MutableList<Runnable> = mutableListOf()

    override fun initialize(config: AdProviderConfig) {
        //初始化完成或30秒超时会走该回调
        MobileAds.initialize(context) {
            isInitialized = true
            executePendingActions()
        }
    }

    override fun loadAd(request: AdPlacementRequest, listener: ProviderEventListener) {
        if (!isInitialized) {
            pendingActions.add(
                Runnable {
                    loadAd(request, listener)
                }
            )
            return
        }
        when (request.adType) {
            AdType.BANNER -> loadBanner(request, listener)
            AdType.SPLASH -> loadSplash(request, listener)
            AdType.REWARDED -> loadRewarded(request, listener)
        }
    }

    private fun executePendingActions() {
        for (action in pendingActions) {
            action.run()
        }
        pendingActions.clear()
    }


    private fun loadBanner(request: AdPlacementRequest, listener: ProviderEventListener) {
        val ad = AdView(context)
        val adUnitId = if (BuildConfig.DEBUG) DEBUG_BANNER_AD_UNIT_ID else request.adUnitId
        ad.adUnitId = adUnitId
        val adSize =
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, defaultBannerAdWidth())
        ad.setAdSize(adSize)
        ad.adListener = object : AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
                Log.d(TAG, "广告被点击")
                listener.onAdClicked()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
                Log.d(TAG, "广告被关闭")
                listener.onAdClosed()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "广告加载失败=${adError.message}")
                // Code to be executed when an ad request fails.
                listener.onAdFailedToLoad(adError.message)
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
                Log.d(TAG, "广告被展示")
                listener.onAdShown()
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.d(TAG, "广告加载成功")
                listener.onAdLoaded(ad)
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.d(TAG, "广告被点击后，出现的弹窗")
            }
        }
        ad.onPaidEventListener = OnPaidEventListener { paid ->
            Log.d(
                TAG,
                "广告预计获得的收益=${paid.currencyCode} ${paid.precisionType} ${paid.valueMicros}"
            )
            listener.onAdPaidEvent()
        }
        ad.loadAd(AdRequest.Builder().build())
    }

    private fun defaultBannerAdWidth(): Int {
        val displayMetrics = context.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        return (adWidthPixels / density).toInt()
    }

    private fun loadRewarded(
        request: AdPlacementRequest,
        listener: ProviderEventListener
    ) {
        val adUnitId =
            if (BuildConfig.DEBUG) DEBUG_REWARDED_AD_UNIT_ID else request.adUnitId
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener.onAdFailedToLoad(adError.message)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "广告被点击")
                            listener.onAdClicked()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "广告被关闭")
                            listener.onAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.d(TAG, "广告全屏展示失败")
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "广告被展示")
                            listener.onAdShown()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "广告全屏展示成功")
                        }
                    }
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        Log.d(
                            TAG,
                            "广告预计获得的收益=${paid.currencyCode} ${paid.precisionType} ${paid.valueMicros}"
                        )
                        listener.onAdPaidEvent()
                    }
                    Log.d(TAG, "广告加载成功")
                    listener.onAdLoaded(ad)
                }
            })
    }


    private fun loadSplash(request: AdPlacementRequest, listener: ProviderEventListener) {
        val adUnitId = if (BuildConfig.DEBUG) DEBUG_OPEN_AD_UNIT_ID else request.adUnitId
        AppOpenAd.load(context, adUnitId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "广告被点击")
                            listener.onAdClicked()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "广告被关闭")
                            listener.onAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.d(TAG, "广告全屏展示失败")
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "广告被展示")
                            listener.onAdShown()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "广告全屏展示成功")
                        }
                    }
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        Log.d(
                            TAG,
                            "广告预计获得的收益=${paid.currencyCode} ${paid.precisionType} ${paid.valueMicros}"
                        )
                        listener.onAdPaidEvent()
                    }
                    Log.d(TAG, "广告加载成功")
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
        request: AdPlacementRequest,
        ad: Any,
        listener: ProviderRewardListener
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


    private fun showRewarded(activity: Activity, ad: RewardedAd, listener: ProviderRewardListener) {
        ad.show(activity) { rewardItem ->
            Log.d(TAG, "用户获得奖励=$rewardItem")
            listener.onAdUserEarnedReward()
        }
    }

    private fun showSplash(activity: Activity, ad: AppOpenAd) {
        ad.show(activity)
    }


    companion object {
        const val TAG = "AdmobProvider"

        private const val DEBUG_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val DEBUG_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val DEBUG_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

}