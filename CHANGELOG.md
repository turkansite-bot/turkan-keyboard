# Changelog

## 0.8.3 — Turkic web interface, script switch, ع / ء
- Web page: all interface text is now Azerbaijani Turkic, with a button (SVG icon) beside the dark/light theme button that switches the interface between Türkcə (Latin) and تورکجه (Arabic script); direction flips RTL/LTR, choice is remembered.
- Arabic → Latin: ع now converts to `ə` (e.g. عمر → əmr, معنا → məna) and ء is dropped, instead of both becoming an apostrophe. Lossy by nature; needs review by a native writer. Tables regenerated; test updated.
- PRIVACY.md is bilingual Türkcə (Latin + Arabic script); README is now in Türkcə (Latin).

## 0.8.2 — Turkic interface, clearer layout labels
- Layout switch key now reads **Türkcə** (Latin layout) and **تورکجه** (Arabic-script layout) instead of "AZ" and the Arabic letter ع, so users know which script they will get.
- Setup/settings screen is now in Azerbaijani Turkic instead of Persian, with a Türkcə ⇄ تورکجه button at the top to switch its script (default: Arabic script, RTL). The choice is stored on the device.
- Web page: tab, heading and convert-button labels use Türkcə / تورکجه. The rest of the web text is still Persian.
- Interface wording (especially the Arabic-script spelling) is a first draft and needs review by a native South Azerbaijani writer.

## 0.8.1 — Larger starter dictionary
- `words_az.txt` grew from ~300 to ~970 common words (pronouns, numbers, months, family, body, food, home, nature, verbs with common conjugations, greetings, computing terms, Iranian Azerbaijani place names).
- Still a seed list in standard spelling: replace with a reviewed frequency list (and add South Azerbaijani/Iran-specific vocabulary) before release.
- No code changes; still not compiled in the authoring environment.

## 0.8.0 — Script converter profile v0.3 (full character coverage)
**Latin → Arabic script**
- Digits become Persian digits (optional); `, ; ? %` become `، ؛ ؟ ٪`; a comma between digits becomes the decimal separator `٫`.
- Input is normalised before conversion: Unicode NFC (so `s` + combining cedilla becomes `ş`, `o` + diaeresis becomes `ö`, …) and look-alikes are folded (`ș`→`ş`, `ǝ`/`ä`→`ə`). Dotted/dotless I are lower-cased without relying on the device locale.
- `w` is handled (one-way, as `v`).

**Arabic script → Latin**
- Complete coverage of the keyboard and common variants: alef family (`آ أ إ ٱ`), hamza and ain (`ء ع` → `'`), `ئ ة ۀ ہ`, loan letters (`ث ح ذ ص ض ط ظ`), `ك ڪ ڭ ڵ ڤ ې ێ ے`.
- Arabic-Indic and Persian digits → ASCII; Arabic punctuation, decimal and thousands separators → Latin.
- Harakat, tatweel, Quranic marks and ZWNJ/ZWJ are removed; Arabic presentation forms (PDF copy-paste, e.g. `ﻻ`) are folded with NFKC.
- Direction detection looks at Arabic-script *letters* only, so digits or punctuation alone never flip it.

**Architecture**
- One source of truth: `tools/gen_script_tables.py` generates `ScriptTables.kt` (Android) and the tables embedded in `web/index.html`, so the two can no longer drift. CI fails if the generated files are out of date.
- Kotlin tests use vectors produced by the web converter; the web page and the app now convert identically.
- Arabic layout shows Persian digits; the web page has a "Persian digits" switch.
- Known limit: Arabic script omits most short vowels, so Arabic → Latin is intentionally lossy (`سلام` → `slam`).


## 0.7.1 — Review pass
- Keyboard: the light/dark flag is cached instead of being read from SharedPreferences for every key.
- Added `web/index.html`, a self-contained web version (typing, three-script live preview, undo/redo) that can be hosted on GitHub Pages.
- Still not compiled in the authoring environment; the first CI run is the first build verification.

## 0.7.0 — Usability, performance and build fixes
**Keyboard**
- New symbols layer (`@ # $ _ & - + ( ) / * " ' : ; ! ? % = < > [ ] { } \ | ~ ^ ` € £`, plus `، ؛ ؟ « » …`) and a small emoji layer, reachable from every layout.
- Added a keyboard-switch key (🌐; long press opens the system picker).
- Arabic layout gained the missing `ى` (U+0649) key, so `ı` is typeable; added by a layout test that checks every converter output letter has a key.
- Settings are now applied when the keyboard opens (previously only when the view was first created).
- The ⇄ and Ork buttons stay visible when word suggestions are turned off.
- Suggestions refresh when the cursor moves or the field changes outside the keyboard.
- Backspace and all special keys now have accessibility labels; Backspace works with TalkBack.
- ⇄ now also converts Old Turkic text back to Latin; word-before-cursor detection understands Old Turkic letters.
- Adaptive launcher icon; keyboard subtype (`az`); `supportsRtl`.

**Suggestions**
- Rewritten engine: learned words are cached in memory instead of copying all SharedPreferences on every key; bundled dictionary is sorted and searched by binary search; fuzzy matching is length-pruned; dictionary loads on a background thread.
- Learned words (5 000) **and** word pairs (10 000) are now capped; the least-used entries are evicted.
- Typo correction compares against the same-length beginning of longer words instead of whole words.
- The engine is storage-agnostic and unit-tested (`SuggesterTest`).

**Old Turkic**
- k/x/h now follow vowel harmony (back words use q-series signs); k/q signs follow the nearest vowel instead of any vowel in the word.
- Added `toLatin` (lossy reverse conversion) and a configurable word separator.

**Build**
- Gradle 8.9, Android Gradle Plugin 8.7.3, compileSdk/targetSdk 35 (settings screen handles edge-to-edge).
- GitHub Actions installs Gradle 8.9 explicitly (runner Gradle may be incompatible), SDK 35, uploads test reports, and also runs on pull requests.
- `gradlew` only uses an installed Gradle if it is exactly 8.9 and verifies downloads against the published SHA-256.
- Project files are now at the archive root so they can be uploaded to a repository as-is.
- Added `PRIVACY.md`; README and changelog reconciled (they previously contradicted each other).
- Not compiled in the authoring environment (no Gradle/Android SDK available): the first CI run is the first verification.

## 0.6.0 — Build preparation
- Fixed the settings screen key-size preference context reference.
- Added cross-platform Gradle bootstrap launchers.
- Added build prerequisites and APK output instructions.

## 0.5.0 — Keyboard customization
- Dark/light themes, five accent colors, key height and keyboard height scale.
- Suggestion-row toggle, default layout selection, optional Old Turkic font toggle.
- Control to clear locally learned words and word pairs. Preferences stored locally only.

## 0.4.0 — Smart suggestions
- Local vocabulary suggestions, typo candidates, and basic next-word predictions.

## 0.3.0 — Old Turkic converter
- Rewrote the converter: vowel-harmony-based paired consonant signs, `ng` → ENG, punctuation preserved.

## Earlier
- Initial Latin / Arabic / Orkhon layouts and script converter.
