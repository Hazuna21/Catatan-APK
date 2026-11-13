package com.catatan.pendapatan

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private fun tulisKeUri(ctx: Context, uri: android.net.Uri, teks: String, bom: Boolean): Boolean = try {
    ctx.contentResolver.openOutputStream(uri, "wt")?.use { out ->
        if (bom) out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        out.write(teks.toByteArray(Charsets.UTF_8))
    } != null
} catch (e: Exception) {
    false
}

private fun bacaDariUri(ctx: Context, uri: android.net.Uri): String? = try {
    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
} catch (e: Exception) {
    null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LainnyaScreen(vm: AppViewModel) {
    val p = LocalPalet.current
    val ctx = LocalContext.current
    var kelola by remember { mutableStateOf(false) }
    var pulihkan by remember { mutableStateOf(false) }
    var impor by remember { mutableStateOf<HasilImpor?>(null) }

    val eksporCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = tulisKeUri(ctx, uri, vm.teksCsv(), bom = true)
            vm.pesan.tryEmit(if (ok) Pesan("CSV berhasil diekspor.") else Pesan("Gagal menulis file.", galat = true))
        }
    }
    val eksporJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = tulisKeUri(ctx, uri, vm.teksJson(), bom = false)
            vm.pesan.tryEmit(if (ok) Pesan("Cadangan berhasil diekspor.") else Pesan("Gagal menulis file.", galat = true))
        }
    }
    val pilihBerkas = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val teks = bacaDariUri(ctx, uri)
            if (teks == null) {
                vm.pesan.tryEmit(Pesan("File tidak dapat dibaca.", galat = true))
            } else {
                try {
                    impor = bacaTeksImpor(teks)
                } catch (e: Exception) {
                    vm.pesan.tryEmit(Pesan(e.message ?: "Format file tidak dikenali.", galat = true))
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Tema warna")
            FlowRow(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for ((kunci, nama) in NAMA_TEMA) Chip(nama, aktif = vm.tema == kunci, warna = PALET.getValue(kunci).biru) { vm.ganti(kunci) }
            }
        }

        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Kategori")
            Text("Atur kategori pemasukan/pengeluaran, warna, dan anggaran bulanan.", color = p.abu, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            OutlinedButton(onClick = { kelola = true }, modifier = Modifier.fillMaxWidth()) { Text("Kelola kategori") }
        }

        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Data dan cadangan")
            Text(
                "Data disimpan di perangkat ini. Cadangan harian dibuat otomatis (30 hari terakhir).",
                color = p.abu, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { eksporCsv.launch("catatan-pendapatan-${LocalDate.now()}.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Ekspor CSV (Excel)") }
                Button(onClick = { eksporJson.launch("cadangan-pendapatan-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("Ekspor cadangan lengkap (.json)") }
                OutlinedButton(onClick = { pilihBerkas.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Impor dari file (CSV / JSON)") }
                OutlinedButton(onClick = { pulihkan = true }, modifier = Modifier.fillMaxWidth()) { Text("Pulihkan dari cadangan otomatis") }
            }
        }

        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Urungkan")
            Text("Batalkan perubahan terakhir (tambah, ubah, hapus, impor).", color = p.abu, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            OutlinedButton(onClick = { vm.urungkan() }, enabled = vm.bisaUrungkan, modifier = Modifier.fillMaxWidth()) { Text("Urungkan perubahan terakhir") }
        }
        Box(Modifier.height(24.dp))
    }

    if (kelola) DialogKelolaKategori(vm) { kelola = false }
    if (pulihkan) DialogPulihkan(vm) { pulihkan = false }
    val h = impor
    if (h != null) DialogImpor(vm, h) { impor = null }
}
