package com.kit.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kit.ads.provider.AdsProviderAdapter
import com.kit.ads.provider.AdsProviderAdapterFactory
import com.kit.ads.provider.AdsProviderConfig
import androidx.annotation.VisibleForTesting
import com.kit.ads.provider.AdsProviderType


object AdsManager {
    // 标签，用于日志输出
    const val TAG = "AdsManager"

    private enum class InitState {
        IDLE,          // 未初始化
        INITIALIZING,  // 初始化中
        READY,         // 初始化成功
        FAILED         // 初始化失败
    }

    // 初始化控制，避免并发初始化重复创建 adapter
    private val stateLock = Any()
    private var initState = InitState.IDLE
    private var initToken = 0

    // 存储唯一的广告提供商适配器
    @Volatile
    private var adsProviderAdapter: AdsProviderAdapter? = null

    @Volatile
    private var initializedProviderType: AdsProviderType? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private fun dispatchToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    /**
     * 初始化广告提供商适配器
     * @param context 应用的上下文
     * @param config 广告提供商的配置
     */
    fun initialize(
        context: Application,
        config: AdsProviderConfig,
        onResult: ((success: Boolean) -> Unit)? = null
    ) {
        val providerType = config.providerType
        val adapter = AdsProviderAdapterFactory.create(providerType)
        val token: Int
        val oldAdapter: AdsProviderAdapter?

        synchronized(stateLock) {
            when (initState) {
                InitState.INITIALIZING -> {
                    // 避免并发/重复调用 initialize，当前初始化尚未返回
                    AdsLogger.w(
                        TAG,
                        "Ignoring initialize($providerType), another initialize is in progress."
                    )
                    return
                }

                InitState.READY -> {
                    // 单适配器模式：先销毁已有适配器，再重新初始化新供应商
                    val currentType = initializedProviderType
                    if (currentType == providerType) {
                        AdsLogger.w(
                            TAG,
                            "AdsManager already initialized with $providerType. Skip duplicate initialize."
                        )
                        return
                    }
                    AdsLogger.w(
                        TAG,
                        "Reinitializing from $currentType to $providerType. Releasing old adapter."
                    )
                }

                InitState.IDLE, InitState.FAILED -> Unit
            }

            oldAdapter = adsProviderAdapter
            adsProviderAdapter = null
            initializedProviderType = null
            initState = InitState.INITIALIZING
            initToken++
            token = initToken
        }
        oldAdapter?.destroy()

        // 初始化广告适配器，完成后通知观察者
        adapter.initialize(context, config) { success ->
            val shouldNotify = synchronized(stateLock) {
                // 丢弃过期回调（例如初始化后又被 destroy 或再次 initialize）
                if (token != initToken) {
                    adapter.destroy()
                    false
                } else {
                    initState = if (success) InitState.READY else InitState.FAILED
                    if (success) {
                        adsProviderAdapter = adapter
                        initializedProviderType = providerType
                    } else {
                        adsProviderAdapter = null
                        initializedProviderType = null
                    }
                    true
                }
            }
            if (shouldNotify) onResult?.invoke(success)
        }
    }

    /**
     * 销毁当前适配器，释放所有资源
     *
     * 调用后 AdsManager 回到未初始化状态，可重新 initialize。
     */
    fun destroy() {
        val toDestroy: AdsProviderAdapter?
        synchronized(stateLock) {
            toDestroy = adsProviderAdapter
            initState = InitState.IDLE
            initToken++
            adsProviderAdapter = null
            initializedProviderType = null
        }
        toDestroy?.destroy()
    }

    /**
     * 开启调试模式，显示广告SDK的调试信息
     * @param activity 当前Activity
     */
    fun openDebug(activity: Activity) {
        val adapter = synchronized(stateLock) { adsProviderAdapter }
        if (adapter != null) {
            adapter.openDebug(activity)
        } else {
            AdsLogger.e(TAG, "No adapter initialized. Please call initialize first.")
        }
    }


    /**
     * 加载广告数据
     * @param context 上下文
     * @param placement 广告触发点请求
     * @param adListener 广告回调监听器
     */
    fun loadAd(context: Context, placement: AdsRequest, adListener: AdsListener) {
        executeAdsRequest(context, placement, adListener)
    }

    /**
     * 构建并执行广告加载
     * @param context 上下文
     * @param placement 广告触发点请求
     * @param adListener 广告回调监听器
     */
    private fun executeAdsRequest(
        context: Context,
        placement: AdsRequest,
        adListener: AdsListener
    ) {
        val adapter: AdsProviderAdapter?
        val currentProviderType: AdsProviderType?
        val state: InitState
        synchronized(stateLock) {
            adapter = adsProviderAdapter
            currentProviderType = initializedProviderType
            state = initState
        }

        AdsLogger.d(
            TAG,
            "请求广告: triggerId=${placement.triggerId}, type=${placement.adType}, provider=${placement.providerType}"
        )
        AdsLogger.d(TAG, "状态校验: state=$state, adapter=${adapter != null}")

        if (state != InitState.READY || adapter == null) {
            // 如果没有初始化适配器，调用失败回调并通知全局观察者
            val errorMsg = when (state) {
                InitState.INITIALIZING -> "AdsManager initializing. Wait for initialize callback then retry."
                else -> "AdsManager not initialized. No adapter found."
            }
            dispatchToMain {
                val code = "STATE_${state}"
                adListener.onAdFailedToLoad(errorMsg, code)
            }
            return
        }

        if (currentProviderType != placement.providerType) {
            val errorMsg = "Provider mismatch. Manager initialized with $currentProviderType, " +
                    "placement requests ${placement.providerType}."
            dispatchToMain {
                adListener.onAdFailedToLoad(errorMsg, "PROVIDER_MISMATCH")
            }
        } else {
            // 使用当前已初始化的适配器执行广告加载
            AdsLogger.d(TAG, "开始加载: 交由 $currentProviderType 适配器处理")
            AdsLoadHandler(placement, adapter)
                .loadAd(context, adListener)
        }
    }
}


