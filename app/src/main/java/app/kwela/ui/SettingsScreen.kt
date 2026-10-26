package app.kwela.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.kwela.BuildConfig
import app.kwela.domain.ThemeMode

private const val LICENSES = "AndroidX, Jetpack Compose, Material 3, Room and DataStore: Apache License 2.0\n\nKotlin and kotlinx.coroutines: Apache License 2.0\n\nNoto Sans: SIL Open Font License 1.1"

/** Full-screen settings, laid out as a few big cards: only what matters in Kwela. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notifications: Boolean, toggleNotifications: (Boolean) -> Unit, analyticsEnabled: Boolean, toggleAnalytics: (Boolean) -> Unit,
    themeMode: ThemeMode, setThemeMode: (ThemeMode) -> Unit, close: () -> Unit
) {
    BackHandler(onBack = close)
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var showAbout by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> toggleNotifications(granted) }
    val setNotifications = { enabled: Boolean ->
        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else toggleNotifications(enabled)
    }
    Column(Modifier.fillMaxSize().background(colors.surface).windowInsetsPadding(WindowInsets.statusBars).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            IconButton(close, Modifier.size(56.dp).align(Alignment.CenterStart).padding(start = 4.dp)) { Icon(Icons.Outlined.Close, "Close settings", Modifier.size(26.dp), tint = colors.onSurface) }
            Text("SETTINGS", Modifier.align(Alignment.Center), style = MaterialTheme.typography.labelLarge, color = colors.primary, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Text("Light, automatic or dark", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeTile(Icons.Outlined.LightMode, "Light", themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) { setThemeMode(ThemeMode.LIGHT) }
                        ThemeTile(Icons.Outlined.BrightnessAuto, "Automatic", themeMode == ThemeMode.SYSTEM, Modifier.weight(1f)) { setThemeMode(ThemeMode.SYSTEM) }
                        ThemeTile(Icons.Outlined.DarkMode, "Dark", themeMode == ThemeMode.DARK, Modifier.weight(1f)) { setThemeMode(ThemeMode.DARK) }
                    }
                }
            }
            Surface(onClick = { setNotifications(!notifications) }, shape = RoundedCornerShape(28.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reply notifications", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                        Text("A message when Kwela finishes while you are away. It never shows your CV on a locked screen.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(notifications, setNotifications)
                }
            }
            Surface(onClick = { toggleAnalytics(!analyticsEnabled) }, shape = RoundedCornerShape(28.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Usage statistics", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                        Text("Help improve Kwela by counting which parts are used. Never your CV, messages or files.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(analyticsEnabled, toggleAnalytics)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val support = BuildConfig.SUPPORT_EMAIL
                SmallCard("Help and feedback", "Questions or ideas? Write to us.", Modifier.weight(1f)) { if (support.isNotBlank()) sendEmail(context, support, "Kwela feedback (${BuildConfig.VERSION_NAME})") }
                SmallCard("About Kwela", "Version, privacy and licences", Modifier.weight(1f)) { showAbout = true }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (showAbout) AboutSheet { showAbout = false }
}

@Composable
private fun ThemeTile(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.height(120.dp).clip(RoundedCornerShape(22.dp)).background(if (selected) colors.surfaceContainerHighest else colors.surfaceContainerLow)
            .clickable(role = Role.RadioButton, onClickLabel = label, onClick = onClick),
        Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(34.dp), tint = if (selected) colors.onSurface else colors.onSurfaceVariant)
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).width(64.dp).height(4.dp).background(if (selected) colors.primary else colors.outlineVariant, CircleShape))
    }
}

@Composable
private fun SmallCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = colors.surfaceContainer, modifier = modifier.height(176.dp)) {
        Column(Modifier.padding(16.dp).fillMaxHeight()) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text(subtitle, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.align(Alignment.End), tint = colors.onSurfaceVariant)
        }
    }
}

/** Version, the legal links, how Kwela treats your data, and the open-source licences. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(dismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var showLicenses by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("About Kwela", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(
                "Your chats and files are saved on this phone. Messages and files you send are processed by Kwela’s servers and OpenAI. Task inputs are kept while work runs, then deleted on success or after 24 hours on failure. Completed replies stay on the server until this phone saves them. Your saved CV profile is kept on our server until you delete your account.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurface
            )
            Row {
                BuildConfig.PRIVACY_URL.takeIf { it.isNotBlank() }?.let { url -> TextButton(onClick = { openUrl(context, url) }) { Text("Privacy Policy") } }
                BuildConfig.TERMS_URL.takeIf { it.isNotBlank() }?.let { url -> TextButton(onClick = { openUrl(context, url) }) { Text("Terms of Service") } }
                TextButton(onClick = { showLicenses = true }) { Text("Licences") }
            }
        }
    }
    if (showLicenses) AlertDialog(onDismissRequest = { showLicenses = false }, title = { Text("Open-source licences") }, text = { Text(LICENSES, Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } })
}
