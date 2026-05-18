package com.lulucloud.touchscript

import android.content.pm.ApplicationInfo
import android.app.Application
import com.lulucloud.touchscript.app.AppContainer
import timber.log.Timber

class TouchWorkshopApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
