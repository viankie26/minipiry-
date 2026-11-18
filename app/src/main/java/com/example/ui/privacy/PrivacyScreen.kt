package com.example.ui.privacy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.i18n.I18nDict
import com.example.domain.model.AppLanguage
import com.example.ui.components.LanguageSwitch
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette

/**
 * Exact Compose port of `src/routes/privacy.tsx` from `viankie26/baozhiqi`.
 */
@Composable
fun PrivacyScreen(
  language: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current
  val context = LocalContext.current

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = palette.warmBg,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(palette.warmBg)
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .widthIn(max = 520.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 28.dp),
      ) {
        LanguageSwitch(
          language = language,
          onSelectLanguage = onSelectLanguage,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
          contentAlignment = Alignment.CenterStart,
          modifier = Modifier
            .testTag("privacy_back_button")
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onNavigateBack),
        ) {
          Text(
            text = "← ${dict.navBackAria}".uppercase(),
            style = EditorialKickerStyle,
            color = palette.mutedForeground,
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = dict.privacyTitle,
          style = MaterialTheme.typography.headlineMedium,
          color = palette.foreground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = dict.privacyUpdated,
          style = MaterialTheme.typography.bodySmall,
          color = palette.mutedForeground,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(palette.rule),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
          dict.privacySections.forEach { section ->
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.foreground,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.bullets.forEach { line ->
                  Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text = "•",
                      style = MaterialTheme.typography.bodyLarge,
                      color = palette.foreground,
                    )
                    Text(
                      text = line,
                      style = MaterialTheme.typography.bodyLarge,
                      color = palette.foreground,
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
          contentAlignment = Alignment.CenterStart,
          modifier = Modifier
            .testTag("privacy_email_link")
            .minimumInteractiveComponentSize()
            .clickable(
              role = Role.Button,
              onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hi@viankie.com"))
                try {
                  context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                  // Ignore if no email client installed on emulator
                }
              },
            ),
        ) {
          Text(
            text = "hi@viankie.com",
            style = MaterialTheme.typography.bodyMedium.copy(
              textDecoration = TextDecoration.Underline,
            ),
            color = palette.foreground,
          )
        }
      }
    }
  }
}
