package com.kit.ads.event

class AdEventObserverManager {
    private val observers: MutableList<AdEventObserver> = mutableListOf()

    /**
     * 注册观察者
     **/
    fun registerObserver(observer: AdEventObserver) {
        observers.add(observer)
    }

    /**
     * 取消注册观察者
     **/
    fun unregisterObserver(observer: AdEventObserver) {
        observers.remove(observer)
    }

    /**
     * 通知所有观察者
     **/
    fun notifyObservers(
        eventType: AdEventType
    ) {
        for (observer in observers) {
            observer.onAdEvent(eventType)
        }
    }
}