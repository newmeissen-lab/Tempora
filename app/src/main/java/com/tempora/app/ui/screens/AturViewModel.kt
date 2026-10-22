package com.tempora.app.ui.screens

import android.app.Application
import android.app.NotificationManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AturUiState(
    val jumlahTerkunci: Int = 0,
    val judulMateriKunci: String? = null,
    val izin: StatusIzin = StatusIzin(),
    val pengingatAktif: Boolean = true,
)

// Tiga izin yang harus diaktifkan pengguna di Pengaturan HP.
data class StatusIzin(
    val aksesPenggunaan: Boolean = false,
    val tampilDiAtas: Boolean = false,
    val notifikasi: Boolean = false,
)

class AturViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TemporaApplication

    private val izin = MutableStateFlow(StatusIzin())

    val uiState: StateFlow<AturUiState> = combine(
        app.database.aplikasiDibatasiDao().semua(),
        app.materi.semua,
        app.pengaturan.idMateriKunci,
        izin,
        app.pengaturan.pengingatAktif,
    ) { dibatasi, materi, idKunci, izin, pengingatAktif ->
        AturUiState(
            jumlahTerkunci = dibatasi.size,
            judulMateriKunci = materi.firstOrNull { it.id == idKunci }?.judul,
            izin = izin,
            pengingatAktif = pengingatAktif,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AturUiState())

    // Status izin tidak bisa dipantau otomatis, jadi diperiksa tiap kali layar Atur tampil.
    fun periksaIzin() {
        izin.value = StatusIzin(
            aksesPenggunaan = app.pemakaian.izinDiberikan(),
            tampilDiAtas = Settings.canDrawOverlays(app),
            notifikasi = app.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
        )
    }

    fun aturPengingat(aktif: Boolean) {
        viewModelScope.launch { app.pengaturan.aturPengingat(aktif) }
    }
}
