package com.tempora.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tempora.app.TemporaApplication
import com.tempora.app.data.Motivasi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MotivasiViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TemporaApplication

    // true = alasan dan mimpi belum pernah diisi, jadi perlu ditanyakan.
    // null = penyimpanan belum selesai dibaca, jadi belum tahu.
    val perluDitanya: StateFlow<Boolean?> = app.pengaturan.motivasi
        .map<Motivasi?, Boolean?> { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Jawaban yang tersimpan, dipakai halaman "Alasan dan mimpi" di Atur sebagai isi awal.
    val motivasi: StateFlow<Motivasi?> = app.pengaturan.motivasi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun simpan(alasan: String, mimpi: String) {
        viewModelScope.launch {
            app.pengaturan.simpanMotivasi(alasan.trim(), mimpi.trim())
        }
    }
}
