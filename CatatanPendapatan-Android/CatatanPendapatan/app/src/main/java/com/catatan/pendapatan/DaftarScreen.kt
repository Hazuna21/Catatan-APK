package com.catatan.pendapatan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private fun teksCari(d: Catatan): String {
    val t = LocalDate.parse(d.tanggal)
    return listOf(d.sumber, d.kategori, d.catatan, d.jumlah.toString(), formatAngka(d.jumlah), d.tanggal, isiTanggal(t), namaJenis(d.jenis))
        .joinToString(" ").lowercase()
}

@Composable
fun BarisCatatan(c: Catatan, data: DataApp, onKlik: () -> Unit) {
    val p = LocalPalet.current
    val warna = warnaKategori(data, c.kategori, c.jenis)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.putih).clickable(onClick = onKlik).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (c.kategori.isEmpty()) p.garis else warna))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(c.sumber, color = p.teks, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOf(c.kategori, c.catatan).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, color = p.abu, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            (if (c.masuk) "+" else "-") + rupiah(c.jumlah),
            color = if (c.masuk) p.hijau else p.merah, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}

@Composable
fun DaftarScreen(vm: AppViewModel, onEdit: (Catatan) -> Unit) {
    val p = LocalPalet.current
    var kata by remember { mutableStateOf("") }
    val mencari = kata.isNotBlank()
    val awalan = vm.bulan.toString()
    val tampil = remember(vm.data, vm.bulan, kata) {
        val k = kata.trim().lowercase()
        vm.data.catatan
            .filter { if (k.isEmpty()) it.tanggal.startsWith(awalan) else teksCari(it).contains(k) }
            .sortedWith(compareByDescending<Catatan> { it.tanggal }.thenByDescending { it.id })
    }
    val masuk = tampil.filter { it.masuk }.sumOf { it.jumlah }
    val keluar = tampil.filter { !it.masuk }.sumOf { it.jumlah }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = kata, onValueChange = { kata = it }, singleLine = true,
            placeholder = { Text("Cari catatan, kategori, jumlah, tanggal…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (mencari) IconButton(onClick = { kata = "" }) { Icon(Icons.Default.Close, "Hapus pencarian") } },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        )
        if (!mencari) {
            NavBulan(
                "${BULAN[vm.bulan.monthValue - 1]} ${vm.bulan.year}",
                { vm.bulan = vm.bulan.minusMonths(1) }, { vm.bulan = vm.bulan.plusMonths(1) },
            )
        } else {
            Text("Hasil pencarian di semua bulan", color = p.abu, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Masuk ${rupiah(masuk)}", color = p.hijau, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Keluar ${rupiah(keluar)}", color = p.merah, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (tampil.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                Text(if (mencari) "Tidak ada catatan yang cocok." else "Belum ada catatan bulan ini.", color = p.abu, fontSize = 14.sp)
            }
        } else {
            val kelompok = tampil.groupBy { it.tanggal }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((tgl, daftar) in kelompok) {
                    item(key = "h$tgl") {
                        val net = daftar.sumOf { if (it.masuk) it.jumlah else -it.jumlah }
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tampilTanggal(tgl), color = p.abu, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(rupiahTanda(net), color = if (net >= 0) p.hijau else p.merah, fontSize = 12.sp)
                        }
                    }
                    items(daftar, key = { it.id }) { c -> BarisCatatan(c, vm.data) { onEdit(c) } }
                }
            }
        }
    }
}
