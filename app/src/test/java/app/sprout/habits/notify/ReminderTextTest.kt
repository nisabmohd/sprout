package app.sprout.habits.notify

import app.sprout.habits.TestStrings
import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTextTest {
    private val read = Habit(name = "Read", icon = "book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages")

    @Test fun measureHabitStatesTheGoal() =
        assertEquals("Read" to "Goal today: 20 pages.", ReminderNotifier.reminderText(read, TestStrings))

    @Test fun minutesHabitReadsInMinutes() {
        val workout = Habit(name = "Workout", icon = "dumbbell", colorHue = 12, trackType = TrackType.AMOUNT, target = 45.0, unit = "min", step = 5.0)
        assertEquals("Workout" to "Goal today: 45 min.", ReminderNotifier.reminderText(workout, TestStrings))
    }

    @Test fun checkHabitIsJustTheName() {
        val vitamins = Habit(name = "Vitamins", icon = "pill", colorHue = 95)
        assertEquals("Vitamins" to "Time to check it off.", ReminderNotifier.reminderText(vitamins, TestStrings))
    }

    @Test fun amountWithoutUnitReadsCleanly() {
        val pushups = Habit(name = "Push-ups", icon = "dumbbell", colorHue = 12, trackType = TrackType.AMOUNT, target = 30.0)
        assertEquals("Push-ups" to "Goal today: 30.", ReminderNotifier.reminderText(pushups, TestStrings))
    }
}
