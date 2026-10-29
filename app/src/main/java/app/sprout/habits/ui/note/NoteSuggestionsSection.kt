package app.sprout.habits.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.domain.Completion
import app.sprout.habits.domain.customOpeners
import app.sprout.habits.domain.quickStartPhrases
import app.sprout.habits.domain.recentNotes
import app.sprout.habits.domain.similarNotes
import app.sprout.habits.ui.components.GroupGap
import app.sprout.habits.ui.components.SectionGapAbove
import app.sprout.habits.ui.components.SectionGapBelow
import app.sprout.habits.ui.components.SectionHeader
import app.sprout.habits.ui.components.groupedShape
import app.sprout.habits.ui.components.tick
import app.sprout.habits.ui.datePattern
import app.sprout.habits.ui.theme.SproutType
import java.time.LocalDate

/*
 * The suggestions under the New note card. Before typing: "Quick start" chips for the day's
 * outcome and "Your recent notes" for this habit. While typing: "Finish with" chips and
 * "Similar notes". Editing a saved note shows only "Finish with". Every section hides when it has
 * nothing to show. They use the app's pieces: section headers, filter-chip sized chips, grouped rows.
 */

@Composable
fun NoteSuggestions(
    data: NoteSuggestionData,
    isNew: Boolean,
    noteId: Long,
    habitId: Long,
    status: EntryStatus?,
    typed: String,
    typing: Boolean,
    completions: List<Completion>,
    onInsertPhrase: (String) -> Unit,
    onUseRecent: (String) -> Unit,
    onUseSimilar: (String) -> Unit,
    onAddEnding: (String) -> Unit,
) {
    if (typing) {
        if (completions.isNotEmpty()) {
            Section(stringResource(R.string.suggest_finish_with), stringResource(R.string.suggest_tap_to_add), chips = true) {
                // Read out once per suggested sentence, not as part of the note.
                val announce = stringResource(R.string.suggest_ghost_a11y, completions.first().sentence)
                Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = announce }) {
                    // The chip shows whole words ("with Huda again" for "Got irritated wi"); the ghost
                    // text and the tap add only the rest.
                    val word = typed.substringAfterLast(' ')
                    ChipRow(completions.map { if (it.ending.first().isWhitespace()) it.ending.trim() else (word + it.ending).trim() }, highlightFirst = true) { i, _ -> onAddEnding(completions[i].ending) }
                }
            }
        }
        if (!isNew) return
        val afterChips = completions.isNotEmpty()
        val similar = remember(data, typed, noteId) { similarNotes(data.notes, typed, excludeId = noteId) }
        if (similar.isNotEmpty()) {
            Section(stringResource(R.string.suggest_similar), pluralStringResource(R.plurals.suggest_found, similar.size, similar.size), afterChips = afterChips) {
                val bold = SpanStyle(fontWeight = SproutType.label.fontWeight)
                NoteRows(
                    similar.map { s ->
                        PastRow(
                            id = s.note.id,
                            text = buildAnnotatedString {
                                append(s.text.substring(0, s.match.first))
                                withStyle(bold) { append(s.text.substring(s.match.first, s.match.last + 1)) }
                                append(s.text.substring(s.match.last + 1))
                            },
                            plain = s.text,
                            date = s.note.date,
                        )
                    },
                    onUse = onUseSimilar,
                )
            }
        }
        return
    }
    if (!isNew) return

    if (status != null) {
        val defaults = stringArrayResource(
            when (status) {
                EntryStatus.SKIP -> R.array.suggest_phrases_skipped
                EntryStatus.PARTIAL -> R.array.suggest_phrases_partial
                EntryStatus.DONE -> R.array.suggest_phrases_done
            },
        ).toList()
        val phrases = remember(data, status, defaults) { quickStartPhrases(customOpeners(data.notes, status), defaults) }
        val meta = stringResource(
            when (status) {
                EntryStatus.SKIP -> R.string.outcome_skipped
                EntryStatus.PARTIAL -> R.string.outcome_partial
                EntryStatus.DONE -> R.string.outcome_done
            },
        )
        Section(stringResource(R.string.suggest_quick_start), meta, chips = true) {
            ChipRow(phrases, highlightFirst = false) { _, phrase -> onInsertPhrase(phrase) }
        }
    }

    val recent = remember(data, habitId, noteId) { recentNotes(data.notes, habitId, excludeId = noteId) }
    if (recent.isNotEmpty()) {
        Section(stringResource(R.string.suggest_recent), stringResource(R.string.suggest_this_habit), afterChips = status != null) {
            NoteRows(recent.map { PastRow(it.id, AnnotatedString(it.text.trim()), it.text.trim(), it.date) }, onUse = onUseRecent)
        }
    }
}

