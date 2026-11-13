package com.catatan.pendapatan

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HasilImpor(
    val item: List<Catatan>,
    val kategori: List<Kategori>?,
    val anggaran: Map<String, Long>?,
    val gagal: Int,
)

data class InfoCadangan(val file: File, val jenis: String, val waktu: Long)

private const val MAKS_CADANGAN = 30
private const val MAKS_PRA_IMPOR = 10
private val KOLOM_CSV = listOf("Tanggal", "Jenis", "Kategori", "Sumber", "Jumlah", "Catatan")

private fun JSONObject.teks(k: String): String = if (isNull(k)) "" else optString(k, "")

// ---------- JSON ----------
fun paketJson(d: DataApp): String {
    val o = JSONObject()
    o.put("aplikasi", "catatan-pendapatan")
    o.put("versi", 2)
    o.put("dibuat", LocalDateTime.now().withNano(0).toString())
    val arr = JSONArray()
    for (c in d.catatan) {
        val x = JSONObject()
        x.put("jenis", c.jenis)
        x.put("tanggal", c.tanggal)
        x.put("sumber", c.sumber)
        x.put("jumlah", c.jumlah)
        x.put("catatan", c.catatan)
        x.put("kategori", c.kategori)
        arr.put(x)
    }
    o.put("data", arr)
    val kat = JSONArray()
    for (k in d.kategori) {
        val x = JSONObject()
        x.put("nama", k.nama)
        x.put("warna", k.warna)
        x.put("jenis", k.jenis)
        kat.put(x)
    }
    o.put("kategori", kat)
    val ang = JSONObject()
    for ((k, v) in d.anggaran) ang.put(k, v)
    o.put("anggaran", ang)
    return o.toString(2)
}

private fun normalisasi(o: JSONObject): Catatan? {
    return try {
        val tgl = LocalDate.parse(o.teks("tanggal").take(10)).toString()
        val jumlah = o.optLong("jumlah", 0L)
        val sumber = o.teks("sumber").trim()
        if (jumlah <= 0 || sumber.isEmpty()) return null
        val jenis = if (o.teks("jenis") == JENIS_KELUAR) JENIS_KELUAR else JENIS_MASUK
        Catatan(0L, jenis, tgl, sumber.take(80), jumlah, o.teks("catatan").trim(), o.teks("kategori").trim().take(24))
    } catch (e: Exception) {
        null
    }
}

/** Baca cadangan lengkap (objek) atau berkas data sederhana (array). */
fun bacaJson(teksMentah: String): HasilImpor {
    val teks = teksMentah.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
    val nilai = try {
        JSONTokener(teks).nextValue()
    } catch (e: Exception) {
        throw IllegalArgumentException("Format file tidak dikenali.")
    }
    var data: JSONArray? = null
    var kat: JSONArray? = null
    var ang: JSONObject? = null
    if (nilai is JSONArray) {
        data = nilai
    } else if (nilai is JSONObject) {
        data = nilai.optJSONArray("data")
        kat = nilai.optJSONArray("kategori")
        ang = nilai.optJSONObject("anggaran")
    }
    if (data == null) throw IllegalArgumentException("Format file tidak dikenali.")
    val item = ArrayList<Catatan>()
    var gagal = 0
    for (i in 0 until data.length()) {
        val o = data.optJSONObject(i)
        val n = if (o != null) normalisasi(o) else null
        if (n != null) item.add(n) else gagal++
    }
    var daftarKat: List<Kategori>? = null
    if (kat != null) {
        val tmp = ArrayList<Kategori>()
        for (i in 0 until kat.length()) {
            val o = kat.optJSONObject(i) ?: continue
            tmp.add(Kategori(o.teks("nama"), o.teks("warna"), o.teks("jenis")))
        }
        daftarKat = kategoriBersih(tmp)
    }
    var daftarAng: Map<String, Long>? = null
    if (ang != null) {
        val m = HashMap<String, Long>()
        val it = ang.keys()
        while (it.hasNext()) {
            val k = it.next()
            m[k] = ang.optLong(k, 0L)
        }
        daftarAng = m
    }
    return HasilImpor(item, daftarKat, daftarAng, gagal)
}

fun dataDariHasil(h: HasilImpor): DataApp {
    val kat = h.kategori ?: KATEGORI_BAWAAN
    return DataApp(
        catatan = h.item.mapIndexed { i, c -> c.copy(id = i + 1L) },
        kategori = kat,
        anggaran = anggaranBersih(h.anggaran, kat),
    )
}

