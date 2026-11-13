package com.catatan.pendapatan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Kartu(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalet.current
    val bentuk = RoundedCornerShape(16.dp)
    val dasar = modifier.clip(bentuk).background(p.putih).border(BorderStroke(1.dp, p.garis), bentuk)
    Column(
        modifier = (if (onClick != null) dasar.clickable(onClick = onClick) else dasar).padding(padding),
        content = content,
    )
}

@Composable
fun Chip(teks: String, aktif: Boolean, warna: Color? = null, onClick: () -> Unit) {
    val p = LocalPalet.current
    val bentuk = RoundedCornerShape(50)
    val latar = if (aktif) (warna?.copy(alpha = 0.18f) ?: p.chipAktif) else p.input
    val tulis = if (aktif) (warna ?: p.chipTeks) else p.abu
    val garis = if (aktif) (warna ?: p.biru) else p.garis
    Row(
        modifier = Modifier.clip(bentuk).background(latar).border(BorderStroke(1.dp, garis), bentuk)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (warna != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(warna))
            Spacer(Modifier.width(6.dp))
        }
        Text(teks, color = tulis, fontSize = 13.sp, fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** Pilihan bersegmen: pasangan (kunci, label). */
@Composable
fun Segmen(pilihan: List<Pair<String, String>>, aktif: String, onPilih: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalet.current
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.header).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for ((kunci, label) in pilihan) {
            val on = kunci == aktif
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (on) p.putih else Color.Transparent)
                    .clickable { onPilih(kunci) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) p.biru else p.abu, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
fun NavBulan(label: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalet.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Sebelumnya", tint = p.teks) }
        Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = p.teks, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Berikutnya", tint = p.teks) }
    }
}

@Composable
fun JudulBagian(teks: String, modifier: Modifier = Modifier) {
    Text(teks, modifier = modifier, color = LocalPalet.current.teks, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun BatangProgres(fraksi: Float, warna: Color, modifier: Modifier = Modifier, tinggi: Dp = 8.dp) {
    val p = LocalPalet.current
    Box(modifier.fillMaxWidth().height(tinggi).clip(RoundedCornerShape(50)).background(p.header)) {
        Box(Modifier.fillMaxWidth(fraksi.coerceIn(0f, 1f)).height(tinggi).clip(RoundedCornerShape(50)).background(warna))
    }
}

fun warnaKategori(data: DataApp, nama: String, jenis: String): Color {
    val k = data.kategori.firstOrNull { it.jenis == jenis && it.nama == nama }
    return hex(k?.warna ?: WARNA_TANPA)
}
