package com.an.ads

interface AdListener {

    fun onAdLoaded(ad: AdEntity)

    fun onAdFailedToLoad(error: String)

    fun onAdShown()

    fun onAdClicked()

    fun onAdPaidEvent()

    fun onAdUserEarnedReward()

    fun onAdClosed()
}

abstract class AdCallback : AdListener {
    override fun onAdLoaded(ad: AdEntity) {
    }

    override fun onAdFailedToLoad(error: String) {
    }

    override fun onAdShown() {
    }

    override fun onAdClicked() {
    }

    override fun onAdPaidEvent() {
    }

    override fun onAdUserEarnedReward() {
    }

    override fun onAdClosed() {
    }

}