package com.kit.ads.ump

import android.app.Activity
import com.kit.ads.AdsLogger
import com.kit.ads.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * 该对象实现了类似于国内隐私政策弹窗的功能，用于在广告展示前询问用户的隐私同意。
 * 通过集成 UMP SDK (User Messaging Platform)，来收集用户的同意信息。
 * 根据用户的同意情况，决定是否展示广告。
 */
object UMP {
    private const val TAG = "AdsKit-UMP"

    /**
     * 启动用户隐私同意弹窗。
     * 根据当前环境（调试模式或正式模式）设置隐私政策请求参数，并请求用户同意。
     *
     * @param activity 当前的 Activity，用于显示隐私同意弹窗。
     * @param callBack 用户同意状态的回调函数，传入 `true` 表示同意，`false` 表示拒绝。
     */
    fun start(activity: Activity, callBack: (gathered: Boolean) -> Unit) {
        // 根据是否为调试环境，设置不同的隐私同意请求参数。
        val params = if (BuildConfig.DEBUG) {
            // 如果是调试模式，使用测试设备 ID 和调试地理位置设置
            val debugDeviceId =
                "113EEF624273EC2397E1488757427CE9" // 测试设备的 ID
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(debugDeviceId) // 添加测试设备 ID
                .build()
            ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(debugSettings) // 设置调试模式的参数
                .build()
        } else {
            // 如果是正式环境，不允许低于法定年龄的用户同意收集广告数据
            ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false) // 设置用户是否同意广告收集
                .build()
        }

        // 获取用户隐私同意信息
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        // 如果是调试模式，重置 SDK 状态以模拟用户首次安装的体验
        if (BuildConfig.DEBUG) {
            consentInformation.reset() // 重置同意信息
        }

        // 请求更新隐私同意信息
        consentInformation.requestConsentInfoUpdate(activity, params,
            {
                if (activity.isFinishing) {
                    AdsLogger.e(TAG, "activity.isFinishing") // 如果 Activity 正在销毁，则不处理
                } else {
                    // 如果用户的同意信息更新成功，加载并展示同意表单
                    showConsentForm(consentInformation, activity, callBack)
                }
            },
            { requestConsentError ->
                // 如果请求隐私同意信息失败
                AdsLogger.d(
                    TAG, "requestConsentError:" + String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
                if (activity.isFinishing) {
                    AdsLogger.e(TAG, "activity.isFinishing")
                } else {
                    // 如果获取同意信息失败，继续检查当前用户的隐私同意状态
                    checkUser(consentInformation, callBack)
                }
            })
    }

    /**
     * 显示隐私同意表单，向用户展示隐私政策。
     *
     * @param consentInformation 用户同意信息，用于确定是否已经收集到同意。
     * @param activity 当前的 Activity，用于显示同意表单。
     * @param callBack 用户同意状态的回调函数，传入 `true` 表示同意，`false` 表示拒绝。
     */
    private fun showConsentForm(
        consentInformation: ConsentInformation,
        activity: Activity,
        callBack: (gathered: Boolean) -> Unit
    ) {
        // 加载并展示同意表单
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
            // 如果加载或展示表单失败
            AdsLogger.d(
                TAG, "showConsentForm:" + String.format(
                    "%s: %s",
                    loadAndShowError?.errorCode,
                    loadAndShowError?.message
                )
            )
            if (activity.isFinishing) {
                AdsLogger.e(TAG, "activity.isFinishing")
            } else {
                // 如果同意表单显示失败，检查当前用户的隐私同意状态
                checkUser(consentInformation, callBack)
            }
        }
    }

    /**
     * 检查用户是否同意收集广告数据。
     *
     * @param consentInformation 用户的隐私同意信息。
     * @param callBack 用户同意状态的回调函数，传入 `true` 表示同意，`false` 表示拒绝。
     */
    private fun checkUser(
        consentInformation: ConsentInformation,
        callBack: (gathered: Boolean) -> Unit
    ) {
        if (consentInformation.canRequestAds()) {
            // 如果用户同意收集广告数据
            callBack.invoke(true)
            AdsLogger.d(TAG, "用户同意")
        } else {
            // 如果用户拒绝收集广告数据
            callBack.invoke(false)
            AdsLogger.d(TAG, "用户拒绝")
        }
    }
}
