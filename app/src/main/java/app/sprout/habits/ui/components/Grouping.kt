package app.sprout.habits.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.SproutType

/*
 * The grouping rules, the same on every screen (the Journal's day groups are the model):
 * - A group is a list of white rows 2 dp apart, 24 dp corners on the outside of the group and
 *   6 dp between rows. Never a divider line.
 * - A group may have a section header: the title (16 sp SemiBold, text colour) and an optional
 *   meta (14 sp muted) or control on the right, 4 dp in from the cards' edge.
 * - 20 dp above a header (none for the first one under the screen header), 8 dp below it.
 */

/** Gap between the rows of one group. */
val GroupGap = 2.dp

/** Space above a section header, and between sections. */
val SectionGapAbove = 20.dp

/** Space between a section header and its group. */
val SectionGapBelow = 8.dp

/** The 24/6 dp corners of a grouped list: big on the outside of the group, small between its rows. */
fun groupedShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 24.dp else 6.dp
    val bottom = if (index == count - 1) 24.dp else 6.dp
    return RoundedCornerShape(top, top, bottom, bottom)
}

/**
 * A section header above a group. It has no space of its own above or below: the caller adds
 * [SectionGapAbove] and [SectionGapBelow] for its list's spacing.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = SproutType.cardTitle, color = colors.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp))
        when {
            trailing != null -> trailing()
            meta != null -> Text(meta, style = SproutType.supporting, color = colors.onSurfaceVariant)
        }
    }
}
