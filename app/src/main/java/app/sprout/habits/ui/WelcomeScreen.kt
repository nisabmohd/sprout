package app.sprout.habits.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import app.sprout.habits.ui.components.HabitTile
import app.sprout.habits.ui.components.HabitTileIconSize
import app.sprout.habits.ui.components.ProgressRing
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import app.sprout.habits.ui.today.UndoSnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sprout.habits.AppContainer
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.SampleData
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.domain.displayUnit
import app.sprout.habits.domain.measure
import app.sprout.habits.domain.step
import app.sprout.habits.rememberStrings
import app.sprout.habits.ui.components.GroupGap
import app.sprout.habits.ui.components.NoteCard
import app.sprout.habits.ui.components.NoteCardUi
import app.sprout.habits.ui.components.SectionGapAbove
import app.sprout.habits.ui.components.SectionGapBelow
import app.sprout.habits.ui.components.SectionHeader
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.SproutSheet
import app.sprout.habits.ui.components.drawRing
import app.sprout.habits.ui.components.groupedShape
import app.sprout.habits.ui.components.toggle
import app.sprout.habits.ui.theme.SproutType
import app.sprout.habits.ui.theme.habitColors
import app.sprout.habits.ui.today.HabitRowUi
import app.sprout.habits.ui.today.LogSheet
import app.sprout.habits.ui.today.LogSheetUi
import app.sprout.habits.ui.today.SwipeableHabitCard
import app.sprout.habits.ui.today.TodayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
/*
 * Get started (2026-10-25, after Burrow's Welcome): the pitch, then two steps in one screen with
 * dots on top. Step 1 picks habits (five of the sample habits as suggestions, plus your own from a sheet); step 2 is a
 * practice card that teaches swipe, tap and hold, where hold logs part of it (Partial) and takes a
 * note, shown as the Journal shows it. Nothing is
 * saved until "Go to Today": picked habits then replace the sample habits; with none picked the
 * sample habits stay, with their banner, as before.
 */

private const val STEPS = 2
private val HUES = listOf(275, 215, 192, 150, 95, 38, 12, 330)

