package com.waheed.universaldownloader.di

import android.content.Context
import com.waheed.universaldownloader.engine.YtDlpEngine
import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Central Hilt module for app-wide singletons.
 * As we add Room and Retrofit, their @Provides functions go here
 * (or dedicated modules like DatabaseModule.kt if this file grows too large).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRemoteConfigManager(): RemoteConfigManager {
        return RemoteConfigManager()
    }

    @Provides
    @Singleton
    fun provideYtDlpEngine(@ApplicationContext context: Context): YtDlpEngine {
        return YtDlpEngine(context)
    }
}
