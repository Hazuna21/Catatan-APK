package com.catatan.pendapatan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun GrafikBatang(bucket: List<Bucket>, modifier: Modifier = Modifier) {
    val p = LocalPalet.current
    val tm = rememberTextMeasurer()
    var pilih by remember(bucket) { mutableIntStateOf(-1) }
    val puncak = bucket.maxOfOrNull { maxOf(it.masuk, it.keluar) } ?: 0L
    val langkah = langkahSkala(puncak)
    val garis = max(1, ceil(puncak.toDouble() / langkah).toInt())
    val atas = (langkah * garis).toDouble()
    val tinggiBatang = 170.dp
    val padAtas = 10.dp
    val ruangLabel = 24.dp
    val n = bucket.size
    val gayaY = TextStyle(fontSize = 10.sp, color = p.abu)
    val skroll = rememberScrollState()
    val density = LocalDensity.current

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            // sumbu Y tetap
            Canvas(Modifier.width(44.dp).height(tinggiBatang + padAtas + ruangLabel)) {
                val h = tinggiBatang.toPx()
                val pt = padAtas.toPx()
                for (i in 0..garis) {
                    val teks = ringkas(langkah * i)
                    val ukuran = tm.measure(teks, gayaY).size
                    val y = pt + h - (i.toFloat() / garis) * h
                    drawText(tm, teks, Offset(size.width - ukuran.width - 6.dp.toPx(), y - ukuran.height / 2f), gayaY)
                }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val minItem = if (n > 14) 26.dp else 0.dp
                val lebarItem: Dp = maxOf(maxWidth / n, minItem)
                val lebarPx = with(density) { lebarItem.toPx() }
                Column(Modifier.horizontalScroll(skroll)) {
                    Canvas(
                        Modifier.width(lebarItem * n).height(tinggiBatang + padAtas + ruangLabel)
                            .pointerInput(bucket) {
                                detectTapGestures { off -> pilih = (off.x / lebarPx).toInt().coerceIn(0, n - 1) }
                            }
                    ) {
                        val h = tinggiBatang.toPx()
                        val pt = padAtas.toPx()
                        for (i in 0..garis) {
                            val y = pt + h - (i.toFloat() / garis) * h
                            drawLine(p.garis, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                        val lb = lebarPx * 0.30f
                        val sela = lebarPx * 0.04f
                        for ((i, b) in bucket.withIndex()) {
                            val x0 = i * lebarPx
                            if (i == pilih) {
                                drawRoundRect(p.biruMuda, Offset(x0 + 1f, pt), Size(lebarPx - 2f, h + ruangLabel.toPx() - 4.dp.toPx()), CornerRadius(8.dp.toPx()))
                            }
                            val tengah = x0 + lebarPx / 2f
                            fun batang(nilai: Long, kiri: Float, warna: Color) {
                                if (nilai <= 0) return
                                val tb = max((nilai / atas * h).toFloat(), 2.dp.toPx())
                                drawRoundRect(warna, Offset(kiri, pt + h - tb), Size(lb, tb), CornerRadius(3.dp.toPx()))
                            }
                            batang(b.masuk, tengah - lb - sela / 2f, p.hijau)
                            batang(b.keluar, tengah + sela / 2f, p.merah)
                            val gaya = TextStyle(fontSize = 10.sp, color = if (b.aktif) p.biru else p.abu)
                            val u = tm.measure(b.label, gaya).size
                            if (lebarPx > u.width * 0.9f || i % 2 == 0) {
                                drawText(tm, b.label, Offset(tengah - u.width / 2f, pt + h + 6.dp.toPx()), gaya)
                            }
                        }
                    }
                }
            }
        }
        val b = bucket.getOrNull(pilih)
        if (b != null) {
            Spacer(Modifier.height(6.dp))
            Text(b.tip, color = p.teks, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
            Text(
                "Masuk ${rupiah(b.masuk)}  ·  Keluar ${rupiah(b.keluar)}",
                color = p.abu, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp),
            )
        } else {
            Text("Ketuk batang untuk melihat rincian", color = p.abu, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
        }
    }
}

@Composable
fun Donat(irisan: List<Pair<Float, Color>>, kosong: Color, modifier: Modifier = Modifier, tebal: Dp = 22.dp) {
    Canvas(modifier) {
        val sisi = minOf(size.width, size.height)
        val t = tebal.toPx()
        val kiriAtas = Offset((size.width - sisi) / 2f + t / 2f, (size.height - sisi) / 2f + t / 2f)
        val uk = Size(sisi - t, sisi - t)
        val total = irisan.sumOf { it.first.toDouble() }.toFloat()
        if (total <= 0f) {
            drawArc(kosong, 0f, 360f, false, kiriAtas, uk, style = Stroke(t))
            return@Canvas
        }
        val celah = if (irisan.size > 1) 2f else 0f
        var awal = -90f
        for ((v, w) in irisan) {
            val sapu = v / total * 360f
            drawArc(w, awal + celah / 2f, max(sapu - celah, 0.5f), false, kiriAtas, uk, style = Stroke(t))
            awal += sapu
        }
    }
}
