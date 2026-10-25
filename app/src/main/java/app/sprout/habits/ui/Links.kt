package app.sprout.habits.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a web page in the browser; does nothing if the phone has none. */
fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No browser installed.
    }
}

/** Opens the mail app with a new message to [address]. */
fun openEmail(context: Context, address: String, subject: String) {
    val uri = Uri.parse("mailto:" + address + "?subject=" + Uri.encode(subject))
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No mail app installed.
    }
}
