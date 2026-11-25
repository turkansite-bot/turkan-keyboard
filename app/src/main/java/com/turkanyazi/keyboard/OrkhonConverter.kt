package com.turkanyazi.keyboard

import java.util.Locale

/**
 * Experimental Azerbaijani Latin <-> Old Turkic Unicode transliteration.
 *
 * IMPORTANT: this is a readable modern transliteration aid, NOT a scholarly
 * transcription of Old Turkic. Historical spelling is not a one-to-one mapping
 * from modern Azerbaijani, vowels were often omitted, and several signs encode
 * syllabic/consonant distinctions rather than modern single letters.
 *
 * Paired consonant signs follow vowel harmony of the word (first vowel). The
 * velar signs (k/q/x/h) additionally follow the nearest vowel in the word, as in
 * the historical practice. Unsupported modern sounds are approximated and
 * documented in [letter]. The reverse direction ([toLatin]) is a lossy
 * best effort. A linguist should review this profile before it is presented as
 * historically authoritative.
 */
object OrkhonConverter {
    private val AZ = Locale.forLanguageTag("az")

    private const val BACK_VOWELS = "aıou"
    private const val FRONT_VOWELS = "əeiöü"

    /** Visible word separator used by default (a display choice, not a manuscript claim). */
    const val DEFAULT_WORD_SEPARATOR = ":"

    private const val BLOCK_START = 0x10C00
    private const val BLOCK_END = 0x10C4F

    private fun cp(codePoint: Int): String = String(Character.toChars(codePoint))

    private fun isVowel(c: Char) = c in BACK_VOWELS || c in FRONT_VOWELS

    /** Returns true when the first identifiable vowel in [word] is back. */
    fun isBack(word: String): Boolean {
        for (ch in word.lowercase(AZ)) {
            when (ch) {
                in BACK_VOWELS -> return true
                in FRONT_VOWELS -> return false
            }
        }
        // Deterministic fallback for vowelless strings.
        return true
    }

