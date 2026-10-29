package app.sprout.habits.domain

import app.sprout.habits.data.EntryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSuggestionsTest {
    private fun note(id: Long, text: String, habit: Long = 1, date: Long = id, status: EntryStatus? = EntryStatus.SKIP) =
        PastNote(id, habit, date, text, status)

    @Test fun typedSentenceStartsAfterTheLastFullStop() {
        val text = "Long day. Got irritated wi"
        assertEquals("Got irritated wi", typedSentence(text, text.length))
        assertEquals("", typedSentence("Done. ", 6))
    }

    @Test fun completesFromPastSentencesSameHabitFirst() {
        val index = SentenceIndex(
            listOf(
                note(1, "Got irritated with work.", habit = 2),
                note(2, "Got irritated with work.", habit = 2),
                note(3, "Got irritated with Huda again", habit = 1),
                note(4, "Got irritated in traffic. Then calm.", habit = 3),
            ),
        )
        val endings = index.complete("got  irritated wi", habitId = 1).map { it.ending }
        assertEquals(listOf("th Huda again", "th work."), endings)
        // Another habit: most written first, then the newest.
        assertEquals(listOf("with work.", "in traffic.", "with Huda again"), index.complete("Got irritated ", habitId = 9).map { it.ending })
    }

    @Test fun noCompletionForShortTextOrShortEnding() {
        val index = SentenceIndex(listOf(note(1, "Tired.")))
        assertTrue(index.complete("Ti", 1).isEmpty())
        // "Tired." minus "Tire" leaves "d.", too short to suggest.
        assertTrue(index.complete("Tire", 1).isEmpty())
    }

    @Test fun similarNotesFindTheTypedPartNewestFirst() {
        val notes = listOf(note(1, "Got irritated at the slow internet", date = 5), note(2, "Got  irritated with Huda.", date = 9), note(3, "got irritated with huda", date = 2))
        val found = similarNotes(notes, "Got irritated")
        assertEquals(listOf("Got irritated with Huda.", "Got irritated at the slow internet"), found.map { it.text })
        assertEquals(0..12, found.first().match)
    }

    @Test fun recentNotesAreThisHabitsWithoutRepeats() {
        val notes = listOf(note(1, "Argument with Huda"), note(2, "argument with huda!"), note(3, "Other habit", habit = 2), note(4, "Snapped at work"))
        assertEquals(listOf("Snapped at work", "argument with huda!"), recentNotes(notes, habitId = 1).map { it.text })
    }

    @Test fun customOpenersNeedThreeUsesAndPreferTheLongerOne() {
        val notes = listOf(
            note(1, "Got irritated with Huda"),
            note(2, "Got irritated at work."),
            note(3, "got irritated, again"),
            note(4, "Felt tired"),
            note(5, "Got up late", status = EntryStatus.DONE),
        )
        assertEquals(listOf("Got irritated"), customOpeners(notes, EntryStatus.SKIP))
        assertEquals(listOf("Got irritated", "Felt tired", "No time"), quickStartPhrases(listOf("Got irritated"), listOf("got irritated", "Felt tired", "No time", "Forgot"), limit = 3))
    }

    @Test fun insertPhraseStartsAnEmptyNote() {
        assertEquals(TextEdit("Felt tired. ", 12), insertPhrase("", 0, "felt tired"))
        assertEquals(TextEdit("Long day, felt tired", 20), insertPhrase("Long day,", 9, "Felt tired"))
        assertEquals(TextEdit("Long day. Felt tired", 20), insertPhrase("Long day. ", 10, "felt tired"))
    }

    @Test fun endingsAndSentenceReplacement() {
        assertEquals(TextEdit("Got irritated with Huda", 23), insertEnding("Got irritated wi", 16, "th Huda"))
        assertEquals(TextEdit("Long day. Argument with Huda", 28), replaceSentence("Long day. Got irri", 18, "Argument with Huda"))
    }
}
