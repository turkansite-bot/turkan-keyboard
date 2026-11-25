package com.turkanyazi.keyboard

import android.content.Context
import java.util.Locale

/** Storage for the words the user has typed. Kept abstract so the engine is unit-testable. */
interface LearnedStore {
    fun loadAll(): Map<String, Int>
    fun put(key: String, value: Int)
    fun putAll(entries: Map<String, Int>)
    fun remove(keys: Collection<String>)
    fun clear()
}

/** On-device SharedPreferences storage. Excluded from backups (allowBackup=false). */
class PrefsLearnedStore(context: Context) : LearnedStore {
    private val prefs = context.getSharedPreferences("user_words", Context.MODE_PRIVATE)

    override fun loadAll(): Map<String, Int> {
        val out = HashMap<String, Int>()
        for ((key, value) in prefs.all) if (value is Int) out[key] = value
        return out
    }

    override fun put(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun putAll(entries: Map<String, Int>) {
        if (entries.isEmpty()) return
        val editor = prefs.edit()
        for ((key, value) in entries) editor.putInt(key, value)
        editor.apply()
    }

    override fun remove(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val editor = prefs.edit()
        for (key in keys) editor.remove(key)
        editor.apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

/**
 * Word completion, light typo correction and next-word prediction.
 *
 * Performance notes: learned words live in memory (the store is only written to,
 * never re-read per keystroke); the bundled dictionary is sorted so prefix lookup
 * is a binary search; fuzzy matching is length-pruned before computing distances.
 * Both learned maps are capped and evict their least-used entries when full.
 */
class Suggester(
    private val store: LearnedStore,
    private val toArabic: (String) -> String = { ScriptConverter.latinToArabic(it) }
) {
    private class Entry(val lower: String, val display: String, val rank: Int)
    private class Dictionary(val latin: Array<Entry>, val arabic: Array<Entry>)

    private val az: Locale = Locale.forLanguageTag("az")
    private val lock = Any()
    private val learnedWords = HashMap<String, Int>()
    private val learnedBigrams = HashMap<String, Int>()

    @Volatile
    private var dictionary: Dictionary? = null

    init {
        loadLearned()
    }

    // ---------- loading ----------

    private fun loadLearned() {
        synchronized(lock) {
            learnedWords.clear()
            learnedBigrams.clear()
            for ((key, value) in store.loadAll()) {
                if (value <= 0) continue
                if (key.startsWith(BIGRAM_PREFIX)) learnedBigrams[key] = value else learnedWords[key] = value
            }
            trimTo(learnedWords, MAX_LEARNED_WORDS)
            trimTo(learnedBigrams, MAX_BIGRAMS)
        }
    }

    /** Installs the bundled dictionary. Order matters: earlier words rank higher. Safe to call from any thread. */
    fun setWords(words: List<String>) {
        val unique = LinkedHashMap<String, String>()
        for (raw in words) {
            val w = raw.trim()
            if (w.isEmpty() || w.startsWith("#")) continue
            val lower = w.lowercase(az)
            if (!unique.containsKey(lower)) unique[lower] = w
        }
        val latin = ArrayList<Entry>(unique.size)
        val arabic = ArrayList<Entry>(unique.size)
        var rank = 0
        for ((lower, display) in unique) {
            latin.add(Entry(lower, display, rank))
            val a = toArabic(display)
            arabic.add(Entry(a.lowercase(az), a, rank))
            rank++
        }
        val byLower = compareBy<Entry> { it.lower }
        dictionary = Dictionary(
            latin.sortedWith(byLower).toTypedArray(),
            arabic.sortedWith(byLower).toTypedArray()
        )
    }

    // ---------- suggestions ----------

    fun suggest(prefix: String, arabicMode: Boolean, limit: Int = 3, previousWord: String? = null): List<String> {
        if (limit <= 0) return emptyList()
        val p = prefix.lowercase(az)
        if (p.isEmpty()) {
            return if (previousWord.isNullOrBlank()) emptyList() else nextWords(previousWord, arabicMode, limit)
        }

        val out = ArrayList<String>()
        val seen = HashSet<String>()
        fun add(word: String) {
            if (out.size < limit && seen.add(word.lowercase(az))) out.add(word)
        }

        // 1. The person's own words, most used first.
        val personal = synchronized(lock) {
            learnedWords.entries
                .filter { it.key.startsWith(p) && it.key != p }
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }
        }
        for (w in personal) add(w)

        // 2. Bundled dictionary completions (binary search + rank order).
        val dict = dictionary
        val base: Array<Entry>? = if (arabicMode) dict?.arabic else dict?.latin
        if (base != null && out.size < limit) {
            var i = lowerBound(base, p)
            val hits = ArrayList<Entry>()
            while (i < base.size && base[i].lower.startsWith(p)) {
                if (base[i].lower != p) hits.add(base[i])
                i++
            }
            hits.sortBy { it.rank }
            for (h in hits) add(h.display)
        }

        // 3. Light typo correction for longer words.
        if (out.size < limit && p.length >= 4) {
            val maxDistance = if (p.length >= 7) 2 else 1
            val scored = ArrayList<Pair<String, Int>>()

            fun consider(display: String, lower: String) {
                if (lower.length < p.length - maxDistance) return
                var best = WordAlgorithms.boundedEditDistance(p, lower, maxDistance)
                if (lower.length > p.length) {
                    // Compare against the same-length beginning of the word, so a typo in a
                    // half-typed long word can still find its completion (small penalty).
                    val head = WordAlgorithms.boundedEditDistance(p, lower.substring(0, p.length), maxDistance) + 1
                    if (head < best) best = head
                }
                if (best in 1..maxDistance) scored.add(display to best)
            }

            if (base != null) for (e in base) consider(e.display, e.lower)
            val keys = synchronized(lock) { ArrayList(learnedWords.keys) }
            for (w in keys) consider(w, w)

            scored.sortWith(compareBy<Pair<String, Int>> { it.second }.thenByDescending { usage(it.first) }.thenBy { it.first })
            for ((w, _) in scored) add(w)
        }

        // Keep the person's capitalisation habit.
        val capitalise = prefix.isNotEmpty() && prefix.first().isUpperCase()
        return if (capitalise) out.map { w -> w.replaceFirstChar { c -> c.uppercase(az) } } else out
    }

    private fun lowerBound(array: Array<Entry>, key: String): Int {
        var lo = 0
        var hi = array.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (array[mid].lower < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun usage(word: String): Int = synchronized(lock) { learnedWords[word.lowercase(az)] ?: 0 }

    private fun nextWords(previous: String, arabicMode: Boolean, limit: Int): List<String> {
        val prev = previous.lowercase(az)
        val scores = HashMap<String, Int>()
        val prefix = BIGRAM_PREFIX + prev + BIGRAM_SEPARATOR
        synchronized(lock) {
            for ((key, count) in learnedBigrams) {
                if (key.startsWith(prefix)) {
                    val next = key.substring(prefix.length)
                    scores[next] = (scores[next] ?: 0) + count + 100
                }
            }
        }
        for ((first, second) in SEED_BIGRAMS) {
            val a = if (arabicMode) toArabic(first) else first
            if (a.lowercase(az) == prev) {
                val next = if (arabicMode) toArabic(second) else second
                scores[next] = (scores[next] ?: 0) + 1
            }
        }
        return scores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key }
    }

    // ---------- learning ----------

    fun learn(word: String, previousWord: String? = null) {
        if (word.length < 2 || !word.all { it.isLetter() }) return
        val key = word.lowercase(az)
        synchronized(lock) {
            if (!learnedWords.containsKey(key)) makeRoom(learnedWords, MAX_LEARNED_WORDS)
            val count = (learnedWords[key] ?: 0) + 1
            learnedWords[key] = count
            val updates = HashMap<String, Int>()
            updates[key] = count

            if (!previousWord.isNullOrBlank() && previousWord.all { it.isLetter() }) {
                val bigram = BIGRAM_PREFIX + previousWord.lowercase(az) + BIGRAM_SEPARATOR + key
                if (!learnedBigrams.containsKey(bigram)) makeRoom(learnedBigrams, MAX_BIGRAMS)
                val bigramCount = (learnedBigrams[bigram] ?: 0) + 1
                learnedBigrams[bigram] = bigramCount
                updates[bigram] = bigramCount
            }
            store.putAll(updates)
        }
    }

    fun clearLearned() {
        synchronized(lock) {
            learnedWords.clear()
            learnedBigrams.clear()
            store.clear()
        }
    }

    internal fun learnedWordCount(): Int = synchronized(lock) { learnedWords.size }
    internal fun learnedBigramCount(): Int = synchronized(lock) { learnedBigrams.size }

    /** Evicts the least-used ~5% when [map] has reached [cap]. */
    private fun makeRoom(map: HashMap<String, Int>, cap: Int) {
        if (map.size < cap) return
        evictLowest(map, maxOf(1, cap / 20))
    }

    private fun trimTo(map: HashMap<String, Int>, cap: Int) {
        if (map.size > cap) evictLowest(map, map.size - cap)
    }

    private fun evictLowest(map: HashMap<String, Int>, count: Int) {
        val victims = map.entries
            .sortedWith(compareBy<Map.Entry<String, Int>>({ it.value }, { it.key }))
            .take(count)
            .map { it.key }
        for (key in victims) map.remove(key)
        store.remove(victims)
    }

    companion object {
        private const val MAX_LEARNED_WORDS = 5000
        private const val MAX_BIGRAMS = 10000
        private const val BIGRAM_PREFIX = "@bi@"
        private const val BIGRAM_SEPARATOR = "|"

        // Small, deliberately conservative seed model. A real frequency corpus should be
        // linguistically reviewed before being shipped as a production language model.
        private val SEED_BIGRAMS = listOf(
            "salam" to "necəsən",
            "salam" to "dost",
            "çox" to "sağ",
            "sağ" to "ol",
            "mən" to "səni",
            "səni" to "sevirəm",
            "ana" to "və",
            "Azərbaycan" to "türkcəsi"
        )

        @Volatile
        private var instance: Suggester? = null

        /** Process-wide instance shared by the keyboard service and the settings screen. */
        fun get(context: Context): Suggester {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val app = context.applicationContext ?: context
                val created = Suggester(PrefsLearnedStore(app))
                instance = created
                // Dictionary loading never blocks the keyboard from appearing.
                Thread({ created.setWords(readAssetWords(app)) }, "turkan-dictionary").start()
                return created
            }
        }

        private fun readAssetWords(context: Context): List<String> = try {
            context.assets.open("words_az.txt").bufferedReader(Charsets.UTF_8).useLines { it.toList() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
