package app.kwela.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import app.kwela.KwelaApplication
import app.kwela.analytics.AnalyticsEvents
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Sign in with a Google account. */
@Composable
fun LoginScreen(
    working: Boolean, error: String?,
    onGoogleToken: (String, String?) -> Unit, onGoogleProblem: (String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analytics = (context.applicationContext as KwelaApplication).analytics
    // True while Google's sheet is open, so a second tap cannot open another one.
    var googleSheetOpen by remember { mutableStateOf(false) }
    var bottomSheetShown by rememberSaveable { mutableStateOf(false) }
    val signInWithGoogle = {
        val activity = context as? Activity
        if (activity != null && !working && !googleSheetOpen) scope.launch {
            googleSheetOpen = true
            analytics.track(AnalyticsEvents.googleButtonClick)
            try {
                when (val result = GoogleSignIn.requestToken(activity)) {
                    is GoogleSignInResult.Token -> onGoogleToken(result.idToken, result.photoUrl)
                    GoogleSignInResult.Dismissed, GoogleSignInResult.NoCredential -> Unit
                    is GoogleSignInResult.Failed -> onGoogleProblem(result.message)
                }
            } finally {
                googleSheetOpen = false
            }
        }
    }
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (!bottomSheetShown && GoogleSignIn.available && activity != null && !working) {
            bottomSheetShown = true
            googleSheetOpen = true
            try {
                when (val result = GoogleSignIn.requestBottomSheet(activity)) {
                    is GoogleSignInResult.Token -> onGoogleToken(result.idToken, result.photoUrl)
                    GoogleSignInResult.Dismissed, GoogleSignInResult.NoCredential -> Unit
                    is GoogleSignInResult.Failed -> onGoogleProblem(result.message)
                }
            } finally { googleSheetOpen = false }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("kwela", Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.height(24.dp))
        ChatPreviewCard()
        Spacer(Modifier.height(24.dp))
        Text(
            "Welcome to Kwela",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Saw a job tonight? Be ready to apply in minutes. No blank page, no cyber café queue.",
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        if (GoogleSignIn.available) {
            Button(
                onClick = { signInWithGoogle() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = !working && !googleSheetOpen,
                shape = CircleShape
            ) { Text("Sign in with Google", style = MaterialTheme.typography.titleMedium) }
        } else {
            Text("Google sign-in is unavailable. Please try again later.", color = colors.error)
        }
        if (working) Text("Signing you in…", Modifier.padding(top = 12.dp), color = colors.onSurfaceVariant)
        if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, color = colors.error) }
        Spacer(Modifier.height(24.dp))
        Text("Your chats and files are saved on this device.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** A small, still picture of the chat, so the first screen shows what Kwela is. */
@Composable
private fun ChatPreviewCard() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(32.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PreviewBubble("Saw a job tonight?", fromMe = true)
            PreviewBubble("Send me the ad and your CV.", fromMe = false)
            PreviewBubble("Your CV and cover letter are ready.", fromMe = false)
        }
    }
}

@Composable
private fun PreviewBubble(text: String, fromMe: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(shape = RoundedCornerShape(22.dp), color = if (fromMe) colors.primaryContainer else colors.surfaceContainerHigh, modifier = Modifier.widthIn(max = 260.dp)) {
            Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = if (fromMe) colors.onPrimaryContainer else colors.onSurface)
        }
    }
}
