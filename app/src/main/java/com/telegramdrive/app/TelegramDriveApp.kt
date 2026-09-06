package com.telegramdrive.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.telegramdrive.app.worker.TrashPurgeWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import javax.inject.Inject

@HiltAndroidApp
class TelegramDriveApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsDataStore: com.telegramdrive.app.data.datastore.SettingsDataStore

    @Inject
    lateinit var telegramManager: com.telegramdrive.app.engine.TelegramManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (settingsDataStore.hasApiCredentials()) {
            telegramManager.initClient()
        }
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val purgeWorkRequest = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrashPurgeWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            purgeWorkRequest
        )

        if (settingsDataStore.isCameraBackupEnabled() || settingsDataStore.isCustomFolderBackupEnabled()) {
            com.telegramdrive.app.worker.CameraBackupWorker.schedule(
                context = this,
                wifiOnly = settingsDataStore.isBackupWifiOnly(),
                chargingOnly = settingsDataStore.isBackupChargingOnly(),
                targetHour = settingsDataStore.getBackupHour(),
                targetMinute = settingsDataStore.getBackupMinute()
            )
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}
