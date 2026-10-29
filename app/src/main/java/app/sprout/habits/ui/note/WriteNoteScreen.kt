package app.sprout.habits.ui.note

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.domain.COMPLETION_MIN_TYPED
import app.sprout.habits.domain.SUGGESTIONS_MAX_LENGTH
import app.sprout.habits.domain.TextEdit
import app.sprout.habits.domain.insertEnding
import app.sprout.habits.domain.insertPhrase
import app.sprout.habits.domain.isScheduled
import app.sprout.habits.domain.replaceSentence
import app.sprout.habits.domain.typedSentence
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import app.sprout.habits.ui.components.HabitTile
import app.sprout.habits.ui.theme.SproutType
import app.sprout.habits.ui.datePattern
import androidx.compose.ui.res.stringResource
import app.sprout.habits.rememberStrings
import java.time.LocalDate
import app.sprout.habits.ui.components.MarkColors
import app.sprout.habits.ui.components.DayMarkView
import app.sprout.habits.ui.components.NoteCardUi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import app.sprout.habits.ui.components.SproutSheet
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.components.DatePickerSheet
import app.sprout.habits.ui.theme.habitColors
import java.time.DayOfWeek
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteNoteScreen(viewModel: WriteNoteViewModel, weekStart: DayOfWeek, writingSuggestions: Boolean, onClose: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onClose() } }
    if (!form.loaded) return

    val colors = MaterialTheme.colorScheme
    var pickingHabit by remember { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    val habit = habits.firstOrNull { it.id == form.habitId }
    // What the day's log says, for the placeholder and the Quick start chips. A past scheduled
    // day with nothing logged counts as Skip.
    val status: EntryStatus? = entry?.status ?: habit?.takeIf {
        form.date.isBefore(LocalDate.now()) && isScheduled(it.daysMask, form.date)
    }?.let { EntryStatus.SKIP }

    // The field keeps its own text and cursor and tells the view model; it takes the view model's
    // text only when the form puts in one of its own (see NoteForm.textVersion).
    var field by remember { mutableStateOf(TextFieldValue(form.text, TextRange(form.text.length))) }
    LaunchedEffect(form.textVersion) {
        if (field.text != form.text) field = TextFieldValue(form.text, TextRange(form.text.length))
    }
    fun apply(edit: TextEdit) {
        field = TextFieldValue(edit.text, TextRange(edit.cursor))
        viewModel.setText(edit.text)
    }
    // Picking one of "Your recent notes" puts the suggestions away until the field is cleared.
    var usedRecent by rememberSaveable { mutableStateOf(false) }
    if (usedRecent && field.text.isEmpty()) usedRecent = false

    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val suggest = writingSuggestions && habit != null && !usedRecent && field.text.length <= SUGGESTIONS_MAX_LENGTH
    val cursor = field.selection.end
    val typed = if (field.selection.collapsed) typedSentence(field.text, cursor) else ""
    val typing = typed.trim().length >= COMPLETION_MIN_TYPED
    // Only complete a sentence when the cursor is at its end, so the ending doesn't land mid-text.
    val atLineEnd = field.text.substring(cursor.coerceIn(0, field.text.length)).substringBefore('\n').isBlank()
    val completions = remember(suggestions, typed, form.habitId, suggest, atLineEnd) {
        if (suggest && typing && atLineEnd) suggestions.index.complete(typed, form.habitId) else emptyList()
    }
    val ghost = completions.firstOrNull()

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClick = onClose)
            Text(
                stringResource(if (form.isNew) R.string.new_note else R.string.edit_note),
                style = SproutType.title,
                color = colors.onBackground,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (!form.isNew) {
                HeaderIconButton(R.drawable.ic_delete, stringResource(R.string.action_delete), danger = true) { confirmDelete = true }
                Spacer(Modifier.width(8.dp))
            }
            HeaderIconButton(R.drawable.ic_check, stringResource(R.string.action_save), primary = true, enabled = form.canSave, onClick = viewModel::save)
        }

        val noteCard: @Composable (Modifier, Modifier) -> Unit = { cardModifier, fieldModifier ->
            // One card: the habit row on top, then the note, with no outline.
            Column(
                cardModifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surfaceContainerLowest)
                    .padding(8.dp),
            ) {
                if (habit == null) {
                    Text(
                        stringResource(R.string.note_no_habits),
                        style = SproutType.body,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    HabitRow(habit, entry, form.date, onPickHabit = { pickingHabit = true }, onPickDate = { pickingDate = true })
                    NoteField(
                        field = field,
                        onChange = { field = it; viewModel.setText(it.text) },
                        placeholder = stringResource(placeholderFor(status)),
                        cursorColor = habitColors(habit.colorHue.toFloat()).solid,
                        ghost = ghost?.ending,
                        onAcceptGhost = { ghost?.let { apply(insertEnding(field.text, cursor, it.ending)) } },
                        modifier = fieldModifier.focusRequester(focus),
                    )
                    // Add note opens ready to type, also when that day's note is already there; the
                    // field only exists once the habit is known.
                    LaunchedEffect(Unit) { if (viewModel.opensTyping) focus.requestFocus() }
                }
            }
        }

        if (!writingSuggestions) {
            // Without suggestions the card reaches down to the keyboard, as it always did.
            noteCard(Modifier.fillMaxSize().padding(bottom = 16.dp), Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 20.dp))
        } else {
            // The card is only as tall as the writing area; the suggestions sit under it and the
            // page scrolls, so the field stays in view above the keyboard.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                noteCard(Modifier, Modifier.fillMaxWidth().height(NoteFieldHeight).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp))
                if (suggest && habit != null) {
                    NoteSuggestions(
                        data = suggestions,
                        isNew = form.isNew,
                        noteId = form.noteId,
                        habitId = habit.id,
                        status = status,
                        typed = typed,
                        typing = typing,
                        completions = completions,
                        onInsertPhrase = { apply(insertPhrase(field.text, cursor, it)) },
                        onUseRecent = { apply(replaceSentence(field.text, cursor, it)); usedRecent = true },
                        onUseSimilar = { apply(replaceSentence(field.text, cursor, it)) },
                        onAddEnding = { apply(insertEnding(field.text, cursor, it)) },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (pickingHabit) {
        HabitPickerSheet(
            habits = habits,
            selectedId = form.habitId,
            onPick = { id -> viewModel.setHabit(id); pickingHabit = false },
            onDismiss = { pickingHabit = false },
        )
    }

    if (pickingDate) {
        DatePickerSheet(
            date = form.date,
            weekStart = weekStart,
            onApply = { viewModel.setDate(it); pickingDate = false },
            onDismiss = { pickingDate = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.action_delete), color = colors.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}


/** Every habit in a scrollable sheet; a dropdown got cut off with more than a handful. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitPickerSheet(
    habits: List<app.sprout.habits.data.Habit>,
    selectedId: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Fully open right away so every habit is visible; the list scrolls if there are many.
    SproutSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.choose_habit),
            style = SproutType.title,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            items(habits, key = { it.id }) { h ->
                val c = habitColors(h.colorHue.toFloat())
                val selected = h.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) c.soft else colors.surfaceContainerLowest)
                        .selectable(selected, role = Role.RadioButton) { onPick(h.id) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitTile(HabitIcon.fromKey(h.icon).drawable, c.soft, c.ink)
                    Text(
                        h.name,
                        style = SproutType.cardTitle,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    if (selected) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.solid, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** The writing area's height when the suggestions sit under the card; longer notes scroll inside it. */
private val NoteFieldHeight = 182.dp

private fun placeholderFor(status: EntryStatus?): Int = when (status) {
    EntryStatus.SKIP -> R.string.note_placeholder_skipped
    EntryStatus.PARTIAL -> R.string.note_placeholder_partial
    EntryStatus.DONE -> R.string.note_placeholder_done
    null -> R.string.note_placeholder_none
}

/** The tinted habit row at the top of the card: tile, name, the day's mark, outcome and date. */
@Composable
private fun HabitRow(habit: Habit, entry: Entry?, date: LocalDate, onPickHabit: () -> Unit, onPickDate: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.colorHue.toFloat())
    val (mark, outcome) = NoteCardUi.dayOutcome(habit, entry, date.toEpochDay(), LocalDate.now().toEpochDay(), rememberStrings())
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(hc.soft)
            .clickable(onClickLabel = stringResource(R.string.choose_habit), onClick = onPickHabit)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitTile(HabitIcon.fromKey(habit.icon).drawable, hc.mid, hc.ink)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(habit.name, style = SproutType.cardTitle, color = hc.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                Modifier.clickable(onClickLabel = stringResource(R.string.change_date), onClick = onPickDate),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayMarkView(mark, hc, MarkColors(colors.outlineVariant, colors.onSurfaceVariant, colors.outline), Modifier.size(16.dp))
                Text(
                    "$outcome · ${date.format(datePattern("EEEE, d MMM"))}",
                    style = SproutType.supporting,
                    color = hc.ink.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        IconButton(onClick = onPickHabit) {
            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = stringResource(R.string.choose_habit), tint = hc.ink, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * The borderless note field. [ghost] is the rest of the best completion, drawn muted right after
 * the cursor; it is not part of the text, so TalkBack doesn't read it as the note. Tab or → on a
 * hardware keyboard accepts it.
 */
@Composable
private fun NoteField(
    field: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    placeholder: String,
    cursorColor: Color,
    ghost: String?,
    onAcceptGhost: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val noteLabel = stringResource(R.string.note)
    val textStyle = SproutType.noteEditor.copy(color = colors.onSurface)
    val transformation = if (ghost != null) GhostText(field.selection.end, ghost, colors.onSurfaceVariant) else VisualTransformation.None
    BasicTextField(
        value = field,
        onValueChange = onChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(cursorColor),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = transformation,
        modifier = modifier
            .onPreviewKeyEvent { event ->
                val accept = ghost != null && event.type == KeyEventType.KeyDown && (event.key == Key.Tab || event.key == Key.DirectionRight)
                if (accept) onAcceptGhost()
                accept
            }
            .semantics { contentDescription = noteLabel },
        decorationBox = { inner ->
            Box {
                if (field.text.isEmpty()) Text(placeholder, style = textStyle.copy(color = colors.onSurfaceVariant))
                inner()
            }
        },
    )
}

/** Shows [ghost] in [color] at [at] without changing the text; the cursor stays before it. */
private data class GhostText(val at: Int, val ghost: String, val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val split = at.coerceIn(0, text.length)
        val shown = buildAnnotatedString {
            append(text.subSequence(0, split))
            withStyle(SpanStyle(color = color)) { append(ghost) }
            append(text.subSequence(split, text.length))
        }
        val g = ghost.length
        return TransformedText(shown, object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset <= split) offset else offset + g
            override fun transformedToOriginal(offset: Int) = when {
                offset <= split -> offset
                offset <= split + g -> split
                else -> offset - g
            }
        })
    }
}
