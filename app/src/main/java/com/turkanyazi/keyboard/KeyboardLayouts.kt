package com.turkanyazi.keyboard

/** Key layouts as plain data, so they can be unit-tested without an Android device. */
object KeyboardLayouts {
    /** Splits [s] into one string per Unicode code point. */
    fun glyphs(s: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        while (i < s.length) {
            val n = Character.charCount(s.codePointAt(i))
            out.add(s.substring(i, i + n))
            i += n
        }
        return out
    }

    private fun fromCodePoints(points: List<Int>): List<String> =
        points.map { String(Character.toChars(it)) }

    val digits: List<String> = glyphs("1234567890")

    /** Persian digits, shown on the Arabic layout (the converter produces these). */
    val arabicDigits: List<String> = glyphs("\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6\u06F7\u06F8\u06F9\u06F0")

    val latinRows: List<List<String>> = listOf(
        "q\u00FCertyuiop\u00F6\u011F",
        "asdfghjkl\u0131\u0259",
        "zxcvbnm\u00E7\u015F"
    ).map { glyphs(it) }

    val arabicRows: List<List<String>> = listOf(
        "\u0636\u0635\u062B\u0642\u0641\u063A\u0639\u0647\u062E\u062D\u062C",
        "\u0634\u0633\u06CC\u0628\u0644\u0627\u062A\u0646\u0645\u06A9\u06AF",
        "\u0638\u0637\u0632\u0631\u0630\u062F\u067E\u0648\u0686\u0698",
        // includes U+0649 (dotless yeh) which the converter uses for the letter "ı"
        "\u06D5\u06C7\u06C6\u06CB\u0624\u0626\u064A\u0649\u0621\u06BE\u0622"
    ).map { glyphs(it) }

    val orkhonCodes: List<Int> = listOf(
        0x10C00, 0x10C03, 0x10C06, 0x10C07, 0x10C09, 0x10C0B, 0x10C0D, 0x10C0F, 0x10C11, 0x10C13,
        0x10C14, 0x10C16, 0x10C18, 0x10C1A, 0x10C1C, 0x10C1E, 0x10C20, 0x10C21, 0x10C22, 0x10C23,
        0x10C24, 0x10C26, 0x10C28, 0x10C2A, 0x10C2D, 0x10C2F, 0x10C30, 0x10C31, 0x10C32, 0x10C34,
        0x10C36, 0x10C38, 0x10C3A, 0x10C3C, 0x10C3D, 0x10C3E, 0x10C3F, 0x10C41, 0x10C43, 0x10C45,
        0x10C47, 0x10C48
    )
    val orkhonRows: List<List<String>> = orkhonCodes.chunked(11).map { fromCodePoints(it) }

    /** Punctuation and symbols shared by every language layer. */
    val symbolRows: List<List<String>> = listOf(
        "@#\$_&-+()/",
        "*\"':;!?%=<>",
        "[]{}\\|~^`\u20AC\u00A3",
        // Arabic comma, semicolon, question mark, guillemets and ellipsis
        "\u060C\u061B\u061F\u00AB\u00BB\u2026"
    ).map { glyphs(it) }

    /** A small, single-code-point emoji set (no variation selectors or ZWJ sequences). */
    val emojiRows: List<List<String>> = listOf(
        listOf(0x1F600, 0x1F603, 0x1F604, 0x1F601, 0x1F606, 0x1F605, 0x1F602, 0x1F642),
        listOf(0x1F609, 0x1F60A, 0x1F60D, 0x1F618, 0x1F60E, 0x1F914, 0x1F622, 0x1F62D),
        listOf(0x1F621, 0x1F631, 0x1F44D, 0x1F44E, 0x1F44F, 0x1F64F, 0x1F4AA, 0x1F91D),
        listOf(0x1F499, 0x1F49A, 0x1F49B, 0x1F9E1, 0x1F525, 0x1F389, 0x1F31F, 0x1F339)
    ).map { fromCodePoints(it) }
}
