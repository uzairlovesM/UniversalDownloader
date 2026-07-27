package com.waheed.universaldownloader

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.waheed.universaldownloader.ads.AdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UDApplication : Application(), Configuration.Provider, DefaultLifecycleObserver {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adManager: AdManager

    /** True once the app has gone to background at least once — used to decide
     *  whether to re-show the PIN lock on the next foreground. Cold-start always locks
     *  (handled separately by NavGraph's start destination check). */
    var requiresPinRecheck: Boolean = false
        private set

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        adManager.initialize(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App moved to background — next time it comes to foreground, re-verify PIN
        requiresPinRecheck = true
    }

    fun clearPinRecheckFlag() {
        requiresPinRecheck = false
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
