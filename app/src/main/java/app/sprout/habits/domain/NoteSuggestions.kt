package app.sprout.habits.domain

import app.sprout.habits.data.EntryStatus

/*
 * Suggestions while writing a note. Everything comes from a fixed list of starter phrases and the
 * user's own notes, on the phone: no model, no network.
 */

/** A saved note as the suggestions see it: its text, habit, day and the day's outcome. */
data class PastNote(
    val id: Long,
    val habitId: Long,
    val date: Long,
    val text: String,
    /** The outcome of the day the note is about; null when the day isn't logged yet. */
    val status: EntryStatus?,
)

/** A completion for the sentence being typed: the [ending] to add after the cursor. */
data class Completion(val ending: String, val sentence: String)

/** A past note that contains the typed text, with where it matched in [text]. */
data class SimilarNote(val note: PastNote, val text: String, val match: IntRange)

/** Notes past this length get no suggestions. */
const val SUGGESTIONS_MAX_LENGTH = 240

/** Completion starts once the current sentence has this many characters. */
const val COMPLETION_MIN_TYPED = 3

/** Shorter endings aren't worth a suggestion. */
const val COMPLETION_MIN_ENDING = 3

private val SENTENCE_END = charArrayOf('.', '!', '?', '\n')
private val SPACES = Regex("\\s+")

/** Lower case with runs of spaces made one, for matching. */
private fun matchKey(s: String) = s.replace(SPACES, " ").lowercase()

/** Lower case, letters, digits and single spaces only, for spotting the same note twice. */
fun dedupeKey(s: String): String = s.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }.replace(SPACES, " ").trim()

/** Where the sentence holding [cursor] starts in [text]: just after the last ., !, ? or line break. */
fun sentenceStart(text: String, cursor: Int): Int {
    var i = cursor.coerceIn(0, text.length)
    while (i > 0 && text[i - 1] !in SENTENCE_END) i--
    while (i < cursor && text[i].isWhitespace()) i++
    return i
}

/** Where the sentence holding [cursor] ends: at its ., !, ? (included) or line break, or the end. */
fun sentenceEnd(text: String, cursor: Int): Int {
    var i = cursor.coerceIn(0, text.length)
    while (i < text.length && text[i] !in SENTENCE_END) i++
    return if (i < text.length && text[i] != '\n') i + 1 else i
}

/** The current sentence from its start to the cursor ("Got irritated wi"). */
fun typedSentence(text: String, cursor: Int): String = text.substring(sentenceStart(text, cursor), cursor.coerceIn(0, text.length))

/** The sentences of a note, trimmed, with runs of spaces made one. */
fun sentencesOf(text: String): List<String> {
    val out = ArrayList<String>()
    var start = 0
    for (i in text.indices) {
        if (text[i] in SENTENCE_END) {
            // Keep the full stop with its sentence, so a completion can end with it.
            val end = if (text[i] == '\n') i else i + 1
            text.substring(start, end).replace(SPACES, " ").trim().takeIf { it.isNotEmpty() }?.let(out::add)
            start = i + 1
        }
    }
    text.substring(start).replace(SPACES, " ").trim().takeIf { it.isNotEmpty() }?.let(out::add)
    return out
}

/**
 * A prefix index over the sentences of the user's notes: the sentences sorted by their lower-case
 * text, so every sentence starting with what was typed sits in one run found by binary search.
 * Built off the main thread whenever the notes change.
 */
class SentenceIndex(notes: List<PastNote>) {
    private class Item(val key: String, val sentence: String, val habits: Set<Long>, val count: Int, val latest: Long)

    private val items: List<Item> = notes
        .flatMap { note -> sentencesOf(note.text).map { it to note } }
        .groupBy { (sentence, _) -> matchKey(sentence) }
        .map { (key, uses) ->
            val newest = uses.maxBy { it.second.date }
            Item(key, newest.first, uses.mapTo(HashSet()) { it.second.habitId }, uses.size, newest.second.date)
        }
        .sortedBy { it.key }

    val isEmpty: Boolean get() = items.isEmpty()

    /**
     * Up to [limit] different endings for [typed], the sentence so far. Sentences of [habitId] come
     * first, then the ones written most often, then the newest.
     */
    fun complete(typed: String, habitId: Long, limit: Int = 3): List<Completion> {
        val prefix = matchKey(typed.trimStart())
        if (prefix.trim().length < COMPLETION_MIN_TYPED) return emptyList()
        var lo = 0
        var hi = items.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (items[mid].key < prefix) lo = mid + 1 else hi = mid
        }
        val matches = ArrayList<Item>()
        var i = lo
        while (i < items.size && items[i].key.startsWith(prefix)) {
            if (items[i].key.length - prefix.length >= COMPLETION_MIN_ENDING) matches += items[i]
            i++
        }
        return matches
            .sortedWith(compareByDescending<Item> { habitId in it.habits }.thenByDescending { it.count }.thenByDescending { it.latest })
            .map { Completion(it.sentence.substring(minOf(prefix.length, it.sentence.length)), it.sentence) }
            .distinctBy { it.ending.lowercase() }
            .take(limit)
    }
}

