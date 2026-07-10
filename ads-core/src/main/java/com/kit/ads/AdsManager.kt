package com.kit.ads

import android.app.Activity
import java.util.concurrent.ConcurrentHashMap
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
    const val TAG = "AdsKit"

    /** 默认预加载 TTL：55 分钟（略短于常见 App Open / Rewarded 约 1 小时有效期） */
    const val DEFAULT_PRELOAD_TTL_MS: Long = 55L * 60L * 1000L

    @VisibleForTesting
    internal var testAdapterFactory: ((AdsProviderType) -> AdsProviderAdapter)? = null

    @VisibleForTesting
    @Volatile
    internal var clockMs: () -> Long = { System.currentTimeMillis() }

    // 预加载广告缓存，按 triggerId 索引，消费后立即移除
    private val preloadedAds = ConcurrentHashMap<String, PreloadedEntry>()

    private data class PreloadedEntry(
        val ad: AdsEntity,
        val cachedAtMs: Long,
        val ttlMs: Long,
    ) {
        fun isExpired(nowMs: Long): Boolean =
            ttlMs > 0L && nowMs - cachedAtMs >= ttlMs
    }

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
     * 当前初始化状态（只读）。
     */
    fun getInitState(): AdsInitState = synchronized(stateLock) {
        when (initState) {
            InitState.IDLE -> AdsInitState.IDLE
            InitState.INITIALIZING -> AdsInitState.INITIALIZING
            InitState.READY -> AdsInitState.READY
            InitState.FAILED -> AdsInitState.FAILED
        }
    }

    /**
     * 当前已初始化成功的 Provider；未 READY 时为 null。
     */
    fun getInitializedProviderType(): AdsProviderType? =
        synchronized(stateLock) { initializedProviderType }

    private fun resolveTtlMs(request: AdsRequest): Long =
        request.preloadTtlMs ?: DEFAULT_PRELOAD_TTL_MS

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
        if (testAdapterFactory == null && !AdsProviderAdapterFactory.isAvailable(providerType)) {
            AdsLogger.e(
                TAG,
                "initialize failed provider=$providerType error=Provider not on classpath. Add AdsKit-admob or AdsKit-applovin.",
            )
            synchronized(stateLock) {
                initState = InitState.FAILED
            }
            dispatchToMain { onResult?.invoke(false) }
            return
        }

        val adapter = testAdapterFactory?.invoke(providerType)
            ?: AdsProviderAdapterFactory.create(providerType)
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
                    dispatchToMain { onResult?.invoke(false) }
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
                        dispatchToMain { onResult?.invoke(true) }
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
        // Provider 切换 / 重新初始化时丢弃旧缓存，避免返回绑定到旧 adapter 的 AdsEntity
        clearPreloadedAds()

        AdsLogger.d(TAG, "initialize start provider=$providerType token=$token")
        // 初始化广告适配器，完成后通知观察者
        adapter.initialize(context, config) { success ->
            val shouldNotify = synchronized(stateLock) {
                // 丢弃过期回调（例如初始化后又被 destroy 或再次 initialize）
                if (token != initToken) {
                    AdsLogger.w(
                        TAG,
                        "initialize staleCallback provider=$providerType token=$token currentToken=$initToken",
                    )
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
            if (shouldNotify) {
                if (success) {
                    AdsLogger.d(TAG, "initialize complete provider=$providerType success=true state=READY")
                } else {
                    AdsLogger.e(TAG, "initialize complete provider=$providerType success=false state=FAILED")
                }
                dispatchToMain { onResult?.invoke(success) }
            }
        }
    }

    /**
     * 销毁当前适配器，释放所有资源
     *
     * 调用后 AdsManager 回到未初始化状态，可重新 initialize。
     */
    fun destroy() {
        val toDestroy: AdsProviderAdapter?
        val previousProvider: AdsProviderType?
        synchronized(stateLock) {
            previousProvider = initializedProviderType
            toDestroy = adsProviderAdapter
            initState = InitState.IDLE
            initToken++
            adsProviderAdapter = null
            initializedProviderType = null
        }
        AdsLogger.d(TAG, "destroy previousProvider=$previousProvider")
        toDestroy?.destroy()
        clearPreloadedAds()
    }

    private fun clearPreloadedAds() {
        val entries = preloadedAds.values.toList()
        if (entries.isEmpty()) {
            return
        }
        preloadedAds.clear()
        AdsLogger.d(TAG, "clearPreloadedAds count=${entries.size}")
        entries.forEach { entry ->
            try {
                entry.ad.destroy()
            } catch (_: Exception) {
                // best-effort cleanup
            }
        }
    }

    @VisibleForTesting
    internal fun resetForTest() {
        destroy()
        testAdapterFactory = null
        clockMs = { System.currentTimeMillis() }
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
     * @param request 广告触发点请求
     * @param adListener 广告回调监听器
     */
    fun preloadAd(context: Context, request: AdsRequest) {
        AdsLogger.d(
            TAG,
            "preloadAd triggerId=${request.triggerId} type=${request.adType} provider=${request.providerType}"
        )
        val ttlMs = resolveTtlMs(request)
        val internalListener = object : AdCallback() {
            override fun onAdLoaded(ad: AdsEntity) {
                preloadedAds[request.triggerId] = PreloadedEntry(
                    ad = ad,
                    cachedAtMs = clockMs(),
                    ttlMs = ttlMs,
                )
                AdsLogger.d(
                    TAG,
                    "preloadAd cached triggerId=${request.triggerId} ttlMs=$ttlMs",
                )
            }

            override fun onAdFailedToLoad(error: String, errorCode: String?) {
                AdsLogger.e(
                    TAG,
                    "preloadAd failed triggerId=${request.triggerId} error=$error errorCode=$errorCode",
                )
            }
        }
        executeAdsRequest(context, request, internalListener)
    }

    fun loadAd(context: Context, request: AdsRequest, adListener: AdsListener) {
        executeAdsRequest(context, request, adListener)
    }

    /**
     * 构建并执行广告加载
     * @param context 上下文
     * @param request 广告触发点请求
     * @param adListener 广告回调监听器
     */
    private fun executeAdsRequest(
        context: Context,
        request: AdsRequest,
        adListener: AdsListener
    ) {
        // 检查预加载缓存（过期则丢弃并走网络）
        val cachedEntry = preloadedAds.remove(request.triggerId)
        if (cachedEntry != null) {
            val now = clockMs()
            if (cachedEntry.isExpired(now)) {
                AdsLogger.d(
                    TAG,
                    "loadAd cache expired triggerId=${request.triggerId} ageMs=${now - cachedEntry.cachedAtMs} ttlMs=${cachedEntry.ttlMs}",
                )
                cachedEntry.ad.destroy()
            } else {
                AdsLogger.d(
                    TAG,
                    "loadAd from cache triggerId=${request.triggerId} type=${request.adType} provider=${request.providerType}"
                )
                dispatchToMain {
                    adListener.onAdLoaded(cachedEntry.ad)
                }
                return
            }
        }

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
            "请求广告: triggerId=${request.triggerId}, type=${request.adType}, provider=${request.providerType}"
        )
        AdsLogger.d(TAG, "loadAd stateCheck state=$state adapter=${adapter != null}")

        if (state != InitState.READY || adapter == null) {
            // 如果没有初始化适配器，调用失败回调并通知全局观察者
            val errorMsg = when (state) {
                InitState.INITIALIZING -> "AdsManager initializing. Wait for initialize callback then retry."
                else -> "AdsManager not initialized. No adapter found."
            }
            val code = "STATE_$state"
            AdsLogger.w(
                TAG,
                "loadAd rejected triggerId=${request.triggerId} type=${request.adType} errorCode=$code",
            )
            dispatchToMain {
                adListener.onAdFailedToLoad(errorMsg, code)
            }
            return
        }

        if (currentProviderType != request.providerType) {
            val errorMsg = "Provider mismatch. Manager initialized with $currentProviderType, " +
                    "request requests ${request.providerType}."
            AdsLogger.w(
                TAG,
                "loadAd rejected triggerId=${request.triggerId} errorCode=PROVIDER_MISMATCH init=$currentProviderType request=${request.providerType}",
            )
            dispatchToMain {
                adListener.onAdFailedToLoad(errorMsg, "PROVIDER_MISMATCH")
            }
        } else {
            // 使用当前已初始化的适配器执行广告加载
            AdsLogger.d(
                TAG,
                "loadAd network triggerId=${request.triggerId} type=${request.adType} provider=${request.providerType}"
            )
            AdsLoadHandler(request, adapter)
                .loadAd(context, adListener)
        }
    }
}
