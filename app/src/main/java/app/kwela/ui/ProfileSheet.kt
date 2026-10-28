package app.kwela.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.kwela.R
import app.kwela.BuildConfig
import app.kwela.domain.DisplayName

/** Account details and shortcuts opened from the profile chip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(email: String, knownName: String?, photoUrl: String?, openSettings: () -> Unit, signOut: () -> Unit, deleteAccount: () -> Unit, dismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val name = DisplayName.of(knownName, email)
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
            IconButton(onClick = dismiss) { Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Back") }
            Text("KWELA", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = colors.primary, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(name, 64.dp, photoUrl)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileRow(R.drawable.ms_settings, "Settings") { dismiss(); openSettings() }
                ProfileRow(R.drawable.ms_share, "Share Kwela") { shareText(context, "Kwela helps you apply for jobs fast, right from your phone: https://play.google.com/store/apps/details?id=${context.packageName}") }
                ProfileRow(R.drawable.ms_star, "Leave a review") { openPlayListing(context) }
                supportPageUrl().takeIf { it.isNotBlank() }?.let { url ->
                    ProfileRow(R.drawable.ms_help, "Help and feedback") { openUrl(context, url) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { dismiss(); signOut() }, Modifier.weight(1f).height(52.dp)) {
                    Icon(painterResource(R.drawable.ms_logout), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Sign out")
                }
                OutlinedButton(
                    onClick = { confirmDelete = true }, Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
                ) { Text("Delete account") }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                BuildConfig.PRIVACY_URL.takeIf { it.isNotBlank() }?.let { url -> TextButton(onClick = { openUrl(context, url) }) { Text("Privacy Policy", color = colors.onSurfaceVariant) } }
                Text("·", color = colors.onSurfaceVariant)
                BuildConfig.TERMS_URL.takeIf { it.isNotBlank() }?.let { url -> TextButton(onClick = { openUrl(context, url) }) { Text("Terms of Service", color = colors.onSurfaceVariant) } }
            }
        }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("Delete account and data?") },
        text = { Text("This removes your chats, files and sign-in details from this phone and signs you out. This can’t be undone.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; dismiss(); deleteAccount() }, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
    )
}

@Composable
private fun ProfileRow(icon: Int, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(56.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = colors.onSurface)
            Spacer(Modifier.width(18.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        }
    }
}

/** A round avatar with the person's first letter. */
@Composable
fun ProfileAvatar(name: String, size: androidx.compose.ui.unit.Dp, photoUrl: String? = null) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(size).background(colors.primaryContainer, CircleShape), Alignment.Center) {
        Text(DisplayName.initial(name), color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        if (photoUrl != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(photoUrl).diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    }
}
