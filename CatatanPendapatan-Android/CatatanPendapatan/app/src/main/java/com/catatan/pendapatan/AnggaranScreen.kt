package com.catatan.pendapatan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnggaranScreen(vm: AppViewModel) {
    val p = LocalPalet.current
    val (baris, tanpa) = remember(vm.data, vm.bulan) {
        hitungAnggaran(vm.data.catatan, vm.data.kategori, vm.data.anggaran, vm.bulan)
    }
    var edit by remember { mutableStateOf<Kategori?>(null) }
    var kelola by remember { mutableStateOf(false) }

    val totalKeluar = baris.sumOf { it.terpakai } + tanpa
    val totalAnggaran = baris.sumOf { it.anggaran }
    val terpakaiBerAnggaran = baris.filter { it.anggaran > 0 }.sumOf { it.terpakai }
    val irisan = baris.filter { it.terpakai > 0 }.map { it.terpakai.toFloat() to hex(it.warna) }
        .let { if (tanpa > 0) it + (tanpa.toFloat() to hex(WARNA_TANPA)) else it }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NavBulan(
            "${BULAN[vm.bulan.monthValue - 1]} ${vm.bulan.year}",
            { vm.bulan = vm.bulan.minusMonths(1) }, { vm.bulan = vm.bulan.plusMonths(1) },
        )

        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Komposisi pengeluaran")
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                    Donat(irisan, p.header, Modifier.size(130.dp), tebal = 20.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = p.abu, fontSize = 11.sp)
                        Text(ringkas(totalKeluar), color = p.teks, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (irisan.isEmpty()) {
                        Text("Belum ada pengeluaran bulan ini.", color = p.abu, fontSize = 13.sp)
                    }
                    for (b in baris.filter { it.terpakai > 0 }) {
                        Legenda(b.nama, hex(b.warna), totalKeluar, b.terpakai)
                    }
                    if (tanpa > 0) Legenda("Tanpa kategori", hex(WARNA_TANPA), totalKeluar, tanpa)
                }
            }
        }

        if (totalAnggaran > 0) {
            Kartu(Modifier.fillMaxWidth()) {
                JudulBagian("Total anggaran")
                Text(
                    "${rupiah(terpakaiBerAnggaran)} dari ${rupiah(totalAnggaran)}",
                    color = p.abu, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                BatangProgres(terpakaiBerAnggaran.toFloat() / totalAnggaran, warnaProgres(terpakaiBerAnggaran, totalAnggaran), tinggi = 10.dp)
            }
        }

        JudulBagian("Per kategori")
        for (b in baris) {
            Kartu(Modifier.fillMaxWidth(), onClick = {
                edit = vm.data.kategori.firstOrNull { it.jenis == JENIS_KELUAR && it.nama == b.nama }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(hex(b.warna)))
                    Text(b.nama, color = p.teks, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text(
                        if (b.anggaran > 0) "${rupiah(b.terpakai)} / ${rupiah(b.anggaran)}" else rupiah(b.terpakai),
                        color = p.abu, fontSize = 12.sp,
                    )
                }
                if (b.anggaran > 0) {
                    val w = warnaProgres(b.terpakai, b.anggaran)
                    BatangProgres(b.terpakai.toFloat() / b.anggaran, w, Modifier.padding(top = 8.dp))
                    val sisa = b.anggaran - b.terpakai
                    Text(
                        if (sisa >= 0) "Sisa ${rupiah(sisa)}" else "Terlampaui ${rupiah(-sisa)}",
                        color = w, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text("Ketuk untuk mengatur anggaran", color = p.abu, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        OutlinedButton(onClick = { kelola = true }, modifier = Modifier.fillMaxWidth()) { Text("Kelola kategori") }
        Box(Modifier.height(88.dp))
    }

    if (edit != null) {
        DialogEditKategori(vm, edit, JENIS_KELUAR, fokusAnggaran = true) { edit = null }
    }
    if (kelola) DialogKelolaKategori(vm) { kelola = false }
}

@Composable
private fun warnaProgres(pakai: Long, batas: Long): Color {
    val p = LocalPalet.current
    return when {
        pakai > batas -> p.merah
        pakai * 100 >= batas * 80 -> p.kuning
        else -> p.hijau
    }
}

@Composable
private fun Legenda(nama: String, warna: Color, total: Long, nilai: Long) {
    val p = LocalPalet.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(warna))
        Text(nama, color = p.teks, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 6.dp))
        Text(if (total > 0) "${nilai * 100 / total}%" else "", color = p.abu, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}
