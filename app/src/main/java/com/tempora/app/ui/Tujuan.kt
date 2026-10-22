package com.tempora.app.ui

import com.tempora.app.R

// Daftar tab di bilah bawah. Menambah tab cukup dengan menambah satu baris di sini.
// rute = nama layar untuk navigasi, label = tulisan di bawah ikon, ikon = gambar di res/drawable.
enum class Tujuan(val rute: String, val label: String, val ikon: Int) {
    Beranda("beranda", "Beranda", R.drawable.ic_tab_beranda),
    Materi("materi", "Materi", R.drawable.ic_tab_materi),
    Progres("progres", "Progres", R.drawable.ic_tab_progres),
    Atur("atur", "Atur", R.drawable.ic_tab_atur),
}
