package com.kit.ads.provider.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import com.kit.ads.AdsLogger
import android.view.ViewGroup
import com.kit.ads.AdsManager.TAG
import com.kit.ads.AdsRequest
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderConfig
import com.kit.ads.AdsType
import com.kit.ads.BuildConfig
import com.kit.ads.provider.AdsProviderListener
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


internal class AdmobProviderAdapter : AdsProviderAdapter {

    override fun initialize(
        context: Application,
        config: AdsProviderConfig,
        listener: (success: Boolean) -> Unit
    ) {
        try {
            //初始化完成或30秒超时会走该回调
            MobileAds.initialize(context) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    AdsLogger.d(
                        TAG, String.format(
                            "Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status!!.description, status.latency
                        )
                    )
                }
                listener.invoke(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listener.invoke(false)
        }

    }

    /**
     * 检查 logcat 输出，查找类似以下格式的日志，其中会显示您的设备 ID 及如何将其添加为测试设备：
     *
     * 示例 Logcat 输出：
     * I/Ads: Use RequestConfiguration.Builder.setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
     * 即可在该设备上获取测试广告。
     *
     * 将测试设备 ID 复制到下方列表后，即可在调试设备上获取测试广告。
     * **/
    override fun openDebug(activity: Activity) {
        val testDeviceIds = listOf("9204E5C14728DD6DD82CE0A440ED41F5")
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.openAdInspector(activity) { error ->
            // 若 error 非 null 表示广告检查器因错误关闭。
            AdsLogger.d(TAG, "Admob openAdInspector=${error?.message}")
        }
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
        }
    }

    /**
     * Admob的Banner广告是没有关闭按钮的
     * **/
    private fun loadBanner(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {

        val ad = AdView(context).apply {
            adUnitId = if (BuildConfig.DEBUG) DEBUG_BANNER_AD_UNIT_ID else request.adUnitId
            // 横幅广告宽度铺满屏幕以保证正常展示
            // 宽充满屏幕，高度50dp为最理想比例
            val adWidth = defaultBannerAdWidth(context)
            AdsLogger.d(TAG, "广告宽=$adWidth")
            val adSize = AdSize.getInlineAdaptiveBannerAdSize(adWidth, 50)
            setAdSize(adSize)
        }

        ad.adListener = object : AdListener() {
            override fun onAdClicked() {
                // 用户点击广告时执行
                listener.onAdClicked()
            }

            override fun onAdClosed() {
                // 用户点击广告后即将返回应用时执行
                // （并非广告关闭）
                AdsLogger.d(TAG, "从广告展开页面返回会被回调，并不是广告被关闭")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // 广告请求失败时执行
                listener.onAdFailedToLoad(adError.message, adError.code.toString())
            }

            override fun onAdImpression() {
                // 广告曝光记录时执行
                // （impression 回调）
                listener.onAdShown()
            }

            override fun onAdLoaded() {
                // 广告加载完成时执行
                listener.onAdLoaded(ad)
            }

            override fun onAdOpened() {
                // 广告打开覆盖屏幕的浮层时执行
                // （overlay 覆盖屏幕）
                AdsLogger.d(TAG, "广告被点击后，跳转到广告展开页面")
            }
        }
        ad.onPaidEventListener = OnPaidEventListener { paid ->
            AdsLogger.d(
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
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val adUnitId =
            if (BuildConfig.DEBUG) DEBUG_REWARDED_AD_UNIT_ID else request.adUnitId
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener.onAdFailedToLoad(adError.message, adError.code.toString())
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
                            AdsLogger.d(TAG, "广告全屏展示失败: ${error.message} (code=${error.code})")
                            listener.onAdFailedToShow(ad, error.message, error.code.toString())
                        }

                        override fun onAdImpression() {
                            listener.onAdShown()
                        }

                        override fun onAdShowedFullScreenContent() {
                            AdsLogger.d(TAG, "广告全屏展示成功")
                        }
                    }
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        AdsLogger.d(
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
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val adUnitId = if (BuildConfig.DEBUG) DEBUG_OPEN_AD_UNIT_ID else request.adUnitId
        AppOpenAd.load(
            context, adUnitId, AdRequest.Builder().build(),
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
                            AdsLogger.d(TAG, "广告全屏展示失败: ${error.message} (code=${error.code})")
                            listener.onAdFailedToShow(ad, error.message, error.code.toString())
                        }

                        override fun onAdImpression() {
                            listener.onAdShown()
                        }

                        override fun onAdShowedFullScreenContent() {
                            AdsLogger.d(TAG, "广告全屏展示成功")
                        }
                    }
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        AdsLogger.d(
                            TAG,
                            "收益=${paid.valueMicros}-${paid.precisionType}-${paid.currencyCode}"
                        )
                        listener.onAdPaidEvent()
                    }
                    listener.onAdLoaded(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    listener.onAdFailedToLoad(loadAdError.message, loadAdError.code.toString())
                }
            })
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdsRequest,
        ad: Any,
        listener: AdsProviderListener
    ) {
        when (request.adType) {
            AdsType.SPLASH -> showSplash(activity, ad as AppOpenAd)
            AdsType.BANNER -> showBanner(container, ad as AdView)
            AdsType.REWARDED -> showRewarded(activity, ad as RewardedAd, listener)
        }
    }


    private fun showBanner(container: ViewGroup, ad: AdView) {
        if (ad.parent != null) {
            (ad.parent as ViewGroup).removeView(ad)
        }
        container.addView(ad)
    }


    private fun showRewarded(activity: Activity, ad: RewardedAd, listener: AdsProviderListener) {
        ad.show(activity) { rewardItem ->
            AdsLogger.d(
                TAG,
                "reward=${rewardItem.amount}-${rewardItem.type}"
            )
            listener.onAdUserEarnedReward()
        }
    }

    private fun showSplash(activity: Activity, ad: AppOpenAd) {
        ad.show(activity)
    }

    override fun destroyAd(ad: Any) {
        when (ad) {
            is AdView -> {
                ad.removeAllViews()
                ad.destroy()
                AdsLogger.d(TAG, "AdMob AdView destroyed")
            }
            // RewardedAd 和 AppOpenAd 是系统管理的，不需要手动 destroy
        }
    }

    companion object {

        private const val DEBUG_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val DEBUG_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val DEBUG_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

}
