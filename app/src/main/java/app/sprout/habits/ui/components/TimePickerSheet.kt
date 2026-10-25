package app.sprout.habits.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.ui.theme.SproutType
import java.text.DateFormatSymbols
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * The one time picker, the same wheel as Burrow's (2026-10-25): three snapping columns — hour ·
 * minute in 5s · AM/PM — over a pill band, the top and bottom rows fading out. A 24-hour phone
 * gets hours 00–23 and no AM/PM column. Reminder time (New habit) and Default reminder (More).
 */

private const val VISIBLE_ROWS = 5
private const val MINUTE_STEP = 5
private val WheelRow = 48.dp
private val WheelColumn = 72.dp

/** The wheel's columns for [minutes] after midnight: hour index, minute index (rounded to 5), PM. */
fun wheelStart(minutes: Int, h24: Boolean): Triple<Int, Int, Boolean> {
    val rounded = ((minutes + MINUTE_STEP / 2) / MINUTE_STEP * MINUTE_STEP).mod(24 * 60)
    val hour = rounded / 60
    val hourIndex = if (h24) hour else (hour % 12).let { if (it == 0) 12 else it } - 1
    return Triple(hourIndex, rounded % 60 / MINUTE_STEP, hour >= 12)
}

/** Minutes after midnight for what the wheel shows. */
fun wheelMinutes(hourIndex: Int, minuteIndex: Int, pm: Boolean, h24: Boolean): Int {
    val hour = if (h24) hourIndex else (hourIndex + 1) % 12 + if (pm) 12 else 0
    return hour * 60 + minuteIndex * MINUTE_STEP
}

/** Pick a time of day (minutes after midnight): [title], an optional [line], the wheel, Set time. */
@Composable
fun TimePickerSheet(title: String, initialMinutes: Int, onApply: (Int) -> Unit, onDismiss: () -> Unit, line: String? = null) {
    val h24 = DateFormat.is24HourFormat(LocalContext.current)
    val locale = LocalConfiguration.current.locales[0]
    val (h0, m0, pm0) = remember(initialMinutes, h24) { wheelStart(initialMinutes, h24) }
    val hours = remember(h24, locale) { if (h24) (0..23).map { String.format(locale, "%02d", it) } else (1..12).map { String.format(locale, "%d", it) } }
    val minutes = remember(locale) { (0 until 60 step MINUTE_STEP).map { String.format(locale, "%02d", it) } }
    val halves = remember(locale) { DateFormatSymbols.getInstance(locale).amPmStrings.toList() }
    val hourState = rememberLazyListState(h0)
    val minuteState = rememberLazyListState(m0)
    val halfState = rememberLazyListState(if (pm0) 1 else 0)
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme

    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(title, style = SproutType.title, color = colors.onSurface)
            if (line != null) Text(line, style = SproutType.supporting, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                // The band behind the middle row.
                Box(Modifier.fillMaxWidth().height(WheelRow).clip(CircleShape).background(colors.surfaceContainerHigh))
                Row(
                    Modifier.height(WheelRow * VISIBLE_ROWS).fadeEdges(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelColumn(hours, hourState, stringResource(R.string.time_hour)) { haptics.stepTick() }
                    Text(":", style = SproutType.screenTitleNumber, color = colors.onSurface)
                    WheelColumn(minutes, minuteState, stringResource(R.string.time_minute)) { haptics.stepTick() }
                    if (!h24) WheelColumn(halves, halfState, stringResource(R.string.time_am_pm)) { haptics.stepTick() }
                }
            }
            Button(
                onClick = {
                    haptics.confirm()
                    onApply(wheelMinutes(hourState.centred(), minuteState.centred(), halfState.centred() == 1, h24))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).height(48.dp),
            ) {
                Text(stringResource(R.string.set_time), style = SproutType.label)
            }
        }
    }
}

/** The row in the middle of a column. */
private fun LazyListState.centred(): Int {
    val info = layoutInfo
    val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centre) }?.index ?: firstVisibleItemIndex
}

@Composable
private fun WheelColumn(items: List<String>, state: LazyListState, label: String, onSnap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val selected by remember { derivedStateOf { state.centred() } }
    // One tick per row that passes the middle while the user scrolls.
    LaunchedEffect(selected) { if (state.isScrollInProgress) onSnap() }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        // Two rows of space above and below, so the first and last rows can reach the middle.
        contentPadding = PaddingValues(vertical = WheelRow * (VISIBLE_ROWS / 2)),
        modifier = Modifier.width(WheelColumn).height(WheelRow * VISIBLE_ROWS)
            .semantics { contentDescription = label; stateDescription = items.getOrElse(selected) { "" } },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(items.size) { i ->
            val on = i == selected
            Box(
                Modifier.height(WheelRow).fillMaxWidth().clickable { scope.launch { state.animateScrollToItem(i) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    items[i], textAlign = TextAlign.Center, maxLines = 1,
                    style = if (on) SproutType.screenTitleNumber else SproutType.titleNumber,
                    color = if (on) colors.onSurface else colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** The top and bottom rows fade out (a vertical gradient mask). */
private fun Modifier.fadeEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }
