package com.catatan.pendapatan

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import kotlin.math.abs

const val JENIS_MASUK = "masuk"
const val JENIS_KELUAR = "keluar"

val BULAN = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)
val BULAN_PENDEK = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
val HARI_PENDEK = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

val WARNA_KATEGORI = listOf(
    "#6366f1", "#06b6d4", "#10b981", "#f59e0b", "#f97316", "#ef4444",
    "#ec4899", "#8b5cf6", "#14b8a6", "#84cc16", "#0ea5e9", "#64748b",
)
const val WARNA_TANPA = "#94a3b8"

data class Catatan(
    val id: Long,
    val jenis: String,
    val tanggal: String,
    val sumber: String,
    val jumlah: Long,
    val catatan: String = "",
    val kategori: String = "",
) {
    val masuk: Boolean get() = jenis != JENIS_KELUAR
}

data class Kategori(val nama: String, val warna: String, val jenis: String)

val KATEGORI_BAWAAN = listOf(
    Kategori("Makan", "#f97316", JENIS_KELUAR),
    Kategori("Transport", "#06b6d4", JENIS_KELUAR),
    Kategori("Tagihan", "#6366f1", JENIS_KELUAR),
    Kategori("Belanja", "#ec4899", JENIS_KELUAR),
    Kategori("Hiburan", "#8b5cf6", JENIS_KELUAR),
    Kategori("Kesehatan", "#10b981", JENIS_KELUAR),
    Kategori("Pendidikan", "#f59e0b", JENIS_KELUAR),
    Kategori("Lainnya", "#64748b", JENIS_KELUAR),
    Kategori("Gaji", "#10b981", JENIS_MASUK),
    Kategori("Bonus", "#f59e0b", JENIS_MASUK),
    Kategori("Usaha", "#06b6d4", JENIS_MASUK),
    Kategori("Lainnya", "#64748b", JENIS_MASUK),
)

data class DataApp(
    val catatan: List<Catatan> = emptyList(),
    val kategori: List<Kategori> = KATEGORI_BAWAAN,
    val anggaran: Map<String, Long> = emptyMap(),
)

fun namaJenis(j: String) = if (j == JENIS_KELUAR) "Pengeluaran" else "Pemasukan"

// ---------- format ----------
fun formatAngka(n: Long): String {
    val s = abs(n).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(s[i])
    }
    return sb.toString()
}

fun rupiah(n: Long) = "Rp " + formatAngka(n)
fun rupiahTanda(n: Long) = (if (n < 0) "-" else "") + rupiah(n)

private fun desimal(x: Double): String {
    val t = Math.round(x * 10) / 10.0
    return if (t % 1.0 == 0.0) t.toLong().toString() else t.toString().replace('.', ',')
}

/** 1500000 -> 1,5jt ; 300000 -> 300rb */
fun ringkas(n: Long): String {
    val a = abs(n)
    return when {
        a >= 1_000_000_000L -> desimal(a / 1e9) + "M"
        a >= 1_000_000L -> desimal(a / 1e6) + "jt"
        a >= 1_000L -> desimal(a / 1e3) + "rb"
        else -> a.toString()
    }
}

fun parseJumlah(teks: String): Long? {
    val digit = teks.filter { it.isDigit() }
    if (digit.isEmpty() || digit.length > 15) return null
    val n = digit.toLong()
    return if (n > 0) n else null
}

