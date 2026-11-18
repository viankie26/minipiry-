package com.example.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.domain.logic.ReminderScheduleLogic
import com.example.domain.logic.ReminderSettings
import java.util.concurrent.TimeUnit

object ExpiryReminderScheduler {
  const val CHANNEL_ID = "minipiry_expiry_reminders"
  private const val UNIQUE_WORK_NAME = "minipiry_daily_expiry_check"

  fun hasNotificationPermission(context: Context): Boolean {
    val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun ensureNotificationChannel(context: Context, channelName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT,
      )
      manager.createNotificationChannel(channel)
    }
  }

  fun syncSchedule(
    context: Context,
    settings: ReminderSettings,
    channelName: String,
  ) {
    val workManager = WorkManager.getInstance(context.applicationContext)
    if (!settings.enabled || !hasNotificationPermission(context)) {
      workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
      return
    }

    ensureNotificationChannel(context, channelName)

    val initialDelayMs = ReminderScheduleLogic.computeInitialDelayMillis(
      targetHour = settings.remindHour,
      targetMinute = settings.remindMinute,
    )

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(24, TimeUnit.HOURS)
      .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
      .setConstraints(constraints)
      .build()

    workManager.enqueueUniquePeriodicWork(
      UNIQUE_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  fun cancelAll(context: Context) {
    WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
  }
}
