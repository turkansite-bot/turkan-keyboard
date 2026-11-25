package com.turkanyazi.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryLearnedStore : LearnedStore {
    val data = HashMap<String, Int>()
    override fun loadAll(): Map<String, Int> = HashMap(data)
    override fun put(key: String, value: Int) { data[key] = value }
    override fun putAll(entries: Map<String, Int>) { data.putAll(entries) }
    override fun remove(keys: Collection<String>) { for (k in keys) data.remove(k) }
    override fun clear() { data.clear() }
}

class SuggesterTest {
    private fun suggester(vararg words: String, store: LearnedStore = MemoryLearnedStore()): Suggester =
        Suggester(store).also { it.setWords(words.toList()) }

    @Test fun completesPrefixesInDictionaryRankOrder() {
        val s = suggester("salam", "salat", "sağ", "sabah", "kitab")
        assertEquals(listOf("salam", "salat", "sağ"), s.suggest("sa", false))
    }

    @Test fun neverSuggestsTheWordJustTyped() {
        val s = suggester("salam", "salamat")
        assertFalse("salam" in s.suggest("salam", false))
        assertTrue("salamat" in s.suggest("salam", false))
    }

    @Test fun keepsCapitalisation() {
        assertEquals(listOf("Salam"), suggester("salam").suggest("Sal", false))
    }

    @Test fun handlesAzerbaijaniDotlessI() {
        assertEquals(listOf("Is\u0131q"), suggester("\u0131s\u0131q").suggest("Is", false))
    }

    @Test fun correctsATransposition() {
        val s = suggester("kitab", "kitablar")
        assertEquals(listOf("kitab"), s.suggest("kitba", false))
    }

    @Test fun suggestsArabicScriptWords() {
        val s = suggester("salam")
        val arabic = ScriptConverter.latinToArabic("salam")
        assertEquals(listOf(arabic), s.suggest(arabic.dropLast(1), true))
    }

    @Test fun learnedWordsOutrankTheDictionary() {
        val s = suggester("salam", "salat")
        s.learn("salat")
        assertEquals(listOf("salat", "salam"), s.suggest("sal", false))
    }

    @Test fun learnedWordsSurviveANewInstance() {
        val store = MemoryLearnedStore()
        Suggester(store).learn("bayraq")
        assertTrue("bayraq" in Suggester(store).suggest("bay", false))
    }

    @Test fun learnsNextWordPairs() {
        val s = suggester("salam")
        s.learn("sağol", "salam")
        assertEquals("sağol", s.suggest("", false, previousWord = "salam").first())
    }

    @Test fun seedPairsWorkWithoutLearning() {
        val next = suggester("salam").suggest("", false, previousWord = "salam")
        assertTrue("necəsən" in next)
    }

    @Test fun ignoresNonWords() {
        val store = MemoryLearnedStore()
        val s = Suggester(store)
        s.learn("a")
        s.learn("abc1")
        s.learn("a-b")
        assertEquals(0, s.learnedWordCount())
        assertTrue(store.data.isEmpty())
    }

    @Test fun clearRemovesEverythingEverywhere() {
        val store = MemoryLearnedStore()
        val s = Suggester(store)
        s.learn("salam", "dost")
        s.clearLearned()
        assertEquals(0, s.learnedWordCount())
        assertEquals(0, s.learnedBigramCount())
        assertTrue(store.data.isEmpty())
    }

    @Test fun learnedWordsAreCappedByEvictingTheLeastUsed() {
        val s = Suggester(MemoryLearnedStore())
        fun word(i: Int): String {
            var n = i
            val sb = StringBuilder()
            do { sb.append('a' + n % 26); n /= 26 } while (n > 0)
            return sb.toString().padEnd(2, 'a')
        }
        repeat(5100) { s.learn(word(it)) }
        assertTrue(s.learnedWordCount() <= 5000)
    }

    @Test fun nextWordPairsAreCappedToo() {
        val s = Suggester(MemoryLearnedStore())
        fun word(i: Int): String {
            var n = i
            val sb = StringBuilder()
            do { sb.append('a' + n % 26); n /= 26 } while (n > 0)
            return sb.toString().padEnd(2, 'a')
        }
        repeat(10200) { s.learn(word(it + 1), word(it)) }
        assertTrue(s.learnedBigramCount() <= 10000)
    }

    @Test fun suggestionsBeforeDictionaryLoadedStillWork() {
        val s = Suggester(MemoryLearnedStore())
        s.learn("salam")
        assertEquals(listOf("salam"), s.suggest("sal", false))
    }
}
