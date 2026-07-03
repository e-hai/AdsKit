package com.kit.ads.sample

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kit.ads.AdCallback
import com.kit.ads.AdsEntity
import com.kit.ads.AdsLogger
import com.kit.ads.AdsManager
import com.kit.ads.AdsRequest
import com.kit.ads.AdsType
import com.kit.ads.provider.AdsProviderType
import com.kit.ads.ump.UMP

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SampleApp(
                    ::showSplash,
                    ::showReward,
                    ::showBanner,
                    ::showDebug,
                    ::preloadBanner,
                    ::showCachedBanner
                )
            }
        }

        val adWidth = defaultBannerAdWidth()
        AdsLogger.d("Main", "screenWidth=$adWidth")
        UMP.start(this) {
            AdsLogger.d("Main", "UMP=$it")
        }
    }

    private fun defaultBannerAdWidth(): Int {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        return (adWidthPixels / density).toInt()
    }

    private fun showDebug() {
        AdsManager.openDebug(this)
    }

    private fun showBanner(viewGroup: ViewGroup) {
        val request = AdsRequest(
            "1",
            "ca-app-pub-3940256099942544/9214589741",
            AdsType.BANNER,
            AdsProviderType.ADMOB
        )
        AdsManager.loadAd(this, request, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "banner failed: $error (code=$errorCode)")
            }

            override fun onAdClosed() {}
        })
    }

    private fun showReward(viewGroup: ViewGroup) {
        val request = AdsRequest(
            "1",
            "ca-app-pub-3940256099942544/5224354917",
            AdsType.REWARDED,
            AdsProviderType.ADMOB
        )
        AdsManager.loadAd(this, request, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "reward failed: $error (code=$errorCode)")
            }

            override fun onAdClosed() {}
        })
    }

    private fun showSplash(viewGroup: ViewGroup) {
        val request = AdsRequest(
            "1",
            "ca-app-pub-3940256099942544/9257395921",
            AdsType.SPLASH,
            AdsProviderType.ADMOB
        )
        AdsManager.loadAd(this, request, object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "splash failed: $error (code=$errorCode)")
            }

            override fun onAdClosed() {}
        })
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
                AdsLogger.d("Main", "showCachedBanner loaded from cache")
                ad.show(this@MainActivity, viewGroup)
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e("Main", "showCachedBanner failed: $error (code=$errorCode)")
            }
        })
    }
}

@Composable
private fun SampleApp(
    onSplash: (ViewGroup) -> Unit,
    onReward: (ViewGroup) -> Unit,
    onBanner: (ViewGroup) -> Unit,
    onDebug: () -> Unit,
    onPreload: () -> Unit,
    onShowCached: (ViewGroup) -> Unit,
) {
    val bannerContainer = FrameLayout(LocalContext.current)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ButtonRow("开屏") { onSplash(bannerContainer) }
            ButtonRow("激励视频") { onReward(bannerContainer) }
            ButtonRow("Banner") { onBanner(bannerContainer) }
            ButtonRow("Debug") { onDebug() }
            ButtonRow("预加载 Banner") { onPreload() }
            ButtonRow("显示缓存") { onShowCached(bannerContainer) }

            Spacer(modifier = Modifier.height(8.dp))

            // Banner ad container — bridges to View system
            AndroidView(
                factory = { bannerContainer },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
private fun ButtonRow(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text)
    }
}
