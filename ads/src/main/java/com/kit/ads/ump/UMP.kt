package com.kit.ads.ump

import android.app.Activity
import android.util.Log
import com.kit.ads.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * 相当于国内隐私弹窗，广告前需要显示
 * **/
object UMP {
    private const val TAG = "UMP"


    fun start(activity: Activity, callBack: (gathered: Boolean) -> Unit) {
        val params = if (BuildConfig.DEBUG) {//113EEF624273EC2397E1488757427CE9
            val debugDeviceId =
                "113EEF624273EC2397E1488757427CE9" //测试手机的ID，在log可以找到，更改测试手机需先查看是否一致//D1EE6CD8C9EE34EA3AD8541BEEE23D1B
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(debugDeviceId)
                .build()
            ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(debugSettings)
                .build()
        } else {
            ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        if (BuildConfig.DEBUG) {
            // 通过 UMP SDK 测试应用时，您可能会发现重置 SDK 的状态很有帮助，以便模拟用户的首次安装体验。该 SDK 提供的 reset() 方法可实现此目的
            consentInformation.reset()
        }

        consentInformation.requestConsentInfoUpdate(activity, params,
            {
                if (activity.isFinishing) {
                    Log.e(TAG, "activity.isFinishing")
                } else {
                    //Load and show the consent form.
                    showConsentForm(consentInformation, activity, callBack)
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                Log.d(
                    TAG, "requestConsentError:" + String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
                if (activity.isFinishing) {
                    Log.e(TAG, "activity.isFinishing")
                } else {
                    checkUser(consentInformation, callBack)
                }

            })
    }


    private fun showConsentForm(
        consentInformation: ConsentInformation,
        activity: Activity,
        callBack: (gathered: Boolean) -> Unit
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
            // Consent gathering failed.
            Log.d(
                TAG, "showConsentForm:" + String.format(
                    "%s: %s",
                    loadAndShowError?.errorCode,
                    loadAndShowError?.message
                )
            )
            if (activity.isFinishing) {
                Log.e(TAG, "activity.isFinishing")
            } else {
                // Consent has been gathered.
                checkUser(consentInformation, callBack)
            }
        }
    }

    private fun checkUser(
        consentInformation: ConsentInformation,
        callBack: (gathered: Boolean) -> Unit
    ) {
        if (consentInformation.canRequestAds()) {
            callBack.invoke(true)
            Log.d(TAG, "用户同意")
        } else {
            callBack.invoke(false)
            Log.d(TAG, "用户拒绝")
        }
    }

}