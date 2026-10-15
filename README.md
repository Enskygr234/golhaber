<div align="center">

<img src="docs/icon.png" width="110" alt="GolHaber ikonu">

# GolHaber

**Canlı futbol skorları ve güncel transfer haberleri, Android için.**

[![APK indir](https://img.shields.io/badge/⬇_APK_indir-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Enskygr234/golhaber/releases/latest/download/GolHaber.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

<img src="docs/ekran.png" width="280" alt="GolHaber ekranı">

</div>

<!-- ENS:TANITIM -->
## 🎬 Tanıtım

<div align="center">

<a href="docs/tanitim.mp4"><img src="docs/tanitim.gif" width="720" alt="GolHaber tanıtım videosu"></a>

<sub>Tam kalitede izlemek için görsele tıkla · MP4, 41 saniye</sub>

</div>
<!-- /ENS:TANITIM -->

## Özellikler

| | |
|---|---|
| ⚽ **Canlı skorlar** | Süper Lig, Premier Lig, La Liga, Serie A, Bundesliga, Şampiyonlar Ligi. Canlı maçlar 30 saniyede bir yenilenir. |
| 🗓️ **5 günlük fikstür** | Dünden yarına (±2 gün) maçlar, başlama saatleri ve sonuçlar. |
| 🥅 **Gol detayları** | Golün dakikası, golcü, penaltı (P) ve kendi kalesine (KK) işaretleri. |
| 🔁 **Transfer haberleri** | Gündem, Galatasaray, Fenerbahçe, Beşiktaş, Trabzonspor ve Avrupa ligleri için son 7 günün haberleri. |
| 🔔 **Bildirimler** | Gol, maç sonu ve yeni transfer haberleri. Maç kartındaki 🔔 ile tek maçı takip et ya da favori takım seç. |
| 🎵 **Özel sesler** | Hakem düdüğü, tribün coşkusu, skorbord zili — uygulamaya gömülü, ayarlardan dinleyip seçebilirsin. |

## Kurulum

1. [Releases](../../releases/latest) sayfasından `GolHaber.apk` dosyasını indir.
2. Telefonda dosyayı aç, "bilinmeyen kaynaklardan yükleme" iznini ver.
3. İlk açılışta bildirim iznini onayla, **Ayarlar**'dan favori takımlarını seç.

Android 8.0 (API 26) ve üzeri gerekir.

## Veri kaynakları (API anahtarı gerekmez)

| Veri | Kaynak |
|---|---|
| Skorlar, golcüler, logolar | ESPN herkese açık skor servisi (`site.api.espn.com/apis/site/v2/sports/soccer/{lig}/scoreboard`) |
| Transfer haberleri | Google Haberler RSS (Türkçe, `hl=tr`) |

Lig kodları: `tur.1`, `eng.1`, `esp.1`, `ita.1`, `ger.1`, `uefa.champions`.

## Bildirimler nasıl çalışır?

- Arka planda **WorkManager** her 15 dakikada bir kontrol eder.
- Takip ettiğin bir maç canlıyken kontrol sıklığı **~2 dakikaya** çıkar, maç bitince normale döner.
- Uygulama açıkken canlı skorlar 30 saniyede bir yenilenir.
- Android, kanal sesini sonradan değiştirmeye izin vermediği için her ses ayrı bir bildirim kanalıdır.

## Proje yapısı

```
app/src/main/java/com/enskygr/golhaber/
├── data/        Modeller, ESPN + RSS istemcileri, ayarlar
├── notify/      Bildirim kanalları, arka plan kontrolü (CheckWorker)
└── ui/          Jetpack Compose ekranları: Skorlar, Transfer, Ayarlar
app/src/main/res/raw/   whistle.ogg · crowd.ogg · classic.ogg · chime.ogg
```

Kotlin 2.0 · Jetpack Compose (Material 3) · WorkManager · Coil

## Kendin derle

```bash
./gradlew assembleRelease
```

Her `main` push'unda GitHub Actions APK'yı derler ve `app/build.gradle.kts` içindeki `versionName` ile aynı adlı Release'e (ör. `v1.0.0`) yükler. Yeni sürüm için `versionCode` ve `versionName` değerlerini artırıp push etmen yeterli.

### Kalıcı imza (isteğe bağlı)

İmza anahtarı tanımlanmazsa CI her derlemede geçici bir anahtarla imzalar; bu durumda yeni sürümü kurmadan önce eskisini kaldırman gerekir. Güncellemelerin üzerine kurulabilmesi için repo **Settings → Secrets and variables → Actions** altına şunları ekle:

| Secret | Değer |
|---|---|
| `KEYSTORE_B64` | `base64 -w0 golhaber.jks` çıktısı |
| `KEYSTORE_PASSWORD` | anahtar deposu şifresi |
| `KEY_ALIAS` | anahtar adı |
| `KEY_PASSWORD` | anahtar şifresi |

<!-- ENS:ALT -->
---

<div align="center">

**❄️ ENS Developer'ın diğer projeleri**

📱 Telefon: [Winterball](https://github.com/Enskygr234/Winterball) · [Yemek Tarifleri](https://github.com/Enskygr234/ensdewloper.github.io/tree/main/tarifler)

[🌐 Bütün projeler](https://enskygr234.github.io/ensdewloper.github.io/) · [👤 GitHub profilim](https://github.com/Enskygr234)

<sub>Kış gelir, kod yazılır.</sub>

</div>
<!-- /ENS:ALT -->
