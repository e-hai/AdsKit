package com.an.ads.event


interface AdEventObserver {
    fun onAdEvent(eventType: AdEventType)
}