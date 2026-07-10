package com.kit.ads

import android.util.Log

/**
 * 日志工具类，封装 Android [Log] API，支持日志开关。
 *
 * 默认情况下日志在调试构建（BuildConfig.DEBUG = true）时开启；
 * 可通过 [enabled] 在运行时动态开关，无需重新打包。
 *
 * 使用方式：
 * ```
 * AdsLogger.d(TAG, "消息")
 * AdsLogger.e(TAG, "错误信息")
 * ```
 */
object AdsLogger {
    /**
     * 日志开关。设为 false 可全局关闭 SDK 内部日志输出。
     */
    var enabled: Boolean = BuildConfig.DEBUG

    /** 输出 DEBUG 级别日志 */
    fun d(tag: String, msg: String) {
        if (enabled) Log.d(tag, msg)
    }

    /** 输出 WARN 级别日志 */
    fun w(tag: String, msg: String) {
        if (enabled) Log.w(tag, msg)
    }

    /** 输出 ERROR 级别日志 */
    fun e(tag: String, msg: String) {
        if (enabled) Log.e(tag, msg)
    }

    /** 输出 ERROR 级别日志（含异常堆栈） */
    fun e(tag: String, msg: String, tr: Throwable) {
        if (enabled) Log.e(tag, msg, tr)
    }
}
