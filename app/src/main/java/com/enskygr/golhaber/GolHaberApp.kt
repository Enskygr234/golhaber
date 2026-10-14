package com.enskygr.golhaber

import android.app.Application
import com.enskygr.golhaber.notify.Notifier
import com.enskygr.golhaber.notify.Scheduler

class GolHaberApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        Scheduler.schedulePeriodic(this)
    }
}
