package app.sprout.habits.ui.note

import app.sprout.habits.domain.isScheduled
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.domain.PastNote
import app.sprout.habits.domain.SentenceIndex
import app.sprout.habits.domain.joinNoteText
import app.sprout.habits.data.EntryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NoteForm(
    val loaded: Boolean = false,
    val noteId: Long = 0,
    val habitId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val text: String = "",
    /**
     * Goes up each time the form puts in a text of its own (a saved note shown, a habit or day
     * changed), and not on [WriteNoteViewModel.setText]. The field takes the text over only then:
     * copying back every echo of what was typed let an older one land after newer keystrokes and
     * drop letters when typing fast.
     */
    val textVersion: Int = 0,
) {
    val isNew: Boolean get() = noteId == 0L
    val canSave: Boolean get() = habitId != 0L && text.isNotBlank()
}

/** What the writing suggestions learn from: the user's notes and a prefix index of their sentences. */
@Immutable
class NoteSuggestionData(val notes: List<PastNote>, val index: SentenceIndex) {
    companion object {
        val EMPTY = NoteSuggestionData(emptyList(), SentenceIndex(emptyList()))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WriteNoteViewModel(
    private val repository: HabitRepository,
    noteId: Long,
    habitId: Long,
    epochDay: Long?,
    private val state: SavedStateHandle,
    /** "Clear suggestion history" time: notes last written before it aren't suggested. */
    suggestionsClearedAt: Flow<Long> = flowOf(0L),
) : ViewModel() {
    /** True when the screen was opened to write, not to edit a note picked from a list. */
    val opensTyping: Boolean = noteId == 0L

    private val _form = MutableStateFlow(NoteForm())
    val form: StateFlow<NoteForm> = _form

    /** Habits to choose from. An archived habit is included only when the note already belongs to it. */
    val habits: StateFlow<List<Habit>> = repository.observeAllHabits()
        .combine(_form) { all, f -> all.filter { !it.archived || it.id == f.habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** What was logged for the chosen habit on the chosen day, shown next to the date. */
    val entry: StateFlow<Entry?> = _form
        .map { it.habitId to it.date.toEpochDay() }
        .flatMapLatest { (habit, day) ->
            if (habit == 0L) flowOf(null) else repository.observeEntries(habit, day, day).map { it.firstOrNull() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Every note with the outcome of its day, and the sentence index, rebuilt off the main thread
     * whenever a note is saved, edited or deleted. A past scheduled day with no entry counts as
     * Skip, like everywhere else.
     */
    val suggestions: StateFlow<NoteSuggestionData> = combine(
        repository.observeNotes(),
        repository.observeEntries(0, LocalDate.now().toEpochDay()),
        repository.observeAllHabits(),
        suggestionsClearedAt,
    ) { notes, entries, habits, clearedAt ->
        val status = entries.associate { (it.habitId to it.date) to it.status }
        val habitById = habits.associateBy { it.id }
        val today = LocalDate.now().toEpochDay()
        val past = notes.filter { it.updatedAt >= clearedAt && it.text.isNotBlank() }.map { n ->
            val logged = status[n.habitId to n.date]
            val missed = n.date < today && habitById[n.habitId]?.let { isScheduled(it.daysMask, LocalDate.ofEpochDay(n.date)) } == true
            PastNote(n.id, n.habitId, n.date, n.text, logged ?: if (missed) EntryStatus.SKIP else null)
        }
        NoteSuggestionData(past, SentenceIndex(past))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteSuggestionData.EMPTY)

    /**
     * The saved note the form shows because the habit and day chosen already have one. Null for a
     * blank note, and for a note opened by its id (changing its date moves that note).
     */
    private var existing: Note? = null

    /** What the user had typed before [existing] was put above it. */
    private var typedBefore = ""
        set(value) {
            field = value
            state[KEY_TYPED_BEFORE] = value
        }

    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    init {
        viewModelScope.launch {
            val note = if (noteId != 0L) repository.getNote(noteId) else null
            val keptHabit: Long? = state[KEY_HABIT]
            val keptDay: Long? = state[KEY_DAY]
            _form.value = if (keptHabit != null && keptDay != null) {
                // Back after the system closed the app in the background: the habit, day and text
                // as they were left, not the ones the screen was first opened with.
                if (note == null) {
                    existing = repository.getNote(keptHabit, keptDay)
                    typedBefore = state[KEY_TYPED_BEFORE] ?: ""
                }
                NoteForm(true, note?.id ?: existing?.id ?: 0, keptHabit, LocalDate.ofEpochDay(keptDay), state[KEY_TEXT] ?: "")
            } else if (note != null) {
                NoteForm(true, note.id, note.habitId, LocalDate.ofEpochDay(note.date), note.text)
            } else {
                val date = epochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
                formFor(if (habitId != 0L) habitId else suggestedHabit(date), date, typed = "")
            }
            _form.collect { f ->
                state[KEY_HABIT] = f.habitId
                state[KEY_DAY] = f.date.toEpochDay()
                state[KEY_TEXT] = f.text
            }
        }
    }

    /**
     * The habit a new note starts on when none was chosen (Add note on Today and in the Journal):
     * the one logged last on [date] (Done, Partial or Skip) that has no note yet, else the first
     * habit due that day.
     */
    private suspend fun suggestedHabit(date: LocalDate): Long {
        val day = date.toEpochDay()
        val habits = repository.observeHabits().first()
        val active = habits.map { it.id }.toSet()
        val logged = repository.observeEntries(day, day).first()
            .filter { it.habitId in active }
            .sortedByDescending { it.loggedAt ?: 0L }
        logged.firstOrNull { repository.getNote(it.habitId, day) == null }?.let { return it.habitId }
        return (habits.firstOrNull { isScheduled(it.daysMask, date) } ?: habits.firstOrNull())?.id ?: 0L
    }

    fun setText(text: String) = _form.update { it.copy(text = text) }
    fun setHabit(id: Long) = moveTo(id, _form.value.date)
    fun setDate(date: LocalDate) = moveTo(_form.value.habitId, minOf(date, LocalDate.now()))

    /**
     * Points the form at another habit or day. If that one already has a note, the form shows it,
     * so saving edits it instead of writing a second note without the first ever being seen.
     */
    private fun moveTo(habitId: Long, date: LocalDate) {
        val f = _form.value
        if (!f.isNew && existing == null) {
            _form.update { it.copy(habitId = habitId, date = date) }
            return
        }
        viewModelScope.launch {
            // Only what the user wrote moves on; a saved note left as shown stays on its own day.
            val typed = if (existing != null && f.text == joinNoteText(existing?.text, typedBefore)) typedBefore else f.text
            val next = formFor(habitId, date, typed)
            // Keep anything typed while the lookup ran.
            _form.update { if (it.text == f.text) next else next.copy(text = it.text) }
        }
    }

    private suspend fun formFor(habitId: Long, date: LocalDate, typed: String): NoteForm {
        val saved = if (habitId != 0L) repository.getNote(habitId, date.toEpochDay()) else null
        existing = saved
        typedBefore = typed
        return NoteForm(
            loaded = true,
            noteId = saved?.id ?: 0,
            habitId = habitId,
            date = date,
            text = joinNoteText(saved?.text, typed),
            textVersion = _form.value.textVersion + 1,
        )
    }

    fun save() {
        val f = _form.value
        if (!f.canSave) return
        viewModelScope.launch {
            repository.saveNote(Note(id = f.noteId, habitId = f.habitId, date = f.date.toEpochDay(), text = f.text.trim()))
            _done.send(Unit)
        }
    }

    fun delete() {
        val id = _form.value.noteId
        if (id == 0L) return
        viewModelScope.launch {
            repository.deleteNote(id)
            _done.send(Unit)
        }
    }

    private companion object {
        const val KEY_HABIT = "habitId"
        const val KEY_DAY = "day"
        const val KEY_TEXT = "text"
        const val KEY_TYPED_BEFORE = "typedBefore"
    }
}
