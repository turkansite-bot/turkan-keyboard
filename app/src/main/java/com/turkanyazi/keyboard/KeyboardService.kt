package com.turkanyazi.keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import java.util.Locale

class KeyboardService : InputMethodService() {

    private enum class Mode { LATIN, ARABIC, ORKHON, SYMBOLS, EMOJI }

    private val letterModes = listOf(Mode.LATIN, Mode.ARABIC, Mode.ORKHON)

    private val az = Locale.forLanguageTag("az")
    private val ui = Handler(Looper.getMainLooper())

    private var mode = Mode.LATIN
    private var letterMode = Mode.LATIN
    private var shift = false
    private var privateField = false
    private var keyBg = "#3A3D43"
    private var specialBg = "#2B2D32"
    private var accentBg = "#3D6DF2"
    private var keyHeight = 40
    private var digitHeight = 32
    private var suggestionHeight = 36
    private var suggestionsEnabled = true
    private var lightTheme = false
    private var appliedSignature = ""

    private lateinit var root: LinearLayout
    private lateinit var suggestionRow: LinearLayout
    private lateinit var keysContainer: LinearLayout
    private lateinit var suggester: Suggester
    private val suggestionViews = ArrayList<Button>()
    private var orkhonTypeface: Typeface? = null

    private val refreshSuggestions = Runnable {
        if (::suggestionRow.isInitialized) updateSuggestions()
    }

    // ---------- lifecycle ----------

