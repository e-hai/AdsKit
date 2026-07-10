package com.kit.ads.sample

import android.app.Application
import com.kit.ads.AdsDebug

/**
 * Sample Application。
 * SDK 初始化在 MainActivity 中按所选 Provider 演示，便于切换 AdMob / AppLovin。
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val testDeviceId = BuildConfig.ADMOB_TEST_DEVICE_ID
        if (testDeviceId.isNotEmpty()) {
            AdsDebug.admobTestDeviceIds = listOf(testDeviceId)
        }
    }
}
