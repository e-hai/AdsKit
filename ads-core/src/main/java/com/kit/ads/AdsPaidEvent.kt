package com.kit.ads

/**
 * 广告收益事件（impression-level revenue）。
 *
 * @param valueMicros 收益，单位 micro（1 USD = 1_000_000 micros）。AppLovin 的 USD 金额会换算为此单位。
 * @param currencyCode ISO 4217 货币码，如 `"USD"`
 * @param precision 精度描述（AdMob precisionType 或 AppLovin revenuePrecision），可能为空
 */
data class AdsPaidEvent(
    val valueMicros: Long,
    val currencyCode: String,
    val precision: String = "",
)