    override fun onCreate() {
        super.onCreate()
        suggester = Suggester.get(this)
        orkhonTypeface = try {
            Typeface.createFromAsset(assets, "fonts/NotoSansOldTurkic-Regular.ttf")
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateInputView(): View {
        applyPreferences()
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_LTR
        root.setBackgroundColor(Color.parseColor(if (lightTheme) "#E8EAF0" else "#1B1C20"))
        root.setPadding(dp(3), dp(4), dp(3), dp(6))

        suggestionRow = newLine()
        keysContainer = LinearLayout(this)
        keysContainer.orientation = LinearLayout.VERTICAL
        root.addView(suggestionRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(keysContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        buildSuggestionRow()
        renderKeys()
        updateSuggestions()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // The input view is normally created once per service lifetime, so settings
        // changed in the app are picked up here, rebuilding only when something changed.
        if (::root.isInitialized && KeyboardPreferences.signature(this) != appliedSignature) {
            setInputView(onCreateInputView())
        } else if (::suggestionRow.isInitialized) {
            updateSuggestions()
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val t = attribute?.inputType ?: 0
        val cls = t and InputType.TYPE_MASK_CLASS
        val variation = t and InputType.TYPE_MASK_VARIATION
        val textPassword = cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        val numberPassword = cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val noLearning = ((attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        privateField = cls == InputType.TYPE_NULL || textPassword || numberPassword || noLearning
        if (::suggestionRow.isInitialized) updateSuggestions()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // The cursor moved or the field changed outside the keyboard (tap, paste, app clearing it).
        ui.removeCallbacks(refreshSuggestions)
        ui.postDelayed(refreshSuggestions, 30)
    }

    override fun onFinishInput() {
        ui.removeCallbacksAndMessages(null)
        super.onFinishInput()
    }

    // ---------- building the UI ----------

    private fun applyPreferences() {
        appliedSignature = KeyboardPreferences.signature(this)
        val dark = KeyboardPreferences.dark(this)
        lightTheme = !dark
        keyBg = if (dark) "#3A3D43" else "#FFFFFF"
        specialBg = if (dark) "#2B2D32" else "#D4D8E0"
        accentBg = when (KeyboardPreferences.accent(this)) {
            "green" -> "#21865B"
            "purple" -> "#7952B3"
            "orange" -> "#D97724"
            "pink" -> "#C64F83"
            else -> "#3D6DF2"
        }
        val scale = KeyboardPreferences.heightScale(this)
        keyHeight = (KeyboardPreferences.keySize(this) * scale).toInt()
        digitHeight = (keyHeight * 0.8f).toInt()
        suggestionHeight = (keyHeight * 0.9f).toInt()
        suggestionsEnabled = KeyboardPreferences.suggestions(this)
        mode = when (KeyboardPreferences.defaultMode(this)) {
            "ARABIC" -> Mode.ARABIC
            "ORKHON" -> Mode.ORKHON
            else -> Mode.LATIN
        }
        letterMode = mode
        shift = false
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun newLine(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        return l
    }

    private fun LinearLayout.addKey(v: View, weight: Float = 1f, heightDp: Int = keyHeight) {
        val lp = LinearLayout.LayoutParams(0, dp(heightDp), weight)
        lp.setMargins(dp(2), dp(2), dp(2), dp(2))
        addView(v, lp)
    }

    private fun keyButton(
        label: String,
        sp: Float = 18f,
        bg: String = keyBg,
        desc: String? = null,
        onClick: (Button) -> Unit
    ): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        if (desc != null) b.contentDescription = desc
        b.setTextColor(if (lightTheme && bg != accentBg) Color.parseColor("#20232A") else Color.WHITE)
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        b.setPadding(0, 0, 0, 0)
        b.minWidth = 0
        b.minimumWidth = 0
        b.minHeight = 0
        b.minimumHeight = 0
        b.stateListAnimator = null
        val shape = GradientDrawable()
        shape.cornerRadius = dp(6).toFloat()
        shape.setColor(Color.parseColor(bg))
        val ripple = if (lightTheme) Color.argb(60, 0, 0, 0) else Color.argb(90, 255, 255, 255)
        b.background = RippleDrawable(ColorStateList.valueOf(ripple), shape, null)
        b.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick(b)
        }
        return b
    }

    /**
     * A key that fires once on press and then repeats while held (used for backspace).
     * Touch events are consumed here, so the click listener only runs for accessibility
     * services (TalkBack "activate"), which is exactly what we want it to do.
     */
    private fun repeatKey(label: String, desc: String, action: () -> Unit): Button {
        val b = keyButton(label, 18f, specialBg, desc) { action() }
        val runner = object : Runnable {
            override fun run() {
                action()
                ui.postDelayed(this, 55)
            }
        }
        b.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    action()
                    ui.postDelayed(runner, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    ui.removeCallbacks(runner)
                }
            }
            true
        }
        return b
    }

    private fun globeKey(): Button {
        val b = keyButton("\uD83C\uDF10", 16f, specialBg, "Switch keyboard") { switchKeyboard() }
        b.setOnLongClickListener {
            showKeyboardPicker()
            true
        }
        return b
    }

    private fun switchKeyboard() {
        val switched = try {
            switchToNextInputMethod(false)
        } catch (e: Exception) {
            false
        }
        if (!switched) showKeyboardPicker()
    }

    private fun showKeyboardPicker() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showInputMethodPicker()
    }

    private fun buildSuggestionRow() {
        suggestionRow.removeAllViews()
        suggestionViews.clear()
        // The conversion buttons stay available even when word suggestions are switched off.
        val visibility = if (suggestionsEnabled) View.VISIBLE else View.INVISIBLE
        suggestionRow.addKey(keyButton("\u21C4", 18f, accentBg, "Convert script") { convertSelectionOrWord(false) }, 1f, suggestionHeight)
        repeat(3) {
            val b = keyButton("", 15f, specialBg) { btn -> pickSuggestion(btn.text.toString()) }
            b.visibility = visibility
            suggestionViews.add(b)
            suggestionRow.addKey(b, 2f, suggestionHeight)
        }
        suggestionRow.addKey(keyButton("Ork", 13f, accentBg, "Convert to Old Turkic") { convertSelectionOrWord(true) }, 1f, suggestionHeight)
    }

    private fun letterLabel(m: Mode): String = when (m) {
        Mode.LATIN -> "T\u00FCrkc\u0259"
        Mode.ARABIC -> "\u062A\u0648\u0631\u06A9\u062C\u0647"
        Mode.ORKHON -> "Ork"
        else -> "ABC"
    }

    private fun setMode(m: Mode) {
        mode = m
        if (m in letterModes) letterMode = m
        shift = false
        renderKeys()
        updateSuggestions()
    }

    private fun renderKeys() {
        keysContainer.removeAllViews()
        val rows: List<List<String>> = when (mode) {
            Mode.LATIN -> KeyboardLayouts.latinRows
            Mode.ARABIC -> KeyboardLayouts.arabicRows
            Mode.ORKHON -> KeyboardLayouts.orkhonRows
            Mode.SYMBOLS -> KeyboardLayouts.symbolRows
            Mode.EMOJI -> KeyboardLayouts.emojiRows
        }
        if (mode != Mode.EMOJI) {
            val digitKeys = if (mode == Mode.ARABIC) KeyboardLayouts.arabicDigits else KeyboardLayouts.digits
            keysContainer.addView(buildKeyRow(digitKeys, true))
        }
        for (row in rows) keysContainer.addView(buildKeyRow(row, false))
        keysContainer.addView(bottomRow())
    }

    private fun buildKeyRow(row: List<String>, digitRow: Boolean): LinearLayout {
        val line = newLine()
        for (g in row) {
            val label = if (mode == Mode.LATIN && shift && !digitRow) g.uppercase(az) else g
            val large = !digitRow && (mode == Mode.ORKHON || mode == Mode.EMOJI)
            val b = keyButton(label, if (large) 22f else 18f, keyBg) { typeText(label) }
            if (mode == Mode.ORKHON && !digitRow && KeyboardPreferences.orkhonFont(this)) {
                orkhonTypeface?.let { b.typeface = it }
            }
            line.addKey(b, 1f, if (digitRow) digitHeight else keyHeight)
        }
        return line
    }

    private fun spaceKey(): Button = keyButton("T\u00DCRKAN", 12f, keyBg, "Space") { space() }

    private fun bottomRow(): LinearLayout {
        val line = newLine()
        when (mode) {
            Mode.SYMBOLS, Mode.EMOJI -> {
                line.addKey(keyButton(letterLabel(letterMode), 11f, specialBg, "Letters") { setMode(letterMode) }, 1.3f)
                if (mode == Mode.SYMBOLS) {
                    line.addKey(keyButton("\uD83D\uDE00", 16f, specialBg, "Emoji") { setMode(Mode.EMOJI) }, 1f)
                } else {
                    line.addKey(keyButton("?#", 14f, specialBg, "Symbols") { setMode(Mode.SYMBOLS) }, 1f)
                }
                line.addKey(globeKey(), 1f)
                line.addKey(spaceKey(), 3f)
            }
            else -> {
                val next = letterModes[(letterModes.indexOf(mode) + 1) % letterModes.size]
                line.addKey(keyButton(letterLabel(next), 11f, specialBg, "Switch layout") { setMode(next) }, 1.6f)
                line.addKey(keyButton("?#", 13f, specialBg, "Symbols") { setMode(Mode.SYMBOLS) }, 1f)
                line.addKey(globeKey(), 1f)
                if (mode == Mode.LATIN) {
                    line.addKey(keyButton("\u21E7", 18f, if (shift) accentBg else specialBg, "Shift") {
                        shift = !shift
                        renderKeys()
                    }, 1f)
                }
                val comma = when (mode) {
                    Mode.ARABIC -> "\u060C"
                    Mode.ORKHON -> ":"
                    else -> ","
                }
                line.addKey(keyButton(comma, 18f, specialBg) { typeText(comma) }, 1f)
                if (mode == Mode.ARABIC) {
                    line.addKey(keyButton("ZWNJ", 10f, specialBg, "Zero width non-joiner") { typeText("\u200C") }, 1.2f)
                }
                line.addKey(spaceKey(), if (mode == Mode.ARABIC) 1.8f else 2.4f)
            }
        }
        line.addKey(keyButton(".", 18f, specialBg) { typeText(".") }, 1f)
        line.addKey(repeatKey("\u232B", "Backspace") { backspace() }, 1.3f)
        line.addKey(keyButton("\u23CE", 18f, accentBg, "Enter") { enter() }, 1.3f)
        return line
    }

    // ---------- typing ----------

    private fun typeText(t: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(t, 1)
        if (shift) {
            shift = false
            renderKeys()
        }
        updateSuggestions()
    }

    private fun space() {
        val ic = currentInputConnection ?: return
        if (!privateField) currentWordBefore()?.let { suggester.learn(it, previousWordBeforeCurrent()) }
        ic.commitText(" ", 1)
        updateSuggestions()
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) ic.commitText("", 1) else ic.deleteSurroundingTextInCodePoints(1, 0)
        updateSuggestions()
    }

    private fun enter() {
        val ic = currentInputConnection ?: return
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED && !noEnterAction) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
        updateSuggestions()
    }

    /** Trailing run of letters (code-point aware, so Old Turkic letters count too) and ZWNJ. */
    private fun trailingWord(s: String): String {
        var i = s.length
        while (i > 0) {
            val c = s.codePointBefore(i)
            if (!(Character.isLetter(c) || c == 0x200C)) break
            i -= Character.charCount(c)
        }
        return s.substring(i)
    }

    private fun currentWordBefore(): String? {
        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString() ?: return null
        val w = trailingWord(before)
        return if (w.isEmpty()) null else w
    }

    /** Finds the token immediately before the token currently being edited. */
    private fun previousWordBeforeCurrent(): String? {
        val before = currentInputConnection?.getTextBeforeCursor(120, 0)?.toString() ?: return null
        val current = trailingWord(before)
        val precedingText = before.dropLast(current.length).trimEnd()
        return trailingWord(precedingText).ifEmpty { null }
    }

    // ---------- suggestions and conversion ----------

    private fun updateSuggestions() {
        if (!::suggestionRow.isInitialized) return
        val enabled = suggestionsEnabled && !privateField && (mode == Mode.LATIN || mode == Mode.ARABIC)
        val word = if (enabled) currentWordBefore().orEmpty() else ""
        val previous = if (enabled) previousWordBeforeCurrent() else null
        val list = if (enabled) suggester.suggest(word, mode == Mode.ARABIC, previousWord = previous) else emptyList()
        for ((i, b) in suggestionViews.withIndex()) {
            val s = list.getOrNull(i)
            b.text = s ?: ""
            b.isEnabled = s != null
            b.alpha = if (s != null) 1f else 0.4f
        }
    }

    private fun pickSuggestion(s: String) {
        if (s.isEmpty()) return
        val ic = currentInputConnection ?: return
        val w = currentWordBefore() ?: ""
        val previous = previousWordBeforeCurrent()
        ic.deleteSurroundingTextInCodePoints(w.codePointCount(0, w.length), 0)
        ic.commitText("$s ", 1)
        if (!privateField) suggester.learn(s, previous)
        updateSuggestions()
    }

    /** "⇄": Old Turkic -> Latin, otherwise Latin <-> Arabic script. */
    private fun smartConvert(s: String): String =
        if (OrkhonConverter.containsOrkhon(s)) OrkhonConverter.toLatin(s) else ScriptConverter.convertSmart(s)

    /** Converts the selected text, or the word before the cursor if nothing is selected. */
    private fun convertSelectionOrWord(toOrkhon: Boolean) {
        val ic = currentInputConnection ?: return
        val convert: (String) -> String =
            if (toOrkhon) { s -> OrkhonConverter.convert(s) } else { s -> smartConvert(s) }
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) {
            ic.commitText(convert(sel.toString()), 1)
            return
        }
        val w = currentWordBefore() ?: return
        ic.deleteSurroundingText(w.length, 0)
        ic.commitText(convert(w), 1)
        updateSuggestions()
    }
}
