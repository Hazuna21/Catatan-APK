package com.catatan.pendapatan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

/** Menampilkan angka dengan pemisah ribuan titik (1.500.000) tanpa mengubah isi teks aslinya. */
private object TransformasiRibuan : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val sb = StringBuilder()
        for (i in d.indices) {
            if (i > 0 && (d.length - i) % 3 == 0) sb.append('.')
            sb.append(d[i])
        }
        val out = sb.toString()
        val peta = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var titik = 0
                for (i in 1 until minOf(offset, d.length)) {
                    if ((d.length - i) % 3 == 0) titik++
                }
                return offset + titik
            }

            override fun transformedToOriginal(offset: Int): Int {
                val titik = out.take(offset).count { it == '.' }
                return (offset - titik).coerceIn(0, d.length)
            }
        }
        return TransformedText(AnnotatedString(out), peta)
    }
}

@Composable
private fun KerangkaDialog(onTutup: () -> Unit, judul: String, isi: @Composable () -> Unit) {
    val p = LocalPalet.current
    Dialog(onDismissRequest = onTutup, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(22.dp), color = p.putih,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(judul, color = p.teks, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                isi()
            }
        }
    }
}

@Composable
private fun Galat(teks: String?) {
    if (teks != null) Text(teks, color = LocalPalet.current.merah, fontSize = 13.sp)
}

