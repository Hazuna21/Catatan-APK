package com.catatan.pendapatan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun StatistikScreen(vm: AppViewModel) {
    val p = LocalPalet.current
    val st = remember(vm.data, vm.periode, vm.bulan) {
        hitungStatistik(vm.data.catatan, vm.periode, vm.bulan, LocalDate.now())
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Segmen(
            Periode.entries.map { it.name to it.label }, vm.periode.name,
            { vm.periode = Periode.valueOf(it) },
        )
        if (vm.periode != Periode.TAHUNAN) {
            NavBulan(
                st.judul,
                { vm.bulan = if (vm.periode == Periode.BULANAN) vm.bulan.minusYears(1) else vm.bulan.minusMonths(1) },
                { vm.bulan = if (vm.periode == Periode.BULANAN) vm.bulan.plusYears(1) else vm.bulan.plusMonths(1) },
            )
        } else {
            Text(st.judul, color = p.teks, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        // kartu saldo
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(p.grad1, p.grad2))).padding(18.dp)
        ) {
            Column {
                Text("Selisih ${st.judul}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Text(rupiahTanda(st.selisih), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                val r = st.rasio
                Text(
                    if (r != null) "Sisa ${r.roundToLong()}% dari pemasukan" else "Belum ada pemasukan",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KartuAngka("Pemasukan", st.masuk, p.hijau, st.sebelumMasuk, st.namaSebelum, naikBaik = true, Modifier.weight(1f))
            KartuAngka("Pengeluaran", st.keluar, p.merah, st.sebelumKeluar, st.namaSebelum, naikBaik = false, Modifier.weight(1f))
        }

        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Arus kas")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                Legenda("Pemasukan", p.hijau)
                Legenda("Pengeluaran", p.merah)
            }
            GrafikBatang(st.bucket, Modifier.fillMaxWidth())
        }

        Kartu(Modifier.fillMaxWidth()) {
            JudulBagian("Ringkasan")
            Spasi()
            BarisInfo("Rata-rata pengeluaran / hari", rupiah(st.rataKeluar.roundToLong()))
            BarisInfo("Rata-rata pemasukan / hari", rupiah(st.rataMasuk.roundToLong()))
            st.hariBoros?.let { BarisInfo("Hari paling boros", "${tampilTanggal(it.first)} · ${rupiah(it.second)}") }
            st.hariTerbaik?.let { BarisInfo("Hari pemasukan terbaik", "${tampilTanggal(it.first)} · ${rupiah(it.second)}") }
        }

        if (st.topMasuk.isNotEmpty()) DaftarTop("Sumber pemasukan teratas", st.topMasuk, p.hijau)
        if (st.topKeluar.isNotEmpty()) DaftarTop("Pengeluaran terbesar", st.topKeluar, p.merah)
        Box(Modifier.size(72.dp)) // ruang untuk tombol tambah
    }
}

@Composable
private fun Spasi() {
    Box(Modifier.size(6.dp))
}

@Composable
private fun Legenda(teks: String, warna: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(warna))
        Text("  $teks", color = LocalPalet.current.abu, fontSize = 12.sp)
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    val p = LocalPalet.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = p.abu, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(nilai, color = p.teks, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun KartuAngka(
    judul: String, nilai: Long, warna: Color, sebelum: Long?, namaSebelum: String, naikBaik: Boolean, modifier: Modifier,
) {
    val p = LocalPalet.current
    Kartu(modifier) {
        Text(judul, color = p.abu, fontSize = 12.sp)
        Text(rupiah(nilai), color = warna, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (sebelum != null) {
            val (teks, baik) = teksPerubahan(nilai, sebelum, namaSebelum, naikBaik)
            Text(teks, color = when (baik) { true -> p.hijau; false -> p.merah; null -> p.abu }, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** (teks, baik?) — baik: true hijau, false merah, null netral */
private fun teksPerubahan(sekarang: Long, sebelum: Long, nama: String, naikBaik: Boolean): Pair<String, Boolean?> {
    if (sebelum <= 0L) return (if (sekarang > 0) "Belum ada data $nama" else "Sama seperti $nama") to null
    val persen = ((sekarang - sebelum) * 100.0 / sebelum).roundToLong()
    if (persen == 0L) return "Sama seperti $nama" to null
    val naik = persen > 0
    return "${if (naik) "▲" else "▼"} ${abs(persen)}% dari $nama" to (naik == naikBaik)
}

@Composable
private fun DaftarTop(judul: String, daftar: List<Pair<String, Long>>, warna: Color) {
    val p = LocalPalet.current
    val maks = daftar.maxOf { it.second }.coerceAtLeast(1L)
    Kartu(Modifier.fillMaxWidth()) {
        JudulBagian(judul)
        for ((nama, nilai) in daftar) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(nama, color = p.teks, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Text(rupiah(nilai), color = p.teks, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            BatangProgres(nilai.toFloat() / maks, warna, Modifier.padding(top = 4.dp), tinggi = 6.dp)
        }
    }
}
