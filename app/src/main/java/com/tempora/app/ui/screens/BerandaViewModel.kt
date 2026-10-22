package com.tempora.app.ui.screens

import android.app.Application
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Satu aplikasi beserta lama pemakaiannya hari ini. batasMenit hanya terisi untuk aplikasi terkunci.
data class PemakaianAplikasi(
    val nama: String,
    val ikon: ImageBitmap,
    val terpakaiMs: Long,
    val batasMenit: Int? = null,
)

// Semua yang perlu ditampilkan layar Beranda.
data class BerandaUiState(
    val memuat: Boolean = true,
    val izinDiberikan: Boolean = false,
    // Izin "Tampil di atas aplikasi lain", dipakai layar kuis.
    val izinTampilDiAtas: Boolean = false,
    val totalMs: Long = 0L,
    val jumlahTerkunci: Int = 0,
    val terkunci: List<PemakaianAplikasi> = emptyList(),
    val palingLama: List<PemakaianAplikasi> = emptyList(),
)

// ViewModel menyiapkan data untuk layar dan tetap hidup saat layar diputar.
class BerandaViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TemporaApplication

    // _uiState bisa diubah dari dalam ViewModel; layar hanya membaca uiState.
    private val _uiState = MutableStateFlow(BerandaUiState())
    val uiState: StateFlow<BerandaUiState> = _uiState.asStateFlow()

    private var tugasMuat: Job? = null

    // Dipanggil tiap kali Beranda tampil, supaya angkanya selalu yang terbaru.
    fun muatUlang() {
        tugasMuat?.cancel()
        tugasMuat = viewModelScope.launch {
            val izin = app.pemakaian.izinDiberikan()
            val terpasang = app.aplikasiTerpasang.semua().associateBy { it.paket }
            val dibatasi = app.database.aplikasiDibatasiDao().semua().first()
            // Waktu layar hanya menghitung aplikasi yang punya ikon di HP.
            val pemakaian = if (izin) {
                app.pemakaian.hariIni().filterKeys { it in terpasang }
            } else {
                emptyMap()
            }

            _uiState.value = BerandaUiState(
                memuat = false,
                izinDiberikan = izin,
                izinTampilDiAtas = Settings.canDrawOverlays(app),
                totalMs = pemakaian.values.sum(),
                jumlahTerkunci = dibatasi.size,
                terkunci = dibatasi.mapNotNull { batas ->
                    terpasang[batas.paket]?.let { aplikasi ->
                        PemakaianAplikasi(
                            nama = aplikasi.nama,
                            ikon = aplikasi.ikon,
                            terpakaiMs = pemakaian[batas.paket] ?: 0L,
                            batasMenit = batas.batasMenit,
                        )
                    }
                }.sortedBy { it.nama.lowercase() },
                palingLama = pemakaian.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .mapNotNull { (paket, lama) ->
                        terpasang[paket]?.let { PemakaianAplikasi(it.nama, it.ikon, lama) }
                    },
            )
        }
    }
}
