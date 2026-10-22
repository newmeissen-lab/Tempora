package com.tempora.app.ui.screens

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import com.tempora.app.data.AplikasiDibatasi
import com.tempora.app.data.AplikasiTerpasang
import com.tempora.app.data.Motivasi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Batas waktu dalam menit: nilai awal saat aplikasi baru dikunci, dan rentang yang boleh dipilih.
const val BATAS_BAWAAN_MENIT = 60
const val BATAS_TERKECIL_MENIT = 5
const val BATAS_TERBESAR_MENIT = 600

// Satu baris di daftar. batasMenit null berarti aplikasi tidak dikunci.
data class BarisAplikasi(
    val paket: String,
    val nama: String,
    val ikon: ImageBitmap,
    val batasMenit: Int?,
)

data class AplikasiTerkunciUiState(
    val memuat: Boolean = true,
    val jumlahTerkunci: Int = 0,
    val daftar: List<BarisAplikasi> = emptyList(),
    // Alasan dan mimpi pengguna, ditampilkan sebelum kunci sebuah aplikasi dimatikan.
    val motivasi: Motivasi? = null,
)

class AplikasiTerkunciViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TemporaApplication
    private val dao = app.database.aplikasiDibatasiDao()

    private val terpasang = MutableStateFlow<List<AplikasiTerpasang>?>(null)
    private val kataCari = MutableStateFlow("")

    // combine menggabungkan empat sumber: daftar aplikasi di HP, isi tabel aplikasi_dibatasi,
    // kata yang dicari, dan alasan serta mimpi pengguna. Kalau salah satunya berubah,
    // isi layar dihitung ulang.
    val uiState: StateFlow<AplikasiTerkunciUiState> =
        combine(terpasang, dao.semua(), kataCari, app.pengaturan.motivasi) { semua, dibatasi, cari, motivasi ->
            val batas = dibatasi.associate { it.paket to it.batasMenit }
            val daftar = semua.orEmpty().map { BarisAplikasi(it.paket, it.nama, it.ikon, batas[it.paket]) }
            AplikasiTerkunciUiState(
                memuat = semua == null,
                jumlahTerkunci = daftar.count { it.batasMenit != null },
                daftar = daftar.filter { it.nama.contains(cari.trim(), ignoreCase = true) },
                motivasi = motivasi,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AplikasiTerkunciUiState())

    init {
        viewModelScope.launch {
            val sudahDikunci = dao.semua().first().map { it.paket }.toSet()
            terpasang.value = app.aplikasiTerpasang.semua(muatUlang = true)
                // Tempora tidak bisa mengunci dirinya sendiri.
                .filter { it.paket != application.packageName }
                // Yang sudah dikunci ditaruh di atas. Urutan ini tetap selama layar terbuka,
                // supaya baris tidak melompat saat sakelarnya digeser.
                .sortedBy { it.paket !in sudahDikunci }
        }
    }

    fun ubahCari(teks: String) {
        kataCari.value = teks
    }

    fun ubahKunci(paket: String, dikunci: Boolean) {
        viewModelScope.launch {
            if (dikunci) dao.simpan(AplikasiDibatasi(paket, BATAS_BAWAAN_MENIT)) else dao.hapus(paket)
        }
    }

    fun ubahBatas(paket: String, menit: Int) {
        viewModelScope.launch {
            val batas = menit.coerceIn(BATAS_TERKECIL_MENIT, BATAS_TERBESAR_MENIT)
            dao.simpan(AplikasiDibatasi(paket, batas))
        }
    }
}
