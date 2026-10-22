package com.tempora.app.kuis

import com.tempora.app.TemporaApplication
import kotlinx.coroutines.flow.first

// Semua yang dibutuhkan layar kuis dari satu materi.
// contoh = true berarti ini soal contoh bawaan, bukan soal buatan AI dari materi pengguna.
data class BahanKuis(
    val judulMateri: String,
    val rangkuman: String,
    val soal: List<Soal>,
    val contoh: Boolean,
)

private val BAHAN_CONTOH = BahanKuis(
    judulMateri = JUDUL_MATERI_CONTOH,
    rangkuman = RANGKUMAN_CONTOH,
    soal = SOAL_CONTOH,
    contoh = true,
)

// Mengambil soal dan rangkuman dari materi yang dipilih untuk soal kunci. Kalau belum ada
// materi yang dipilih, atau soalnya belum dibuat AI, yang dipakai adalah soal contoh supaya
// kunci tetap berfungsi.
suspend fun muatBahanKuis(app: TemporaApplication): BahanKuis {
    val idMateri = app.pengaturan.idMateriKunci.first() ?: return BAHAN_CONTOH
    val materi = app.database.materiDao().cari(idMateri) ?: return BAHAN_CONTOH
    val soal = app.database.soalDao().untuk(idMateri)
    if (soal.isEmpty()) return BAHAN_CONTOH

    return BahanKuis(
        judulMateri = materi.judul,
        rangkuman = materi.rangkuman.orEmpty(),
        soal = soal.map {
            Soal(
                pertanyaan = it.pertanyaan,
                pilihan = listOf(it.pilihanA, it.pilihanB, it.pilihanC, it.pilihanD),
                indeksBenar = it.indeksBenar,
                penjelasan = it.penjelasan,
            )
        },
        contoh = false,
    )
}
