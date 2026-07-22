package com.waheed.universaldownloader.di

import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Central Hilt module for app-wide singletons.
 * As we add Room, Retrofit, and the download engine, their @Provides
 * functions go here (or in dedicated modules like DatabaseModule.kt,
 * NetworkModule.kt if this file grows too large).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRemoteConfigManager(): RemoteConfigManager {
        return RemoteConfigManager()
    }
}
