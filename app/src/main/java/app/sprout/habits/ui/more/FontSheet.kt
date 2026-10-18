package app.sprout.habits.ui.more

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.sprout.habits.R
import app.sprout.habits.ui.components.ChoiceRow
import app.sprout.habits.ui.components.ChoiceSheet
import app.sprout.habits.ui.components.tick
import app.sprout.habits.ui.theme.BodyFont
import app.sprout.habits.ui.theme.SproutType
import app.sprout.habits.ui.theme.family
import androidx.compose.ui.platform.LocalHapticFeedback

/** Each font's name (the same in every language; null = "System default") and its one-line description. */
private val FONT_INFO: Map<BodyFont, Pair<String?, Int>> = mapOf(
    BodyFont.LEXEND to ("Lexend" to R.string.font_lexend_desc),
    BodyFont.SYSTEM to (null to R.string.font_system_desc),
    BodyFont.SPACE_GROTESK to ("Space Grotesk" to R.string.font_space_grotesk_desc),
    BodyFont.OUTFIT to ("Outfit" to R.string.font_outfit_desc),
    BodyFont.ATKINSON to ("Atkinson Hyperlegible" to R.string.font_atkinson_desc),
)

@Composable
fun fontName(font: BodyFont): String = FONT_INFO.getValue(font).first ?: stringResource(R.string.font_system)

@StringRes
private fun fontDescription(font: BodyFont): Int = FONT_INFO.getValue(font).second

/** Font, as a sheet like Language: every font written in itself, with an "Aa" sample. */
@Composable
fun FontSheet(current: BodyFont, onPick: (BodyFont) -> Unit, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    ChoiceSheet(title = stringResource(R.string.font), onDismiss = onDismiss) {
        FONT_INFO.keys.forEach { font ->
            val family = font.family()
            ChoiceRow(
                fontName(font),
                stringResource(fontDescription(font)),
                selected = current == font,
                fontFamily = family,
                trailing = { Text("Aa", style = SproutType.title.copy(fontFamily = family), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            ) {
                if (current != font) haptics.tick()
                onPick(font)
            }
        }
    }
}
