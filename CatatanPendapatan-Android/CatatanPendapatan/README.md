# Catatan Pendapatan (Android, Kotlin + Jetpack Compose)

Versi Android native dari aplikasi desktop `catatan_pendapatan.py`.

## Fitur
- **Statistik**: harian / mingguan / bulanan / tahunan, kartu selisih, grafik batang (ketuk batang untuk rincian), perbandingan dengan periode sebelumnya, rata-rata, hari paling boros, sumber teratas.
- **Daftar**: catatan per bulan, pencarian di semua bulan (sumber, kategori, catatan, jumlah, tanggal). Ketuk catatan untuk ubah/hapus.
- **Kalender**: kotak bulanan dengan selisih per hari, ketuk hari untuk melihat/menambah catatan.
- **Anggaran**: donat komposisi pengeluaran, progres anggaran per kategori (peringatan di 80% dan saat terlampaui).
- **Kategori**: tambah/ubah/hapus, warna, anggaran bulanan.
- **Data**: ekspor CSV (Excel) dan cadangan JSON, impor CSV/JSON (tambahkan atau ganti), pulihkan dari cadangan otomatis harian, urungkan perubahan terakhir.
- **4 tema**: Terang, Gelap, Midnight Purple, Samudra.
- Format cadangan JSON **sama** dengan versi desktop, jadi file cadangan dari aplikasi Windows bisa diimpor ke HP, dan sebaliknya.

## Cara membuat APK

### Cara 1 - Android Studio (paling umum)
1. Pasang Android Studio terbaru (sudah termasuk JDK 17).
2. `File > Open` lalu pilih folder `CatatanPendapatan` ini. Tunggu Gradle sync selesai (butuh internet pertama kali).
3. Menu `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.
5. Kirim ke HP, buka, lalu izinkan "install dari sumber tidak dikenal".

### Cara 2 - Tanpa Android Studio (GitHub Actions)
1. Buat repositori GitHub, unggah seluruh isi folder ini.
2. Buka tab **Actions > Build APK > Run workflow**.
3. Setelah selesai, unduh artifact `CatatanPendapatan-APK` (isinya `app-debug.apk`).

## Catatan
- APK debug sudah bisa dipasang dan dipakai sehari-hari. Untuk Play Store perlu APK/AAB release yang ditandatangani sendiri.
- Data tersimpan di penyimpanan internal aplikasi. Mencopot aplikasi menghapus data, jadi rutin lakukan **Ekspor cadangan lengkap**.
