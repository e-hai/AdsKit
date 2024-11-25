package com.kit.ads.provider


interface ProviderListener {

    fun onAdStartedToLoad()

    fun onAdLoaded(ad: Any)

    fun onAdFailedToLoad(error: String)

    fun onAdShown()

    fun onAdClicked()

    fun onAdPaidEvent()

    fun onAdClosed()

    fun onAdUserEarnedReward()

}