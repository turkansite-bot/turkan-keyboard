package com.turkanyazi.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {
    @Test fun latinArabicRoundTripProjectAlphabet() {
        // Includes both dotted i and dotless ı; they must remain distinguishable.
        val alphabet = "abcçdeəfgğhxıijklmnoöpqrsştuüvyz"
        assertEquals(
            alphabet,
            ScriptConverter.arabicToLatin(ScriptConverter.latinToArabic(alphabet))
        )
    }

    @Test fun dottedAndDotlessIStayDistinct() {
        assertEquals("i", ScriptConverter.arabicToLatin(ScriptConverter.latinToArabic("i")))
        assertEquals("ı", ScriptConverter.arabicToLatin(ScriptConverter.latinToArabic("ı")))
        assertTrue(ScriptConverter.latinToArabic("i") != ScriptConverter.latinToArabic("ı"))
    }

    @Test fun latinArabicRoundTripCommonWords() {
        for (w in listOf("salam", "qarda\u015F", "\u00E7ay", "\u0131\u015F\u0131q", "i\u015F\u0131q")) {
            assertEquals(w, ScriptConverter.arabicToLatin(ScriptConverter.latinToArabic(w)))
        }
    }

    @Test fun smartConvertPicksDirection() {
        assertTrue(ScriptConverter.containsArabic(ScriptConverter.convertSmart("salam")))
        assertFalse(ScriptConverter.containsArabic(ScriptConverter.convertSmart("\u0633\u0627\u0644\u0627\u0645")))
    }

    @Test fun orkhonHarmony() {
        assertTrue(OrkhonConverter.isBack("qap\u0131"))
        assertFalse(OrkhonConverter.isBack("g\u00FCl"))
    }

    @Test fun orkhonOutputStaysInBlock() {
        val out = OrkhonConverter.convert("T\u0259briz Ali")
        assertTrue(out.contains(":"))
        var i = 0
        while (i < out.length) {
            val c = out.codePointAt(i)
            if (c != ':'.code) assertTrue(c in 0x10C00..0x10C48)
            i += Character.charCount(c)
        }
    }
}
