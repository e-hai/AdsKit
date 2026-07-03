package com.kit.ads.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kit.ads.AdsLogger
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.kit.ads.AdCallback
import com.kit.ads.AdsEntity
import com.kit.ads.AdsManager
import com.kit.ads.AdsRequest
import com.kit.ads.AdsType
import com.kit.ads.provider.AdsProviderType
import com.kit.ads.ump.UMP

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val adViewGroup = findViewById<LinearLayout>(R.id.root)
        findViewById<View>(R.id.splash).setOnClickListener {
            showSplash(adViewGroup)
        }
        findViewById<View>(R.id.reward).setOnClickListener {
            showReward(adViewGroup)
        }
        findViewById<View>(R.id.banner).setOnClickListener {
            showBanner(findViewById(R.id.bannerViewGroup))
        }
        findViewById<View>(R.id.ad_debug).setOnClickListener {
            showDebug()
        }
        findViewById<View>(R.id.preload).setOnClickListener {
            preloadBanner()
        }
        findViewById<View>(R.id.show_cached).setOnClickListener {
            showCachedBanner(findViewById(R.id.bannerViewGroup))
        }
        val adWidth = defaultBannerAdWidth()
        AdsLogger.d("Main", "屏幕宽=$adWidth")
        UMP.start(this) {
            AdsLogger.d("Main", "UMP=$it")
        }
    }

    private fun showDebug() {
        AdsManager.openDebug(this)
    }

    private fun preloadBanner() {
        val request = AdsRequest(
            "preload_home",
            "ca-app-pub-3940256099942544/9214589741",
            AdsType.BANNER,
            AdsProviderType.ADMOB
        )
        AdsLogger.d("Main", "preloadBanner triggerId=${request.triggerId}")
        AdsManager.preloadAd(this, request)
    }

    private fun showCachedBanner(viewGroup: ViewGroup) {
        val request = AdsRequest(
            "preload_home",
            "ca-app-pub-3940256099942544/9214589741",
            AdsType.BANNER,
            AdsProviderType.ADMOB
        )
        AdsLogger.d("Main", "showCachedBanner triggerId=${request.triggerId}")
        AdsManager.loadAd(this, request, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                AdsLogger.d("Main", "showCachedBanner loaded from (cache or network)")
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "showCachedBanner failed: $error (code=$errorCode)")
            }
        })
    }

    private fun defaultBannerAdWidth(): Int {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        return (adWidthPixels / density).toInt()
    }

    private fun showBanner(viewGroup: ViewGroup) {
        val requestConfig = AdsRequest(
            "1",
            "ca-app-pub-3940256099942544/9214589741",
            AdsType.BANNER,
            AdsProviderType.ADMOB
        )

        AdsManager.loadAd(this, requestConfig, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "广告加载失败: $error (code=$errorCode)")
            }

            override fun onAdClosed() {
            }
        })
    }

    private fun showReward(viewGroup: ViewGroup) {
        val requestConfig = AdsRequest(
            "1",
            "ca-app-pub-3940256099942544/5224354917",
            AdsType.REWARDED,
            AdsProviderType.ADMOB
        )

        AdsManager.loadAd(this, requestConfig, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                // REWARDED 不需要使用 banner 容器，仅需一个有效的 ViewGroup 占位参数。
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "广告加载失败: $error (code=$errorCode)")
            }

            override fun onAdClosed() {
            }
        })
    }

    private fun showSplash(viewGroup: ViewGroup) {
        val requestConfig = AdsRequest(
            "1",
            "ca-app-pub-3940256099942544/9257395921",
            AdsType.SPLASH,
            AdsProviderType.ADMOB
        )

        AdsManager.loadAd(this, requestConfig, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                // SPLASH 不需要依赖 banner 容器，仅需传入任意有效的 ViewGroup。
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "广告加载失败: $error (code=$errorCode)")
            }

            override fun onAdClosed() {
            }
        })
    }
}
