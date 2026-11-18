package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.GoogleAuthManager
import com.example.domain.i18n.I18nDict
import com.example.domain.model.AppLanguage
import com.example.ui.components.LanguageSwitch
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette
import kotlinx.coroutines.launch

/**
 * Exact Swiss editorial AuthScreen ported from `src/routes/auth.tsx` in `viankie26/baozhiqi`,
 * authenticated exclusively via Firebase Google Sign-In (`CredentialManager`).
 */
@Composable
fun AuthScreen(
  language: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  onOpenPrivacy: () -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }
  var isLoading by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    GoogleAuthManager.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      scope = scope,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = { /* Stay on interactive sign-in screen */ },
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = palette.warmBg,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(palette.warmBg)
        .padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 448.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.Start,
      ) {
        LanguageSwitch(
          language = language,
          onSelectLanguage = onSelectLanguage,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = dict.authKicker.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = dict.authTitle,
          style = MaterialTheme.typography.displayMedium,
          color = palette.foreground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2dp solid ink rule (`border-t-2 border-foreground`)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(palette.rule),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = dict.authSigninSub,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.mutedForeground,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
          onClick = {
            if (isLoading) return@Button
            isLoading = true
            GoogleAuthManager.signInWithGoogleInteractive(
              context = context,
              credentialManager = credentialManager,
              scope = scope,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthCancelled = {
                isLoading = false
              },
              onAuthError = { errMsg ->
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar(errMsg.ifBlank { dict.authFail }) }
              },
            )
          },
          enabled = !isLoading,
          shape = RectangleShape,
          border = BorderStroke(2.dp, palette.foreground),
          colors = ButtonDefaults.buttonColors(
            containerColor = palette.foreground,
            contentColor = palette.primaryForeground,
            disabledContainerColor = palette.muted,
            disabledContentColor = palette.mutedForeground,
          ),
          elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = palette.mutedForeground,
              strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = dict.authGoogleBusy.uppercase(),
              style = EditorialKickerStyle,
            )
          } else {
            Text(
              text = dict.authGoogle.uppercase(),
              style = EditorialKickerStyle,
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = dict.authPrivacyPre,
            style = MaterialTheme.typography.bodySmall,
            color = palette.mutedForeground,
          )
          Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clickable(
                role = Role.Button,
                onClick = onOpenPrivacy,
              )
              .testTag("auth_privacy_link"),
          ) {
            Text(
              text = dict.authPrivacyLink,
              style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = TextDecoration.Underline,
              ),
              color = palette.foreground,
            )
          }
        }
      }
    }
  }
}
