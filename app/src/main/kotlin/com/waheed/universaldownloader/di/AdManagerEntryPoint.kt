package com.waheed.universaldownloader.di

import com.waheed.universaldownloader.ads.AdManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdManagerEntryPoint {
    fun adManager(): AdManager
}

fun getAdManager(context: Context): AdManager {
    val appContext = context.applicationContext
    val entryPoint = EntryPointAccessors.fromApplication(appContext, AdManagerEntryPoint::class.java)
    return entryPoint.adManager()
}
