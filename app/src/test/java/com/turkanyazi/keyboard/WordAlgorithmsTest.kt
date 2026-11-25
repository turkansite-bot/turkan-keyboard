package com.turkanyazi.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class WordAlgorithmsTest {
    @Test fun identicalWordsHaveZeroDistance() {
        assertEquals(0, WordAlgorithms.editDistance("salam", "salam"))
    }

    @Test fun oneInsertionHasDistanceOne() {
        assertEquals(1, WordAlgorithms.editDistance("salam", "salaam"))
    }

    @Test fun oneSubstitutionHasDistanceOne() {
        assertEquals(1, WordAlgorithms.editDistance("kitab", "kitap"))
    }

    @Test fun adjacentTranspositionHasDistanceOne() {
        assertEquals(1, WordAlgorithms.editDistance("kitba", "kitab"))
    }

    @Test fun emptyStringsAreHandled() {
        assertEquals(4, WordAlgorithms.editDistance("", "dil1"))
        assertEquals(0, WordAlgorithms.editDistance("", ""))
    }

    @Test fun supplementaryUnicodeCharactersAreCountedAsCodePoints() {
        assertEquals(1, WordAlgorithms.editDistance(String(Character.toChars(0x10C00)), String(Character.toChars(0x10C01))))
    }

    @Test fun boundedDistanceStopsEarlyOnLengthGap() {
        assertEquals(2, WordAlgorithms.boundedEditDistance("a", "abcdef", 1))
    }

    @Test fun boundedDistanceReturnsExactValueWithinLimit() {
        assertEquals(1, WordAlgorithms.boundedEditDistance("kitba", "kitab", 2))
        assertEquals(0, WordAlgorithms.boundedEditDistance("salam", "salam", 1))
    }

    @Test fun boundedDistanceCapsWhenOverLimit() {
        assertEquals(2, WordAlgorithms.boundedEditDistance("abcd", "wxyz", 1))
    }
}
