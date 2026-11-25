package com.turkanyazi.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Expected values were produced by the web converter (same tables, same algorithm). */
class ScriptConverterTest {
    @Test fun latinToArabicCase1() = assertEquals("\u0633\u0627\u0644\u0627\u0645\u060C \u0646\u0647\u062C\u06D5\u0633\u06D5\u0646\u061F", ScriptConverter.latinToArabic("Salam\u002C nec\u0259s\u0259n\u003F"))
    @Test fun latinToArabicCase2() = assertEquals("\u06D5 \u0686 \u0634 \u063A \u0624 \u06C6 \u06CC \u0649 \u0649 \u06CC", ScriptConverter.latinToArabic("\u018F \u00C7 \u015E \u011E \u00D6 \u00DC \u0130 I \u0131 i"))
    @Test fun latinToArabicCase3() = assertEquals("\u06D5 \u0634 \u06D5 \u06D5 \u0634 \u06D5", ScriptConverter.latinToArabic("\u01DD \u0219 \u00E4 \u018E \u0218 \u00C4"))
    @Test fun latinToArabicCase4() = assertEquals("\u062A\u0647\u0644\u003A \u06F0\u06F1\u06F2\u06F3\u002D\u06F4\u06F5\u06F6\u060C \u06F1\u066B\u06F5 \u06CB\u06D5 \u06F5\u06F0\u066A\u061B \u0648\u06A9\u061F", ScriptConverter.latinToArabic("Tel\u003A 0123\u002D456\u002C 1\u002C5 v\u0259 50\u0025\u003B ok\u003F"))
    @Test fun latinToArabicCase5() = assertEquals("\u06CB\u06CC\u0641\u06CC \u06CB\u06CB\u06CB", ScriptConverter.latinToArabic("wifi WWW"))
    @Test fun latinToArabicCase6() = assertEquals("\u0634 \u0686 \u0624 \u06C6 \u063A \u0647\u06D5", ScriptConverter.latinToArabic("s\u0327 c\u0327 o\u0308 u\u0308 g\u0306 e\u0259"))
    @Test fun latinToArabicCase7() = assertEquals("\u0628\u0627\u06A9\u0649\u060C \u06F2\u06F0\u06F2\u06F6\u002D\u062C\u0649 \u06CC\u0644 \u0028\u06F1\u06F0\u066B\u06F5\u066A\u0029\u0021", ScriptConverter.latinToArabic("Bak\u0131\u002C 2026\u002Dc\u0131 il \u002810\u002C5\u0025\u0029\u0021"))
    @Test fun latinToArabicCase8() = assertEquals("\u0022\u0633\u0627\u0644\u0627\u0645\u0022 \u002D \u062F\u0648\u0633\u062A\u002E", ScriptConverter.latinToArabic("\u0022salam\u0022 \u002D dost\u002E"))
    @Test fun arabicToLatinCase1() = assertEquals("salam\u002C nec\u0259s\u0259n\u003F", ScriptConverter.arabicToLatin("\u0633\u0627\u0644\u0627\u0645\u060C \u0646\u0647\u062C\u06D5\u0633\u06D5\u0646\u061F"))
    @Test fun arabicToLatinCase2() = assertEquals("0123 456789", ScriptConverter.arabicToLatin("\u0660\u0661\u0662\u0663 \u06F4\u06F5\u06F6\u06F7\u06F8\u06F9"))
    @Test fun arabicToLatinCase3() = assertEquals("la", ScriptConverter.arabicToLatin("\uFEFB"))
    @Test fun arabicToLatinCase4() = assertEquals("ktab", ScriptConverter.arabicToLatin("\u0643\u0650\u062A\u064E\u0627\u0628\u064C"))
    @Test fun arabicToLatinCase5() = assertEquals("\u0259  t i a a", ScriptConverter.arabicToLatin("\u0639 \u0621 \u0629 \u0626 \u0622 \u0623"))
    @Test fun arabicToLatinCase6() = assertEquals("ng", ScriptConverter.arabicToLatin("\u06AD"))
    @Test fun arabicToLatinCase7() = assertEquals("1\u002C5 1\u002E000", ScriptConverter.arabicToLatin("\u06F1\u066B\u06F5 \u06F1\u066C\u06F0\u06F0\u06F0"))
    @Test fun arabicToLatinCase8() = assertEquals("n\u0259", ScriptConverter.arabicToLatin("\u0646\u200C\u06D5"))
    @Test fun arabicToLatinCase9() = assertEquals("s h z s z t z", ScriptConverter.arabicToLatin("\u062B \u062D \u0630 \u0635 \u0636 \u0637 \u0638"))
    @Test fun arabicToLatinCase10() = assertEquals("v k y", ScriptConverter.arabicToLatin("\u06A4 \u06AA \u06D2"))
    @Test fun arabicToLatinCase11() = assertEquals("slam salam 123", ScriptConverter.arabicToLatin("\u0633\u0644\u0627\u0645 salam \u06F1\u06F2\u06F3"))
    @Test fun digitsCanBeLeftAlone() = assertEquals("0123 1\u066B5", ScriptConverter.latinToArabic("0123 1,5", false))
    @Test fun smartPicksTheDirection() {
        assertEquals("\u0633\u0627\u0644\u0627\u0645 \u06F1\u06F2", ScriptConverter.convertSmart("salam 12"))
        assertEquals("slam 12", ScriptConverter.convertSmart("\u0633\u0644\u0627\u0645 12"))
    }
    @Test fun punctuationAndDigitsAloneAreNotArabicLetters() {
        assertFalse(ScriptConverter.containsArabic("\u06F1\u06F2 \u060C \u061F"))
        assertTrue(ScriptConverter.containsArabic("\u0633"))
    }

    @Test fun letterProfileRoundTripsIncludingPunctuation() {
        val text = "abc\u00E7de\u0259fg\u011Fhx\u0131ijklmno\u00F6pqrs\u015Ftu\u00FCvyz,?;%"
        assertEquals(text, ScriptConverter.arabicToLatin(ScriptConverter.latinToArabic(text)))
    }

    @Test fun everyLetterOfTheProfileHasAnArabicKeyOnTheKeyboard() {
        val keys = KeyboardLayouts.arabicRows.flatten().toSet()
        for ((_, arabic) in ScriptTables.latinToArabic) assertTrue(arabic in keys)
    }

    @Test fun persianDigitKeysConvertToAsciiDigits() {
        assertEquals("1234567890", ScriptConverter.arabicToLatin(KeyboardLayouts.arabicDigits.joinToString("")))
    }

    @Test fun everyArabicKeyConvertsToSomething() {
        for (key in KeyboardLayouts.arabicRows.flatten()) {
            val latin = ScriptConverter.arabicToLatin(key)
            assertTrue("No Latin value for $key", latin.isNotEmpty() && latin != key)
        }
    }
}
