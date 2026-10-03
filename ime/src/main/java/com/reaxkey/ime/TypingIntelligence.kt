package com.reaxkey.ime

import android.content.Context
import java.util.Locale

/** Small, deliberately conservative offline English model. No sentence history is retained. */
class TypingIntelligence(context: Context) {
    private val store = context.applicationContext.getSharedPreferences("learned_words", Context.MODE_PRIVATE)
    private val sessionUses = HashMap<String, Int>()
    private var observedClearEpoch=store.getLong("__clear_epoch",0L)
    private val common = linkedMapOf(
        "the" to 1000, "and" to 900, "you" to 850, "to" to 820, "I" to 800,
        "a" to 780, "is" to 760, "it" to 750, "for" to 740, "in" to 730,
        "of" to 720, "that" to 700, "on" to 680, "with" to 660, "this" to 650,
        "have" to 640, "be" to 620, "are" to 610, "was" to 590, "not" to 580,
        "we" to 570, "my" to 560, "your" to 550, "can" to 540, "do" to 530,
        "what" to 520, "when" to 510, "where" to 500, "how" to 490, "there" to 480,
        "here" to 470, "yes" to 460, "no" to 450, "please" to 440, "thanks" to 430,
        "thank" to 420, "good" to 410, "great" to 400, "hello" to 390, "hi" to 380,
        "today" to 370, "tomorrow" to 360, "now" to 350, "later" to 340,
        "because" to 330, "about" to 320, "from" to 310, "just" to 300,
        "really" to 290, "like" to 280, "want" to 270, "need" to 260,
        "will" to 250, "would" to 240, "could" to 230, "should" to 220,
        "don't" to 210, "can't" to 200, "it's" to 190, "I'm" to 180,
        "they" to 170, "them" to 160, "their" to 150, "our" to 140,
        "one" to 130, "two" to 120, "three" to 110, "time" to 100,
        "work" to 99, "home" to 98, "phone" to 97, "message" to 96,
        "send" to 95, "sent" to 94, "see" to 93, "seen" to 92,
        "meet" to 91, "meeting" to 90, "call" to 89, "talk" to 88,
        "okay" to 87, "ok" to 86, "maybe" to 85, "sure" to 84,
        "love" to 83, "happy" to 82, "sorry" to 81, "soon" to 80
    )
    private val contextNext = mapOf(
        "how" to listOf("are", "do", "is"), "thank" to listOf("you", "the", "for"),
        "see" to listOf("you", "the", "it"), "i" to listOf("am", "have", "will"),
        "good" to listOf("morning", "night", "day")
    )
    private val typo = mapOf("teh" to "the", "becuase" to "because", "dont" to "don't",
        "thier" to "their", "recieve" to "receive", "adn" to "and", "waht" to "what")

    fun suggestions(prefix: String, previousWord: String): List<String> {
        val lower = prefix.lowercase(Locale.ROOT)
        if (lower.length > 32 || prefix.any { !it.isLetter() && it != '\'' }) return emptyList()
        val learned = store.all.values.filterIsInstance<String>().filter { it.startsWith(lower, true) }
        val candidates = (common.keys + learned + if(lower.isNotEmpty()) listOfNotNull(typo[lower]) else emptyList())
            .distinctBy { it.lowercase(Locale.ROOT) }
            .filter { it.startsWith(lower, true) || (lower.length >= 3 && typo[lower]?.equals(it, true) == true) }
        val context = contextNext[previousWord.lowercase(Locale.ROOT)].orEmpty()
        val ranked = candidates.sortedWith(compareByDescending<String> { candidate ->
            (common[candidate] ?: 0) + (if(candidate in learned) 350 else 0) +
                (if(context.any { it.equals(candidate, true) }) 900 else 0) +
                (if(candidate.equals(prefix, true)) 250 else 0) +
                (if(typo[lower]?.equals(candidate, true) == true) 600 else 0)
        }.thenBy { it.length })
        return ranked.take(3).map { matchCase(it, prefix) }
    }

    fun correction(word: String): String? {
        if(word.length < 3 || word.length > 32 || word.any { !it.isLetter() && it != '\'' }) return null
        val lower = word.lowercase(Locale.ROOT)
        if(common.keys.any { it.equals(word, true) } || store.contains(lower)) return null
        // Only curated, high-confidence transpositions/omissions are corrected automatically.
        val candidate = typo[lower] ?: return null
        return matchCase(candidate, word)
    }

    fun observeDeliberateWord(word: String) {
        val clearEpoch=store.getLong("__clear_epoch",0L)
        if(clearEpoch!=observedClearEpoch) {sessionUses.clear();observedClearEpoch=clearEpoch}
        if(word.length !in 2..32 || word.any { !it.isLetter() } || common.keys.any { it.equals(word, true) }) return
        val lower = word.lowercase(Locale.ROOT)
        if(store.contains(lower)) return
        val count = (sessionUses[lower] ?: 0) + 1
        if(count >= 3) { store.edit().putString(lower, word).apply(); sessionUses.remove(lower) }
        else sessionUses[lower] = count
    }

    fun clearLearnedWords() { val next=store.getLong("__clear_epoch",0L)+1;store.edit().clear().putLong("__clear_epoch",next).apply(); sessionUses.clear() }

    companion object {
        fun clear(context: Context) { val prefs=context.applicationContext.getSharedPreferences("learned_words", Context.MODE_PRIVATE)
            val next=prefs.getLong("__clear_epoch",0L)+1;prefs.edit().clear().putLong("__clear_epoch",next).apply() }
        private fun matchCase(candidate: String, typed: String): String = when {
            typed.isNotEmpty() && typed.all { it.isUpperCase() } -> candidate.uppercase(Locale.ROOT)
            typed.firstOrNull()?.isUpperCase() == true -> candidate.replaceFirstChar { it.uppercase(Locale.ROOT) }
            else -> candidate
        }
    }
}
