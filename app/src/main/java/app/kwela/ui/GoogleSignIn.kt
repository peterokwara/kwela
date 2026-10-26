package app.kwela.ui

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import app.kwela.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlin.coroutines.cancellation.CancellationException

/** What came of showing Google's "Choose an account" sheet. */
sealed interface GoogleSignInResult {
    data class Token(val idToken: String, val photoUrl: String?) : GoogleSignInResult
    /** The person closed the sheet. Nothing to say. */
    data object Dismissed : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

object GoogleSignIn {
    val available: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Shows Google's account sheet over the app and returns the signed ID token for the account the person picks. */
    suspend fun requestToken(activity: Activity): GoogleSignInResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
            .build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdTokenCredential.createFrom(credential.data).let { GoogleSignInResult.Token(it.idToken, it.profilePictureUri?.toString()) }
            } else GoogleSignInResult.Failed("Google sign-in didn’t work. Please try again.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Dismissed
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Failed("No Google account found on this phone. Add one in Settings and try again.")
        } catch (error: GoogleIdTokenParsingException) {
            Log.w("KwelaGoogleSignIn", "Google sent a token the app could not read", error)
            GoogleSignInResult.Failed("Google sign-in didn’t work. Please try again.")
        } catch (error: GetCredentialException) {
            // A wrong client id or signing fingerprint lands here, so keep the real reason for debugging.
            Log.w("KwelaGoogleSignIn", "Google sign-in failed: ${error.type}", error)
            GoogleSignInResult.Failed("Google sign-in didn’t work. Please try again.")
        }
    }
}