/**
 * A section header 20 dp under what is above it and 8 dp over its content, 16 dp in from the
 * screen. A chip row's 48 dp touch target already adds 8 dp above and below the 32 dp chips, so
 * the gaps next to [chips] (or [afterChips]) are 8 dp smaller and still look 8 and 20 dp.
 */
@Composable
private fun Section(title: String, meta: String, chips: Boolean = false, afterChips: Boolean = false, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title,
            Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (afterChips) SectionGapAbove - ChipTouchMargin else SectionGapAbove,
                bottom = if (chips) SectionGapBelow - ChipTouchMargin else SectionGapBelow,
            ),
            meta = meta,
        )
        content()
    }
}

/**
 * One line of chips that scrolls sideways and runs to the screen edges. The chips are sized like
 * the filter chips (32 dp, 8 dp corners, 14 sp SemiBold) with a 48 dp touch target; the best one
 * is filled secondaryContainer, the rest outlined.
 */
@Composable
private fun ChipRow(labels: List<String>, highlightFirst: Boolean, onPick: (Int, String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val addLabel = stringResource(R.string.suggest_add)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(labels, key = { i, label -> "$i:$label" }) { i, label ->
            val filled = highlightFirst && i == 0
            val shape = RoundedCornerShape(8.dp)
            val description = stringResource(R.string.suggest_chip_a11y, label)
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .heightIn(min = 32.dp)
                    .clip(shape)
                    .background(if (filled) colors.secondaryContainer else Color.Transparent)
                    .then(if (filled) Modifier else Modifier.border(1.dp, colors.outlineVariant, shape))
                    .clickable(onClickLabel = addLabel, role = Role.Button) { haptics.tick(); onPick(i, label) }
                    .semantics { contentDescription = description }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = SproutType.label,
                    color = if (filled) colors.onSecondaryContainer else colors.onSurface,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

/** What [minimumInteractiveComponentSize] adds above and below a 32 dp chip. */
private val ChipTouchMargin = 8.dp

private data class PastRow(val id: Long, val text: AnnotatedString, val plain: String, val date: Long)

/** Past notes as a grouped list like the Journal's: white rows 2 dp apart, the day on the right. */
@Composable
private fun NoteRows(rows: List<PastRow>, onUse: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val useLabel = stringResource(R.string.suggest_use)
    val today = LocalDate.now()
    val strings = DayLabels(stringResource(R.string.today), stringResource(R.string.yesterday))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        rows.forEachIndexed { i, row ->
            val day = LocalDate.ofEpochDay(row.date)
            val description = stringResource(R.string.suggest_past_note_a11y, strings.label(day, today, long = true), row.plain)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(groupedShape(i, rows.size))
                    .background(colors.surfaceContainerLowest)
                    .clickable(onClickLabel = useLabel, role = Role.Button) { onUse(row.plain) }
                    .clearAndSetSemantics { contentDescription = description }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.text, style = SproutType.body, color = colors.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp))
                Text(strings.label(day, today, long = false), style = SproutType.supporting, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** "Today", "Yesterday", a weekday within the last six days, else "2 Oct" (with the year if not this one). */
private class DayLabels(val today: String, val yesterday: String) {
    fun label(day: LocalDate, now: LocalDate, long: Boolean): String = when {
        day == now -> today
        day == now.minusDays(1) -> yesterday
        !day.isBefore(now.minusDays(6)) && day.isBefore(now) -> day.format(datePattern(if (long) "EEEE" else "EEE"))
        day.year == now.year -> day.format(datePattern("d MMM"))
        else -> day.format(datePattern("d MMM yyyy"))
    }
}
