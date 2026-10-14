<p align="center"><img src="docs/icon.png" width="120" alt="GolHaber ikonu"></p>

<h1 align="center">GolHaber</h1>
<p align="center">Canlı futbol skorları ve güncel transfer haberleri — Android</p>

<p align="center">
  <a href="../../releases/latest"><b>📥 APK'yı indir</b></a> ·
  <a href="docs/golhaber-tanitim.mp4">🎬 Tanıtım videosu</a>
</p>

---

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

Her `main` push'unda GitHub Actions APK'yı derler (Actions → son çalışma → *GolHaber-apk*). `v*` etiketi atıldığında APK otomatik olarak Release'e eklenir:

```bash
git tag v1.0.1 && git push --tags
```

### Kalıcı imza (isteğe bağlı)

İmza anahtarı tanımlanmazsa CI her derlemede geçici bir anahtarla imzalar; bu durumda yeni sürümü kurmadan önce eskisini kaldırman gerekir. Güncellemelerin üzerine kurulabilmesi için repo **Settings → Secrets and variables → Actions** altına şunları ekle:

| Secret | Değer |
|---|---|
| `KEYSTORE_B64` | `base64 -w0 golhaber.jks` çıktısı |
| `KEYSTORE_PASSWORD` | anahtar deposu şifresi |
| `KEY_ALIAS` | anahtar adı |
| `KEY_PASSWORD` | anahtar şifresi |
