package app.sprout.habits.ui

import app.sprout.habits.ui.components.wheelMinutes
import app.sprout.habits.ui.components.wheelStart
import org.junit.Assert.assertEquals
import org.junit.Test

/** The time wheel's maths: minutes after midnight ↔ hour / minute / AM-PM columns. */
class TimeWheelTest {
    private fun roundTrip(minutes: Int, h24: Boolean): Int {
        val (h, m, pm) = wheelStart(minutes, h24)
        return wheelMinutes(h, m, pm, h24)
    }

    @Test fun nineAmIn12HourColumns() = assertEquals(Triple(8, 0, false), wheelStart(9 * 60, h24 = false))

    @Test fun ninePmIn12HourColumns() = assertEquals(Triple(8, 0, true), wheelStart(21 * 60, h24 = false))

    @Test fun midnightIsTwelveAm() = assertEquals(Triple(11, 0, false), wheelStart(0, h24 = false))

    @Test fun noonIsTwelvePm() = assertEquals(Triple(11, 0, true), wheelStart(12 * 60, h24 = false))

    @Test fun hours24ColumnIsTheHour() = assertEquals(Triple(21, 6, true), wheelStart(21 * 60 + 30, h24 = true))

    @Test fun minutesRoundToTheNearestFive() {
        assertEquals(9 * 60 + 30, roundTrip(9 * 60 + 32, h24 = false))
        assertEquals(9 * 60 + 35, roundTrip(9 * 60 + 33, h24 = false))
    }

    @Test fun roundingUpCarriesIntoTheNextHour() = assertEquals(10 * 60, roundTrip(9 * 60 + 58, h24 = false))

    @Test fun roundingUpPastMidnightWrapsToMidnight() = assertEquals(0, roundTrip(23 * 60 + 58, h24 = true))

    @Test fun everyFiveMinuteTimeSurvivesBothClocks() {
        for (minutes in 0 until 24 * 60 step 5) {
            assertEquals("12h $minutes", minutes, roundTrip(minutes, h24 = false))
            assertEquals("24h $minutes", minutes, roundTrip(minutes, h24 = true))
        }
    }

    @Test fun pmColumnAddsTwelveHours() {
        assertEquals(21 * 60 + 15, wheelMinutes(8, 3, pm = true, h24 = false))
        assertEquals(9 * 60 + 15, wheelMinutes(8, 3, pm = false, h24 = false))
    }
}
