package app.sprout.habits.ui

import app.sprout.habits.ui.components.HabitTileCorner
import app.sprout.habits.ui.components.HabitTileIconSize
import app.sprout.habits.ui.components.HabitTileSize
import app.sprout.habits.ui.components.ProgressRingSize
import app.sprout.habits.ui.components.ProgressRingStroke
import app.sprout.habits.ui.components.ringFraction
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** The shared components' sizes match SPACING.md, and the ring's fill is clamped. */
class ComponentsTest {
    @Test fun habitTileMatchesSpacingRules() {
        assertEquals(44.dp, HabitTileSize)
        assertEquals(14.dp, HabitTileCorner)
        assertEquals(22.dp, HabitTileIconSize)
    }

    @Test fun progressRingMatchesTodayCard() {
        assertEquals(44.dp, ProgressRingSize)
        assertEquals(4.dp, ProgressRingStroke)
    }

    @Test fun ringFractionIsClamped() {
        assertEquals(0f, ringFraction(-0.5f))
        assertEquals(0.75f, ringFraction(0.75f))
        assertEquals(1f, ringFraction(1.6f))
        assertEquals(0f, ringFraction(Float.NaN))
    }
}
