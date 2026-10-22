package com.tempora.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// Fungsi kecil untuk mengubah angka dan tanggal menjadi tulisan yang ditampilkan di layar.

private val BAHASA_INDONESIA: Locale = Locale.forLanguageTag("id-ID")

// Lama pemakaian dalam milidetik, misalnya 8.040.000 menjadi "2j 14m".
fun formatDurasi(ms: Long): String {
    val menit = ms / 60_000
    return when {
        menit >= 60 -> "${menit / 60}j ${menit % 60}m"
        menit >= 1 -> "${menit}m"
        ms > 0 -> "<1m"
        else -> "0m"
    }
}

// Batas waktu dalam menit, misalnya 90 menjadi "1 jam 30 menit".
fun formatBatas(menit: Int): String {
    val jam = menit / 60
    val sisa = menit % 60
    return when {
        jam > 0 && sisa > 0 -> "$jam jam $sisa menit"
        jam > 0 -> "$jam jam"
        else -> "$sisa menit"
    }
}

// Misalnya "Minggu, 4 Okt".
fun formatTanggal(tanggal: LocalDate): String =
    tanggal.format(DateTimeFormatter.ofPattern("EEEE, d MMM", BAHASA_INDONESIA))

// Misalnya "Oktober 2026".
fun formatBulan(bulan: YearMonth): String =
    bulan.format(DateTimeFormatter.ofPattern("MMMM yyyy", BAHASA_INDONESIA))

// Misalnya "4 Oktober".
fun formatTanggalPanjang(tanggal: LocalDate): String =
    tanggal.format(DateTimeFormatter.ofPattern("d MMMM", BAHASA_INDONESIA))

// Misalnya "Sabtu".
fun namaHari(tanggal: LocalDate): String =
    tanggal.dayOfWeek.getDisplayName(TextStyle.FULL, BAHASA_INDONESIA)

// Kapan sesuatu ditambahkan, dibanding hari ini: "hari ini", "kemarin", "3 hari lalu", atau tanggalnya.
fun formatKapan(waktuMs: Long): String {
    val zona = ZoneId.systemDefault()
    val tanggal = Instant.ofEpochMilli(waktuMs).atZone(zona).toLocalDate()
    val selisihHari = ChronoUnit.DAYS.between(tanggal, LocalDate.now(zona))
    return when {
        selisihHari <= 0L -> "hari ini"
        selisihHari == 1L -> "kemarin"
        selisihHari < 7L -> "$selisihHari hari lalu"
        else -> tanggal.format(DateTimeFormatter.ofPattern("d MMM yyyy", BAHASA_INDONESIA))
    }
}
