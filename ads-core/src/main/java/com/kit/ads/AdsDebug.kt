package com.kit.ads

/**
 * Debug-mode toggle shared across core and provider modules.
 * Defaults to [BuildConfig.DEBUG] in the core artifact.
 */
object AdsDebug {
    @JvmField
    var isEnabled: Boolean = BuildConfig.DEBUG

    /**
     * AdMob test device hashed IDs (from Logcat "Use RequestConfiguration...").
     * Applied during AdMob initialize when [isEnabled] is true.
     */
    @JvmField
    var admobTestDeviceIds: List<String> = emptyList()
}
