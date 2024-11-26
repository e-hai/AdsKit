package com.kit.ads.event

import com.kit.ads.placement.AdTriggerPoint
import com.kit.ads.provider.AdProviderType


sealed class AdEventType
data class InitSuccess(val providerType: AdProviderType) : AdEventType()       // 初始化成功
data class InitFailure(val providerType: AdProviderType) : AdEventType()       // 初始化失败
data class LoadAdStart(val triggerPoint: AdTriggerPoint) : AdEventType()        // 开始加载广告
data class LoadAdSuccess(val triggerPoint: AdTriggerPoint) : AdEventType()      // 广告加载成功
data class LoadAdFailure(val triggerPoint: AdTriggerPoint) : AdEventType()      // 广告加载失败
data class AdShown(val triggerPoint: AdTriggerPoint) : AdEventType()            // 广告展示
data class AdClicked(val triggerPoint: AdTriggerPoint) : AdEventType()          // 广告点击
data class AdPaidEvent(val triggerPoint: AdTriggerPoint) : AdEventType()        // 广告付费事件
data class AdClosed(val triggerPoint: AdTriggerPoint) : AdEventType()           // 广告关闭
data class AdRewarded(val triggerPoint: AdTriggerPoint) : AdEventType()         // 广告奖励

