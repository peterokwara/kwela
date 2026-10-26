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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlin.coroutines.cancellation.CancellationException

/** What came of showing Google's "Choose an account" sheet. */
sealed interface GoogleSignInResult {
    data class Token(val idToken: String, val photoUrl: String?) : GoogleSignInResult
    /** The person closed the sheet. Nothing to say. */
    data object Dismissed : GoogleSignInResult
    data object NoCredential : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

object GoogleSignIn {
    val available: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Shows the persistent button flow, including accounts that need reauthentication. */
    suspend fun requestToken(activity: Activity): GoogleSignInResult = request(activity, false, false)

    /** Offer previously authorised accounts first, then all accounts when none are available. */
    suspend fun requestBottomSheet(activity: Activity, allAccounts: Boolean = false): GoogleSignInResult {
        if (allAccounts) return request(activity, true, false)
        val authorised = request(activity, true, true)
        return if (authorised == GoogleSignInResult.NoCredential) request(activity, true, false) else authorised
    }

    private suspend fun request(activity: Activity, bottomSheet: Boolean, authorisedOnly: Boolean): GoogleSignInResult {
        val option = if (bottomSheet) {
            GetGoogleIdOption.Builder().setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(authorisedOnly).build()
        } else GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
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
            if (bottomSheet) GoogleSignInResult.NoCredential
            else GoogleSignInResult.Failed("No Google account found on this phone. Add one in Settings and try again.")
        } catch (error: GoogleIdTokenParsingException) {
            Log.w("KwelaGoogleSignIn", "Google sent a token the app could not read", error)
            GoogleSignInResult.Failed("Google sign-in didn’t work. Please try again.")
        } catch (error: GetCredentialException) {
            Log.w("KwelaGoogleSignIn", "Google sign-in failed: ${error.type}", error)
            GoogleSignInResult.Failed("Google sign-in didn’t work. Please try again.")
        }
    }
}
