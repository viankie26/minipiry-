package com.example.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.CalendarIcsGenerator
import com.example.domain.model.AppLanguage
import com.example.domain.model.Item
import java.io.File

object CalendarExporter {

  /**
   * Generates a standard RFC 5545 `.ics` file via [CalendarIcsGenerator.buildCalendar]
   * and launches an Android system calendar/share sheet via [FileProvider].
   */
  fun exportItemsToCalendar(
    context: Context,
    items: List<Item>,
    language: AppLanguage,
    fileName: String = "minipiry-expiry.ics",
  ): Result<Unit> {
    return runCatching {
      val dict = I18nDict.of(language)
      val icsContent = CalendarIcsGenerator.buildCalendar(items, language)
      val dir = File(context.cacheDir, "calendar").apply { mkdirs() }
      val icsFile = File(dir, fileName)
      icsFile.writeText(icsContent, Charsets.UTF_8)

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        icsFile,
      )

      val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/calendar")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val resolved = viewIntent.resolveActivity(context.packageManager)
      val launchIntent = if (resolved != null) {
        Intent.createChooser(viewIntent, dict.addAll).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      } else {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/calendar"
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(sendIntent, dict.addAll).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      }

      context.startActivity(launchIntent)
    }
  }
}