@Composable
fun WelcomeScreen(container: AppContainer, onDone: suspend () -> Unit) {
    val strings = rememberStrings()
    val suggestions = remember(strings) { welcomeSuggestions(strings) }
    // Indexes into suggestions, and the user's own habits by name. Kept across rotation.
    var page by rememberSaveable { mutableIntStateOf(0) }
    val picked = rememberSaveable(saver = intListSaver()) { mutableStateListOf() }
    val own = rememberSaveable(saver = stringListSaver()) { mutableStateListOf() }
    val ownOff = rememberSaveable(saver = stringListSaver()) { mutableStateListOf() }
    val scope = rememberCoroutineScope()
    var finishing by remember { mutableStateOf(false) }

    fun chosen(): List<Habit> = picked.sorted().map { suggestions[it] } +
        own.filter { it !in ownOff }.mapIndexed { i, name -> ownHabit(name, i) }

    BackHandler(page > 0) { page-- }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
        if (page > 0) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderIconButton(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onClick = { page-- })
            }
            StepDots(page - 1, Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 16.dp))
        }
        AnimatedContent(page, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "welcome") { p ->
            when (p) {
                0 -> Pitch()
                1 -> PickStep(suggestions, picked, own, ownOff)
                else -> PracticeStep(practiceHabit(chosen(), suggestions), strings)
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (page) {
                0 -> BottomButton(stringResource(R.string.welcome_start), Modifier.fillMaxWidth()) { page = 1 }
                1 -> {
                    FilledTonalButton(onClick = { picked.clear(); ownOff.clear(); ownOff.addAll(own); page = 2 }, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(stringResource(R.string.welcome_skip), style = SproutType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BottomButton(stringResource(R.string.welcome_continue), Modifier.weight(1f), enabled = chosen().isNotEmpty()) { page = 2 }
                }
                else -> BottomButton(stringResource(R.string.welcome_go), Modifier.fillMaxWidth(), enabled = !finishing) {
                    finishing = true
                    val habits = chosen()
                    scope.launch {
                        if (habits.isNotEmpty()) {
                            SampleData.remove(container.repository, container.settings)
                            val now = System.currentTimeMillis()
                            habits.forEach { container.repository.saveHabit(it.copy(createdAt = now)) }
                        }
                        onDone()
                    }
                }
            }
        }
        if (page == 0) {
            Text(
                stringResource(R.string.welcome_footer),
                style = SproutType.supporting,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun BottomButton(text: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp)) {
        Text(text, style = SproutType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Step 1's five suggestions, check-off and measured: Wake up at 7, Read, Drink water, Workout, Evening journal. */
internal fun welcomeSuggestions(strings: Strings): List<Habit> = SampleData.habits(strings).slice(listOf(0, 1, 2, 4, 7))

/** A habit the user typed: check off, every day, the next colour in turn. */
internal fun ownHabit(name: String, index: Int) = Habit(name = name.trim(), icon = HabitIcon.TODO.key, colorHue = HUES[index % HUES.size])

/** The practice card's habit: the first picked one you can measure (to show Partial), else the first picked, else Read. */
internal fun practiceHabit(chosen: List<Habit>, suggestions: List<Habit>): Habit =
    chosen.firstOrNull { it.trackType == TrackType.AMOUNT } ?: chosen.firstOrNull() ?: suggestions[1]

// ---- The pitch ----

/** The logo, a panel of what Sprout looks like in use, the promise. */
@Composable
internal fun Pitch() {
    val colors = MaterialTheme.colorScheme
    val strings = rememberStrings()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitTile(R.drawable.ic_seedling, colors.primaryContainer, colors.onPrimaryContainer)
            Text(stringResource(R.string.app_name), style = SproutType.title, color = colors.onBackground)
        }
        val read = SampleData.habits(strings)[1]
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.primaryContainer).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A partial card: the fill and the ring both at 75%.
            PreviewCard(Modifier.fillMaxWidth().rotate(-1.5f), fill = 0.75f, hue = read.colorHue.toFloat()) {
                HabitTile(HabitIcon.fromKey(read.icon).drawable, read.colorHue.toFloat())
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(read.name, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(TodayViewModel.amountOf(read, 15.0, strings), style = SproutType.supporting, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ProgressRing(0.75f, read.colorHue.toFloat(), stringResource(R.string.percent, 75))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val wake = SampleData.habits(strings)[0]
                PreviewCard(Modifier.weight(1f).rotate(1f), column = true) {
                    habitColors(wake.colorHue.toFloat()).let { HabitTile(R.drawable.ic_check, it.solid, it.on) }
                    Spacer(Modifier.height(12.dp))
                    Text(wake.name, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.done_at, TodayViewModel.formatTime(6 * 60 + 52)), style = SproutType.supporting, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val calm = SampleData.habits(strings)[3]
                PreviewCard(Modifier.weight(1f).rotate(2f), column = true) {
                    HabitTile(HabitIcon.fromKey(calm.icon).drawable, calm.colorHue.toFloat())
                    Spacer(Modifier.height(12.dp))
                    Text(pluralStringResource(R.plurals.streak_days, 11, 11), style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(calm.name, style = SproutType.supporting, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.welcome_title), style = SproutType.display, color = colors.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.welcome_body), style = SproutType.body, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(16.dp))
    }
}

/** A white card in the pitch panel; [fill] tints part of it in the habit's soft colour, like a partial habit card. */
@Composable
private fun PreviewCard(modifier: Modifier, fill: Float = 0f, hue: Float = 0f, column: Boolean = false, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainerLowest)) {
        if (fill > 0f) Box(Modifier.matchParentSize()) { Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(habitColors(hue).soft)) }
        if (column) Column(Modifier.padding(16.dp)) { content() }
        else Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

// ---- The steps ----

/** Dots above a step: the current one a wide pill. */
@Composable
private fun StepDots(current: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(STEPS) { i ->
            Box(Modifier.size(width = if (i == current) 24.dp else 8.dp, height = 8.dp).clip(CircleShape).background(if (i == current) colors.primary else colors.outline))
        }
    }
}

/** "Step 1 of 2", the title, the line under it. */
@Composable
private fun ColumnScope.StepHeading(step: Int, title: Int, line: Int) {
    val colors = MaterialTheme.colorScheme
    Text(stringResource(R.string.welcome_step_of, step, STEPS), style = SproutType.label, color = colors.primary)
    Spacer(Modifier.height(4.dp))
    Text(stringResource(title), style = SproutType.screenTitle, color = colors.onBackground)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(line), style = SproutType.supporting, color = colors.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
}

/**
 * Step 1: the suggestions and the user's own habits as one group of rows, each with a checkbox,
 * and a last row "Add your own habit" that opens the Your own habit sheet.
 */
@Composable
internal fun PickStep(suggestions: List<Habit>, picked: MutableList<Int>, own: MutableList<String>, ownOff: MutableList<String>) {
    val strings = rememberStrings()
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        StepHeading(1, R.string.welcome_pick_title, R.string.welcome_pick_body)
        val rows = suggestions.indices.map { it to null } + own.map { null to it }
        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            rows.forEachIndexed { i, (index, name) ->
                val habit = if (index != null) suggestions[index] else ownHabit(name!!, own.indexOf(name))
                val on = if (index != null) index in picked else name !in ownOff
                PickRow(habit, goalLine(habit, strings), on, groupedShape(i, rows.size + 1)) { checked ->
                    if (index != null) { if (checked) picked.add(index) else picked.remove(index) }
                    else if (checked) ownOff.remove(name) else ownOff.add(name!!)
                }
            }
            AddOwnRow(groupedShape(rows.size, rows.size + 1)) { adding = true }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (adding) {
        OwnHabitSheet(ownHabit("", own.size), onAdd = { name ->
            if (name !in own) own.add(name)
            ownOff.remove(name)
            adding = false
        }, onDismiss = { adding = false })
    }
}

/** The group's last row: a + tile, "Add your own habit", "Any habit, by name". */
@Composable
private fun AddOwnRow(shape: RoundedCornerShape, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerLowest).clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 68.dp).padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitTile(R.drawable.ic_plus, colors.surfaceContainerHigh, colors.onSurface)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.welcome_own_row), style = SproutType.cardTitle, color = colors.onSurface)
            Text(stringResource(R.string.welcome_own_row_desc), style = SproutType.supporting, color = colors.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/**
 * "Your own habit": the habit tile and the Name field exactly as in New habit, then "Add this
 * habit". The keyboard opens with the sheet.
 */
@Composable
private fun OwnHabitSheet(preview: Habit, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(300); focus.requestFocus() }
    fun add() { name.trim().takeIf { it.isNotEmpty() }?.let(onAdd) }
    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().imePadding()) {
            Text(stringResource(R.string.welcome_own_hint), style = SproutType.title, color = colors.onSurface)
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                HabitTile(HabitIcon.fromKey(preview.icon).drawable, preview.colorHue.toFloat())
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    textStyle = SproutType.cardTitle,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.weight(1f).padding(start = 12.dp).focusRequester(focus),
                )
            }
            Button(
                onClick = { add() },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp).height(48.dp),
            ) {
                Text(stringResource(R.string.welcome_own_add), style = SproutType.label)
            }
        }
    }
}

