package com.tempora.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import com.tempora.app.data.Materi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Nama bagian alamat halaman yang berisi id materi, misalnya "rangkuman/12".
const val ARGUMEN_ID_MATERI = "idMateri"

data class RangkumanUiState(
    val memuat: Boolean = true,
    // null setelah selesai memuat berarti materinya sudah dihapus.
    val materi: Materi? = null,
    val jumlahSoal: Int = 0,
    val dipakaiUntukKunci: Boolean = false,
)

// savedStateHandle berisi data yang dibawa saat pindah halaman; di sini id materi yang dibuka.
class RangkumanViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as TemporaApplication
    private val idMateri: Long = savedStateHandle[ARGUMEN_ID_MATERI] ?: 0L

    val uiState: StateFlow<RangkumanUiState> = combine(
        app.materi.semua,
        app.materi.jumlahSoal,
        app.pengaturan.idMateriKunci,
    ) { semua, jumlahSoal, idKunci ->
        RangkumanUiState(
            memuat = false,
            materi = semua.firstOrNull { it.id == idMateri },
            jumlahSoal = jumlahSoal[idMateri] ?: 0,
            dipakaiUntukKunci = idKunci == idMateri,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RangkumanUiState())
}
