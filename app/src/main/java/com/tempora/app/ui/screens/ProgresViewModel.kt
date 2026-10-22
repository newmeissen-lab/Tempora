package com.tempora.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private const val JUMLAH_HARI = 7

// Satu batang di bagan: tanggal dan total waktu layar hari itu.
data class BatangHari(val tanggal: LocalDate, val totalMs: Long)

data class ProgresUiState(
    val memuat: Boolean = true,
    val izinDiberikan: Boolean = false,
    val hari: List<BatangHari> = emptyList(),
    val soalHariIni: Int = 0,
    // Bulan yang sedang ditampilkan di kalender.
    val bulan: YearMonth = YearMonth.now(),
    // Jumlah soal yang dijawab per tanggal di bulan itu. Tanggal tanpa jawaban tidak ada di sini.
    val soalPerTanggal: Map<LocalDate, Int> = emptyMap(),
    // false kalau kalender sudah menampilkan bulan ini, jadi tidak bisa maju lagi.
    val bisaMaju: Boolean = false,
)

// Bagian waktu layar, dimuat ulang tiap halaman Progres tampil.
private data class WaktuLayar(
    val memuat: Boolean = true,
    val izinDiberikan: Boolean = false,
    val hari: List<BatangHari> = emptyList(),
)

class ProgresViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TemporaApplication

    private val waktuLayar = MutableStateFlow(WaktuLayar())
    private val bulan = MutableStateFlow(YearMonth.now())
    private val hariIni = MutableStateFlow(LocalDate.now())

    // Daftar waktu semua jawaban dikelompokkan per tanggal. Karena sumbernya Flow dari
    // database, angka di layar bertambah sendiri begitu ada soal yang dijawab.
    val uiState: StateFlow<ProgresUiState> = combine(
        waktuLayar,
        app.database.jawabanDao().semuaWaktu(),
        bulan,
        hariIni,
    ) { layar, waktuJawaban, bulan, hariIni ->
        val zona = ZoneId.systemDefault()
        val perTanggal = waktuJawaban
            .groupingBy { Instant.ofEpochMilli(it).atZone(zona).toLocalDate() }
            .eachCount()
        ProgresUiState(
            memuat = layar.memuat,
            izinDiberikan = layar.izinDiberikan,
            hari = layar.hari,
            soalHariIni = perTanggal[hariIni] ?: 0,
            bulan = bulan,
            soalPerTanggal = perTanggal.filterKeys { YearMonth.from(it) == bulan },
            bisaMaju = bulan < YearMonth.from(hariIni),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgresUiState())

    private var tugasMuat: Job? = null

    fun muatUlang() {
        // Kalau halaman dibuka lagi setelah lewat tengah malam, "hari ini" ikut berganti.
        hariIni.value = LocalDate.now()
        tugasMuat?.cancel()
        tugasMuat = viewModelScope.launch {
            val izin = app.pemakaian.izinDiberikan()
            val hari = if (izin) {
                // Sama seperti di Beranda: hanya aplikasi yang punya ikon yang dihitung.
                val terpasang = app.aplikasiTerpasang.semua().map { it.paket }.toSet()
                app.pemakaian.perHari(JUMLAH_HARI).map { harian ->
                    BatangHari(
                        tanggal = harian.tanggal,
                        totalMs = harian.perPaket.filterKeys { it in terpasang }.values.sum(),
                    )
                }
            } else {
                emptyList()
            }
            waktuLayar.value = WaktuLayar(memuat = false, izinDiberikan = izin, hari = hari)
        }
    }

    fun bulanSebelumnya() {
        bulan.update { it.minusMonths(1) }
    }

    fun bulanBerikutnya() {
        bulan.update { if (it < YearMonth.now()) it.plusMonths(1) else it }
    }
}
