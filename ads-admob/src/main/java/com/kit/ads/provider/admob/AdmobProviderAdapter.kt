package com.kit.ads.provider.admob

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import com.kit.ads.AdsLogger
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresPermission
import com.kit.ads.AdsRequest
import com.kit.ads.AdsPaidEvent
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderConfig
import com.kit.ads.AdsType
import com.kit.ads.AdsDebug
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
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


private const val TAG = "AdsKit-Admob"

internal class AdmobProviderAdapter : AdsProviderAdapter {

    @SuppressLint("DefaultLocale")
    override fun initialize(
        context: Application,
        config: AdsProviderConfig,
        listener: (success: Boolean) -> Unit
    ) {
        try {
            if (AdsDebug.isEnabled && AdsDebug.admobTestDeviceIds.isNotEmpty()) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setTestDeviceIds(AdsDebug.admobTestDeviceIds)
                        .build()
                )
                AdsLogger.d(TAG, "testDeviceIds=${AdsDebug.admobTestDeviceIds}")
            }
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
        val testDeviceIds = AdsDebug.admobTestDeviceIds.ifEmpty {
            listOf("9204E5C14728DD6DD82CE0A440ED41F5")
        }
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.openAdInspector(activity) { error ->
            // 若 error 非 null 表示广告检查器因错误关闭。
            AdsLogger.d(TAG, "Admob openAdInspector=${error?.message}")
        }
    }

    @RequiresPermission(Manifest.permission.INTERNET)
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

    /**
     * Admob的Banner广告是没有关闭按钮的
     * **/
    @RequiresPermission(Manifest.permission.INTERNET)
    private fun loadBanner(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {

        val ad = AdView(context).apply {
            adUnitId = if (AdsDebug.isEnabled) DEBUG_BANNER_AD_UNIT_ID else request.adUnitId
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
                logLoadAdError("banner", adError)
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
            listener.onAdPaidEvent(
                AdsPaidEvent(
                    valueMicros = paid.valueMicros,
                    currencyCode = paid.currencyCode,
                    precision = paid.precisionType.toString(),
                )
            )
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
            if (AdsDebug.isEnabled) DEBUG_REWARDED_AD_UNIT_ID else request.adUnitId
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logLoadAdError("rewarded", adError)
                    listener.onAdFailedToLoad(adError.message, adError.code.toString())
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    attachFullScreenCallbacks(ad, listener)
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        listener.onAdPaidEvent(
                            AdsPaidEvent(
                                valueMicros = paid.valueMicros,
                                currencyCode = paid.currencyCode,
                                precision = paid.precisionType.toString(),
                            )
                        )
                    }
                    listener.onAdLoaded(ad)
                }
            })
    }

    private fun loadInterstitial(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val adUnitId =
            if (AdsDebug.isEnabled) DEBUG_INTERSTITIAL_AD_UNIT_ID else request.adUnitId
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logLoadAdError("interstitial", adError)
                    listener.onAdFailedToLoad(adError.message, adError.code.toString())
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    attachFullScreenCallbacks(ad, listener)
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        listener.onAdPaidEvent(
                            AdsPaidEvent(
                                valueMicros = paid.valueMicros,
                                currencyCode = paid.currencyCode,
                                precision = paid.precisionType.toString(),
                            )
                        )
                    }
                    listener.onAdLoaded(ad)
                }
            })
    }

    private fun loadNative(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val adUnitId = if (AdsDebug.isEnabled) DEBUG_NATIVE_AD_UNIT_ID else request.adUnitId
        AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                nativeAd.setOnPaidEventListener { paid ->
                    listener.onAdPaidEvent(
                        AdsPaidEvent(
                            valueMicros = paid.valueMicros,
                            currencyCode = paid.currencyCode,
                            precision = paid.precisionType.toString(),
                        )
                    )
                }
                listener.onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logLoadAdError("native", adError)
                    listener.onAdFailedToLoad(adError.message, adError.code.toString())
                }

                override fun onAdClicked() {
                    listener.onAdClicked()
                }

                override fun onAdImpression() {
                    listener.onAdShown()
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    private fun loadMrec(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        val ad = AdView(context).apply {
            adUnitId = if (AdsDebug.isEnabled) DEBUG_BANNER_AD_UNIT_ID else request.adUnitId
            setAdSize(AdSize.MEDIUM_RECTANGLE)
        }
        ad.adListener = object : AdListener() {
            override fun onAdClicked() {
                listener.onAdClicked()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                logLoadAdError("mrec", adError)
                listener.onAdFailedToLoad(adError.message, adError.code.toString())
            }

            override fun onAdImpression() {
                listener.onAdShown()
            }

            override fun onAdLoaded() {
                listener.onAdLoaded(ad)
            }
        }
        ad.onPaidEventListener = OnPaidEventListener { paid ->
            listener.onAdPaidEvent(
                AdsPaidEvent(
                    valueMicros = paid.valueMicros,
                    currencyCode = paid.currencyCode,
                    precision = paid.precisionType.toString(),
                )
            )
        }
        listener.onAdStartedToLoad()
        ad.loadAd(AdRequest.Builder().build())
    }

    private fun attachFullScreenCallbacks(
        ad: Any,
        listener: AdsProviderListener,
    ) {
        val callback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                listener.onAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                listener.onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdsLogger.e(
                    TAG,
                    "onAdFailedToShow type=fullscreen error=${error.message} errorCode=${error.code}",
                )
                listener.onAdFailedToShow(ad, error.message, error.code.toString())
            }

            override fun onAdImpression() {
                listener.onAdShown()
            }

            override fun onAdShowedFullScreenContent() {
                AdsLogger.d(TAG, "广告全屏展示成功")
            }
        }
        when (ad) {
            is RewardedAd -> ad.fullScreenContentCallback = callback
            is InterstitialAd -> ad.fullScreenContentCallback = callback
            is AppOpenAd -> ad.fullScreenContentCallback = callback
        }
    }

    private fun loadSplash(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener
    ) {
        listener.onAdStartedToLoad()
        val adUnitId = if (AdsDebug.isEnabled) DEBUG_OPEN_AD_UNIT_ID else request.adUnitId
        AppOpenAd.load(
            context, adUnitId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    attachFullScreenCallbacks(ad, listener)
                    ad.onPaidEventListener = OnPaidEventListener { paid ->
                        listener.onAdPaidEvent(
                            AdsPaidEvent(
                                valueMicros = paid.valueMicros,
                                currencyCode = paid.currencyCode,
                                precision = paid.precisionType.toString(),
                            )
                        )
                    }
                    listener.onAdLoaded(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logLoadAdError("app_open", loadAdError)
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
            AdsType.BANNER, AdsType.MREC -> showBanner(container, ad as AdView)
            AdsType.REWARDED -> showRewarded(activity, ad as RewardedAd, listener)
            AdsType.INTERSTITIAL -> showInterstitial(activity, ad as InterstitialAd)
            AdsType.NATIVE -> showNative(container, ad as NativeAd)
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

    private fun showInterstitial(activity: Activity, ad: InterstitialAd) {
        ad.show(activity)
    }

    private fun showNative(container: ViewGroup, nativeAd: NativeAd) {
        val context = container.context
        val adView = NativeAdView(context)
        val headlineView = TextView(context)
        headlineView.text = nativeAd.headline
        adView.headlineView = headlineView
        adView.addView(
            headlineView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        nativeAd.mediaContent?.let {
            val mediaView = MediaView(context)
            adView.mediaView = mediaView
            adView.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        adView.setNativeAd(nativeAd)
        container.removeAllViews()
        container.addView(adView)
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
            is NativeAd -> {
                ad.destroy()
                AdsLogger.d(TAG, "AdMob NativeAd destroyed")
            }
            // RewardedAd / InterstitialAd / AppOpenAd 由系统管理
        }
    }

    companion object {

        private const val DEBUG_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val DEBUG_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val DEBUG_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val DEBUG_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val DEBUG_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

        private fun logLoadAdError(adType: String, error: LoadAdError) {
            val hint = when (error.code) {
                0 -> "INTERNAL_ERROR — 检查网络/UMP 同意/测试设备 ID；可用 openDebug 查看详情"
                2 -> "NETWORK_ERROR"
                3 -> "NO_FILL"
                else -> null
            }
            AdsLogger.e(
                TAG,
                "load $adType failed code=${error.code} message=${error.message} domain=${error.domain} hint=$hint response=${error.responseInfo}",
            )
        }
    }

}
