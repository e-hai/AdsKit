package com.kit.ads

import com.kit.ads.provider.AdsProviderType

/**
 * 广告触发点配置
 *
 * 该类用于标识广告触发的特定位置或场景，例如应用中的某个界面或功能模块。
 * 由调用者定义，用于决定何时以及在哪个位置触发广告的加载和展示。
 */
data class AdsRequest(
    /**
     * 广告位标识
     *
     * 唯一标识广告触发点的位置或场景。这个标识通常由开发者根据需要指定，用于区分不同的广告位置或场景。
     */
    val triggerId: String,  // 广告触发点标识

    /**
     * 广告单元ID
     *
     * 每个广告位置关联的广告单元的ID，通常在广告平台上创建广告单元时自动生成。
     */
    val adUnitId: String,  // 广告ID

    /**
     * 广告类型
     *
     * 定义广告的类型，如插屏广告、激励广告等，广告类型通常会影响广告的加载和展示方式。
     */
    val adType: AdsType,  // 广告类型

    /**
     * 广告提供商类型
     *
     * 指定广告提供商的类型，广告SDK根据该类型加载相应的广告SDK进行展示。
     */
    val providerType: AdsProviderType,  // 广告提供商类型
)