    /** True if [text] contains any character from the Old Turkic block. */
    fun containsOrkhon(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val c = text.codePointAt(i)
            if (c in BLOCK_START..BLOCK_END) return true
            i += Character.charCount(c)
        }
        return false
    }

    /**
     * Converts words while preserving punctuation. [wordSeparator] replaces each
     * space; pass " " to keep spaces, or e.g. "\u205A" for TWO DOT PUNCTUATION
     * (check that the font in use has a glyph for it).
     */
    fun convert(text: String, wordSeparator: String = DEFAULT_WORD_SEPARATOR): String {
        if (text.isEmpty()) return text
        val out = StringBuilder(text.length)
        val word = StringBuilder()

        fun flushWord() {
            if (word.isNotEmpty()) {
                out.append(convertWord(word.toString()))
                word.setLength(0)
            }
        }

        var i = 0
        while (i < text.length) {
            val c = text.codePointAt(i)
            val n = Character.charCount(c)
            if (Character.isLetter(c) && c !in BLOCK_START..BLOCK_END) {
                word.appendCodePoint(c)
            } else {
                flushWord()
                if (c == ' '.code) out.append(wordSeparator) else out.appendCodePoint(c)
            }
            i += n
        }
        flushWord()
        return out.toString()
    }

    private fun convertWord(original: String): String {
        val word = original.lowercase(AZ)
        val back = isBack(word)
        val out = StringBuilder()
        var i = 0

        while (i < word.length) {
            val current = word[i]
            val next = word.getOrNull(i + 1)

            // A modern "ng" sequence is approximated with the Orkhon ENG sign.
            if (current == 'n' && next == 'g') {
                out.append(cp(0x10C2D)) // OLD TURKIC LETTER ORKHON ENG
                i += 2
                continue
            }

            out.append(letter(current, back, word, i))
            i++
        }
        return out.toString()
    }

    /** Nearest vowel after [index], otherwise the nearest one before it. */
    private fun vowelNear(word: String, index: Int): Char? {
        for (j in index + 1 until word.length) if (isVowel(word[j])) return word[j]
        for (j in index - 1 downTo 0) if (isVowel(word[j])) return word[j]
        return null
    }

    private fun qSign(vowel: Char?): Int = when (vowel) {
        'o', 'u' -> 0x10C38 // ORKHON OQ
        'ı' -> 0x10C36 // ORKHON IQ
        else -> 0x10C34 // ORKHON AQ
    }

    private fun kSign(vowel: Char?): Int = when (vowel) {
        'ö', 'ü' -> 0x10C1C // ORKHON OEK
        else -> 0x10C1A // ORKHON AEK
    }

    private fun letter(c: Char, back: Boolean, word: String, index: Int): String {
        val codePoint = when (c) {
            // Vowel approximations: modern Azerbaijani vowel distinctions are
            // richer than the basic Orkhon vowel inventory.
            'a', 'ə', 'e' -> 0x10C00 // ORKHON A (ə/e are approximate)
            'ı', 'i' -> 0x10C03 // ORKHON I
            'o', 'u' -> 0x10C06 // ORKHON O
            'ö', 'ü' -> 0x10C07 // ORKHON OE

            'b', 'v' -> if (back) 0x10C09 else 0x10C0B // AB / AEB; v -> b approximation
            'd' -> if (back) 0x10C11 else 0x10C13 // AD / AED
            'g', 'ğ' -> if (back) 0x10C0D else 0x10C0F // AG / AEG
            'y', 'j' -> if (back) 0x10C16 else 0x10C18 // AY / AEY; j -> y approximation
            'l' -> if (back) 0x10C1E else 0x10C20 // AL / AEL
            'n' -> if (back) 0x10C23 else 0x10C24 // AN / AEN
            'r' -> if (back) 0x10C3A else 0x10C3C // AR / AER
            's' -> if (back) 0x10C3D else 0x10C3E // AS / AES
            't' -> if (back) 0x10C43 else 0x10C45 // AT / AET
            'ş' -> if (back) 0x10C3F else 0x10C41 // ASH / ESH
            'm' -> 0x10C22 // EM
            'z' -> 0x10C14 // EZ
            'ç', 'c' -> 0x10C32 // EC; c/ç are approximated by one sign
            'p', 'f' -> 0x10C2F // EP; f -> p approximation
            'q' -> qSign(vowelNear(word, index))
            // k/x/h: back-series (q) signs in back words, front-series (k) signs in front words.
            'k', 'x', 'h' -> if (back) qSign(vowelNear(word, index)) else kSign(vowelNear(word, index))
            else -> return String(Character.toChars(c.code))
        }
        return cp(codePoint)
    }

    // ---------- reverse direction (lossy) ----------

    private fun pair(back: String, front: String = back) = back to front

    /** Old Turkic code point -> (back-word reading, front-word reading). */
    private val LATIN_OF: Map<Int, Pair<String, String>> = mapOf(
        0x10C00 to pair("a", "ə"), 0x10C03 to pair("ı", "i"), 0x10C06 to pair("o"), 0x10C07 to pair("ö"),
        0x10C09 to pair("b"), 0x10C0B to pair("b"), 0x10C0D to pair("g"), 0x10C0F to pair("g"),
        0x10C11 to pair("d"), 0x10C13 to pair("d"), 0x10C14 to pair("z"), 0x10C16 to pair("y"),
        0x10C18 to pair("y"), 0x10C1A to pair("k"), 0x10C1C to pair("k"), 0x10C1E to pair("l"),
        0x10C20 to pair("l"), 0x10C21 to pair("lt"), 0x10C22 to pair("m"), 0x10C23 to pair("n"),
        0x10C24 to pair("n"), 0x10C26 to pair("nt"), 0x10C28 to pair("nc"), 0x10C2A to pair("ny"),
        0x10C2D to pair("ng"), 0x10C2F to pair("p"), 0x10C30 to pair("p"), 0x10C31 to pair("ç"),
        0x10C32 to pair("ç"), 0x10C34 to pair("q"), 0x10C36 to pair("q"), 0x10C38 to pair("q"),
        0x10C3A to pair("r"), 0x10C3C to pair("r"), 0x10C3D to pair("s"), 0x10C3E to pair("s"),
        0x10C3F to pair("ş"), 0x10C41 to pair("ş"), 0x10C43 to pair("t"), 0x10C45 to pair("t"),
        0x10C47 to pair("ot"), 0x10C48 to pair("baş")
    )

    /** Signs that only occur in front-harmony words (used to pick the front readings). */
    private val FRONT_SIGNS = setOf(
        0x10C07, 0x10C0B, 0x10C0F, 0x10C13, 0x10C18, 0x10C1A, 0x10C1C, 0x10C20, 0x10C24,
        0x10C3C, 0x10C3E, 0x10C41, 0x10C45
    )

    /**
     * Best-effort conversion of Old Turkic text back to Azerbaijani Latin. Lossy:
     * the forward mapping merges several letters, so e.g. "ü" comes back as "ö".
     * Word separators (":") become spaces.
     */
    fun toLatin(text: String): String {
        val out = StringBuilder(text.length)
        val run = ArrayList<Int>()

        fun flushRun() {
            if (run.isEmpty()) return
            val front = run.any { it in FRONT_SIGNS }
            for (c in run) {
                val reading = LATIN_OF.getValue(c)
                out.append(if (front) reading.second else reading.first)
            }
            run.clear()
        }

        var i = 0
        while (i < text.length) {
            val c = text.codePointAt(i)
            i += Character.charCount(c)
            if (LATIN_OF.containsKey(c)) {
                run.add(c)
            } else {
                flushRun()
                if (c == ':'.code) out.append(' ') else out.appendCodePoint(c)
            }
        }
        flushRun()
        return out.toString()
    }
}
