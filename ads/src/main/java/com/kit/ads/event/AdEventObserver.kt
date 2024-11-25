package com.kit.ads.event


interface AdEventObserver {
    fun onAdEvent(eventType: AdEventType)
}