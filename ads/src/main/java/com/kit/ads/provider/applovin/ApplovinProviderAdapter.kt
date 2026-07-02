package com.kit.ads.provider.applovin

import android.app.Activity
import android.app.Application
import android.content.Context
import com.kit.ads.AdsLogger
import android.view.ViewGroup
import android.widget.FrameLayout
import com.kit.ads.AdsType
import com.kit.ads.BuildConfig
import com.kit.ads.AdsRequest
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderConfig
import com.kit.ads.provider.AdsProviderListener
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

private const val TAG = "AdsKit-AppLovin"

internal class ApplovinProviderAdapter : AdsProviderAdapter {

    override fun initialize(
        context: Application,
        config: AdsProviderConfig,
        listener: (success: Boolean) -> Unit
    ) {
        try {
            // 默认启用当前设备的测试模式。不能在主线程运行。
            val currentGaid = AdvertisingIdClient.getAdvertisingIdInfo(context).id

            // 创建初始化配置
            val initConfig = AppLovinSdkInitializationConfiguration.builder(config.apiKey, context)
                .setTestDeviceAdvertisingIds(Collections.singletonList(currentGaid))
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .build()

            // 使用配置初始化 SDK
            val appLovinSdk = AppLovinSdk.getInstance(context)
            appLovinSdk.settings.setVerboseLogging(BuildConfig.DEBUG)
            appLovinSdk.initialize(initConfig) {
                // 开始加载广告
                listener.invoke(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listener.invoke(false)
        }
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
                AdsLogger.d(TAG, "激励广告加载失败: ${error.message} (code=${error.code})")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.d(TAG, "激励广告展示失败: ${error.message} (code=${error.code})")
                listener.onAdFailedToLoad(error.message, "DISPLAY_" + error.code)
            }

            override fun onUserRewarded(maxAd: MaxAd, maxReward: MaxReward) {
                AdsLogger.d(TAG, "用户获得奖励: amount=${maxReward.amount}")
                listener.onAdUserEarnedReward()
            }
        })
        ad.setRevenueListener {
            listener.onAdPaidEvent()
        }
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
                AdsLogger.d(TAG, "开屏广告加载失败: ${error.message} (code=${error.code})")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.d(TAG, "开屏广告展示失败: ${error.message} (code=${error.code})")
                listener.onAdFailedToLoad(error.message, "DISPLAY_" + error.code)
            }
        })
        ad.setRevenueListener {
            listener.onAdPaidEvent()
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
                AdsLogger.d(TAG, "横幅广告加载失败: ${error.message} (code=${error.code})")
                listener.onAdFailedToLoad(error.message, error.code.toString())
            }

            override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                AdsLogger.d(TAG, "横幅广告展示失败: ${error.message} (code=${error.code})")
                listener.onAdFailedToLoad(error.message, "DISPLAY_" + error.code)
            }

            override fun onAdExpanded(maxAd: MaxAd) {
            }

            override fun onAdCollapsed(maxAd: MaxAd) {
            }
        })
        ad.setRevenueListener {
            listener.onAdPaidEvent()
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
            AdsType.BANNER -> showBanner(container, ad as MaxAdView)
            AdsType.REWARDED -> showRewarded(activity, ad as MaxRewardedAd)
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

    override fun destroyAd(ad: Any) {
        when (ad) {
            is MaxAdView -> {
                ad.removeAllViews()
                ad.setListener(null)
                ad.destroy()

                AdsLogger.d(TAG, "AppLovin MaxAdView destroyed")
            }
            // MaxRewardedAd 和 MaxAppOpenAd 是单例/系统管理的，不需要手动 destroy
        }
    }

}
