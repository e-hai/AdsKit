package com.an.ads.sample

import android.app.Application
import com.an.ads.AdManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(applicationContext)
    }
}