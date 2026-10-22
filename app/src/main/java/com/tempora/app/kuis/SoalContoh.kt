package com.tempora.app.kuis

// Satu soal pilihan ganda. indeksBenar adalah urutan jawaban benar di dalam pilihan,
// dihitung dari 0 (0 = A, 1 = B, dan seterusnya).
data class Soal(
    val pertanyaan: String,
    val pilihan: List<String>,
    val indeksBenar: Int,
    val penjelasan: String,
)

// Soal dan rangkuman cadangan. Dipakai layar kuis hanya selama belum ada soal buatan AI
// dari materi yang dipilih pengguna (lihat BahanKuis.kt).
const val JUDUL_MATERI_CONTOH = "Biologi Sel (materi contoh)"

const val RANGKUMAN_CONTOH =
    "Sel adalah satuan terkecil makhluk hidup. Di dalam sel ada organel, yaitu bagian-bagian " +
        "yang punya tugas sendiri.\n\n" +
        "Nukleus (inti sel) menyimpan DNA dan mengatur kegiatan sel. Ribosom menyusun protein. " +
        "Badan Golgi mengolah dan mengemas protein itu lalu mengirimkannya. Mitokondria adalah " +
        "tempat respirasi sel yang menghasilkan energi dalam bentuk ATP. Lisosom berisi enzim " +
        "pencerna untuk menghancurkan zat sisa dan organel yang rusak.\n\n" +
        "Membran sel membungkus sel dan mengatur zat yang keluar dan masuk. Sel tumbuhan punya " +
        "dua bagian yang tidak ada di sel hewan: dinding sel yang membuat sel kaku, dan " +
        "kloroplas sebagai tempat fotosintesis."

val SOAL_CONTOH = listOf(
    Soal(
        pertanyaan = "Organel mana yang menjadi tempat utama respirasi sel dan pembentukan ATP?",
        pilihan = listOf("Ribosom", "Mitokondria", "Badan Golgi", "Lisosom"),
        indeksBenar = 1,
        penjelasan = "Mitokondria adalah tempat respirasi sel. Energi dalam bentuk ATP dibentuk di sana.",
    ),
    Soal(
        pertanyaan = "Organel mana yang bertugas menyusun protein?",
        pilihan = listOf("Ribosom", "Lisosom", "Kloroplas", "Dinding sel"),
        indeksBenar = 0,
        penjelasan = "Ribosom menyusun protein. Organel ini tidak menghasilkan energi.",
    ),
    Soal(
        pertanyaan = "Di organel mana fotosintesis berlangsung pada sel tumbuhan?",
        pilihan = listOf("Mitokondria", "Nukleus", "Kloroplas", "Badan Golgi"),
        indeksBenar = 2,
        penjelasan = "Kloroplas adalah tempat fotosintesis dan hanya ada di sel tumbuhan.",
    ),
    Soal(
        pertanyaan = "Bagian sel mana yang menyimpan DNA dan mengatur kegiatan sel?",
        pilihan = listOf("Membran sel", "Lisosom", "Ribosom", "Nukleus"),
        indeksBenar = 3,
        penjelasan = "Nukleus atau inti sel menyimpan DNA dan mengatur kegiatan sel.",
    ),
    Soal(
        pertanyaan = "Organel mana yang berisi enzim pencerna untuk menghancurkan zat sisa?",
        pilihan = listOf("Lisosom", "Ribosom", "Kloroplas", "Nukleus"),
        indeksBenar = 0,
        penjelasan = "Lisosom berisi enzim pencerna yang menghancurkan zat sisa dan organel yang rusak.",
    ),
    Soal(
        pertanyaan = "Bagian mana yang ada di sel tumbuhan tetapi tidak ada di sel hewan?",
        pilihan = listOf("Membran sel", "Mitokondria", "Dinding sel", "Ribosom"),
        indeksBenar = 2,
        penjelasan = "Dinding sel membuat sel tumbuhan kaku. Sel hewan hanya punya membran sel.",
    ),
    Soal(
        pertanyaan = "Bagian sel mana yang mengatur zat yang keluar dan masuk sel?",
        pilihan = listOf("Nukleus", "Membran sel", "Badan Golgi", "Lisosom"),
        indeksBenar = 1,
        penjelasan = "Membran sel membungkus sel dan mengatur zat yang keluar dan masuk.",
    ),
    Soal(
        pertanyaan = "Organel mana yang mengolah, mengemas, dan mengirim protein?",
        pilihan = listOf("Mitokondria", "Kloroplas", "Lisosom", "Badan Golgi"),
        indeksBenar = 3,
        penjelasan = "Badan Golgi mengolah dan mengemas protein buatan ribosom, lalu mengirimkannya.",
    ),
)
