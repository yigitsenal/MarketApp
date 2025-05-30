package com.yigitsenal.marketapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MarketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}