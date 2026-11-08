package com.arborecence

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArborecenceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensure(this)
    }
}
