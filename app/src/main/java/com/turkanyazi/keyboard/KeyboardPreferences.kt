package com.turkanyazi.keyboard

import android.content.Context

/** Shared, on-device preferences used by the setup screen and the IME service. */
object KeyboardPreferences {
    private const val FILE = "turkan_keyboard_settings"
    private const val KEY_THEME = "theme"
    private const val KEY_ACCENT = "accent"
    private const val KEY_KEY_SIZE = "key_size"
    private const val KEY_HEIGHT = "keyboard_height"
    private const val KEY_SUGGESTIONS = "suggestions"
    private const val KEY_DEFAULT_MODE = "default_mode"
    private const val KEY_ORKHON_FONT = "orkhon_font"
    private const val KEY_UI_SCRIPT = "ui_script"

    fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun dark(context: Context) = prefs(context).getBoolean(KEY_THEME, true)
    fun accent(context: Context) = prefs(context).getString(KEY_ACCENT, "blue") ?: "blue"
    fun keySize(context: Context) = prefs(context).getInt(KEY_KEY_SIZE, 40).coerceIn(34, 52)
    fun heightPercent(context: Context) = prefs(context).getInt(KEY_HEIGHT, 100).coerceIn(80, 120)
    fun heightScale(context: Context) = heightPercent(context) / 100f
    fun suggestions(context: Context) = prefs(context).getBoolean(KEY_SUGGESTIONS, true)
    fun defaultMode(context: Context) = prefs(context).getString(KEY_DEFAULT_MODE, "LATIN") ?: "LATIN"
    fun orkhonFont(context: Context) = prefs(context).getBoolean(KEY_ORKHON_FONT, true)

    /** Script of the setup-screen text: "ARABIC" (default) or "LATIN". */
    fun uiArabic(context: Context) = (prefs(context).getString(KEY_UI_SCRIPT, "ARABIC") ?: "ARABIC") == "ARABIC"
    fun setUiArabic(context: Context, arabic: Boolean) {
        prefs(context).edit().putString(KEY_UI_SCRIPT, if (arabic) "ARABIC" else "LATIN").apply()
    }

    /** Changes whenever any setting changes; lets the IME rebuild its view only when needed. */
    fun signature(context: Context): String =
        prefs(context).all.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }

    fun save(context: Context, dark: Boolean, accent: String, keySize: Int, height: Int,
             suggestions: Boolean, defaultMode: String, orkhonFont: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_THEME, dark)
            .putString(KEY_ACCENT, accent)
            .putInt(KEY_KEY_SIZE, keySize.coerceIn(34, 52))
            .putInt(KEY_HEIGHT, height.coerceIn(80, 120))
            .putBoolean(KEY_SUGGESTIONS, suggestions)
            .putString(KEY_DEFAULT_MODE, defaultMode)
            .putBoolean(KEY_ORKHON_FONT, orkhonFont)
            .apply()
    }
}
