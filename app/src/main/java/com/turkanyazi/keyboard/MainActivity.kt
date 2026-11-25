package com.turkanyazi.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Switch
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Setup screen (enable + pick the keyboard) and an offline Orkhon / Arabic name converter. */
class MainActivity : Activity() {

    /** True when the setup screen is shown in Arabic script (Türkcə / تورکجه switch at the top). */
    private var arUi = true
    private fun tr(latin: String, arabic: String): String = if (arUi) arabic else latin

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(text: String, sp: Float, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) t.setTypeface(t.typeface, Typeface.BOLD)
        t.setPadding(0, dp(8), 0, dp(4))
        return t
    }

    private fun action(text: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.setOnClickListener { onClick() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arUi = KeyboardPreferences.uiArabic(this)
        val orkhonFont: Typeface? = try {
            Typeface.createFromAsset(assets, "fonts/NotoSansOldTurkic-Regular.ttf")
        } catch (e: Exception) {
            null
        }

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = if (arUi) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        col.setPadding(dp(16), dp(16), dp(16), dp(16))

        col.addView(label("TÜRKAN Klaviatura", 26f, true))
        col.addView(action("Türkcə  ⇄  تورکجه") {
            KeyboardPreferences.setUiArabic(this, !arUi)
            recreate()
        })
        col.addView(label(tr("Azərbaycan türkcəsi klaviaturası: Latın, ərəb əlifbası və Orxon yazısı. İnternetsiz işləyir, məlumat göndərmir.", "آذربایجان تورکجه‌سی کیبوردو: لاتین، عرب الفباسی و اورخون یازیسی. اینترنتسیز ایشلییر، معلومات گؤندرمز."), 15f))
        col.addView(action(tr("1) Klaviaturanı aktiv et", "۱) کیبوردو فعال ائت")) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        col.addView(action(tr("2) Klaviaturanı seç", "۲) کیبوردو سئچ")) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        val test = EditText(this)
        test.hint = tr("Burada yazıb yoxla", "بورادا یازیب یوخلا")
        col.addView(test)

        col.addView(label(tr("Orxon yazısına çevirici", "اورخون یازیسینا چئویریجی"), 20f, true))
        val nameInput = EditText(this)
        nameInput.hint = tr("Latınla ad və ya cümlə yaz, məs. Təbriz", "لاتینله آد یا جومله یاز، مثلاً Təbriz")
        col.addView(nameInput)

        val outOrkhon = label("", 34f)
        if (orkhonFont != null) outOrkhon.typeface = orkhonFont
        val outArabic = label("", 24f)
        col.addView(outOrkhon)
        col.addView(outArabic)
        if (orkhonFont == null) {
            col.addView(label(tr("Orxon yazısı kvadrat kimi görünsə, telefonunuzda uyğun şrift yoxdur.", "اورخون یازیسی کوادرات کیمی گؤرونسه، تلفونونوزدا اویغون فونت یوخدور."), 12f))
        }

        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val t = s?.toString().orEmpty()
                outOrkhon.text = OrkhonConverter.convert(t)
                outArabic.text = ScriptConverter.latinToArabic(t)
            }
        })

        col.addView(action(tr("Paylaş", "پایلاش")) {
            val text = outOrkhon.text.toString() + "\n" + outArabic.text.toString()
            val send = Intent(Intent.ACTION_SEND)
            send.type = "text/plain"
            send.putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(send, null))
        })

        col.addView(label(tr("Klaviatura ayarları", "کیبورد تنظیملری"), 22f, true))
        val savedDark = KeyboardPreferences.dark(this)
        val themeSwitch = Switch(this).apply {
            text = tr("Qaranlıq tema", "قارانلیق تم")
            isChecked = savedDark
        }
        col.addView(themeSwitch)

        col.addView(label(tr("Vurğu rəngi", "وورغو رنگی"), 15f, true))
        val accentSpinner = Spinner(this)
        val accents = listOf(tr("Mavi", "گؤی") to "blue", tr("Yaşıl", "یاشیل") to "green", tr("Bənövşəyi", "بنفشه‌یی") to "purple", tr("Narıncı", "نارنجی") to "orange", tr("Çəhrayı", "چهرایی") to "pink")
        accentSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, accents.map { it.first })
        accentSpinner.setSelection(accents.indexOfFirst { it.second == KeyboardPreferences.accent(this) }.coerceAtLeast(0))
        col.addView(accentSpinner)

        col.addView(label(tr("Düymə ölçüsü", "دوگمه اؤلچوسو"), 15f, true))
        val keySize = SeekBar(this).apply { max = 18; progress = KeyboardPreferences.keySize(this@MainActivity) - 34 }
        col.addView(keySize)
        val keySizeLabel = label(tr("Düymə hündürlüyü: ${keySize.progress + 34} dp", "دوگمه اوجالیغی: ${keySize.progress + 34} dp"), 13f)
        col.addView(keySizeLabel)
        keySize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { keySizeLabel.text = tr("Düymə hündürlüyü: ${progress + 34} dp", "دوگمه اوجالیغی: ${progress + 34} dp") }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        col.addView(label(tr("Klaviaturanın ümumi hündürlüyü", "کیبوردون عمومی اوجالیغی"), 15f, true))
        val keyboardHeight = SeekBar(this).apply { max = 40; progress = KeyboardPreferences.heightPercent(this@MainActivity) - 80 }
        col.addView(keyboardHeight)
        val heightLabel = label(tr("Hündürlük miqyası: ${keyboardHeight.progress + 80}%", "اوجالیق اؤلچوسو: ${keyboardHeight.progress + 80}%"), 13f)
        col.addView(heightLabel)
        keyboardHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { heightLabel.text = tr("Hündürlük miqyası: ${progress + 80}%", "اوجالیق اؤلچوسو: ${progress + 80}%") }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val suggestionsSwitch = Switch(this).apply {
            text = tr("Söz təklifləri", "سؤز تکلیفلری")
            isChecked = KeyboardPreferences.suggestions(this@MainActivity)
        }
        col.addView(suggestionsSwitch)
        val fontSwitch = Switch(this).apply {
            text = tr("Orxon üçün xüsusi şrift (quraşdırılıbsa)", "اورخون اوچون خوصوصی فونت (قورولوبسا)")
            isChecked = KeyboardPreferences.orkhonFont(this@MainActivity)
        }
        col.addView(fontSwitch)

        col.addView(label(tr("İlkin yazı", "ایلکین یازی"), 15f, true))
        val modeSpinner = Spinner(this)
        val modes = listOf("Türkcə" to "LATIN", "تورکجه" to "ARABIC", "Orxon" to "ORKHON")
        modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes.map { it.first })
        modeSpinner.setSelection(modes.indexOfFirst { it.second == KeyboardPreferences.defaultMode(this) }.coerceAtLeast(0))
        col.addView(modeSpinner)

        col.addView(action(tr("Ayarları saxla", "تنظیملری ساخلا")) {
            KeyboardPreferences.save(this, themeSwitch.isChecked, accents[accentSpinner.selectedItemPosition].second,
                keySize.progress + 34, keyboardHeight.progress + 80, suggestionsSwitch.isChecked,
                modes[modeSpinner.selectedItemPosition].second, fontSwitch.isChecked)
            Toast.makeText(this, tr("Ayarlar saxlandı; tətbiq üçün klaviaturanı yenidən aç.", "تنظیملر ساخلاندی؛ اعمال اولماسی اوچون کیبوردو یئنیدن آچ."), Toast.LENGTH_LONG).show()
        })
        col.addView(action(tr("Öyrənilmiş sözləri sil", "اؤیرنیلمیش سؤزلری سیل")) {
            Suggester.get(this).clearLearned()
            Toast.makeText(this, tr("Şəxsi sözlər silindi.", "شخصی سؤزلر سیلیندی."), Toast.LENGTH_SHORT).show()
        })

        val scroll = ScrollView(this)
        // targetSdk 35 draws edge-to-edge; keep the settings clear of the system bars.
        scroll.fitsSystemWindows = true
        scroll.addView(col)
        setContentView(scroll)
    }
}
