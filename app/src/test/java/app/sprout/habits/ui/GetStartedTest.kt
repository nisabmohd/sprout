package app.sprout.habits.ui

import app.sprout.habits.TestStrings
import app.sprout.habits.data.Habit
import app.sprout.habits.data.SampleData
import app.sprout.habits.data.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Get started's rules: the suggestions, your own habits, the practice habit and the goal lines. */
class GetStartedTest {
    private val suggestions = welcomeSuggestions(TestStrings)

    @Test fun fiveSuggestionsInTheAppLanguage() =
        assertEquals(listOf("Wake up at 7", "Read", "Drink water", "Workout", "Evening journal"), suggestions.map { it.name })

    @Test fun suggestionsMixCheckOffAndMeasure() {
        assertTrue(suggestions.any { it.trackType == TrackType.CHECK })
        assertTrue(suggestions.any { it.trackType == TrackType.AMOUNT })
    }

    @Test fun suggestionsAreSampleHabits() =
        assertTrue(SampleData.habits(TestStrings).map { it.name }.containsAll(suggestions.map { it.name }))

    @Test fun ownHabitIsCheckOffEveryDayTrimmed() {
        val h = ownHabit("  Stretch  ", 0)
        assertEquals("Stretch", h.name)
        assertEquals(TrackType.CHECK, h.trackType)
        assertEquals(Habit.EVERY_DAY, h.daysMask)
        assertEquals("task_alt", h.icon)
    }

    @Test fun ownHabitsTakeTheNextColourInTurn() {
        val hues = (0 until 9).map { ownHabit("x", it).colorHue }
        assertEquals(8, hues.take(8).toSet().size)
        assertEquals(hues[0], hues[8])
    }

    @Test fun practiceUsesTheFirstMeasuredPick() {
        val picks = listOf(suggestions[0], suggestions[3], suggestions[1])
        assertEquals("Workout", practiceHabit(picks, suggestions).name)
    }

    @Test fun practiceFallsBackToFirstPickThenRead() {
        assertEquals("Wake up at 7", practiceHabit(listOf(suggestions[0]), suggestions).name)
        assertEquals("Read", practiceHabit(emptyList(), suggestions).name)
    }

    @Test fun halfWayLandsOnAStep() {
        assertEquals(10.0, halfWay(suggestions[1]), 0.0) // 20 pages → 10
        assertEquals(4.0, halfWay(suggestions[2]), 0.0) // 8 glasses → 4
        assertEquals(20.0, halfWay(suggestions[3]), 0.0) // 45 min, step 5 → 20
        assertEquals(1.0, halfWay(Habit(name = "x", icon = "a", colorHue = 1, trackType = TrackType.AMOUNT, target = 1.0)), 0.0)
    }

    @Test fun goalLines() {
        assertEquals("20 pages", goalLine(suggestions[1], TestStrings))
        assertEquals("Once a day", goalLine(suggestions[0], TestStrings))
        val walk = SampleData.habits(TestStrings).first { it.daysMask != Habit.EVERY_DAY }
        assertEquals("Weekdays", goalLine(walk, TestStrings))
        assertEquals("30 min · Weekdays", goalLine(walk.copy(trackType = TrackType.AMOUNT, target = 30.0, unit = "min"), TestStrings))
    }
}
