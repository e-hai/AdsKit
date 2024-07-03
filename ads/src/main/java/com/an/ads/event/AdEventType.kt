package com.an.ads.event

import com.an.ads.placement.AdPlacementRequest
import com.an.ads.provider.AdProviderType


sealed class AdEventType
data class InitSuccess(val providerType: AdProviderType) : AdEventType()       // 初始化成功
data class InitFailure(val providerType: AdProviderType) : AdEventType()       // 初始化失败
data class LoadAdStart(val request: AdPlacementRequest) : AdEventType()        // 开始加载广告
data class LoadAdSuccess(val request: AdPlacementRequest) : AdEventType()      // 广告加载成功
data class LoadAdFailure(val request: AdPlacementRequest) : AdEventType()      // 广告加载失败
data class AdShown(val request: AdPlacementRequest) : AdEventType()            // 广告展示
data class AdClicked(val request: AdPlacementRequest) : AdEventType()          // 广告点击
data class AdPaidEvent(val request: AdPlacementRequest) : AdEventType()        // 广告付费事件
data class AdClosed(val request: AdPlacementRequest) : AdEventType()           // 广告关闭
data class AdRewarded(val request: AdPlacementRequest) : AdEventType()         // 广告奖励

