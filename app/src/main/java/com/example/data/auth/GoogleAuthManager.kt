package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.local.LanguagePreferencesRepository
import com.example.data.reminder.ExpiryReminderScheduler
import com.example.data.remote.FirestoreItemsRepository
import com.example.data.remote.FirestoreUserSettingsRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

object GoogleAuthManager {
  private const val TAG = "Auth"

  /**
   * Silent startup auto sign-in using a single `GetGoogleIdOption` per request.
   */
  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
  ) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
      return
    }
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }
    if (clientId.isBlank()) {
      onUnauthenticated()
      return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val cred = credentialManager.getCredential(context, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (_: Exception) {
        onUnauthenticated()
      }
    }
  }

  /**
   * Interactive Google Sign-In using a single `GetSignInWithGoogleOption` per request
   * to keep the Credential Manager bottom sheet responsive on API 36.
   */
  fun signInWithGoogleInteractive(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit,
  ) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }
    val activityContext = context.findActivity() ?: context
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val cred = credentialManager.getCredential(activityContext, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
          onAuthSuccess()
        } else {
          onAuthError("Unexpected credential type")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Google Sign-In failed")
      }
    }
  }

  /**
   * Clears Credential Manager session inside `NonCancellable` before signing out of Firebase Auth
   * so unmounting UI does not cancel teardown.
   */
  fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit = {},
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          Firebase.auth.signOut()
          onComplete()
        }
      }
    }
  }

  /**
   * Permanently deletes all user items and reminder settings in Firestore, cancels local
   * WorkManager schedules, deletes the Firebase Auth user (re-authenticating via Google if needed),
   * and clears Credential Manager state.
   */
  suspend fun deleteAccountAndAllData(
    context: Context,
    credentialManager: CredentialManager,
  ): Result<Unit> {
    val user = Firebase.auth.currentUser
      ?: return Result.failure(IllegalStateException("Not signed in"))
    val uid = user.uid
    val appContext = context.applicationContext

    return try {
      // 1. Delete all owned items & user_settings in Cloud Firestore while still authenticated
      val itemsRepo = FirestoreItemsRepository(appContext)
      val settingsRepo = FirestoreUserSettingsRepository(appContext)
      itemsRepo.deleteAllItemsForUser(uid).getOrThrow()
      settingsRepo.deleteSettings(uid).getOrThrow()

      // 2. Cancel local WorkManager reminders & clear local history
      ExpiryReminderScheduler.cancelAll(appContext)
      LanguagePreferencesRepository(appContext).clearNotifiedHistory()

      // 3. Delete Firebase Auth account (re-authenticate via Google if recent login required)
      try {
        user.delete().await()
      } catch (_: FirebaseAuthRecentLoginRequiredException) {
        val clientId = context.getString(R.string.default_web_client_id)
        val activityContext = context.findActivity() ?: context
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val req = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        val cred = credentialManager.getCredential(activityContext, req).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          user.reauthenticate(GoogleAuthProvider.getCredential(token, null)).await()
          user.delete().await()
        } else {
          throw IllegalStateException("Re-authentication failed")
        }
      }

      // 4. Clear Credential Manager session
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state after deletion: ${e.message}")
        } finally {
          Firebase.auth.signOut()
        }
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Account deletion failed", e)
      Result.failure(e)
    }
  }
}
