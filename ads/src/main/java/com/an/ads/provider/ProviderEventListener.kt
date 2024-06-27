package com.an.ads.provider


interface ProviderEventListener {

    fun onAdLoaded(ad: Any)

    fun onAdFailedToLoad(error: String)

    fun onAdShown()

    fun onAdClicked()

    fun onAdPaidEvent()

    fun onAdClosed()
}