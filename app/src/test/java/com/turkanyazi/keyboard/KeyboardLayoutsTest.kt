package com.turkanyazi.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutsTest {
    private val alphabet = "abcçdeəfgğhxıijklmnoöpqrsştuüvyz"

    @Test fun everyConverterOutputLetterIsTypeableOnTheArabicKeyboard() {
        val keys = KeyboardLayouts.arabicRows.flatten().toSet()
        for (c in alphabet) {
            val arabic = ScriptConverter.latinToArabic(c.toString())
            assertTrue("Arabic keyboard has no key for '$c' -> $arabic", arabic in keys)
        }
    }

    @Test fun everyLatinLetterIsTypeableOnTheLatinKeyboard() {
        val keys = KeyboardLayouts.latinRows.flatten().toSet()
        for (c in alphabet) assertTrue("Latin keyboard has no key for '$c'", c.toString() in keys)
    }

    @Test fun symbolLayerHasEverydayPunctuation() {
        val keys = KeyboardLayouts.symbolRows.flatten().toSet()
        for (s in listOf("@", "-", "/", ":", "!", "?", "\"", "'", "\u061F", "\u060C")) {
            assertTrue("Symbol layer is missing '$s'", s in keys)
        }
    }

    @Test fun rowsFitOnAPhoneScreen() {
        for (row in KeyboardLayouts.latinRows + KeyboardLayouts.arabicRows + KeyboardLayouts.orkhonRows + KeyboardLayouts.symbolRows) {
            assertTrue("Row too long: $row", row.size <= 11)
        }
        for (row in KeyboardLayouts.emojiRows) assertTrue(row.size <= 8)
    }

    @Test fun orkhonKeysAreUniqueAndInsideTheBlock() {
        val codes = KeyboardLayouts.orkhonCodes
        assertEquals(codes.size, codes.toSet().size)
        for (c in codes) assertTrue(c in 0x10C00..0x10C48)
    }

    @Test fun emojiKeysAreSingleCodePoints() {
        for (g in KeyboardLayouts.emojiRows.flatten()) assertEquals(1, g.codePointCount(0, g.length))
    }

    @Test fun glyphsKeepsSurrogatePairsTogether() {
        assertEquals(2, KeyboardLayouts.glyphs("a\uD83D\uDE00").size)
    }
}
