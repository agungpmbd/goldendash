<h1 align="center">GoldenDash</h1>

<p align="center">
  <img src="docs/home.png" width="49%" alt="GoldenDash tema Terang: jam, cuaca, speedometer, navigasi, pintasan, pemutar musik, slider volume dan kecerahan">
  <img src="docs/home-dark.png" width="49%" alt="GoldenDash tema Gelap">
</p>

<p align="center">
  Launcher ringan, gratis, dan open source untuk headunit Android mobil (Android 7 atau lebih baru): tampilan kaca
  bergaya dashboard dengan jam dan cuaca, speedometer GPS, petunjuk belokan dari Google Maps/Waze, kartu pemutar
  untuk aplikasi musik apa pun, slider volume dan kecerahan, serta tema Terang, Gelap, atau Otomatis. Tanpa akun.
  Dirancang untuk NWD K2401 (1280×720).
</p>

<p align="center">
  ⚠️ <b>Tidak resmi.</b> GoldenDash adalah launcher buatan komunitas. Tidak dibuat, didukung, maupun disahkan oleh
  NWD atau produsen headunit mana pun.
</p>

<p align="center">
  <a href="https://github.com/agungpmbd/goldendash/releases/latest"><img src="https://img.shields.io/github/v/release/agungpmbd/goldendash?label=release" alt="Release"></a>
  <a href="https://github.com/agungpmbd/goldendash/releases"><img src="https://img.shields.io/github/downloads/agungpmbd/goldendash/total?color=1fa77a" alt="Downloads"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/agungpmbd/goldendash?color=blue" alt="MIT License"></a>
  <img src="https://img.shields.io/badge/Android-7%2B-3DDC84?logo=android&logoColor=white" alt="Android 7+">
  <img src="https://img.shields.io/badge/layar-1280%C3%97720-lightgrey" alt="Layar 1280x720">
  <br>
  <img src="https://img.shields.io/badge/APK-%C2%B190%20KB-D9B67A" alt="APK sekitar 80 KB">
  <img src="https://img.shields.io/badge/tanpa%20library-Java%20murni-D9B67A" alt="Java murni tanpa library">
  <img src="https://img.shields.io/badge/tema-Terang%20%7C%20Gelap%20%7C%20Otomatis-7076F0" alt="Tema Terang, Gelap, Otomatis">
</p>

<p align="center">
  <b>Bahasa Indonesia</b> | <a href="README.en.md">English</a>
</p>

## Tentang

Launcher bawaan banyak headunit Android berat, kaku, dan tidak bisa diatur. GoldenDash menggantinya dengan satu
layar yang menampilkan semua yang dibutuhkan saat menyetir: jam, cuaca, kecepatan, petunjuk belokan, musik, volume,
dan kecerahan. Semua bisa disentuh dengan jempol tanpa membuka aplikasi lain.

Ditulis dengan Java murni tanpa library dan tanpa Gradle. APK-nya sekitar 90 KB. Latar digambar sekali saja, GPS
hanya aktif saat home terlihat, dan progres lagu hanya diperbarui saat kartunya tampil, jadi CPU saat diam tetap rendah.

## Fitur

