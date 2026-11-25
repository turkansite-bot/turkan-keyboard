package com.turkanyazi.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrkhonConverterTest {
    @Test fun harmonyUsesFirstVowel() {
        assertTrue(OrkhonConverter.isBack("qapı"))
        assertTrue(OrkhonConverter.isBack("quş"))
        assertTrue(!OrkhonConverter.isBack("gül"))
        assertTrue(!OrkhonConverter.isBack("kəpənək"))
    }

    @Test fun mapsPairedConsonantsByHarmony() {
        val backB = OrkhonConverter.convert("ba")
        val frontB = OrkhonConverter.convert("bə")
        assertEquals(0x10C09, backB.codePointAt(0))
        assertEquals(0x10C0B, frontB.codePointAt(0))
    }

    @Test fun mapsNgSequenceToEngSign() {
        val converted = OrkhonConverter.convert("rəng")
        assertTrue(converted.codePoints().anyMatch { it == 0x10C2D })
    }

    @Test fun preservesPunctuationAndSeparatesWords() {
        assertEquals(":", OrkhonConverter.convert(" "))
        assertEquals("!", OrkhonConverter.convert("!"))
        assertTrue(OrkhonConverter.convert("Salam, dünya!").contains(","))
        assertTrue(OrkhonConverter.convert("Salam, dünya!").endsWith("!"))
    }

    @Test fun outputContainsOnlyAssignedOrkhonRangeOrPunctuation() {
        val output = OrkhonConverter.convert("Təbriz, Azərbaycan! 123")
        var offset = 0
        while (offset < output.length) {
            val codePoint = output.codePointAt(offset)
            val isOrkhon = codePoint in 0x10C00..0x10C48
            val isPreserved = !Character.isLetter(codePoint) && codePoint != ':'.code
            assertTrue("Unexpected code point U+${codePoint.toString(16)}", isOrkhon || isPreserved)
            offset += Character.charCount(codePoint)
        }
    }

    @Test fun velarSignFollowsHarmonyAndNearestVowel() {
        fun first(text: String) = OrkhonConverter.convert(text).codePointAt(0)
        assertEquals(0x10C34, first("kar"))   // back word: k -> AQ
        assertEquals(0x10C1A, first("kitab")) // front word: k -> AEK
        assertEquals(0x10C1C, first("k\u00FCt")) // front, rounded: OEK
        assertEquals(0x10C36, first("q\u0131z")) // q before dotless i: IQ
        assertEquals(0x10C38, first("qol"))   // q before o: OQ
    }

    @Test fun customWordSeparatorIsUsed() {
        assertEquals("\u205A", OrkhonConverter.convert(" ", "\u205A"))
        assertEquals(" ", OrkhonConverter.convert(" ", " "))
    }

    @Test fun containsOrkhonDetectsBlock() {
        assertTrue(OrkhonConverter.containsOrkhon(OrkhonConverter.convert("salam")))
        assertFalse(OrkhonConverter.containsOrkhon("salam"))
    }

    @Test fun reverseConversionRestoresSimpleBackWords() {
        assertEquals("salam", OrkhonConverter.toLatin(OrkhonConverter.convert("salam")))
        assertEquals("salam qap\u0131", OrkhonConverter.toLatin(OrkhonConverter.convert("salam qap\u0131")))
    }

    @Test fun reverseConversionLeavesOtherTextAlone() {
        assertEquals("abc, 123!", OrkhonConverter.toLatin("abc, 123!"))
    }
}
