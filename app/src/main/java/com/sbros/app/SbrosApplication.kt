package com.sbros.app

import android.app.Application
import com.sbros.app.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SbrosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedulePeriodicSync(this)
    }
}
