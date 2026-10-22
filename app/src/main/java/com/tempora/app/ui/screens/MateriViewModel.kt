package com.tempora.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import com.tempora.app.data.Materi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Satu baris di daftar materi, beserta keadaan soal buatan AI-nya.
data class BarisMateri(
    val materi: Materi,
    val dipakaiUntukKunci: Boolean,
    // 0 berarti soalnya belum dibuat.
    val jumlahSoal: Int,
    val sedangDibuat: Boolean,
    // Pesan gagal terakhir dari AI, atau null kalau tidak ada.
    val pesanGagalAi: String?,
)

data class MateriUiState(
    val memuat: Boolean = true,
    // Beda dengan daftar kosong karena pencarian: adaMateri false berarti memang belum ada PDF.
    val adaMateri: Boolean = false,
    val daftar: List<BarisMateri> = emptyList(),
    val sedangMenambah: Boolean = false,
    val pesanGagal: String? = null,
)

// Keadaan pekerjaan AI untuk semua materi, digabung supaya mudah dipakai di bawah.
private data class KeadaanAi(
    val jumlahSoal: Map<Long, Int>,
    val sedangDibuat: Set<Long>,
    val gagalDibuat: Map<Long, String>,
)

private data class KeadaanTambah(val sedangMenambah: Boolean, val pesanGagal: String?)

class MateriViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TemporaApplication

    private val kataCari = MutableStateFlow("")
    private val sedangMenambah = MutableStateFlow(false)
    private val pesanGagal = MutableStateFlow<String?>(null)

    private val keadaanAi = combine(
        app.materi.jumlahSoal,
        app.materi.sedangDibuat,
        app.materi.gagalDibuat,
        ::KeadaanAi,
    )
    private val keadaanTambah = combine(sedangMenambah, pesanGagal, ::KeadaanTambah)

    val uiState: StateFlow<MateriUiState> = combine(
        app.materi.semua,
        app.pengaturan.idMateriKunci,
        kataCari,
        keadaanAi,
        keadaanTambah,
    ) { semua, idKunci, cari, ai, tambah ->
        MateriUiState(
            memuat = false,
            adaMateri = semua.isNotEmpty(),
            daftar = semua
                .filter { it.judul.contains(cari.trim(), ignoreCase = true) }
                .map {
                    BarisMateri(
                        materi = it,
                        dipakaiUntukKunci = it.id == idKunci,
                        jumlahSoal = ai.jumlahSoal[it.id] ?: 0,
                        sedangDibuat = it.id in ai.sedangDibuat,
                        pesanGagalAi = ai.gagalDibuat[it.id],
                    )
                },
            sedangMenambah = tambah.sedangMenambah,
            pesanGagal = tambah.pesanGagal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MateriUiState())

    fun ubahCari(teks: String) {
        kataCari.value = teks
    }

    // uri = alamat PDF yang dipilih pengguna lewat pemilih file bawaan Android.
    fun tambah(uri: Uri) {
        viewModelScope.launch {
            sedangMenambah.value = true
            pesanGagal.value = app.materi.tambahDariPdf(uri)
            sedangMenambah.value = false
        }
    }

    fun pilihUntukKunci(id: Long) {
        viewModelScope.launch { app.pengaturan.pilihMateriKunci(id) }
    }

    // Meminta AI membuat soal dan rangkuman. Dipakai untuk materi yang soalnya belum ada
    // dan untuk mencoba lagi setelah gagal.
    fun buatSoal(materi: Materi) {
        app.materi.buatSoal(materi)
    }

    fun hapus(materi: Materi) {
        viewModelScope.launch { app.materi.hapus(materi) }
    }
}
