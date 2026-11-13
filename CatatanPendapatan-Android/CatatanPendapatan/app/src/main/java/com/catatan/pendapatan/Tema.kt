package com.catatan.pendapatan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun hex(s: String): Color = try {
    Color(android.graphics.Color.parseColor(s))
} catch (e: Exception) {
    Color(0xFF64748B)
}

data class Palet(
    val gelap: Boolean,
    val bg: Color, val putih: Color, val garis: Color, val abu: Color, val teks: Color,
    val biru: Color, val biruGelap: Color, val biruMuda: Color,
    val grad1: Color, val grad2: Color,
    val hijau: Color, val hijauMuda: Color, val merah: Color, val merahMuda: Color, val kuning: Color,
    val strip: Color, val header: Color, val input: Color,
    val chipAktif: Color, val chipTeks: Color,
)

private fun palet(
    gelap: Boolean, bg: String, putih: String, garis: String, abu: String, teks: String,
    biru: String, biruGelap: String, biruMuda: String, grad1: String, grad2: String,
    hijau: String, hijauMuda: String, merah: String, merahMuda: String, kuning: String,
    strip: String, header: String, input: String, chipAktif: String, chipTeks: String,
) = Palet(
    gelap, hex(bg), hex(putih), hex(garis), hex(abu), hex(teks),
    hex(biru), hex(biruGelap), hex(biruMuda), hex(grad1), hex(grad2),
    hex(hijau), hex(hijauMuda), hex(merah), hex(merahMuda), hex(kuning),
    hex(strip), hex(header), hex(input), hex(chipAktif), hex(chipTeks),
)

val NAMA_TEMA = linkedMapOf(
    "terang" to "Terang", "gelap" to "Gelap", "midnight" to "Midnight Purple", "samudra" to "Samudra",
)

val PALET: Map<String, Palet> = mapOf(
    "terang" to palet(
        false, "#f3f5fb", "#ffffff", "#e4e8f1", "#6a7490", "#0f172a",
        "#4f46e5", "#4338ca", "#e8eaff", "#4f46e5", "#0891b2",
        "#059669", "#dcf5ea", "#e11d48", "#ffe3e9", "#d97706",
        "#f8f9fd", "#f0f3fa", "#f6f7fc", "#d6daff", "#3730a3",
    ),
    "gelap" to palet(
        true, "#0b0f17", "#151b27", "#242d3d", "#8c98af", "#e8edf7",
        "#6366f1", "#4f46e5", "#222d5c", "#4f46e5", "#0e7490",
        "#34d399", "#11342b", "#fb7185", "#3b1b27", "#fbbf24",
        "#18202e", "#1b2333", "#0f141d", "#2f3d80", "#c7d2fe",
    ),
    "midnight" to palet(
        true, "#0d0820", "#1a1236", "#2f2455", "#a89fca", "#f0ecff",
        "#8b5cf6", "#7c3aed", "#35276b", "#7c3aed", "#db2777",
        "#4ade80", "#163a35", "#fb7185", "#40183a", "#fbbf24",
        "#1e1540", "#241a47", "#120b2b", "#4c3a8a", "#e9d5ff",
    ),
    "samudra" to palet(
        true, "#06141a", "#0c222b", "#173842", "#84a5b0", "#e6f6fa",
        "#0d9488", "#0f766e", "#0f3d44", "#0d9488", "#2563eb",
        "#4ade80", "#12392f", "#fb7185", "#3a1c28", "#fbbf24",
        "#0f2a34", "#12303a", "#08191f", "#14585a", "#99f6e4",
    ),
)

val LocalPalet = staticCompositionLocalOf { PALET.getValue("terang") }

@Composable
fun TemaApp(nama: String, content: @Composable () -> Unit) {
    val p = PALET[nama] ?: PALET.getValue("terang")
    val skema = if (p.gelap) {
        darkColorScheme(
            primary = p.biru, onPrimary = Color.White, background = p.bg, onBackground = p.teks,
            surface = p.putih, onSurface = p.teks, surfaceVariant = p.input, onSurfaceVariant = p.abu,
            outline = p.garis, outlineVariant = p.garis, error = p.merah,
            secondaryContainer = p.biruMuda, onSecondaryContainer = p.chipTeks,
            surfaceContainer = p.putih, surfaceContainerHigh = p.putih, surfaceContainerHighest = p.header,
        )
    } else {
        lightColorScheme(
            primary = p.biru, onPrimary = Color.White, background = p.bg, onBackground = p.teks,
            surface = p.putih, onSurface = p.teks, surfaceVariant = p.input, onSurfaceVariant = p.abu,
            outline = p.garis, outlineVariant = p.garis, error = p.merah,
            secondaryContainer = p.biruMuda, onSecondaryContainer = p.chipTeks,
            surfaceContainer = p.putih, surfaceContainerHigh = p.putih, surfaceContainerHighest = p.header,
        )
    }
    CompositionLocalProvider(LocalPalet provides p) {
        MaterialTheme(colorScheme = skema, content = content)
    }
}
