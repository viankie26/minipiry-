package com.example.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.LanguagePreferencesRepository
import com.example.data.remote.FirestoreItemsRepository
import com.example.data.remote.FirestoreUserSettingsRepository
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.DateLogic
import com.example.domain.logic.ReminderScheduleLogic
import com.example.domain.logic.ReminderSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class ExpiryReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    if (!ExpiryReminderScheduler.hasNotificationPermission(applicationContext)) {
      return Result.success()
    }

    val currentUser = Firebase.auth.currentUser ?: return Result.success()
    val uid = currentUser.uid

    val prefsRepo = LanguagePreferencesRepository(applicationContext)
    val language = prefsRepo.getLanguage()
    val dict = I18nDict.of(language)

    val settingsRepo = FirestoreUserSettingsRepository(applicationContext)
    val settings = settingsRepo.loadSettings(uid).getOrNull() ?: ReminderSettings()
    if (!settings.enabled) {
      return Result.success()
    }

    val itemsRepo = FirestoreItemsRepository(applicationContext)
    val items = itemsRepo.refreshItems(uid).getOrElse { return Result.retry() }
    val alreadyNotified = prefsRepo.getNotifiedItemIds()

    val dueItems = ReminderScheduleLogic.itemsDueForNotification(
      items = items,
      daysBefore = settings.daysBefore,
      alreadyNotifiedIds = alreadyNotified,
    )

    if (dueItems.isEmpty()) {
      return Result.success()
    }

    ExpiryReminderScheduler.ensureNotificationChannel(applicationContext, dict.notifChannelName)

    val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      applicationContext,
      0,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val bodyText = dueItems.take(4).joinToString(" · ") { item ->
      val days = DateLogic.daysUntil(item.expiryDate)
      "${item.name}（${DateLogic.remainingLabel(days, language)}）"
    }

    val notification = NotificationCompat.Builder(
      applicationContext,
      ExpiryReminderScheduler.CHANNEL_ID,
    )
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(dict.notifTitle(dueItems.size))
      .setContentText(bodyText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      NotificationManagerCompat.from(applicationContext)
        .notify(NOTIFICATION_ID, notification)
      prefsRepo.markItemsNotified(dueItems.map { it.id })
    }

    return Result.success()
  }

  companion object {
    private const val NOTIFICATION_ID = 2026
  }
}