/** "20 pages", "Once a day", or "Weekdays" for a check habit that isn't every day ("20 pages · Weekdays" for one you measure). */
internal fun goalLine(habit: Habit, strings: Strings): String {
    val goal = TodayViewModel.goalOf(habit, strings)
    return when {
        habit.daysMask == Habit.EVERY_DAY -> goal
        habit.trackType == TrackType.CHECK -> strings(R.string.weekdays)
        else -> "$goal · ${strings(R.string.weekdays)}"
    }
}

@Composable
private fun PickRow(habit: Habit, line: String, on: Boolean, shape: RoundedCornerShape, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerLowest)
            .toggleable(on, role = Role.Checkbox) { haptics.toggle(it); onChange(it) }
            .heightIn(min = 68.dp).padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitTile(HabitIcon.fromKey(habit.icon).drawable, habit.colorHue.toFloat())
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(habit.name, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = SproutType.supporting, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Checkbox(checked = on, onCheckedChange = null)
    }
}

private enum class Gesture { RIGHT, LEFT, TAP, HOLD, UNDO, NOTE }

/**
 * Step 2: a real Today card for [habit] that saves nothing, and the ways to log, each ticked
 * once tried. Holding opens the real amount sheet, so a Partial shows its fill and ring. Every
 * change shows Today's Undo bar; tapping its Undo ticks the Undo row.
 */
