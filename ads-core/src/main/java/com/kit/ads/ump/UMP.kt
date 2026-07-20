package com.kit.ads.ump

import android.app.Activity
import com.kit.ads.AdsDebug
import com.kit.ads.AdsLogger
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google UMP 隐私同意封装，供 AdMob / AppLovin 共用。
 */
object UMP {
    private const val TAG = "AdsKit-UMP"

    /**
     * 启动隐私同意流程。
     *
     * @param activity 用于展示同意表单
     * @param callBack `true` 表示 [ConsentInformation.canRequestAds] 为 true
     */
    fun start(activity: Activity, callBack: (gathered: Boolean) -> Unit) {
        AdsLogger.d(TAG, "start debug=${AdsDebug.isEnabled}")
        val params = if (AdsDebug.isEnabled) {
            val debugDeviceId = AdsDebug.admobTestDeviceIds.firstOrNull()
                ?: "113EEF624273EC2397E1488757427CE9"
            AdsLogger.d(TAG, "debugDeviceId=$debugDeviceId geography=EEA")
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(debugDeviceId)
                .build()
            ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build()
        } else {
            ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        if (AdsDebug.isEnabled) {
            consentInformation.reset()
            AdsLogger.d(TAG, "consent reset for debug")
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (activity.isFinishing) {
                    AdsLogger.e(TAG, "requestConsentInfoUpdate skipped reason=activityFinishing")
                } else {
                    AdsLogger.d(TAG, "requestConsentInfoUpdate success")
                    showConsentForm(consentInformation, activity, callBack)
                }
            },
            { requestConsentError ->
                AdsLogger.e(
                    TAG,
                    "requestConsentInfoUpdate failed errorCode=${requestConsentError.errorCode} error=${requestConsentError.message}",
                )
                if (activity.isFinishing) {
                    AdsLogger.e(TAG, "requestConsentInfoUpdate error skipped reason=activityFinishing")
                } else {
                    checkUser(consentInformation, callBack)
                }
            },
        )
    }

    private fun showConsentForm(
        consentInformation: ConsentInformation,
        activity: Activity,
        callBack: (gathered: Boolean) -> Unit,
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
            if (loadAndShowError != null) {
                AdsLogger.e(
                    TAG,
                    "showConsentForm failed errorCode=${loadAndShowError.errorCode} error=${loadAndShowError.message}",
                )
            } else {
                AdsLogger.d(TAG, "showConsentForm complete")
            }
            if (activity.isFinishing) {
                AdsLogger.e(TAG, "showConsentForm skipped reason=activityFinishing")
            } else {
                checkUser(consentInformation, callBack)
            }
        }
    }

    private fun checkUser(
        consentInformation: ConsentInformation,
        callBack: (gathered: Boolean) -> Unit,
    ) {
        val canRequestAds = consentInformation.canRequestAds()
        AdsLogger.d(
            TAG,
            "consentResult canRequestAds=$canRequestAds status=${consentInformation.consentStatus}",
        )
        callBack.invoke(canRequestAds)
    }
}
