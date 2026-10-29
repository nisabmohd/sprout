package app.sprout.habits.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.sprout.habits.ResourceStrings
import app.sprout.habits.ui.theme.SproutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Get started's two steps: picking habits (and adding your own) and the practice card. */
@RunWith(AndroidJUnit4::class)
class GetStartedTest {
    @get:Rule val rule = createComposeRule()

    private val strings = ResourceStrings(InstrumentationRegistry.getInstrumentation().targetContext)
    private val suggestions = welcomeSuggestions(strings)

    @Test fun pickStepListsFiveSuggestionsAndAddYourOwn() {
        rule.setContent { SproutTheme { PickStep(suggestions, mutableStateListOf(), mutableStateListOf(), mutableStateListOf()) } }
        listOf("Wake up at 7", "Read", "Drink water", "Workout", "Evening journal", "Add your own habit").forEach {
            rule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun tappingASuggestionPicksIt() {
        val picked = mutableStateListOf<Int>()
        rule.setContent { SproutTheme { PickStep(suggestions, picked, mutableStateListOf(), mutableStateListOf()) } }
        rule.onNodeWithText("Read").performClick()
        rule.runOnIdle { assertEquals(listOf(1), picked.toList()) }
        rule.onNodeWithText("Read").performClick()
        rule.runOnIdle { assertEquals(emptyList<Int>(), picked.toList()) }
    }

    @Test fun addYourOwnHabitFromTheSheet() {
        val own = mutableStateListOf<String>()
        rule.setContent { SproutTheme { PickStep(suggestions, mutableStateListOf(), own, mutableStateListOf()) } }
        rule.onNodeWithText("Add your own habit").performScrollTo().performClick()
        rule.onNodeWithText("Your own habit").assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextInput("Stretch")
        rule.onNodeWithText("Add this habit").performClick()
        rule.runOnIdle { assertEquals(listOf("Stretch"), own.toList()) }
        rule.onNodeWithText("Stretch").performScrollTo().assertIsOn()
    }

    @Test fun practiceHoldLogsPartial() {
        rule.setContent { SproutTheme { PracticeStep(suggestions[1], strings) } }
        rule.onNodeWithText("20 pages").performTouchInput { longClick() }
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("10 / 20 pages").assertIsDisplayed()
        rule.onAllNodesWithContentDescription("Tried").assertCountEquals(1)
    }

    @Test fun practiceSwipesAndTapTickTheirRows() {
        rule.setContent { SproutTheme { PracticeStep(suggestions[1], strings) } }
        rule.onNodeWithText("20 pages").performTouchInput { swipeRight() }
        rule.onNodeWithText("20 / 20 pages").assertIsDisplayed()
        // Left on a done card undoes it; left again skips.
        rule.onNodeWithText("20 / 20 pages").performTouchInput { swipeLeft() }
        rule.onNodeWithText("20 pages").performTouchInput { swipeLeft() }
        rule.onAllNodesWithText("Skipped").onFirst().assertIsDisplayed()
        rule.onAllNodesWithContentDescription("Tried").assertCountEquals(2)
        // Right on a skipped card undoes it; then tap the circle for Done.
        rule.onAllNodesWithText("Skipped").onFirst().performTouchInput { swipeRight() }
        rule.onNodeWithContentDescription("Mark Read done").performClick()
        rule.onNodeWithText("20 / 20 pages").assertIsDisplayed()
        rule.onAllNodesWithContentDescription("Tried").assertCountEquals(3)
    }

    @Test fun practiceUndoBarPutsTheCardBackAndTicksUndo() {
        rule.setContent { SproutTheme { PracticeStep(suggestions[1], strings) } }
        rule.onNodeWithContentDescription("Mark Read done").performClick()
        rule.onNodeWithText("Read marked done").assertIsDisplayed()
        // The bar's Undo, not the "Undo" row title: the bar comes last.
        rule.onAllNodesWithText("Undo").onLast().performClick()
        rule.onNodeWithText("20 pages").assertIsDisplayed()
        // Tap the circle and Undo are both ticked.
        rule.onAllNodesWithContentDescription("Tried").assertCountEquals(2)
    }

    @Test fun practiceNoteShowsInTheJournal() {
        rule.setContent { SproutTheme { PracticeStep(suggestions[1], strings) } }
        rule.onNodeWithText("20 pages").performTouchInput { longClick() }
        rule.onNode(hasSetTextAction()).performTextInput("Fell asleep early")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("10 / 20 pages · note added").assertIsDisplayed()
        rule.onNodeWithText("Journal").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Fell asleep early").performScrollTo().assertIsDisplayed()
        // Hold and Add a note are both ticked.
        rule.onAllNodesWithContentDescription("Tried").assertCountEquals(2)
    }
}