@Composable
internal fun PracticeStep(habit: Habit, strings: Strings) {
    val colors = MaterialTheme.colorScheme
    var outcome by rememberSaveable { mutableStateOf(DayOutcome.OPEN) }
    var amount by rememberSaveable { mutableStateOf(0.0) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    val tried = rememberSaveable(saver = intListSaver()) { mutableStateListOf() }
    fun tried(g: Gesture) { if (g.ordinal !in tried) tried.add(g.ordinal) }
    fun set(o: DayOutcome, a: Double) { outcome = o; amount = a }

    // The same Undo bar as Today: each change says what happened, and Undo puts the card back.
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)
    fun change(o: DayOutcome, a: Double, message: String) {
        val before = outcome to amount
        set(o, a)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                set(before.first, before.second)
                tried(Gesture.UNDO)
            }
        }
    }

    val measured = habit.trackType == TrackType.AMOUNT
    val row = HabitRowUi(
        id = -1, name = habit.name, icon = HabitIcon.fromKey(habit.icon).drawable, hue = habit.colorHue.toFloat(),
        outcome = outcome,
        progress = if (outcome == DayOutcome.PARTIAL) (amount / habit.target).toFloat().coerceIn(0f, 1f) else 0f,
        subtitle = when (outcome) {
            DayOutcome.DONE -> if (measured) TodayViewModel.amountOf(habit, habit.target, strings) else strings(R.string.outcome_done)
            DayOutcome.PARTIAL -> TodayViewModel.amountOf(habit, amount, strings)
            DayOutcome.SKIP -> strings(R.string.outcome_skipped)
            DayOutcome.OPEN -> TodayViewModel.goalOf(habit, strings)
        }.let { if (note.isNotEmpty()) strings(R.string.note_added, it) else it },
        hasNote = note.isNotEmpty(),
    )
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        StepHeading(2, R.string.welcome_try_title, R.string.welcome_try_body)
        SwipeableHabitCard(
            habit = row,
            onDone = { change(DayOutcome.DONE, habit.target, strings(R.string.log_marked_done, habit.name)); tried(Gesture.RIGHT) },
            onSkip = { change(DayOutcome.SKIP, 0.0, strings(R.string.log_skipped, habit.name)); tried(Gesture.LEFT) },
            onUndo = { change(DayOutcome.OPEN, 0.0, strings(R.string.log_undone, habit.name)) },
            onToggle = {
                if (outcome == DayOutcome.DONE) change(DayOutcome.OPEN, 0.0, strings(R.string.log_marked_not_done, habit.name))
                else change(DayOutcome.DONE, habit.target, strings(R.string.log_marked_done, habit.name))
                tried(Gesture.TAP)
            },
            onLongPress = { sheetOpen = true },
            onClick = {},
        )
        Spacer(Modifier.height(16.dp))
        val gestures = listOf(
            Triple(Gesture.RIGHT, R.string.welcome_try_right, R.string.outcome_done),
            Triple(Gesture.LEFT, R.string.welcome_try_left, R.string.welcome_try_left_desc),
            Triple(Gesture.TAP, R.string.welcome_try_tap, R.string.welcome_try_tap_desc),
            Triple(Gesture.HOLD, R.string.welcome_try_hold, R.string.welcome_try_hold_desc),
            Triple(Gesture.UNDO, R.string.welcome_try_undo, R.string.welcome_try_undo_desc),
            Triple(Gesture.NOTE, R.string.welcome_try_note, R.string.welcome_try_note_desc),
        )
        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            gestures.forEachIndexed { i, (g, title, line) ->
                GestureRow(g, stringResource(title), stringResource(line), g.ordinal in tried, habit.colorHue.toFloat(), groupedShape(i, gestures.size))
            }
        }
        // Once a note is written: the note as the Journal shows it, under a Journal header.
        if (note.isNotEmpty()) {
            Spacer(Modifier.height(SectionGapAbove))
            SectionHeader(stringResource(R.string.journal_title))
            Spacer(Modifier.height(SectionGapBelow))
            val status = when (outcome) { DayOutcome.DONE -> EntryStatus.DONE; DayOutcome.SKIP -> EntryStatus.SKIP; DayOutcome.PARTIAL -> EntryStatus.PARTIAL; else -> null }
            val today = java.time.LocalDate.now().toEpochDay()
            val (mark, line) = NoteCardUi.dayOutcome(habit, status?.let { Entry(-1, today, it, amount) }, today, today, strings)
            NoteCard(
                NoteCardUi(
                    id = -1, day = today, habitName = habit.name, icon = HabitIcon.fromKey(habit.icon).drawable, hue = habit.colorHue.toFloat(),
                    mark = mark, outcome = line, time = TodayViewModel.formatTime(java.time.LocalTime.now().let { it.hour * 60 + it.minute }), text = note,
                ),
                onOpen = null,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp))
    }
    if (sheetOpen) {
        val goal = if (measured) habit.measure(habit.target) else null
        LogSheet(
            sheet = LogSheetUi(
                habitId = -1, day = 0, name = habit.name, icon = HabitIcon.fromKey(habit.icon).drawable, hue = habit.colorHue.toFloat(),
                trackType = habit.trackType, target = habit.target, unit = habit.displayUnit(), step = habit.step(),
                isToday = true, dayLabel = strings(R.string.today),
                subtitle = goal?.let { strings(R.string.log_goal_today, it) } ?: strings(R.string.today),
                status = when (outcome) { DayOutcome.DONE -> EntryStatus.DONE; DayOutcome.SKIP -> EntryStatus.SKIP; else -> EntryStatus.PARTIAL },
                // A first hold starts half way, so the Partial fill and ring show at once.
                amount = if (outcome == DayOutcome.OPEN) halfWay(habit) else amount,
                noteId = null, noteText = note, hasEntry = outcome != DayOutcome.OPEN,
            ),
            onSave = { status, a, text ->
                note = text.trim()
                if (note.isNotEmpty()) tried(Gesture.NOTE)
                when (status) {
                    EntryStatus.DONE -> change(DayOutcome.DONE, habit.target, strings(R.string.log_marked_done, habit.name))
                    EntryStatus.PARTIAL -> change(
                        DayOutcome.PARTIAL, a,
                        strings(R.string.log_partial, habit.name, TodayViewModel.formatNumber(a), "${TodayViewModel.formatNumber(habit.target)} ${habit.displayUnit()}".trimEnd()),
                    )
                    EntryStatus.SKIP -> change(DayOutcome.SKIP, 0.0, strings(R.string.log_skipped, habit.name))
                }
                tried(Gesture.HOLD)
                sheetOpen = false
            },
            onUndo = { change(DayOutcome.OPEN, 0.0, strings(R.string.log_cleared, habit.name)); sheetOpen = false },
            onDismiss = { sheetOpen = false },
        )
    }
}

