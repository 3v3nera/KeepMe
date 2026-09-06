package com.telegramdrive.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.worker.CameraBackupWorker

/**
 * BroadcastReceiver triggered when the device boots or when the app is updated.
 * Guarantees automated daily backups continue even if the phone reboots or
 * the user does not open the app for weeks/months.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received action: $action - checking backup schedule")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val settings = SettingsDataStore(context.applicationContext)
            val isCameraBackup = settings.isCameraBackupEnabled()
            val isCustomFolder = settings.isCustomFolderBackupEnabled()

            if (isCameraBackup || isCustomFolder) {
                Log.d("BootReceiver", "Rescheduling 24h backup worker after boot")
                CameraBackupWorker.schedule(
                    context = context.applicationContext,
                    wifiOnly = settings.isBackupWifiOnly(),
                    chargingOnly = settings.isBackupChargingOnly(),
                    targetHour = settings.getBackupHour(),
                    targetMinute = settings.getBackupMinute()
                )
            }
        }
    }
}