/** Terima: 2026-11-09, 09/11/2026, 9-11, atau cukup 9 (bulan dan tahun ini). */
fun parseTanggal(teks: String, hariIni: LocalDate = LocalDate.now()): LocalDate? {
    val t = teks.trim()
    if (t.isEmpty()) return null
    val bagian = t.replace('.', '/').replace('-', '/').split('/').map { it.trim() }
    if (bagian.any { it.isEmpty() || it.length > 4 || !it.all { c -> c.isDigit() } }) return null
    val angka = bagian.map { it.toInt() }
    return try {
        when (angka.size) {
            3 -> {
                val tahunDulu = bagian[0].length == 4
                val d = if (tahunDulu) angka[2] else angka[0]
                val m = angka[1]
                var y = if (tahunDulu) angka[0] else angka[2]
                if (y < 100) y += 2000
                LocalDate.of(y, m, d)
            }
            2 -> LocalDate.of(hariIni.year, angka[1], angka[0])
            1 -> LocalDate.of(hariIni.year, hariIni.monthValue, angka[0])
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun isiTanggal(d: LocalDate): String =
    "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)

fun namaHariPendek(d: LocalDate) = HARI_PENDEK[d.dayOfWeek.value - 1]

fun tampilTanggal(iso: String): String {
    val d = LocalDate.parse(iso)
    return "${namaHariPendek(d)}, ${d.dayOfMonth} ${BULAN_PENDEK[d.monthValue - 1]} ${d.year}"
}

fun langkahSkala(puncak: Long): Long {
    var dasar = 1L
    while (dasar < 1_000_000_000_000_000L) {
        for (k in longArrayOf(1, 2, 5)) {
            if (dasar * k * 4 >= puncak) return dasar * k
        }
        dasar *= 10
    }
    return dasar
}

// ---------- statistik ----------
enum class Periode(val label: String) {
    HARIAN("Harian"), MINGGUAN("Mingguan"), BULANAN("Bulanan"), TAHUNAN("Tahunan")
}

data class Bucket(val label: String, val tip: String, val masuk: Long, val keluar: Long, val aktif: Boolean)

data class Statistik(
    val judul: String,
    val bucket: List<Bucket>,
    val masuk: Long,
    val keluar: Long,
    val selisih: Long,
    val rasio: Double?,
    val hari: Int,
    val rataKeluar: Double,
    val rataMasuk: Double,
    val sebelumMasuk: Long?,
    val sebelumKeluar: Long?,
    val namaSebelum: String,
    val topMasuk: List<Pair<String, Long>>,
    val topKeluar: List<Pair<String, Long>>,
    val hariBoros: Pair<String, Long>?,
    val hariTerbaik: Pair<String, Long>?,
)

private fun rentang(p: Periode, bulan: YearMonth, data: List<Catatan>, hariIni: LocalDate): Pair<LocalDate, LocalDate> =
    when (p) {
        Periode.HARIAN, Periode.MINGGUAN -> bulan.atDay(1) to bulan.atEndOfMonth()
        Periode.BULANAN -> LocalDate.of(bulan.year, 1, 1) to LocalDate.of(bulan.year, 12, 31)
        Periode.TAHUNAN -> {
            val tahun = data.map { it.tanggal.substring(0, 4).toInt() } + hariIni.year
            val akhir = tahun.max()
            LocalDate.of(maxOf(tahun.min(), akhir - 7), 1, 1) to LocalDate.of(akhir, 12, 31)
        }
    }

/** {tanggal_iso: [masuk, keluar]} */
fun perHari(data: List<Catatan>, awal: LocalDate, akhir: LocalDate): Map<String, LongArray> {
    val a = awal.toString()
    val b = akhir.toString()
    val hasil = HashMap<String, LongArray>()
    for (d in data) {
        if (d.tanggal >= a && d.tanggal <= b) {
            val baris = hasil.getOrPut(d.tanggal) { LongArray(2) }
            baris[if (d.masuk) 0 else 1] += d.jumlah
        }
    }
    return hasil
}

private fun jumlahAwalan(per: Map<String, LongArray>, awalan: String): Pair<Long, Long> {
    var masuk = 0L
    var keluar = 0L
    for ((t, v) in per) {
        if (t.startsWith(awalan)) {
            masuk += v[0]
            keluar += v[1]
        }
    }
    return masuk to keluar
}

private fun buatBucket(p: Periode, awal: LocalDate, akhir: LocalDate, per: Map<String, LongArray>, hariIni: LocalDate): List<Bucket> {
    val hasil = ArrayList<Bucket>()
    when (p) {
        Periode.HARIAN -> {
            val n = ChronoUnit.DAYS.between(awal, akhir).toInt()
            for (i in 0..n) {
                val tgl = awal.plusDays(i.toLong())
                val v = per[tgl.toString()]
                hasil.add(
                    Bucket(
                        label = tgl.dayOfMonth.toString(),
                        tip = "${namaHariPendek(tgl)}, ${tgl.dayOfMonth} ${BULAN_PENDEK[tgl.monthValue - 1]} ${tgl.year}",
                        masuk = v?.get(0) ?: 0L, keluar = v?.get(1) ?: 0L, aktif = tgl == hariIni,
                    )
                )
            }
        }
        Periode.MINGGUAN -> {
            val kelompok = LinkedHashMap<Pair<Int, Int>, MutableList<LocalDate>>()
            var tgl = awal
            while (!tgl.isAfter(akhir)) {
                val kunci = tgl.get(IsoFields.WEEK_BASED_YEAR) to tgl.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                kelompok.getOrPut(kunci) { mutableListOf() }.add(tgl)
                tgl = tgl.plusDays(1)
            }
            for (hari in kelompok.values) {
                val a = hari.first()
                val b = hari.last()
                var mk = 0L
                var kl = 0L
                for (t in hari) {
                    val v = per[t.toString()]
                    if (v != null) {
                        mk += v[0]
                        kl += v[1]
                    }
                }
                hasil.add(
                    Bucket(
                        label = if (a != b) "${a.dayOfMonth}–${b.dayOfMonth}" else a.dayOfMonth.toString(),
                        tip = "${a.dayOfMonth} ${BULAN_PENDEK[a.monthValue - 1]} – ${b.dayOfMonth} ${BULAN_PENDEK[b.monthValue - 1]} ${b.year}",
                        masuk = mk, keluar = kl, aktif = !hariIni.isBefore(a) && !hariIni.isAfter(b),
                    )
                )
            }
        }
        Periode.BULANAN -> {
            for (mo in 1..12) {
                val (mk, kl) = jumlahAwalan(per, "%04d-%02d".format(awal.year, mo))
                hasil.add(
                    Bucket(
                        label = BULAN_PENDEK[mo - 1], tip = "${BULAN[mo - 1]} ${awal.year}",
                        masuk = mk, keluar = kl, aktif = awal.year == hariIni.year && mo == hariIni.monthValue,
                    )
                )
            }
        }
        Periode.TAHUNAN -> {
            for (th in awal.year..akhir.year) {
                val (mk, kl) = jumlahAwalan(per, "%04d-".format(th))
                hasil.add(Bucket(th.toString(), "Tahun $th", mk, kl, th == hariIni.year))
            }
        }
    }
    return hasil
}

private fun top(data: List<Catatan>, awal: LocalDate, akhir: LocalDate, masuk: Boolean, n: Int = 4): List<Pair<String, Long>> {
    val a = awal.toString()
    val b = akhir.toString()
    val jumlah = LinkedHashMap<String, Long>()
    val nama = HashMap<String, String>()
    for (d in data) {
        if (d.tanggal >= a && d.tanggal <= b && d.masuk == masuk) {
            val kunci = d.sumber.trim().uppercase()
            jumlah[kunci] = (jumlah[kunci] ?: 0L) + d.jumlah
            nama.getOrPut(kunci) { d.sumber.trim() }
        }
    }
    return jumlah.entries.sortedByDescending { it.value }.take(n).map { (nama[it.key] ?: it.key) to it.value }
}

fun hitungStatistik(data: List<Catatan>, p: Periode, bulan: YearMonth, hariIni: LocalDate): Statistik {
    val (awal, akhir) = rentang(p, bulan, data, hariIni)
    val per = perHari(data, awal, akhir)
    val bucket = buatBucket(p, awal, akhir, per, hariIni)
    val masuk = bucket.sumOf { it.masuk }
    val keluar = bucket.sumOf { it.keluar }
    val hariMentah = if (!awal.isAfter(hariIni)) {
        ChronoUnit.DAYS.between(awal, minOf(akhir, hariIni)).toInt() + 1
    } else {
        ChronoUnit.DAYS.between(awal, akhir).toInt() + 1
    }
    val hari = maxOf(1, hariMentah)

    var sebelumMasuk: Long? = null
    var sebelumKeluar: Long? = null
    var namaSebelum = ""
    var pb: YearMonth? = null
    if (p == Periode.HARIAN || p == Periode.MINGGUAN) {
        pb = bulan.minusMonths(1)
        namaSebelum = BULAN[pb.monthValue - 1]
    } else if (p == Periode.BULANAN) {
        pb = YearMonth.of(bulan.year - 1, bulan.monthValue)
        namaSebelum = (bulan.year - 1).toString()
    }
    if (pb != null) {
        val (a2, b2) = rentang(p, pb, data, hariIni)
        val (mk2, kl2) = jumlahAwalan(perHari(data, a2, b2), "")
        sebelumMasuk = mk2
        sebelumKeluar = kl2
    }

    val boros = per.entries.maxByOrNull { it.value[1] }
    val terbaik = per.entries.maxByOrNull { it.value[0] }
    val judul = when (p) {
        Periode.HARIAN, Periode.MINGGUAN -> "${BULAN[bulan.monthValue - 1]} ${bulan.year}"
        Periode.BULANAN -> "Tahun ${bulan.year}"
        Periode.TAHUNAN -> if (awal.year == akhir.year) awal.year.toString() else "${awal.year}–${akhir.year}"
    }
    return Statistik(
        judul = judul, bucket = bucket, masuk = masuk, keluar = keluar, selisih = masuk - keluar,
        rasio = if (masuk > 0) (masuk - keluar).toDouble() / masuk * 100 else null,
        hari = hari, rataKeluar = keluar.toDouble() / hari, rataMasuk = masuk.toDouble() / hari,
        sebelumMasuk = sebelumMasuk, sebelumKeluar = sebelumKeluar, namaSebelum = namaSebelum,
        topMasuk = top(data, awal, akhir, true), topKeluar = top(data, awal, akhir, false),
        hariBoros = if (boros != null && boros.value[1] > 0) boros.key to boros.value[1] else null,
        hariTerbaik = if (terbaik != null && terbaik.value[0] > 0) terbaik.key to terbaik.value[0] else null,
    )
}

// ---------- anggaran ----------
data class BarisAnggaran(val nama: String, val warna: String, val anggaran: Long, val terpakai: Long)

fun hitungAnggaran(
    data: List<Catatan>, kategori: List<Kategori>, anggaran: Map<String, Long>, bulan: YearMonth,
): Pair<List<BarisAnggaran>, Long> {
    val awalan = bulan.toString()
    val namaKeluar = kategori.filter { it.jenis == JENIS_KELUAR }.map { it.nama }.toSet()
    val pakai = HashMap<String, Long>()
    var tanpa = 0L
    for (d in data) {
        if (d.masuk || !d.tanggal.startsWith(awalan)) continue
        if (d.kategori in namaKeluar) pakai[d.kategori] = (pakai[d.kategori] ?: 0L) + d.jumlah else tanpa += d.jumlah
    }
    val baris = kategori.filter { it.jenis == JENIS_KELUAR }.map {
        BarisAnggaran(it.nama, it.warna, anggaran[it.nama] ?: 0L, pakai[it.nama] ?: 0L)
    }
    val urut = baris.sortedWith(
        compareBy<BarisAnggaran>({ if (it.anggaran > 0) 0 else 1 }, {
            if (it.anggaran > 0) -(it.terpakai.toDouble() / it.anggaran) else -it.terpakai.toDouble()
        })
    )
    return urut to tanpa
}

/** (teks, melebihi) bila pengeluaran bulan itu mendekati/melewati anggaran kategorinya. */
fun peringatanAnggaran(item: Catatan, data: DataApp): Pair<String, Boolean>? {
    if (item.masuk) return null
    val batas = data.anggaran[item.kategori] ?: 0L
    if (item.kategori.isEmpty() || batas <= 0) return null
    val awalan = item.tanggal.substring(0, 7)
    val pakai = data.catatan.filter { !it.masuk && it.kategori == item.kategori && it.tanggal.startsWith(awalan) }.sumOf { it.jumlah }
    if (pakai > batas) return "Anggaran ${item.kategori} terlampaui ${rupiah(pakai - batas)}" to true
    if (pakai * 100 >= batas * 80) return "Anggaran ${item.kategori} sudah ${pakai * 100 / batas}% terpakai" to false
    return null
}

fun kategoriBersih(daftar: List<Kategori>): List<Kategori> {
    val hasil = ArrayList<Kategori>()
    val lihat = HashSet<Pair<String, String>>()
    for (k in daftar) {
        val nama = k.nama.trim().take(24)
        if (nama.isEmpty() || (k.jenis != JENIS_MASUK && k.jenis != JENIS_KELUAR)) continue
        if (!lihat.add(k.jenis to nama.lowercase())) continue
        val warna = if (Regex("#[0-9a-fA-F]{6}").matches(k.warna)) k.warna.lowercase() else WARNA_KATEGORI[hasil.size % WARNA_KATEGORI.size]
        hasil.add(Kategori(nama, warna, k.jenis))
    }
    return hasil
}

fun anggaranBersih(d: Map<String, Long>?, kategori: List<Kategori>): Map<String, Long> {
    if (d == null) return emptyMap()
    val keluar = kategori.filter { it.jenis == JENIS_KELUAR }.map { it.nama }.toSet()
    return d.filter { (k, v) -> k in keluar && v > 0 }
}
