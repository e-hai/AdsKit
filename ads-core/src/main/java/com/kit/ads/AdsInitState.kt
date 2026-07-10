package com.kit.ads

/**
 * AdsManager 初始化状态（对外只读）。
 */
enum class AdsInitState {
    /** 未调用 [AdsManager.initialize]，或已 [AdsManager.destroy] */
    IDLE,

    /** [AdsManager.initialize] 进行中 */
    INITIALIZING,

    /** 初始化成功，可 [AdsManager.loadAd] / [AdsManager.preloadAd] */
    READY,

    /** 初始化失败 */
    FAILED,
}
