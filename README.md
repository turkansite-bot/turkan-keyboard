# TÜRKAN Klaviatura — Azərbaycan türkcəsi klaviaturası (Android)

Latın + ərəb əlifbası + Orxon yazısı klaviaturası; simvol və emoji qatı, söz təklifi, seçilmiş mətndə yazı çevirmə və Orxon ad çeviricisi. İnternet icazəsi yoxdur. Cari versiya: **0.8.3**

## APK-nı GitHub Actions ilə qurmaq (Android Studio olmadan)
1. GitHub-da repo yarat və bu qovluğun **içindəkiləri** (qovluğun özünü yox) repo kökünə yüklə: `app/`, `.github/`, `gradlew` və s. birbaşa kökdə olmalıdır, yoxsa Actions işləməz.
2. **Actions** ← **Build TÜRKAN Keyboard APK** ← **Run workflow** (və ya `main`/`master`-ə push).
3. Uğurlu olduqdan sonra `TURKAN-Keyboard-debug-APK` artefaktını yüklə və içindəki `app-debug.apk`-nı quraşdır. Uğursuz olsa, `unit-test-reports` artefaktında test hesabatı var.

Workflow özü Gradle 8.9, JDK 17 və Android SDK 35 quraşdırır, APK-dan əvvəl vahid testləri işlədir. Bu APK yalnız sınaq üçündür; ictimai yayım üçün imzalı release versiyası lazımdır.

## Android Studio ilə işə salmaq
1. Android Studio-nu (Ladybug və ya daha yeni) aç, bu qovluğu Open et və Gradle-ın sinxronlaşmasını gözlə.
2. Telefonda və ya emulyatorda Run et. Tətbiqdə: «Klaviaturanı aktiv et» ← «Klaviaturanı seç».
3. (İstəyə bağlı) Orxon hərfləri hər telefonda eyni görünsün deyə `NotoSansOldTurkic-Regular.ttf` şriftini `app/src/main/assets/fonts/` qovluğuna qoy.

Terminaldan: `./gradlew testDebugUnitTest assembleDebug` (Windows-da `gradlew.bat`). Ətraflı: `gradle/wrapper/README.md`.

## Veb versiyası
`web/index.html` müstəqil veb səhifədir: Türkcə/تورکجه/Orxon klaviaturası, üç yazılı önbaxış, geri qaytar/yenidən, və interfeys yazısını (Türkcə ⇄ تورکجه) dəyişən düymə. Pulsuz yerləşdirmək üçün GitHub **Settings ← Pages** bölməsində `main` budağı və `/web` qovluğunu seç. Bu yalnız yazı və çevirmə alətidir; telefonun sistem klaviaturası yalnız Android tətbiqi ilə mümkündür.

## İmkanlar
- Üç düzülüş: Latın (Türkcə), ərəb əlifbası (تورکجه), Orxon; simvol qatı və emoji.
- 🌐 düyməsi klaviatura dəyişmək üçün (uzun basma: klaviatura seçicisi).
- Təklif sırası: sözün tamamlanması, təxmini düzəliş, növbəti söz; ⇄ (Türkcə ↔ تورکجه, Orxon ← Türkcə) və Ork düymələri.
- Ayarlar (tema, rəng, ölçü, ilkin düzülüş, interfeys yazısı) klaviatura növbəti dəfə açılanda tətbiq olunur.
- ع hərfi Türkcəyə «ə» kimi, ء isə buraxılmaqla çevrilir (təxmini qayda; yerli yazıçı yoxlamalıdır).

## Quruluş
- `KeyboardLayouts.kt` — düymə düzülüşləri (test edilə bilən məlumat)
- `KeyboardService.kt` — klaviaturanın özü; `MainActivity.kt` — quraşdırma səhifəsi və ad çeviricisi
- `ScriptConverter.kt` + `ScriptTables.kt` — Latın ↔ ərəb əlifbası; cədvəllər `tools/gen_script_tables.py` ilə yaranır (dəyişmək üçün həmin faylı redaktə edib işlət)
- `OrkhonConverter.kt` — Latın ↔ Orxon (təxmini və itkili geri çevirmə)
- `Suggester.kt` — təklif mühərriki; `WordAlgorithms.kt` — redaktə məsafəsi

## Məxfilik
Bütün emal və öyrənmə telefonda gedir, internet icazəsi yoxdur, `allowBackup=false`-dur. Tam mətn: [PRIVACY.md](PRIVACY.md).

## Yayımdan əvvəl mütləq
- Ərəb əlifbası profilini (v0.3) və Orxon çeviricisini dilçi və ya yerli yazıçı yoxlasın. İnterfeys mətnlərini də.
- Söz siyahısını (təxminən 970 söz) real tezlik siyahısı ilə əvəz et; fayldakı sıra = təklif sırası.
- **LICENSE seç**; paketdə qəsdən heç biri qoyulmayıb.
- Google Play üçün `targetSdk` 35-dir; Play-in cari tələbini yoxla. Release imzası: `Build > Generate Signed Bundle / APK`.
- İlk GitHub Actions icrası ilk quraşdırma təsdiqidir (layihə hələ heç vaxt kompilyasiya olunmayıb).