// ---------- CSV ----------
fun tulisCsv(data: List<Catatan>): String {
    fun sel(s: String): String =
        if (s.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    val sb = StringBuilder()
    sb.append(KOLOM_CSV.joinToString(";")).append("\r\n")
    for (c in data.sortedWith(compareBy<Catatan>({ it.tanggal }, { it.id }))) {
        sb.append(
            listOf(c.tanggal, namaJenis(c.jenis), c.kategori, c.sumber, c.jumlah.toString(), c.catatan)
                .joinToString(";") { sel(it) }
        ).append("\r\n")
    }
    return sb.toString()
}

private fun barisCsv(teks: String, pemisah: Char): List<List<String>> {
    val hasil = ArrayList<List<String>>()
    var baris = ArrayList<String>()
    val sel = StringBuilder()
    var kutip = false
    var i = 0
    while (i < teks.length) {
        val c = teks[i]
        if (kutip) {
            if (c == '"') {
                if (i + 1 < teks.length && teks[i + 1] == '"') {
                    sel.append('"')
                    i++
                } else {
                    kutip = false
                }
            } else {
                sel.append(c)
            }
        } else if (c == '"') {
            kutip = true
        } else if (c == pemisah) {
            baris.add(sel.toString())
            sel.setLength(0)
        } else if (c == '\r') {
            // abaikan
        } else if (c == '\n') {
            baris.add(sel.toString())
            sel.setLength(0)
            hasil.add(baris)
            baris = ArrayList()
        } else {
            sel.append(c)
        }
        i++
    }
    if (sel.isNotEmpty() || baris.isNotEmpty()) {
        baris.add(sel.toString())
        hasil.add(baris)
    }
    return hasil.filter { b -> b.any { it.isNotBlank() } }
}

private val SINONIM = mapOf(
    "tanggal" to "tanggal", "tgl" to "tanggal", "date" to "tanggal",
    "jenis" to "jenis", "tipe" to "jenis", "type" to "jenis",
    "kategori" to "kategori", "category" to "kategori",
    "sumber" to "sumber", "keperluan" to "sumber", "deskripsi" to "sumber", "source" to "sumber",
    "jumlah" to "jumlah", "nominal" to "jumlah", "amount" to "jumlah",
    "catatan" to "catatan", "keterangan" to "catatan", "note" to "catatan", "notes" to "catatan",
)

/** 'Rp 1.500.000', '1500000,00', '-25000' -> (1500000, false) */
private fun angkaCsv(teks: String): Pair<Long, Boolean>? {
    var s = teks.trim().replace("Rp", "").replace("rp", "").replace(" ", "")
    val negatif = s.startsWith("-") || (s.startsWith("(") && s.endsWith(")"))
    s = s.replace(Regex("[.,]\\d{1,2}$"), "")
    val digit = s.filter { it.isDigit() }
    if (digit.isEmpty() || digit.length > 15) return null
    return digit.toLong() to negatif
}

private fun tanggalCsv(teks: String): LocalDate? {
    val t = teks.trim()
    return try {
        LocalDate.parse(t.take(10))
    } catch (e: Exception) {
        parseTanggal(t.split(" ")[0])
    }
}

fun bacaCsv(teksMentah: String): HasilImpor {
    val teks = teksMentah.trimStart('\uFEFF')
    val barisPertama = teks.lineSequence().firstOrNull() ?: ""
    val pemisah = listOf(';', ',', '\t').maxByOrNull { p -> barisPertama.count { it == p } }.let {
        if (it == null || barisPertama.count { c -> c == it } == 0) ';' else it
    }
    val baris = barisCsv(teks, pemisah)
    if (baris.isEmpty()) throw IllegalArgumentException("File kosong.")
    val kepala = baris[0].map { SINONIM[it.trim().lowercase()] }
    if ("tanggal" !in kepala || "jumlah" !in kepala || ("sumber" !in kepala && "kategori" !in kepala)) {
        throw IllegalArgumentException("Baris judul tidak ditemukan.\nGunakan kolom: Tanggal; Jenis; Kategori; Sumber; Jumlah; Catatan.")
    }
    val item = ArrayList<Catatan>()
    var gagal = 0
    for (b in baris.drop(1)) {
        val r = HashMap<String, String>()
        for ((i, c) in b.withIndex()) {
            val k = kepala.getOrNull(i)
            if (k != null) r[k] = c.trim()
        }
        val tgl = tanggalCsv(r["tanggal"] ?: "")
        val angka = angkaCsv(r["jumlah"] ?: "")
        if (tgl == null || angka == null) {
            gagal++
            continue
        }
        val sumber = (r["sumber"]?.takeIf { it.isNotEmpty() } ?: r["kategori"] ?: "")
        if (angka.first <= 0 || sumber.isEmpty()) {
            gagal++
            continue
        }
        val j = (r["jenis"] ?: "").lowercase()
        val jenis = when {
            listOf("keluar", "pengeluaran", "expense", "debit").any { it in j } -> JENIS_KELUAR
            listOf("masuk", "income", "kredit").any { it in j } -> JENIS_MASUK
            angka.second -> JENIS_KELUAR
            else -> JENIS_MASUK
        }
        item.add(Catatan(0L, jenis, tgl.toString(), sumber.take(80), angka.first, r["catatan"] ?: "", (r["kategori"] ?: "").take(24)))
    }
    return HasilImpor(item, null, null, gagal)
}

fun bacaTeksImpor(teks: String): HasilImpor {
    val awal = teks.trimStart('\uFEFF', ' ', '\n', '\r', '\t').firstOrNull()
    return if (awal == '[' || awal == '{') bacaJson(teks) else bacaCsv(teks)
}

// ---------- penyimpanan di perangkat ----------
class Penyimpanan(private val ctx: Context) {
    private val berkas = File(ctx.filesDir, "pendapatan.json")
    private val folderCadangan = File(ctx.filesDir, "backup")
    private val prefs = ctx.getSharedPreferences("pengaturan", Context.MODE_PRIVATE)

    fun muatTema(): String = prefs.getString("tema", "terang") ?: "terang"
    fun simpanTema(nama: String) {
        prefs.edit().putString("tema", nama).apply()
    }

    /** (data, rusak). rusak = true bila berkas ada tetapi tidak terbaca. */
    fun muat(): Pair<DataApp, Boolean> {
        if (!berkas.exists()) return DataApp() to false
        return try {
            dataDariHasil(bacaJson(berkas.readText(Charsets.UTF_8))) to false
        } catch (e: Exception) {
            try {
                folderCadangan.mkdirs()
                berkas.copyTo(File(folderCadangan, "rusak-" + stempel() + ".json"), overwrite = true)
                putar("rusak-", 5)
            } catch (e: Exception) {
            }
            DataApp() to true
        }
    }

    fun simpan(d: DataApp) {
        try {
            val sementara = File(ctx.filesDir, "pendapatan.json.tmp")
            sementara.writeText(paketJson(d), Charsets.UTF_8)
            Files.move(sementara.toPath(), berkas.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
        }
    }

    private fun stempel(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

    private fun putar(awalan: String, maks: Int) {
        val daftar = folderCadangan.listFiles { f -> f.name.startsWith(awalan) && f.name.endsWith(".json") } ?: return
        daftar.sortedBy { it.name }.dropLast(maks).forEach { it.delete() }
    }

    /** awalan "pendapatan" = satu berkas per hari; awalan lain = berkas baru bercap waktu. */
    fun cadanganOtomatis(d: DataApp, awalan: String = "pendapatan"): File? {
        if (d.catatan.isEmpty() && awalan == "pendapatan") return null
        return try {
            folderCadangan.mkdirs()
            val nama = if (awalan == "pendapatan") "pendapatan-${LocalDate.now()}.json" else "$awalan-${stempel()}.json"
            val f = File(folderCadangan, nama)
            f.writeText(paketJson(d), Charsets.UTF_8)
            putar("pendapatan-", MAKS_CADANGAN)
            putar("pra-impor-", MAKS_PRA_IMPOR)
            f
        } catch (e: Exception) {
            null
        }
    }

    fun daftarCadangan(): List<InfoCadangan> {
        val files = folderCadangan.listFiles { f -> f.name.endsWith(".json") } ?: return emptyList()
        return files.map {
            val jenis = when {
                it.name.startsWith("pendapatan-") -> "Harian"
                it.name.startsWith("pra-impor-") -> "Sebelum impor/pulihkan"
                else -> "Berkas rusak (salinan)"
            }
            InfoCadangan(it, jenis, it.lastModified())
        }.sortedByDescending { it.waktu }
    }
}
