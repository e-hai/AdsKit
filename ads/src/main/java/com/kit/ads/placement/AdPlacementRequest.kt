package com.kit.ads.placement

import com.kit.ads.AdType
import com.kit.ads.provider.AdProviderType

/**
 * 广告位配置
 * **/
data class AdPlacementRequest(
    val placement: String,    //广告位标识
    val adUnitId: String,     //广告ID
    val adType: AdType,       //广告类型
    val providerType: AdProviderType, //广告提供商
)
