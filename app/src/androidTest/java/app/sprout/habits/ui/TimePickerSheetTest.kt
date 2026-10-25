package app.sprout.habits.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.sprout.habits.ui.components.TimePickerSheet
import app.sprout.habits.ui.theme.SproutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The one time picker: title and line, Set time returns the time, tapping a row picks it. */
@RunWith(AndroidJUnit4::class)
class TimePickerSheetTest {
    @get:Rule val rule = createComposeRule()

    private var picked: Int? = null

    private fun show(minutes: Int, line: String? = "Read") = rule.setContent {
        SproutTheme { TimePickerSheet("Reminder time", minutes, onApply = { picked = it }, onDismiss = {}, line = line) }
    }

    @Test fun showsTitleLineAndButton() {
        show(9 * 60)
        rule.onNodeWithText("Reminder time").assertIsDisplayed()
        rule.onNodeWithText("Read").assertIsDisplayed()
        rule.onNodeWithText("Set time").assertIsDisplayed()
    }

    @Test fun setTimeReturnsTheStartTime() {
        show(21 * 60 + 30)
        rule.onNodeWithText("Set time").performClick()
        rule.runOnIdle { assertEquals(21 * 60 + 30, picked) }
    }

    @Test fun startTimeRoundsToFiveMinutes() {
        show(7 * 60 + 52)
        rule.onNodeWithText("Set time").performClick()
        rule.runOnIdle { assertEquals(7 * 60 + 50, picked) }
    }

    @Test fun tappingAnHourRowPicksIt() {
        show(9 * 60)
        // The hour column comes first, so its "10" is the first match (minutes have a "10" too).
        rule.onAllNodesWithText("10")[0].performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Set time").performClick()
        rule.runOnIdle { assertEquals(10 * 60, picked) }
    }
}