/** About half the target, on a step: 10 of 20 pages, 4 of 8 glasses, 20 of 45 min. */
internal fun halfWay(habit: Habit): Double {
    if (habit.trackType != TrackType.AMOUNT) return 0.5
    val step = habit.step()
    return (Math.floor(habit.target / 2 / step) * step).coerceAtLeast(step)
}

/** One way to log: a tile showing the result, the gesture, what it does, and a check once tried. */
@Composable
private fun GestureRow(gesture: Gesture, title: String, line: String, tried: Boolean, hue: Float, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(hue)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerLowest).heightIn(min = 68.dp).padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitTile(hc.soft) {
            val glyph = Modifier.size(HabitTileIconSize)
            when (gesture) {
                Gesture.RIGHT -> Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = hc.ink, modifier = glyph)
                Gesture.LEFT -> Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = null, tint = hc.ink, modifier = glyph)
                Gesture.TAP -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = hc.ink, modifier = glyph)
                Gesture.NOTE -> Icon(painterResource(R.drawable.ic_note), contentDescription = null, tint = hc.ink, modifier = glyph)
                Gesture.UNDO -> Icon(painterResource(R.drawable.ic_undo), contentDescription = null, tint = hc.ink, modifier = glyph)
                // A small partial ring, the sign for "part of it".
                Gesture.HOLD -> Canvas(glyph) { drawRing(0.6f, hc.solid, hc.mid, 3.dp.toPx()) }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SproutType.cardTitle, color = colors.onSurface)
            Text(line, style = SproutType.supporting, color = colors.onSurfaceVariant)
        }
        TriedMark(tried)
    }
}

@Composable
private fun RowScope.TriedMark(tried: Boolean) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(R.string.welcome_tried)
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(if (tried) colors.primary else colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (tried) Icon(painterResource(R.drawable.ic_check), contentDescription = label, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
    }
    Spacer(Modifier.width(4.dp))
}

private fun intListSaver() = androidx.compose.runtime.saveable.listSaver<MutableList<Int>, Int>(
    save = { it.toList() }, restore = { mutableStateListOf<Int>().apply { addAll(it) } },
)

private fun stringListSaver() = androidx.compose.runtime.saveable.listSaver<MutableList<String>, String>(
    save = { it.toList() }, restore = { mutableStateListOf<String>().apply { addAll(it) } },
)
