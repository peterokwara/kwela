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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** The first screen: Kwela, one big "Continue with Google" button, and email as the way round it. */
@Composable
fun LoginScreen(
    email: String, otp: Boolean, testCode: String?, working: Boolean, error: String?,
    onEmail: (String) -> Unit, request: () -> Unit, verify: (String) -> Unit, back: () -> Unit,
    onGoogleToken: (String) -> Unit, onGoogleProblem: (String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var emailFormOpen by remember { mutableStateOf(!GoogleSignIn.available) }
    val requestCode = { focus.clearFocus(); request() }
    val signInWithGoogle = {
        val activity = context as? Activity
        if (activity != null && !working) scope.launch {
            when (val result = GoogleSignIn.requestToken(activity)) {
                is GoogleSignInResult.Token -> onGoogleToken(result.idToken)
                GoogleSignInResult.Dismissed -> onGoogleProblem(null)
                is GoogleSignInResult.Failed -> { onGoogleProblem(result.message); emailFormOpen = true }
            }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("kwela", Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.height(24.dp))
        if (!otp) ChatPreviewCard()
        Spacer(Modifier.height(24.dp))
        Text(
            if (otp && testCode != null) "Your sign-in code" else if (otp) "Check your email" else "Welcome to Kwela",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (otp && testCode != null) "Private test code: $testCode. Enter it to continue."
            else if (otp) "We sent a 6-digit code to $email. Enter it to continue."
            else "Saw a job tonight? Be ready to apply in minutes. No blank page, no cyber café queue.",
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        if (!otp) {
            if (GoogleSignIn.available) {
                Button(
                    onClick = { signInWithGoogle() }, Modifier.fillMaxWidth().height(56.dp), enabled = !working, shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceContainerHighest, contentColor = colors.onSurface),
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) { Text(googleLabel(), style = MaterialTheme.typography.titleMedium) }
            }
            if (emailFormOpen) {
                if (GoogleSignIn.available) Spacer(Modifier.height(20.dp))
                OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { requestCode() }))
                Spacer(Modifier.height(12.dp))
                if (GoogleSignIn.available) {
                    OutlinedButton(onClick = requestCode, Modifier.fillMaxWidth().height(52.dp), enabled = !working, shape = CircleShape) { Text(if (working) "Sending code…" else "Continue") }
                } else {
                    Button(onClick = requestCode, Modifier.fillMaxWidth().height(52.dp), enabled = !working, shape = CircleShape) { Text(if (working) "Sending code…" else "Continue") }
                }
            } else {
                TextButton(onClick = { emailFormOpen = true }, Modifier.fillMaxWidth().height(52.dp)) { Text("Use email instead") }
            }
        } else {
            OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it }, Modifier.fillMaxWidth(), label = { Text("6-digit code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (code.length == 6) { focus.clearFocus(); verify(code) } }))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { focus.clearFocus(); verify(code) }, Modifier.fillMaxWidth().height(52.dp), enabled = !working && code.length == 6, shape = CircleShape) { Text(if (working) "Checking…" else "Sign in") }
            Row {
                TextButton(onClick = back, enabled = !working) { Text("Use a different email") }
                TextButton(onClick = request, enabled = !working) { Text("Send a new code") }
            }
        }
        if (working && !otp && GoogleSignIn.available) Text("Signing you in…", Modifier.padding(top = 12.dp), color = colors.onSurfaceVariant)
        if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, color = colors.error) }
        Spacer(Modifier.height(24.dp))
        Text("Your chats and files are saved on this device.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** "Continue with Google", with the Google G in its own colours. */
@Composable
private fun googleLabel(): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)) { append("G") }
    append("   Continue with Google")
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
