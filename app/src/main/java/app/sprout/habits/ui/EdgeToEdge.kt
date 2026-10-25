package app.sprout.habits.ui

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat

/**
 * Draws the app behind the status and navigation bars, with bar icons that suit [dark].
 *
 * Replaces androidx.activity's enableEdgeToEdge(), whose code for Android 8–14 calls
 * Window.setStatusBarColor / setNavigationBarColor. Those are deprecated on Android 15, and Play
 * Console flags any app that contains them ("uses deprecated APIs or parameters for edge-to-edge").
 * Here: Android 15+ is edge-to-edge already (targetSdk 35+), so only the icon colours are set; on
 * older versions the theme makes the bars transparent (values/themes.xml) and the window is laid
 * out behind them.
 */
fun Activity.drawEdgeToEdge(dark: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
    }
    // No grey scrim behind 3-button navigation: the screens pad themselves above the bar.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }
}
