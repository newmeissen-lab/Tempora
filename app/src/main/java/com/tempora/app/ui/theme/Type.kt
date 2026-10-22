// Memilih ketebalan font variabel (variationSettings) masih ditandai eksperimental oleh Compose,
// jadi perlu diizinkan untuk file ini.
@file:OptIn(ExperimentalTextApi::class)

package com.tempora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.tempora.app.R

// Font mockup. File font ada di res/font dan ikut di dalam APK, jadi tidak butuh internet.
// Tiga yang pertama adalah "font variabel": satu file berisi semua ketebalan, dan ketebalan
// yang dipakai dipilih lewat FontVariation.weight.

private val KETEBALAN = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

// DM Sans: tulisan biasa di seluruh aplikasi.
val HurufIsi = FontFamily(
    KETEBALAN.map { tebal ->
        Font(
            resId = R.font.dm_sans,
            weight = tebal,
            variationSettings = FontVariation.Settings(FontVariation.weight(tebal.weight)),
        )
    },
)

// Bricolage Grotesque: judul layar, nama aplikasi, nama materi, dan pertanyaan kuis.
// "opsz" adalah ukuran optik: bentuk huruf yang dirancang untuk tulisan besar.
val HurufJudul = FontFamily(
    KETEBALAN.map { tebal ->
        Font(
            resId = R.font.bricolage_grotesque,
            weight = tebal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(tebal.weight),
                FontVariation.Setting("opsz", 28f),
            ),
        )
    },
)

// Unbounded: angka besar, misalnya waktu layar dan hitungan mundur.
val HurufAngka = FontFamily(
    KETEBALAN.map { tebal ->
        Font(
            resId = R.font.unbounded,
            weight = tebal,
            variationSettings = FontVariation.Settings(FontVariation.weight(tebal.weight)),
        )
    },
)

// Belleza: hanya untuk tulisan nama "Tempora".
val HurufLogo = FontFamily(Font(R.font.belleza, FontWeight.Normal))

// Gaya tulisan Material 3 dengan font Tempora: gaya judul memakai Bricolage Grotesque,
// sisanya DM Sans. Ukurannya tetap ukuran bawaan Material.
private val Bawaan = Typography()

val Tipografi = Typography(
    displayLarge = Bawaan.displayLarge.copy(fontFamily = HurufJudul),
    displayMedium = Bawaan.displayMedium.copy(fontFamily = HurufJudul),
    displaySmall = Bawaan.displaySmall.copy(fontFamily = HurufJudul),
    headlineLarge = Bawaan.headlineLarge.copy(fontFamily = HurufJudul),
    headlineMedium = Bawaan.headlineMedium.copy(fontFamily = HurufJudul),
    headlineSmall = Bawaan.headlineSmall.copy(fontFamily = HurufJudul),
    titleLarge = Bawaan.titleLarge.copy(fontFamily = HurufJudul),
    titleMedium = Bawaan.titleMedium.copy(fontFamily = HurufJudul),
    titleSmall = Bawaan.titleSmall.copy(fontFamily = HurufJudul),
    bodyLarge = Bawaan.bodyLarge.copy(fontFamily = HurufIsi),
    bodyMedium = Bawaan.bodyMedium.copy(fontFamily = HurufIsi),
    bodySmall = Bawaan.bodySmall.copy(fontFamily = HurufIsi),
    labelLarge = Bawaan.labelLarge.copy(fontFamily = HurufIsi),
    labelMedium = Bawaan.labelMedium.copy(fontFamily = HurufIsi),
    labelSmall = Bawaan.labelSmall.copy(fontFamily = HurufIsi),
)
