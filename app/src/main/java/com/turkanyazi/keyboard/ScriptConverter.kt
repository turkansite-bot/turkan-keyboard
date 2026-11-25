package com.turkanyazi.keyboard

import java.text.Normalizer

/**
 * Latin <-> Arabic-script converter for Azerbaijani Turkic.
 *
 * Project transliteration profile v0.3. The mapping tables live in [ScriptTables], which is
 * generated from tools/gen_script_tables.py (the single source shared with the web version).
 *
 * Latin -> Arabic script:
 *  - one Latin letter -> one Arabic-script character (reversible for the Azerbaijani alphabet);
 *    i -> U+06CC and dotless ı -> U+0649 so the two letters never collapse;
 *  - input is normalised first (Unicode NFC, so "s" + combining cedilla becomes "ş"; look-alikes
 *    such as ș, ǝ and ä are folded to ş and ə); Turkic dotted/dotless I is lower-cased
 *    independently of the device locale;
 *  - digits become Persian digits (optional); , ; ? % become ، ؛ ؟ ٪, and a comma between two
 *    digits becomes the Arabic decimal separator ٫.
 *
 * Arabic script -> Latin:
 *  - all letters of the profile plus common variants and Arabic/Persian loan letters;
 *  - Arabic-Indic and Persian digits become ASCII digits; Arabic punctuation becomes Latin;
 *  - harakat, tatweel, Quranic marks and ZWNJ/ZWJ are removed; presentation forms (PDF copy-paste)
 *    are folded with NFKC first.
 *
 * Loan letters (ع ء ئ ة ث ح ذ ص ض ط ظ) and w are one-way. Spelling conventions of South
 * Azerbaijani vary; have native writers review the profile before public release.
 */
object ScriptConverter {
    private const val PERSIAN_ZERO = '\u06F0'

    fun latinToArabic(text: String, digits: Boolean = true): String {
        val s = Normalizer.normalize(text, Normalizer.Form.NFC)
        val sb = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            val folded = ScriptTables.latinFold[c] ?: c
            val lower = when (folded) {
                '\u0049' -> '\u0131' // I -> ı
                '\u0130' -> '\u0069' // İ -> i
                else -> folded.lowercaseChar()
            }
            val letter = ScriptTables.latinToArabic[lower]
            when {
                letter != null -> sb.append(letter)
                digits && c in '0'..'9' -> sb.append(PERSIAN_ZERO + (c - '0'))
                c == ',' -> sb.append(if (betweenDigits(s, i)) '\u066B' else '\u060C')
                else -> sb.append(ScriptTables.punctToArabic[c] ?: c.toString())
            }
        }
        return sb.toString()
    }

    private fun betweenDigits(s: String, i: Int): Boolean =
        i > 0 && i < s.length - 1 && s[i - 1] in '0'..'9' && s[i + 1] in '0'..'9'

    fun arabicToLatin(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) appendLatin(ch, sb)
        return sb.toString()
    }

    private fun appendLatin(ch: Char, sb: StringBuilder) {
        val mapped = ScriptTables.arabicToLatin[ch]
        when {
            mapped != null -> sb.append(mapped)
            ScriptTables.dropRanges.any { ch in it } -> Unit
            ch in '\u0660'..'\u0669' -> sb.append('0' + (ch - '\u0660'))
            ch in PERSIAN_ZERO..'\u06F9' -> sb.append('0' + (ch - PERSIAN_ZERO))
            ch in '\uFB50'..'\uFDFF' || ch in '\uFE70'..'\uFEFF' -> {
                // Presentation forms (e.g. from PDFs): fold to base letters, then convert those.
                val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFKC)
                if (base == ch.toString()) sb.append(ch) else for (b in base) appendLatin(b, sb)
            }
            else -> sb.append(ch)
        }
    }

    /** True for a character that is a *letter* of the Arabic script (digits and punctuation do not count). */
    fun isArabicScript(c: Char): Boolean =
        c.isLetter() && Character.UnicodeScript.of(c.code) == Character.UnicodeScript.ARABIC

    fun containsArabic(s: String): Boolean = s.any { isArabicScript(it) }

    /** Picks the direction from the text itself: Arabic-script text -> Latin, anything else -> Arabic. */
    fun convertSmart(text: String): String =
        if (containsArabic(text)) arabicToLatin(text) else latinToArabic(text)
}