// ======================================================================
//  Tambah / ubah catatan
// ======================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogCatatan(vm: AppViewModel, awal: Catatan?, tanggalAwal: LocalDate?, onTutup: () -> Unit) {
    val p = LocalPalet.current
    val tglAwal = if (awal != null) LocalDate.parse(awal.tanggal) else (tanggalAwal ?: LocalDate.now())
    var jenis by remember { mutableStateOf(awal?.jenis ?: JENIS_MASUK) }
    var tanggalTeks by remember { mutableStateOf(isiTanggal(tglAwal)) }
    var sumber by remember { mutableStateOf(awal?.sumber ?: "") }
    var jumlah by remember { mutableStateOf(awal?.jumlah?.toString() ?: "") }
    var catatan by remember { mutableStateOf(awal?.catatan ?: "") }
    var kategori by remember { mutableStateOf(awal?.kategori ?: "") }
    var galat by remember { mutableStateOf<String?>(null) }

    val daftarKategori = vm.data.kategori.filter { it.jenis == jenis }
    if (kategori.isNotEmpty() && daftarKategori.none { it.nama == kategori }) kategori = ""
    val saran = vm.daftarSumber(jenis).filter {
        sumber.isBlank() || (it.contains(sumber, true) && !it.equals(sumber, true))
    }.take(6)

    fun geserTanggal(hari: Long) {
        val t = parseTanggal(tanggalTeks) ?: LocalDate.now()
        tanggalTeks = isiTanggal(t.plusDays(hari))
    }

    fun simpan(lagi: Boolean) {
        val tgl = parseTanggal(tanggalTeks)
        val jml = parseJumlah(jumlah)
        when {
            tgl == null -> galat = "Tanggal tidak valid. Contoh: 09/11/2026 atau cukup 9."
            sumber.isBlank() -> galat = "Sumber / keperluan tidak boleh kosong."
            jml == null -> galat = "Jumlah harus berupa angka lebih dari 0."
            else -> {
                val kat = kategori.ifEmpty { vm.kategoriUntukSumber(sumber, jenis) }
                if (awal == null) {
                    vm.tambah(jenis, tgl, sumber, jml, catatan, kat)
                } else {
                    vm.ubah(Catatan(awal.id, jenis, tgl.toString(), sumber.trim(), jml, catatan.trim(), kat))
                }
                if (lagi) {
                    sumber = ""
                    jumlah = ""
                    catatan = ""
                    kategori = ""
                    galat = null
                } else {
                    onTutup()
                }
            }
        }
    }

    KerangkaDialog(onTutup, if (awal == null) "Tambah catatan" else "Ubah catatan") {
        Segmen(
            listOf(JENIS_MASUK to "Pemasukan", JENIS_KELUAR to "Pengeluaran"), jenis,
            { jenis = it },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { geserTanggal(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Hari sebelumnya") }
            OutlinedTextField(
                value = tanggalTeks, onValueChange = { tanggalTeks = it }, singleLine = true,
                label = { Text("Tanggal") }, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { geserTanggal(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Hari berikutnya") }
        }
        TextButton(onClick = { tanggalTeks = isiTanggal(LocalDate.now()) }) { Text("Hari ini") }
        OutlinedTextField(
            value = sumber, onValueChange = { sumber = it.take(80) }, singleLine = true,
            label = { Text(if (jenis == JENIS_MASUK) "Sumber" else "Keperluan") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (saran.isNotEmpty()) {
            Text("Pilih cepat", color = p.abu, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in saran) Chip(if (s.length > 16) s.take(15) + "…" else s, aktif = false) { sumber = s }
            }
        }
        OutlinedTextField(
            value = jumlah, onValueChange = { jumlah = it.filter { c -> c.isDigit() }.take(13) },
            singleLine = true, label = { Text("Jumlah (Rp)") }, visualTransformation = TransformasiRibuan,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        if (daftarKategori.isNotEmpty()) {
            Text("Kategori", color = p.abu, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in daftarKategori) {
                    Chip(k.nama, aktif = kategori == k.nama, warna = hex(k.warna)) {
                        kategori = if (kategori == k.nama) "" else k.nama
                    }
                }
            }
        }
        OutlinedTextField(
            value = catatan, onValueChange = { catatan = it }, label = { Text("Catatan (opsional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Galat(galat)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (awal != null) {
                TextButton(onClick = { vm.hapus(awal.id); onTutup() }, colors = ButtonDefaults.textButtonColors(contentColor = p.merah)) { Text("Hapus") }
                Spacer(Modifier.weight(1f))
            }
            TextButton(onClick = onTutup) { Text("Batal") }
            if (awal == null) OutlinedButton(onClick = { simpan(true) }) { Text("Simpan +") }
            Button(onClick = { simpan(false) }) { Text("Simpan") }
        }
    }
}

// ======================================================================
//  Kategori
// ======================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogEditKategori(
    vm: AppViewModel, awal: Kategori?, jenisAwal: String, fokusAnggaran: Boolean = false, onTutup: () -> Unit,
) {
    val p = LocalPalet.current
    var nama by remember { mutableStateOf(awal?.nama ?: "") }
    var warna by remember { mutableStateOf(awal?.warna ?: WARNA_KATEGORI[vm.data.kategori.size % WARNA_KATEGORI.size]) }
    var jenis by remember { mutableStateOf(awal?.jenis ?: jenisAwal) }
    var anggaran by remember {
        mutableStateOf(if (awal != null && awal.jenis == JENIS_KELUAR) (vm.data.anggaran[awal.nama] ?: 0L).let { if (it > 0) it.toString() else "" } else "")
    }
    var galat by remember { mutableStateOf<String?>(null) }

    KerangkaDialog(onTutup, if (awal == null) "Kategori baru" else "Ubah kategori") {
        Segmen(listOf(JENIS_MASUK to "Pemasukan", JENIS_KELUAR to "Pengeluaran"), jenis, { jenis = it })
        OutlinedTextField(
            value = nama, onValueChange = { nama = it.take(24) }, singleLine = true,
            label = { Text("Nama kategori") }, modifier = Modifier.fillMaxWidth(),
        )
        Text("Warna", color = p.abu, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (w in WARNA_KATEGORI) {
                val aktif = w.equals(warna, true)
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(hex(w))
                        .border(if (aktif) 3.dp else 0.dp, if (aktif) p.teks else hex(w), CircleShape)
                        .clickable { warna = w }
                )
            }
        }
        if (jenis == JENIS_KELUAR) {
            OutlinedTextField(
                value = anggaran, onValueChange = { anggaran = it.filter { c -> c.isDigit() }.take(13) },
                singleLine = true, label = { Text("Anggaran per bulan (Rp, kosongkan bila tanpa batas)") },
                visualTransformation = TransformasiRibuan, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Galat(galat)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (awal != null) {
                TextButton(onClick = { vm.hapusKategori(awal); onTutup() }, colors = ButtonDefaults.textButtonColors(contentColor = p.merah)) { Text("Hapus") }
                Spacer(Modifier.weight(1f))
            }
            TextButton(onClick = onTutup) { Text("Batal") }
            Button(onClick = {
                val g = vm.simpanKategori(awal, nama, warna, jenis, anggaran.toLongOrNull() ?: 0L)
                if (g == null) onTutup() else galat = g
            }) { Text("Simpan") }
        }
    }
}

@Composable
fun DialogKelolaKategori(vm: AppViewModel, onTutup: () -> Unit) {
    val p = LocalPalet.current
    var jenis by remember { mutableStateOf(JENIS_KELUAR) }
    var edit by remember { mutableStateOf<Kategori?>(null) }
    var baru by remember { mutableStateOf(false) }
    val daftar = vm.data.kategori.filter { it.jenis == jenis }

    KerangkaDialog(onTutup, "Kelola kategori") {
        Segmen(listOf(JENIS_KELUAR to "Pengeluaran", JENIS_MASUK to "Pemasukan"), jenis, { jenis = it })
        if (daftar.isEmpty()) Text("Belum ada kategori.", color = p.abu, fontSize = 13.sp)
        for (k in daftar) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.input).clickable { edit = k }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(hex(k.warna)))
                Text(k.nama, color = p.teks, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                val a = vm.data.anggaran[k.nama] ?: 0L
                if (k.jenis == JENIS_KELUAR && a > 0) Text(rupiah(a), color = p.abu, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = { baru = true }) { Text("+ Kategori baru") }
            Spacer(Modifier.weight(1f))
            Button(onClick = onTutup) { Text("Selesai") }
        }
    }
    if (edit != null) DialogEditKategori(vm, edit, jenis) { edit = null }
    if (baru) DialogEditKategori(vm, null, jenis) { baru = false }
}

// ======================================================================
//  Impor dan pulihkan
// ======================================================================
@Composable
fun DialogImpor(vm: AppViewModel, hasil: HasilImpor, onTutup: () -> Unit) {
    val p = LocalPalet.current
    KerangkaDialog(onTutup, "Impor data") {
        Text("Ditemukan ${hasil.item.size} catatan" + (if (hasil.gagal > 0) " (${hasil.gagal} baris dilewati karena tidak valid)." else "."), color = p.teks, fontSize = 14.sp)
        Text("Cadangan otomatis dibuat sebelum impor, jadi Anda bisa memulihkannya bila perlu.", color = p.abu, fontSize = 12.sp)
        Button(onClick = {
            val (t, l) = vm.impor(hasil, false)
            val akhiran = if (l > 0) ", $l duplikat dilewati." else "."
            vm.pesan.tryEmit(Pesan("Ditambahkan $t catatan$akhiran", urungkan = true))
            onTutup()
        }, modifier = Modifier.fillMaxWidth()) { Text("Tambahkan ke data saya") }
        OutlinedButton(onClick = {
            val (t, _) = vm.impor(hasil, true)
            vm.pesan.tryEmit(Pesan("Data diganti dengan $t catatan.", urungkan = true))
            onTutup()
        }, modifier = Modifier.fillMaxWidth()) { Text("Ganti seluruh data") }
        TextButton(onClick = onTutup, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
    }
}

@Composable
fun DialogPulihkan(vm: AppViewModel, onTutup: () -> Unit) {
    val p = LocalPalet.current
    val daftar = remember { vm.daftarCadangan() }
    var galat by remember { mutableStateOf<String?>(null) }
    KerangkaDialog(onTutup, "Pulihkan dari cadangan") {
        Text("Memulihkan akan mengganti seluruh data saat ini. Data sekarang dicadangkan dulu.", color = p.abu, fontSize = 12.sp)
        if (daftar.isEmpty()) Text("Belum ada cadangan otomatis.", color = p.abu, fontSize = 13.sp)
        for (c in daftar) {
            val waktu = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy HH:mm").format(
                java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(c.waktu), java.time.ZoneId.systemDefault())
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.input)
                    .clickable {
                        val g = vm.pulihkan(c.file)
                        if (g == null) {
                            vm.pesan.tryEmit(Pesan("Data dipulihkan.", urungkan = true))
                            onTutup()
                        } else galat = g
                    }.padding(12.dp)
            ) {
                Text(c.jenis, color = p.teks, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("$waktu  ·  ${c.file.name}", color = p.abu, fontSize = 11.sp)
            }
        }
        Galat(galat)
        TextButton(onClick = onTutup, modifier = Modifier.fillMaxWidth()) { Text("Tutup") }
    }
}
