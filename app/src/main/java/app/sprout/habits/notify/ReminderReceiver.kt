package app.sprout.habits.notify

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sprout.habits.SproutApp
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at a habit's reminder time (posts it, then schedules the next one) and at the evening
 * note check (nudges skipped habits that ask for a note, then schedules tomorrow's check).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as SproutApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Battery saving can hold an alarm back for hours. One that arrives after its day
                // is over is dropped: it would check the new day, which has nothing logged yet,
                // and remind about a habit that was already done or skipped.
                val day = intent.getLongExtra(ReminderScheduler.EXTRA_DAY, ReminderScheduler.NO_DAY)
                val onTime = day == ReminderScheduler.NO_DAY || day == LocalDate.now().toEpochDay()
                when (intent.action) {
                    ReminderScheduler.ACTION_REMINDER -> {
                        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, 0L)
                        val habit = container.repository.getHabit(habitId) ?: return@launch
                        if (onTime) container.reminderNotifier.showReminder(habit)
                        container.reminders.schedule(habit)
                    }
                    ReminderScheduler.ACTION_NOTE_NUDGE -> {
                        val habits = container.repository.observeHabits().first()
                        if (onTime) container.reminderNotifier.showNoteNudges(habits)
                        container.reminders.scheduleNoteNudge(habits.any { it.askForNote })
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms don't survive a reboot, and a clock or zone change moves wall-clock times. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> Unit
            else -> return
        }
        val container = (context.applicationContext as SproutApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.reminders.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