/**
 * Past notes containing [typed] (ignoring case and extra spaces), newest first, at most [limit].
 * Each comes with its text tidied to single spaces and where the typed part is in it.
 */
fun similarNotes(notes: List<PastNote>, typed: String, excludeId: Long = 0, limit: Int = 3): List<SimilarNote> {
    val needle = matchKey(typed.trim())
    if (needle.length < COMPLETION_MIN_TYPED) return emptyList()
    val seen = HashSet<String>()
    return notes.asSequence()
        .filter { it.id != excludeId }
        .sortedByDescending { it.date }
        .mapNotNull { note ->
            val text = note.text.replace(SPACES, " ").trim()
            val at = text.lowercase().indexOf(needle)
            if (at < 0 || !seen.add(dedupeKey(text))) null else SimilarNote(note, text, at until at + needle.length)
        }
        .take(limit)
        .toList()
}

/** The newest notes of [habitId], one of each text (ignoring case and punctuation), at most [limit]. */
fun recentNotes(notes: List<PastNote>, habitId: Long, excludeId: Long = 0, limit: Int = 3): List<PastNote> =
    notes.asSequence()
        .filter { it.habitId == habitId && it.id != excludeId && it.text.isNotBlank() }
        .sortedByDescending { it.date }
        .distinctBy { dedupeKey(it.text) }
        .take(limit)
        .toList()

/**
 * The user's own ways of starting a note for [status]: the first one to three words of their
 * notes, used at least [minUses] times ("Got irritated"). A longer opener wins over the shorter
 * one inside it when both are used as often. Most used first, at most [limit].
 */
fun customOpeners(notes: List<PastNote>, status: EntryStatus, minUses: Int = 3, limit: Int = 3): List<String> {
    val counts = HashMap<String, Int>()
    val shown = HashMap<String, String>()
    notes.filter { it.status == status }.forEach { note ->
        val words = sentencesOf(note.text).firstOrNull()?.trimEnd('.', '!', '?', ',', ';', ':')?.split(' ').orEmpty()
        for (n in 1..minOf(3, words.size)) {
            val phrase = words.take(n).joinToString(" ").trimEnd(',', ';', ':')
            val key = dedupeKey(phrase)
            if (key.isEmpty()) continue
            counts[key] = (counts[key] ?: 0) + 1
            shown.putIfAbsent(key, phrase)
        }
    }
    val often = counts.filter { it.value >= minUses }
    return often.keys
        .filter { key -> often.none { (other, n) -> other != key && other.startsWith("$key ") && n >= often.getValue(key) } }
        .sortedWith(compareByDescending<String> { often.getValue(it) }.thenByDescending { it.count { c -> c == ' ' } })
        .take(limit)
        .map { shown.getValue(it) }
}

/** The Quick start chips: the user's [custom] openers, then [defaults], no repeats, at most [limit]. */
fun quickStartPhrases(custom: List<String>, defaults: List<String>, limit: Int = 6): List<String> =
    (custom + defaults).distinctBy { dedupeKey(it) }.take(limit)

/** A text edit: the new text and where the cursor goes. */
data class TextEdit(val text: String, val cursor: Int)

/**
 * Puts a Quick start [phrase] at the [cursor]. In an empty note it starts the note with a capital
 * and ". " after it, so the user keeps typing. Elsewhere it gets a space before it when needed, a
 * capital at the start of a sentence and a small letter in the middle of one.
 */
fun insertPhrase(text: String, cursor: Int, phrase: String): TextEdit {
    val p = phrase.trim().trimEnd('.')
    if (text.isBlank()) {
        val started = p.replaceFirstChar { it.uppercase() } + ". "
        return TextEdit(started, started.length)
    }
    val at = cursor.coerceIn(0, text.length)
    val before = text.substring(0, at)
    val startsSentence = before.trimEnd().let { it.isEmpty() || it.last() in SENTENCE_END }
    val word = if (startsSentence) p.replaceFirstChar { it.uppercase() } else p.replaceFirstChar { it.lowercase() }
    val gap = if (before.isEmpty() || before.last().isWhitespace()) "" else " "
    val inserted = gap + word
    return TextEdit(before + inserted + text.substring(at), at + inserted.length)
}

/** Adds a completion's [ending] at the [cursor] and moves the cursor after it. */
fun insertEnding(text: String, cursor: Int, ending: String): TextEdit {
    val at = cursor.coerceIn(0, text.length)
    return TextEdit(text.substring(0, at) + ending + text.substring(at), at + ending.length)
}

/** Replaces the sentence holding the [cursor] with a past note's [noteText]. */
fun replaceSentence(text: String, cursor: Int, noteText: String): TextEdit {
    val start = sentenceStart(text, cursor)
    val end = sentenceEnd(text, cursor)
    val after = text.substring(end)
    val joined = text.substring(0, start) + noteText + after
    return TextEdit(joined, start + noteText.length)
}
