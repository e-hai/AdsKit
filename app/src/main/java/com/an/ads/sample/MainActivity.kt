package com.an.ads.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.an.ads.AdCallback
import com.an.ads.AdEntity
import com.an.ads.AdManager
import com.an.ads.AdPlacementRequest
import com.an.ads.AdType
import com.an.ads.AdProviderType

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
    }

    private fun showBanner(viewGroup: ViewGroup) {
        val placementConfig = AdPlacementRequest(
            "1",
            "admob_unit_id_666",
            AdType.BANNER,
            AdProviderType.ADMOB
        )

        AdManager.loadAd(placementConfig, object : AdCallback() {
            override fun onAdLoaded(ad: AdEntity) {
                ad.show(this@MainActivity,viewGroup)
            }

            override fun onAdFailedToLoad(error: String) {
            }

            override fun onAdClosed() {
            }
        })
    }

    private fun showReward(viewGroup: ViewGroup) {
        val placementConfig = AdPlacementRequest(
            "1",
            "admob_unit_id_666",
            AdType.REWARDED,
            AdProviderType.ADMOB
        )

        AdManager.loadAd(placementConfig, object : AdCallback() {
            override fun onAdLoaded(ad: AdEntity) {
                ad.show(this@MainActivity,viewGroup)
            }

            override fun onAdFailedToLoad(error: String) {
            }

            override fun onAdClosed() {
            }
        })
    }

    private fun showSplash(viewGroup: ViewGroup) {
        val placementConfig = AdPlacementRequest(
            "1",
            "admob_unit_id_666",
            AdType.SPLASH,
            AdProviderType.ADMOB
        )

        AdManager.loadAd(placementConfig, object : AdCallback() {
            override fun onAdLoaded(ad: AdEntity) {
                ad.show(this@MainActivity,viewGroup)
            }

            override fun onAdFailedToLoad(error: String) {
            }

            override fun onAdClosed() {
            }
        })
    }
}