package app.sprout.habits.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.SproutType
import app.sprout.habits.ui.theme.habitColors

/** The habit tile (SPACING.md → Sizes): 44dp, 14dp corners, a 22dp icon. Everywhere a habit is listed. */
val HabitTileSize = 44.dp
val HabitTileCorner = 14.dp
val HabitTileIconSize = 22.dp
val HabitTileShape = RoundedCornerShape(HabitTileCorner)

/** Today's partial ring: 44dp, 4dp stroke, round cap, from 12 o'clock. Also Insights and Get started. */
val ProgressRingSize = 44.dp
val ProgressRingStroke = 4.dp

/** Test tags, so component tests can find the pieces. */
object ComponentTags {
    const val HABIT_TILE = "habitTile"
    const val PROGRESS_RING = "progressRing"
}

/** The tile with any content: [container] behind it, [shape] (the habit tile's by default). */
@Composable
fun HabitTile(container: Color, modifier: Modifier = Modifier, shape: Shape = HabitTileShape, content: @Composable () -> Unit) {
    Box(
        modifier.size(HabitTileSize).background(container, shape).testTag(ComponentTags.HABIT_TILE),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The tile with an icon in [ink] on [container]. */
@Composable
fun HabitTile(@DrawableRes icon: Int, container: Color, ink: Color, modifier: Modifier = Modifier, shape: Shape = HabitTileShape) {
    HabitTile(container, modifier, shape) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(HabitTileIconSize))
    }
}

/** The usual habit tile: the habit's soft colour with its icon in the ink tone. */
@Composable
fun HabitTile(@DrawableRes icon: Int, hue: Float, modifier: Modifier = Modifier) {
    val hc = habitColors(hue)
    HabitTile(icon, hc.soft, hc.ink, modifier)
}

/**
 * The partial ring: the arc in the habit's solid colour on a track in its mid tone, [label] inside
 * (11sp SemiBold, text colour; stops scaling at 1.3x since the ring has a fixed size).
 * [progress] is clamped to 0..1; at 0 only the track shows.
 */
@Composable
fun ProgressRing(progress: Float, hue: Float, label: String, modifier: Modifier = Modifier, description: String = label) {
    val hc = habitColors(hue)
    val fraction = ringFraction(progress)
    Box(
        modifier.size(ProgressRingSize).testTag(ComponentTags.PROGRESS_RING).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = ProgressRingStroke.toPx()
            val radius = (size.minDimension - stroke) / 2
            drawCircle(hc.mid, radius, style = Stroke(stroke))
            if (fraction > 0f) drawArc(
                hc.solid, -90f, 360f * fraction, false,
                Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        CappedFontScale {
            Text(label, style = SproutType.tiny, color = MaterialTheme.colorScheme.onSurface, softWrap = false)
        }
    }
}

/** How much of the ring is drawn: [progress] clamped to 0..1 (NaN counts as 0). */
fun ringFraction(progress: Float): Float = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
