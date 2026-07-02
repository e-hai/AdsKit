package com.kit.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kit.ads.AdsManager.TAG
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderListener

/**
 * 广告触发点执行器
 *
 * 该类用于处理广告位的加载。通过封装广告请求和适配器，协调广告的加载、展示、点击等过程。
 * 它接收广告位请求并通知外部，负责在指定广告提供商的SDK中加载。
 */
class AdsLoadHandler constructor(
    private val placement: AdsRequest,  // 广告触发点请求配置
    private val providerAdapter: AdsProviderAdapter  // 广告提供商适配器

) {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private fun dispatchToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }


    /**
     * 加载广告
     *
     * 调用广告提供商适配器加载广告资源，并通过 `providerListener` 监听广告加载的过程。
     *
     * @param context 上下文
     */
    fun loadAd(context: Context, listener: AdsListener) {
        // 提供商回调监听器
        // 该监听器负责接收广告提供商SDK的各类事件回调，如广告加载、展示、点击、关闭等。
        val adsProviderListener = object : AdsProviderListener {

            override fun onAdStartedToLoad() {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告开始加载 (trigger=${placement.triggerId})")
                    listener.onAdStartedToLoad()
                }
            }

            override fun onAdLoaded(ad: Any) {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告加载成功 (trigger=${placement.triggerId})")
                    val adEntity = AdsEntity(providerAdapter, placement, this, ad)
                    listener.onAdLoaded(adEntity)  // 通知外部广告加载成功
                }
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告加载失败 (trigger=${placement.triggerId})")
                    listener.onAdFailedToLoad(error, errorCode)  // 通知外部广告加载失败
                }
            }

            override fun onAdShown() {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告被展示 (trigger=${placement.triggerId})")
                    listener.onAdShown()  // 通知外部广告已展示
                }
            }

            override fun onAdClicked() {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告被点击 (trigger=${placement.triggerId})")
                    listener.onAdClicked()  // 通知外部广告被点击
                }
            }

            override fun onAdPaidEvent() {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告预计获得的收益 (trigger=${placement.triggerId})")
                    listener.onAdPaidEvent()  // 通知外部广告收益事件
                }
            }

            override fun onAdClosed() {
                dispatchToMain {
                    AdsLogger.d(TAG, "广告被关闭 (trigger=${placement.triggerId})")
                    listener.onAdClosed()  // 通知外部广告已关闭
                }
            }

            override fun onAdUserEarnedReward() {
                dispatchToMain {
                    AdsLogger.d(TAG, "用户获得奖励 (trigger=${placement.triggerId})")
                    listener.onAdUserEarnedReward()  // 通知外部用户已获得奖励
                }
            }
        }
        providerAdapter.loadAd(context, placement, adsProviderListener)  // 使用适配器加载广告
    }

}
