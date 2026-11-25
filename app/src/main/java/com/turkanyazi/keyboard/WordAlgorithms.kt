package com.turkanyazi.keyboard

/** Pure Kotlin helpers so the ranking logic can be tested without an Android device. */
object WordAlgorithms {
    /** Optimal-string-alignment distance: Levenshtein plus adjacent transpositions. */
    fun editDistance(left: String, right: String): Int {
        val a = left.codePoints().toArray()
        val b = right.codePoints().toArray()
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size

        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in 0..a.size) dp[i][0] = i
        for (j in 0..b.size) dp[0][j] = j

        for (i in 1..a.size) {
            for (j in 1..b.size) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = minOf(best, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = best
            }
        }
        return dp[a.size][b.size]
    }

    /**
     * Edit distance with an early exit: returns `max + 1` as soon as the strings
     * cannot be within [max] edits (checked by length first, which is O(1)).
     */
    fun boundedEditDistance(left: String, right: String, max: Int): Int {
        val lengthGap = Math.abs(left.codePointCount(0, left.length) - right.codePointCount(0, right.length))
        if (lengthGap > max) return max + 1
        val d = editDistance(left, right)
        return if (d > max) max + 1 else d
    }
}
