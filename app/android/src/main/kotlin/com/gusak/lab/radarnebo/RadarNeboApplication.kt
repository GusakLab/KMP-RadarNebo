package com.gusak.lab.radarnebo

import android.app.Application
import com.gusak.lab.radarnebo.di.androidModule
import com.gusak.lab.radarnebo.di.initKoin
import com.gusak.lab.radarnebo.di.trafficLogModule
import org.koin.android.ext.koin.androidContext

/** Starts Koin once per process, before any activity asks for its ViewModel. */
class RadarNeboApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule) {
            androidContext(this@RadarNeboApplication)
            if (BuildConfig.DEBUG) modules(trafficLogModule())
        }
    }
}
