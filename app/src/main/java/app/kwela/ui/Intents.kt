package app.kwela.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import app.kwela.BuildConfig

/** Small helpers for handing off to other apps: the browser, the share sheet and the Play Store. */
fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** The website's Support page, found next to the privacy page. Empty when no privacy page is set, which hides the Help rows. */
fun supportPageUrl(): String =
    BuildConfig.PRIVACY_URL.takeIf { it.isNotBlank() }
        ?.let { Uri.parse(it).buildUpon().encodedPath("/support").clearQuery().fragment(null).build().toString() }
        .orEmpty()

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(intent, "Share Kwela").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Opens Kwela's page in the Play Store app, or in the browser when the Play Store is not there. */
fun openPlayListing(context: Context) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(market) }.onFailure { openUrl(context, "https://play.google.com/store/apps/details?id=$id") }
}
