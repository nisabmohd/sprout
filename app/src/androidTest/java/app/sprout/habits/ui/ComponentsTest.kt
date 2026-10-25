package app.sprout.habits.ui

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.sprout.habits.R
import app.sprout.habits.ui.components.ComponentTags
import app.sprout.habits.ui.components.HabitTile
import app.sprout.habits.ui.components.ProgressRing
import app.sprout.habits.ui.theme.SproutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The shared habit tile and progress ring draw at their spec sizes and say what they show. */
@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun habitTileIs44dp() {
        rule.setContent { SproutTheme { HabitTile(R.drawable.ic_sym_menu_book, 215f) } }
        rule.onNodeWithTag(ComponentTags.HABIT_TILE).assertWidthIsEqualTo(44.dp).assertHeightIsEqualTo(44.dp)
    }

    @Test fun progressRingIs44dpWithItsLabel() {
        rule.setContent { SproutTheme { ProgressRing(0.75f, 215f, "75%") } }
        rule.onNodeWithTag(ComponentTags.PROGRESS_RING).assertWidthIsEqualTo(44.dp).assertHeightIsEqualTo(44.dp)
        rule.onNodeWithText("75%", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun progressRingReadsItsDescription() {
        rule.setContent { SproutTheme { ProgressRing(5 / 7f, 150f, "5d", description = "5-day streak") } }
        rule.onNodeWithContentDescription("5-day streak").assertIsDisplayed()
    }
}
