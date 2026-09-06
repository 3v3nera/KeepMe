package com.telegramdrive.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.worker.CameraBackupWorker

/**
 * BroadcastReceiver triggered by AlarmManager at the exact scheduled backup time.
 * Bypasses Android Doze mode and JobScheduler deferrals to guarantee the backup runs
 * even when the app is swiped away from Recents or the phone is locked in deep sleep.
 */
class CameraBackupAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BackupAlarmReceiver", "Alarm fired for scheduled backup! Starting worker immediately.")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TDrive::AlarmWakeLock")
        wakeLock?.acquire(3 * 60 * 1000L) // 3-minute temporary safety lock

        try {
            val settings = SettingsDataStore(context.applicationContext)
            val isCamera = settings.isCameraBackupEnabled()
            val isCustom = settings.isCustomFolderBackupEnabled()

            if (isCamera || isCustom) {
                // 1. Immediately trigger the backup worker (OneTimeWorkRequest runs with zero delay)
                CameraBackupWorker.runOnce(
                    context = context.applicationContext,
                    wifiOnly = settings.isBackupWifiOnly(),
                    chargingOnly = settings.isBackupChargingOnly()
                )

                // 2. Schedule the next alarm 24 hours later
                CameraBackupWorker.schedule(
                    context = context.applicationContext,
                    wifiOnly = settings.isBackupWifiOnly(),
                    chargingOnly = settings.isBackupChargingOnly(),
                    targetHour = settings.getBackupHour(),
                    targetMinute = settings.getBackupMinute()
                )
            }
        } catch (e: Exception) {
            Log.e("BackupAlarmReceiver", "Error triggering scheduled backup", e)
        } finally {
            try {
                wakeLock?.release()
            } catch (_: Exception) {}
        }
    }

    companion object {
        const val ACTION_TRIGGER_BACKUP = "com.telegramdrive.app.ACTION_SCHEDULED_BACKUP"
    }
}