- **Home:** jam besar dan tanggal berbahasa Indonesia, serta cuaca dan nama kota dari lokasi GPS
  ([Open-Meteo](https://open-meteo.com), gratis dan tanpa API key).
- **Speedometer:** kecepatan dari GPS dengan cincin 0–140 km/j. Turun ke 0 sendiri bila sinyal GPS hilang
  (terowongan, basement). Bisa disembunyikan (**⚙ Pengaturan › Speedometer**); petanya lalu melebar memenuhi kartu.
- **Navigasi:** peta langsung [OpenStreetMap](https://www.openstreetmap.org) di posisi GPS, dengan penanda mobil yang
  mengikuti arah gerak (versi gelap untuk tema Gelap). Jarak dan petunjuk belokan diambil dari notifikasi navigasi
  **Google Maps**, **Waze**, Sygic, atau OsmAnd dan tampil di atas peta. Ketuk panelnya untuk kembali ke aplikasi
  navigasi. Garis rute Google Maps tidak bisa ditampilkan: Android tidak mengizinkan launcher biasa menampilkan
  layar aplikasi lain.
- **Pintasan 3×2:** terisi otomatis (Maps, Spotify, Telepon, Radio, Bluetooth, Pengaturan). Ketuk ✏️ atau tekan lama
  sebuah ikon untuk menggantinya.
- **Kartu pemutar:** sampul album, judul, artis, progres, dan tombol sebelumnya/putar/berikutnya untuk aplikasi
  musik apa pun (Spotify, YouTube Music, JOOX, radio, ...). Tombol ♡ muncul bila aplikasinya menyediakan.
  Saat tidak ada yang diputar, tombol ▶ dan tombol **Musik** di menu membuka aplikasi yang terakhir memutar musik.
- **Slider Volume dan Kecerahan:** ikut berubah bila diatur dari knob, tombol setir, atau sistem. Di headunit NWD,
  volume asli diatur MCU, jadi slider menaikkan/menurunkan volume selangkah demi selangkah seperti knob.
- **Posisi menu:** bilah menu di kiri atau kanan layar (**⚙ Pengaturan › Posisi menu**).
- **Semua Aplikasi:** pencarian, tab Semua/Navigasi/Media/Sistem, dan grid 6×2 yang bisa digeser per halaman.
  - Tekan lama sebuah ikon untuk menambahkannya ke pintasan, membuka info aplikasi, atau menyembunyikannya.
  - **Atur** untuk mengurutkan A–Z atau menurut yang paling sering dibuka, dan untuk menampilkan lagi aplikasi
    tersembunyi.
- **Tema:** **Terang** (sutra krem keemasan), **Gelap** (lipatan biru-ungu dengan tepi bercahaya), atau
  **Otomatis** (gelap pukul 18.00–06.00). Berganti langsung tanpa restart. Atur di **⚙ Pengaturan › Tema**.
- **Pengaturan:** status setiap izin, masing-masing dengan tombol untuk mengaturnya.

## Tangkapan layar

| Terang | Gelap |
|---|---|
| ![Home](docs/home.png) | ![Home gelap](docs/home-dark.png) |
| ![Semua Aplikasi](docs/apps.png) | ![Semua Aplikasi gelap](docs/apps-dark.png) |
| ![Pengaturan](docs/settings.png) | ![Pengaturan gelap](docs/settings-dark.png) |

<sub>Diambil dari emulator Android 10 1280×720. Aplikasi yang tampil adalah aplikasi bawaan emulator.</sub>

## Pemasangan

### Tanpa komputer (dari flashdisk)

1. Unduh `goldendash-<versi>.apk` dari [Releases](https://github.com/agungpmbd/goldendash/releases/latest), lalu
   salin ke flashdisk.
2. Di headunit, buka APK-nya lewat file manager, ketuk **INSTALL**, lalu **OPEN**. Bila diminta, izinkan
   pemasangan dari sumber tidak dikenal.
3. Izinkan **Lokasi** saat diminta.
4. Buka **⚙ Pengaturan** di GoldenDash, lalu ketuk tombol di tiap baris:
   - **Akses notifikasi:** untuk kartu pemutar dan petunjuk belokan.
   - **Ubah kecerahan layar:** supaya slider Kecerahan berlaku di seluruh sistem.
   - **Jadikan tampilan utama:** supaya GoldenDash tampil setiap headunit menyala (lihat catatan NWD di bawah).
5. Jalankan [daftar uji sebelum berkendara](#disclaimer) dalam keadaan mobil diam.

> **Firmware NWD:** saat menyala, headunit memilih launcher dari properti sistem `persist.nwd.launcher.default`,
> bukan dari pilihan Home Android, jadi tanpa langkah ini GoldenDash kembali ke launcher bawaan tiap mobil
> dinyalakan. Tombol **Jadikan tampilan utama** mengubah properti itu lewat ADB headunit sendiri, tanpa komputer:
> 1. Aktifkan **USB debugging**: Pengaturan Android › Tentang perangkat › ketuk **Nomor build** 7 kali, lalu
>    **Opsi pengembang › USB debugging**.
> 2. Ketuk **Jadikan tampilan utama** di ⚙ Pengaturan GoldenDash.
> 3. Bila muncul **"Allow USB debugging?"**, centang **Always allow**, lalu **OK**.
>
> Cukup sekali; pilihannya tersimpan permanen. Ketuk tombol yang sama (**Aktif**) untuk kembali ke launcher bawaan.
> Bila muncul pesan port 5555 belum terbuka (umum di Android 11 ke atas), jalankan `adb tcpip 5555` sekali dari
> komputer, atau pakai cara di bawah.

### Dengan ADB (disarankan untuk NWD)

Aktifkan USB debugging atau ADB WiFi di headunit, lalu dari komputer:

```bash
adb connect 192.168.1.17:5555        # IP headunit Anda
./build.sh install
```

`./build.sh install` memasang APK, memberikan semua izin, lalu menjadikan GoldenDash launcher default, termasuk
properti NWD `persist.nwd.launcher.default`.

**Memperbarui:** pasang APK baru di atas yang lama. Pengaturan, pintasan, dan tema tetap tersimpan.

## Perangkat yang didukung

GoldenDash butuh **Android 7.0 atau lebih baru** dan dirancang untuk layar **1280×720 landscape**. Tata letaknya
diskalakan dari ukuran desain 1280×720, jadi layar landscape lain tetap proporsional.

| Perangkat | Layar | Android | Status |
|---|---|---|---|
| Emulator Android 10 | 1280×720 | 10 | ✅ diuji: semua layar, kedua tema, GPS, cuaca, slider |
| Audioteq AQ-A9 (NWD K2401, Allwinner sun50iw10) | 1280×720 | 10 | ✅ target utama: home, pemutar, petunjuk belokan, kecerahan diuji; volume lewat MCU (0.2.0) belum diuji |
| Peerless AI Box (Cello Audio) | layar mobil | – | ✅ dilaporkan berfungsi oleh pengguna |
| Headunit Android lain (FYT, MTK, Rockchip, …) | 1280×720 | 7+ | ⚠️ seharusnya jalan; properti khusus NWD tidak berlaku |
| Layar landscape lain | 800×480 ke atas | 7+ | ⚠️ diskalakan proporsional, belum diuji |

Laporan dari headunit lain sangat membantu: buka [issue](https://github.com/agungpmbd/goldendash/issues) dengan
menyebut model, ukuran layar, dan versi Android.

## Perubahan pada headunit

Semua yang diubah GoldenDash, dan cara mengembalikannya. **GoldenDash tidak menghapus, mematikan, atau mengubah
aplikasi lain.** Tanpa ADB, yang berubah hanya izin standar Android yang Anda berikan sendiri.

| Perubahan | Kapan | Cara mengembalikan |
|---|---|---|
| Launcher default NWD `persist.nwd.launcher.default=com.goldendash.launcher` | Tombol **Jadikan tampilan utama** (lewat ADB headunit sendiri) atau `./build.sh install` | Ketuk tombol **Aktif** di baris yang sama, atau `adb shell setprop persist.nwd.launcher.default com.android.launcher` |
| Kunci ADB GoldenDash ("Always allow") | Saat Anda mengizinkan "Allow USB debugging?" | Opsi pengembang › Cabut otorisasi USB debugging |
| Preferensi aplikasi HOME | `./build.sh install`, atau saat Anda memilihnya di Pengaturan | Pengaturan Android › Aplikasi › Aplikasi default › Aplikasi layar utama |
| Izin "Ubah setelan sistem" (kecerahan) | `./build.sh install`, atau saat Anda mengizinkannya | Pengaturan Android › Aplikasi › GoldenDash › Ubah setelan sistem |
| Akses notifikasi | `./build.sh install`, atau saat Anda mengizinkannya | Pengaturan Android › Akses notifikasi › GoldenDash |
| Kecerahan layar sistem | Saat Anda menggeser slider Kecerahan | Geser lagi, atau atur dari Pengaturan Android |

Untuk kembali ke launcher bawaan sepenuhnya:

```bash
adb shell setprop persist.nwd.launcher.default com.android.launcher
adb shell cmd package query-activities -c android.intent.category.HOME   # cari nama launcher bawaan
adb shell cmd package set-home-activity <paket>/<activity>
adb uninstall com.goldendash.launcher
```

## Privasi

- **Tanpa akun, tanpa iklan, tanpa analitik.** Tidak ada server GoldenDash.
- **Lokasi** dipakai di perangkat untuk speedometer. Peta mengunduh tile 256 px dari server tile OpenStreetMap untuk area
  di sekitar mobil (±15–30 KB per tile, disimpan 7 hari di cache). Untuk cuaca, koordinat (4 desimal) dikirim ke
  [Open-Meteo](https://open-meteo.com) paling sering tiap 30 menit, dan nama kota dicari lewat Geocoder bawaan Android.
- **Akses notifikasi** hanya dipakai untuk membaca sesi media (judul, artis, sampul) dan notifikasi navigasi
  Google Maps, Waze, Sygic, dan OsmAnd. Notifikasi aplikasi lain tidak dibaca, dan tidak ada yang dikirim keluar.

## Build dari sumber

Kebutuhan: macOS atau Linux, JDK 17, Android SDK (`build-tools` dan `platforms/android-35`) di
`~/Library/Android/sdk` atau `$ANDROID_HOME`, dan `adb`.

```bash
brew install openjdk@17
brew install --cask android-commandlinetools android-platform-tools
export PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH
sdkmanager --sdk_root=$HOME/Library/Android/sdk "build-tools;35.0.0" "platforms;android-35"

./build.sh              # → build/goldendash.apk
./build.sh install      # build + pasang + izin + launcher default
DEVICE=1.2.3.4:5555 ./build.sh install   # bila ada lebih dari satu perangkat ADB
```

Kunci tanda tangan (`goldendash.keystore` + `.keystore-pass`) dibuat otomatis saat build pertama dan tidak ikut di
repo. **Cadangkan keduanya**: tanpa kunci yang sama, APK baru tidak bisa memperbarui aplikasi yang sudah terpasang.

**Wallpaper tema Gelap sendiri (opsional):** taruh gambar 16:9 di `res/drawable-nodpi/wall_dark.jpg` (misalnya
1600×900), lalu build ulang. Tanpa file itu, tema Gelap memakai latar lipatan bawaan. File ini diabaikan Git.

### Struktur

```
AndroidManifest.xml
build.sh                     build + pasang tanpa Gradle (aapt2 → javac → d8 → zipalign → apksigner)
res/                         ikon vektor, tema, root sertifikat ISRG untuk Android 7.0
src/com/goldendash/launcher/
├── HomeActivity             Activity HOME; Home / Semua Aplikasi / Pengaturan dalam satu panggung 1280×720
├── Ui                       satuan desain, token warna tema, panel kaca, pembuat view
├── SilkBackground           latar Terang/Gelap (digambar sekali ke bitmap)
├── Sidebar                  bilah kiri
├── HomeScreen               tata letak kartu home
├── ClockCard, WeatherIcon, Weather, Clock      jam, tanggal, cuaca
├── DriveCard, SpeedGauge, Nav                  speedometer dan petunjuk belokan
├── LiveMap, Tiles, MapDecor                    peta langsung OpenStreetMap (+ cache tile), peta hiasan saat belum ada GPS
├── ShortcutsCard, Shortcuts, AppPicker         pintasan
├── MediaCard, MiniPlayer, Controls, Media      pemutar
├── SliderCard               volume dan kecerahan
├── AppsScreen, AppGrid, Apps                   Semua Aplikasi
├── SettingsScreen           tema dan izin
├── Loc                      GPS (hanya aktif saat home di depan)
└── NotifListener            akses notifikasi: sesi media + notifikasi navigasi
```

Semua posisi ditulis dalam satuan desain 1280×720 dan diskalakan lewat `Ui.px()`. Untuk menggeser elemen, ubah
angka di `Ui.add(parent, view, x, y, lebar, tinggi)`. Warna kedua tema ada di `Ui.theme()`.

## Disclaimer

**Gunakan dengan risiko sendiri.** Perangkat lunak ini diberikan "apa adanya", tanpa jaminan apa pun
(lihat [LICENSE](LICENSE)).

> ⚠️ **Uji dulu sebelum berkendara.** Setelah memasang GoldenDash, dalam keadaan mobil diam, pastikan semua ini
> masih berfungsi: kamera mundur, panggilan telepon Bluetooth, audio dan tombol setir, navigasi, serta kembali ke
> home dengan tombol Home. Jangan mengoperasikan layar saat mobil berjalan.

GoldenDash tidak berafiliasi dengan NWD, Google, Waze, Spotify, atau merek lain yang disebut di sini. Semua merek
adalah milik pemiliknya masing-masing.

## Terima kasih

- [KlikDash](https://github.com/ashafizullah/klikdash) (MIT): inspirasi pendekatan launcher ringan tanpa Gradle
  untuk headunit NWD, cara build, sertifikat root ISRG untuk Android 7.0, dan klien ADB lokal untuk tombol
  Jadikan tampilan utama.
- [Material Icons](https://fonts.google.com/icons) oleh Google (Apache 2.0) untuk ikon antarmuka.
- [OpenStreetMap](https://www.openstreetmap.org/copyright) (data © kontributor OpenStreetMap, ODbL) untuk peta.
- [Open-Meteo](https://open-meteo.com) (CC BY 4.0) untuk data cuaca.

## Lisensi

[MIT](LICENSE) © 2026 agungpmbd
