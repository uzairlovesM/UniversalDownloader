package com.waheed.universaldownloader.di

import android.content.Context
import androidx.room.Room
import com.waheed.universaldownloader.data.local.AppDatabase
import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.engine.YtDlpEngine
import com.waheed.universaldownloader.data.settings.SettingsDataStore
import com.waheed.universaldownloader.data.settings.PinManager
import com.waheed.universaldownloader.ads.AdManager
import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "universal_downloader.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: AppDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun providePinManager(@ApplicationContext context: Context): PinManager {
        return PinManager(context)
    }

    @Provides
    @Singleton
    fun provideAdManager(remoteConfigManager: RemoteConfigManager): AdManager {
        return AdManager(remoteConfigManager)
    }
}
