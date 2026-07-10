package com.kit.ads.provider

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.ViewGroup
import com.kit.ads.AdsRequest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

internal class FakeAdsProviderAdapter(
    private val initSuccess: Boolean = true,
    private val deferInitCallback: Boolean = false,
    private val loadSucceeds: Boolean = true,
    private val showFails: Boolean = false,
    private val callbackOnBackground: Boolean = false,
) : AdsProviderAdapter {

    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val pendingBackgroundCallbacks = AtomicInteger(0)

    fun awaitBackgroundCallbacks(timeoutMs: Long = 2_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (pendingBackgroundCallbacks.get() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
    }

    private fun dispatch(block: () -> Unit) {
        if (callbackOnBackground) {
            pendingBackgroundCallbacks.incrementAndGet()
            backgroundExecutor.execute {
                try {
                    block()
                } finally {
                    pendingBackgroundCallbacks.decrementAndGet()
                }
            }
        } else {
            block()
        }
    }

    var initializeCallCount: Int = 0
        private set
    var destroyCallCount: Int = 0
        private set
    var loadAdCallCount: Int = 0
        private set

    private var pendingInitListener: ((Boolean) -> Unit)? = null

    fun completeInitialize(success: Boolean = initSuccess) {
        pendingInitListener?.invoke(success)
        pendingInitListener = null
    }

    override fun initialize(
        context: Application,
        config: AdsProviderConfig,
        listener: (Boolean) -> Unit,
    ) {
        initializeCallCount++
        if (deferInitCallback) {
            pendingInitListener = listener
        } else {
            listener(initSuccess)
        }
    }

    override fun loadAd(
        context: Context,
        request: AdsRequest,
        listener: AdsProviderListener,
    ) {
        loadAdCallCount++
        dispatch {
            if (loadSucceeds) {
                listener.onAdLoaded(Any())
            } else {
                listener.onAdFailedToLoad("fake load failed", "FAKE_LOAD_FAILED")
            }
        }
    }

    override fun showAd(
        activity: Activity,
        container: ViewGroup,
        request: AdsRequest,
        ad: Any,
        listener: AdsProviderListener,
    ) {
        dispatch {
            if (showFails) {
                listener.onAdFailedToShow(ad, "fake show failed", "DISPLAY_FAKE")
            }
        }
    }

    override fun openDebug(activity: Activity) = Unit

    override fun destroy() {
        destroyCallCount++
    }
}
