package com.telegramdrive.app.di

import android.content.Context
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.engine.TelegramManager
import androidx.work.WorkManager
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
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideTelegramManager(
        @ApplicationContext context: Context,
        settingsDataStore: SettingsDataStore
    ): TelegramManager {
        return TelegramManager(context, settingsDataStore)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }


}
