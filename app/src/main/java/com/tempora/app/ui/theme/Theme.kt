package com.tempora.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Memasangkan warna Tempora ke "peran" warna Material 3. Tombol, kartu, dan sakelar bawaan
// Material membaca peran ini, jadi warnanya ikut tema tanpa diatur satu per satu.
private val SkemaWarna = darkColorScheme(
    primary = Kuning,
    onPrimary = TeksDiAtasTerang,
    secondary = TombolUtama,
    onSecondary = TeksDiAtasTerang,
    background = Latar,
    onBackground = Teks,
    surface = Latar,
    onSurface = Teks,
    surfaceVariant = PermukaanKedua,
    onSurfaceVariant = TeksRedup,
    surfaceContainerLow = Permukaan,
    surfaceContainer = Permukaan,
    surfaceContainerHigh = PermukaanKedua,
    surfaceContainerHighest = PermukaanKedua,
    outline = TeksRedup,
    outlineVariant = Garis,
    error = Salah,
    onError = TeksDiAtasTerang,
)

// Tempora hanya punya tema gelap, jadi tidak ada pilihan terang/gelap di sini.
@Composable
fun TemporaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SkemaWarna, typography = Tipografi, content = content)
}
