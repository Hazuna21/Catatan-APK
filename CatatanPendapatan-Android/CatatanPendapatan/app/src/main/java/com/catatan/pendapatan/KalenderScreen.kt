package com.catatan.pendapatan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun KalenderScreen(vm: AppViewModel, onEdit: (Catatan) -> Unit, onTambah: (LocalDate) -> Unit) {
    val p = LocalPalet.current
    val ym = vm.bulan
    val hariIni = LocalDate.now()
    val per = remember(vm.data, ym) { perHari(vm.data.catatan, ym.atDay(1), ym.atEndOfMonth()) }
    var dipilih by remember(ym) { mutableStateOf<LocalDate?>(if (ym == java.time.YearMonth.now()) hariIni else null) }
    val geser = ym.atDay(1).dayOfWeek.value - 1
    val jumlahHari = ym.lengthOfMonth()
    val baris = (geser + jumlahHari + 6) / 7

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        NavBulan(
            "${BULAN[ym.monthValue - 1]} ${ym.year}",
            { vm.bulan = ym.minusMonths(1) }, { vm.bulan = ym.plusMonths(1) },
        )
        Row(Modifier.fillMaxWidth()) {
            for (h in HARI_PENDEK) {
                Text(h, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = p.abu, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until baris) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (k in 0 until 7) {
                        val hari = r * 7 + k - geser + 1
                        if (hari < 1 || hari > jumlahHari) {
                            Box(Modifier.weight(1f).height(58.dp))
                        } else {
                            val tgl = ym.atDay(hari)
                            val v = per[tgl.toString()]
                            val net = if (v != null) v[0] - v[1] else 0L
                            val ada = v != null
                            val terpilih = tgl == dipilih
                            val bentuk = RoundedCornerShape(10.dp)
                            Column(
                                Modifier.weight(1f).height(58.dp).clip(bentuk)
                                    .background(if (terpilih) p.biruMuda else p.putih)
                                    .border(1.dp, if (terpilih) p.biru else if (tgl == hariIni) p.biru.copy(alpha = 0.5f) else p.garis, bentuk)
                                    .clickable { dipilih = tgl }.padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(hari.toString(), color = if (tgl == hariIni) p.biru else p.teks, fontSize = 13.sp, fontWeight = if (tgl == hariIni) FontWeight.Bold else FontWeight.Medium)
                                if (ada) {
                                    Text(
                                        (if (net < 0) "-" else "") + ringkas(net),
                                        color = if (net >= 0) p.hijau else p.merah, fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val d = dipilih
        if (d != null) {
            val daftar = vm.data.catatan.filter { it.tanggal == d.toString() }.sortedByDescending { it.id }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tampilTanggal(d.toString()), color = p.teks, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onTambah(d) }) { Text("+ Tambah") }
            }
            if (daftar.isEmpty()) {
                Text("Tidak ada catatan di tanggal ini.", color = p.abu, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in daftar) BarisCatatan(c, vm.data) { onEdit(c) }
                }
            }
        }
        Box(Modifier.size(88.dp))
    }
}
