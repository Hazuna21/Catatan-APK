package com.catatan.pendapatan

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

enum class Halaman(val judul: String) {
    STATISTIK("Statistik"), DAFTAR("Daftar"), KALENDER("Kalender"), ANGGARAN("Anggaran"), LAINNYA("Lainnya")
}

data class Pesan(val teks: String, val galat: Boolean = false, val urungkan: Boolean = false)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val simpanan = Penyimpanan(app)
    private val kunci = Mutex()
    private val riwayat = ArrayDeque<DataApp>()
    private var dataRusak = false

    var data by mutableStateOf(DataApp())
        private set
    var tema by mutableStateOf(simpanan.muatTema())
        private set
    var bulan by mutableStateOf(YearMonth.now())
    var periode by mutableStateOf(Periode.HARIAN)
    var halaman by mutableStateOf(Halaman.STATISTIK)
    var bisaUrungkan by mutableStateOf(false)
        private set

    val pesan = MutableSharedFlow<Pesan>(extraBufferCapacity = 8)

    init {
        val (d, rusak) = simpanan.muat()
        data = d
        dataRusak = rusak
        if (rusak) {
            pesan.tryEmit(Pesan("Data lama tidak terbaca. Salinannya disimpan di cadangan (Berkas rusak).", galat = true))
        }
    }

    fun ganti(nama: String) {
        tema = nama
        simpanan.simpanTema(nama)
    }

    // ---------- penyimpanan ----------
    private fun simpanKeDisk(d: DataApp) {
        val rusak = dataRusak
        viewModelScope.launch(Dispatchers.IO) {
            kunci.withLock {
                simpanan.simpan(d)
                if (!rusak) simpanan.cadanganOtomatis(d)
            }
        }
    }

    private fun terapkan(baru: DataApp) {
        riwayat.addLast(data)
        while (riwayat.size > 50) riwayat.removeFirst()
        bisaUrungkan = true
        data = baru
        simpanKeDisk(baru)
    }

    fun urungkan() {
        val sebelum = riwayat.removeLastOrNull() ?: return
        data = sebelum
        bisaUrungkan = riwayat.isNotEmpty()
        simpanKeDisk(sebelum)
        pesan.tryEmit(Pesan("Perubahan terakhir diurungkan."))
    }

    // ---------- catatan ----------
    fun tambah(jenis: String, tanggal: LocalDate, sumber: String, jumlah: Long, catatan: String, kategori: String): Pesan? {
        val id = (data.catatan.maxOfOrNull { it.id } ?: 0L) + 1
        val c = Catatan(id, jenis, tanggal.toString(), sumber.trim(), jumlah, catatan.trim(), kategori)
        terapkan(data.copy(catatan = data.catatan + c))
        val w = peringatanAnggaran(c, data)
        pesan.tryEmit(Pesan("Catatan disimpan."))
        return w?.let { Pesan(it.first, galat = it.second) }?.also { pesan.tryEmit(it) }
    }

    fun ubah(c: Catatan) {
        terapkan(data.copy(catatan = data.catatan.map { if (it.id == c.id) c else it }))
        pesan.tryEmit(Pesan("Catatan diperbarui.", urungkan = true))
        peringatanAnggaran(c, data)?.let { pesan.tryEmit(Pesan(it.first, galat = it.second)) }
    }

    fun hapus(id: Long) {
        terapkan(data.copy(catatan = data.catatan.filter { it.id != id }))
        pesan.tryEmit(Pesan("Catatan dihapus.", urungkan = true))
    }

    fun daftarSumber(jenis: String): List<String> =
        data.catatan.filter { it.jenis == jenis }.groupingBy { it.sumber }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }

    fun kategoriUntukSumber(sumber: String, jenis: String): String {
        val nama = data.kategori.filter { it.jenis == jenis }.map { it.nama }.toSet()
        return data.catatan.sortedByDescending { it.id }
            .firstOrNull { it.jenis == jenis && it.sumber.equals(sumber.trim(), true) && it.kategori in nama }
            ?.kategori ?: ""
    }

    // ---------- kategori ----------
    /** Mengembalikan pesan galat, atau null bila berhasil. */
    fun simpanKategori(lama: Kategori?, nama: String, warna: String, jenis: String, anggaran: Long): String? {
        val n = nama.trim().take(24)
        if (n.isEmpty()) return "Nama kategori tidak boleh kosong."
        if (data.kategori.any { it != lama && it.jenis == jenis && it.nama.equals(n, true) }) return "Kategori \"$n\" sudah ada."
        val baru = Kategori(n, warna, jenis)
        val daftar = if (lama == null) data.kategori + baru else data.kategori.map { if (it == lama) baru else it }
        var catatan = data.catatan
        val ang = data.anggaran.toMutableMap()
        if (lama != null) {
            if (lama.nama != n || lama.jenis != jenis) {
                catatan = catatan.map {
                    if (it.jenis == lama.jenis && it.kategori == lama.nama) {
                        it.copy(kategori = if (jenis == lama.jenis) n else "")
                    } else it
                }
            }
            if (lama.jenis == JENIS_KELUAR) ang.remove(lama.nama)
        }
        if (jenis == JENIS_KELUAR && anggaran > 0) ang[n] = anggaran
        terapkan(DataApp(catatan, daftar, anggaranBersih(ang, daftar)))
        return null
    }

    fun hapusKategori(k: Kategori) {
        val daftar = data.kategori.filter { it != k }
        val catatan = data.catatan.map { if (it.jenis == k.jenis && it.kategori == k.nama) it.copy(kategori = "") else it }
        val ang = data.anggaran.toMutableMap()
        if (k.jenis == JENIS_KELUAR) ang.remove(k.nama)
        terapkan(DataApp(catatan, daftar, anggaranBersih(ang, daftar)))
        pesan.tryEmit(Pesan("Kategori dihapus.", urungkan = true))
    }

    // ---------- ekspor / impor / cadangan ----------
    fun teksJson(): String = paketJson(data)
    fun teksCsv(): String = tulisCsv(data.catatan)

    /** Mengembalikan (jumlah ditambah, jumlah duplikat dilewati). */
    fun impor(hasil: HasilImpor, ganti: Boolean): Pair<Int, Int> {
        simpanan.cadanganOtomatis(data, "pra-impor")
        var kategori = data.kategori
        var anggaran = data.anggaran
        var catatan: List<Catatan> = if (ganti) emptyList() else data.catatan
        val hk = hasil.kategori
        if (ganti && hk != null) {
            kategori = hk
            anggaran = anggaranBersih(hasil.anggaran, hk)
        }
        val ada = HashSet<List<Any>>()
        for (d in catatan) ada.add(listOf(d.tanggal, d.jenis, d.sumber, d.jumlah, d.catatan))
        var nomor = catatan.maxOfOrNull { it.id } ?: 0L
        val baru = ArrayList<Catatan>()
        val kat = kategori.toMutableList()
        var tambah = 0
        var lewat = 0
        for (it0 in hasil.item) {
            val kunciItem = listOf<Any>(it0.tanggal, it0.jenis, it0.sumber, it0.jumlah, it0.catatan)
            if (!ganti && kunciItem in ada) {
                lewat++
                continue
            }
            var item = it0
            if (item.kategori.isNotEmpty()) {
                var cocok = kat.firstOrNull { it.jenis == item.jenis && it.nama.equals(item.kategori, true) }
                if (cocok == null) {
                    cocok = Kategori(item.kategori, WARNA_KATEGORI[kat.size % WARNA_KATEGORI.size], item.jenis)
                    kat.add(cocok)
                }
                item = item.copy(kategori = cocok.nama)
            }
            nomor++
            baru.add(item.copy(id = nomor))
            ada.add(kunciItem)
            tambah++
        }
        catatan = catatan + baru
        dataRusak = false
        terapkan(DataApp(catatan, kat, anggaran))
        return tambah to lewat
    }

    fun daftarCadangan(): List<InfoCadangan> = simpanan.daftarCadangan()

    /** Mengembalikan pesan galat, atau null bila berhasil. */
    fun pulihkan(file: File): String? {
        return try {
            val hasil = bacaJson(file.readText(Charsets.UTF_8))
            impor(hasil, true)
            null
        } catch (e: Exception) {
            e.message ?: "Cadangan tidak dapat dibaca."
        }
    }
}
