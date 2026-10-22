package com.tempora.app.pengingat

import com.tempora.app.data.RiwayatPengingat
import com.tempora.app.ui.formatBatas
import com.tempora.app.ui.formatDurasi
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// File ini berisi ATURAN notifikasi pengingat: kapan boleh dikirim, jenis mana yang dipilih,
// dan apa tulisannya. Isinya hanya perhitungan (tidak menyentuh Android), supaya mudah diuji.
// Yang mengumpulkan data ada di PemeriksaPengingat.kt, yang menggambar notifikasinya ada di
// TampilanPengingat.kt.

// Halaman Tempora yang dibuka saat notifikasi atau tombolnya diketuk.
const val BUKA_APLIKASI_TERKUNCI = "aplikasi-terkunci"
const val BUKA_MATERI = "materi"
const val BUKA_PROGRES = "progres"
const val BUKA_ATUR = "atur"

// Halaman Pengaturan HP yang bisa dibuka langsung dari tombol notifikasi.
const val PENGATURAN_TAMPIL_DI_ATAS = "tampil-di-atas"
const val PENGATURAN_AKSES_PENGGUNAAN = "akses-penggunaan"

// ---- Batas umum supaya pengingat tidak terlalu sering ----
// Pengingat hanya dikirim antara pukul 07.00 dan 21.00.
const val JAM_MULAI = 7
const val JAM_SELESAI = 21
// Jarak antar pengingat berubah-ubah antara 2 dan 4 jam, dan paling banyak 4 pengingat per hari.
// Selain itu pengingat hanya dikirim saat pengguna sedang memakai HP (lihat PemeriksaPengingat.kt).
const val MAKS_PER_HARI = 4
const val JEDA_PALING_PENDEK_MS = 2 * 60 * 60_000L
const val JEDA_PALING_PANJANG_MS = 4 * 60 * 60_000L

// ---- Ambang tiap jenis ----
private const val DUA_JAM_MS = 2 * 60 * 60_000L
private const val SATU_JAM_MS = 60 * 60_000L
// "Kemarin lebih hemat" hanya dikirim pagi hari (sebelum pukul 11.00), kalau waktu layar
// kemarin turun paling sedikit 15 menit dari hari sebelumnya yang paling sedikit 30 menit.
private const val JAM_KEMAJUAN_SAMPAI = 11
private const val HEMAT_PALING_SEDIKIT_MS = 15 * 60_000L
private const val PEMBANDING_PALING_SEDIKIT_MS = 30 * 60_000L
// Rekap dikirim hari Minggu mulai pukul 17.00.
private const val JAM_REKAP_MULAI = 17
// Kalimat motivasi hanya keluar kalau undiannya 0, yaitu 1 dari 6 pemeriksaan.
const val PELUANG_MOTIVASI = 6

// Jenis-jenis pengingat, DIURUTKAN DARI YANG PALING PENTING. Kalau beberapa syarat terpenuhi
// sekaligus, yang dikirim adalah yang paling atas.
// jedaHari = jenis itu paling cepat dikirim lagi setelah sekian hari.
// warnaAwal dan warnaAkhir = dua warna gradasi notifikasinya (format 0xAARRGGBB).
enum class JenisPengingat(val nomor: Int, val jedaHari: Int, val warnaAwal: Int, val warnaAkhir: Int) {
    // Ada aplikasi terkunci tetapi izinnya belum lengkap, sehingga kunci tidak bekerja.
    Izin(1, 1, 0xFFFFB199.toInt(), 0xFFFF7A7A.toInt()),
    // Waktu layar hari ini melewati kelipatan 2 jam (2 jam, 4 jam, 6 jam, dan seterusnya).
    // Boleh lebih dari sekali sehari (jedaHari 0), tetapi hanya sekali untuk tiap kelipatan.
    DuaJam(2, 0, 0xFFFFDD4A.toInt(), 0xFFFF9A4A.toInt()),
    // Ada aplikasi yang dipakai lebih dari 1 jam hari ini tetapi belum dibatasi.
    AplikasiBoros(3, 2, 0xFFFFB3D1.toInt(), 0xFFC9A2FF.toInt()),
    // Belum ada satu pun aplikasi yang dibatasi.
    BelumAdaKunci(4, 3, 0xFF9AD8FF.toInt(), 0xFFB3A6FF.toInt()),
    // Kuis masih memakai soal contoh karena belum ada soal dari materi pengguna.
    SoalContoh(5, 3, 0xFF8FF0DC.toInt(), 0xFF7FC4FF.toInt()),
    // Waktu layar kemarin lebih sedikit daripada hari sebelumnya.
    Kemajuan(6, 2, 0xFFC4F58A.toInt(), 0xFF6FE3B2.toInt()),
    // Rekap 7 hari, tiap Minggu sore.
    RekapMingguan(7, 6, 0xFFD9B8FF.toInt(), 0xFFFF9FD6.toInt()),
    // Kalimat motivasi berisi mimpi pengguna. Paling jarang dan paling rendah urutannya.
    Motivasi(8, 4, 0xFFFFE38A.toInt(), 0xFFFFA8B8.toInt()),
}

// Satu tombol di notifikasi. buka = halaman Tempora yang dibuka (null = Beranda).
// pengaturanHp = kalau diisi, tombol membuka halaman Pengaturan HP itu, bukan Tempora.
data class TombolPengingat(val teks: String, val buka: String? = null, val pengaturanHp: String? = null)

// Satu notifikasi pengingat yang siap ditampilkan.
// teksBesar dan teksKecil = tulisan di gambar berwarna; null berarti notifikasi tanpa gambar.
data class Pengingat(
    val jenis: JenisPengingat,
    val judul: String,
    val isi: String,
    val teksBesar: String? = null,
    val teksKecil: String? = null,
    val buka: String? = null,
    val tombol: List<TombolPengingat> = emptyList(),
)

// Nama aplikasi dan lama pemakaiannya hari ini.
data class PemakaianNama(val nama: String, val ms: Long)

// Gambaran keadaan pada saat pemeriksaan. Aturan di bawah hanya membaca data ini.
data class Keadaan(
    val jumlahTerkunci: Int,
    val izinPemakaian: Boolean,
    val izinTampilDiAtas: Boolean,
    // Total waktu layar hari ini dan aplikasi yang paling lama dipakai.
    val totalHariIniMs: Long,
    val terlamaHariIni: PemakaianNama?,
    // Aplikasi belum dibatasi yang paling lama dipakai hari ini.
    val belumDibatasiTerlama: PemakaianNama?,
    val kuisPakaiSoalContoh: Boolean,
    val adaMateri: Boolean,
    // Waktu layar kemarin dan sehari sebelumnya. 0 kalau tidak dihitung (lihat perluDataKemarin).
    val kemarinMs: Long,
    val lusaMs: Long,
    // Rata-rata waktu layar per hari dan jumlah soal dijawab dalam 7 hari terakhir.
    // 0 kalau tidak dihitung (lihat perluDataMinggu).
    val rataRataMingguMs: Long,
    val soalMingguIni: Int,
    // Mimpi yang ditulis pengguna, atau null kalau belum diisi.
    val mimpi: String?,
)

// Data kemarin dan data seminggu cukup berat dihitung, jadi hanya dikumpulkan pada jam
// ketika aturannya memang bisa terpenuhi.
fun perluDataKemarin(sekarang: LocalDateTime): Boolean = sekarang.hour < JAM_KEMAJUAN_SAMPAI

fun perluDataMinggu(sekarang: LocalDateTime): Boolean =
    sekarang.dayOfWeek == DayOfWeek.SUNDAY && sekarang.hour >= JAM_REKAP_MULAI

// Jarak ke pengingat berikutnya, antara 2 dan 4 jam. Angkanya diturunkan dari waktu pengingat
// terakhir, jadi berubah-ubah seperti acak tetapi tidak perlu disimpan.
fun jedaSetelah(waktuTerakhirMs: Long): Long {
    val rentangMenit = (JEDA_PALING_PANJANG_MS - JEDA_PALING_PENDEK_MS) / 60_000L
    val tambahanMenit = (waktuTerakhirMs / 1_000L) % (rentangMenit + 1)
    return JEDA_PALING_PENDEK_MS + tambahanMenit * 60_000L
}

// Waktu layar dibagi per 2 jam: 0 = belum 2 jam, 1 = sudah 2 jam, 2 = sudah 4 jam, dan seterusnya.
fun tingkatWaktuLayar(totalMs: Long): Int = (totalMs / DUA_JAM_MS).toInt()

// Batas umum: di luar jam yang diizinkan, sudah 4 pengingat hari ini, atau jarak dari
// pengingat terakhir belum cukup (2 sampai 4 jam) -> tidak boleh mengirim apa pun.
fun bolehMengirim(riwayat: RiwayatPengingat, sekarang: LocalDateTime, zona: ZoneId): Boolean {
    if (sekarang.hour < JAM_MULAI || sekarang.hour >= JAM_SELESAI) return false
    if (riwayat.terkirimHariIni >= MAKS_PER_HARI) return false
    val sekarangMs = sekarang.atZone(zona).toInstant().toEpochMilli()
    val palingAkhir = riwayat.terakhir.values.maxOrNull() ?: 0L
    return sekarangMs - palingAkhir >= jedaSetelah(palingAkhir)
}

// Memilih SATU pengingat untuk dikirim sekarang, atau null kalau tidak ada yang layak.
// undian = angka acak dari 0 sampai PELUANG_MOTIVASI - 1, dipakai jenis Motivasi.
fun pilihPengingat(
    keadaan: Keadaan,
    riwayat: RiwayatPengingat,
    sekarang: LocalDateTime,
    zona: ZoneId,
    undian: Int,
): Pengingat? {
    if (!bolehMengirim(riwayat, sekarang, zona)) return null
    val hariIni = sekarang.toLocalDate()
    val jenis = JenisPengingat.entries.firstOrNull { jenis ->
        val terakhirDikirim = riwayat.terakhir[jenis.name]
        val sudahCukupLama = terakhirDikirim == null ||
            ChronoUnit.DAYS.between(tanggal(terakhirDikirim, zona), hariIni) >= jenis.jedaHari
        sudahCukupLama && syaratTerpenuhi(jenis, keadaan, riwayat, sekarang, undian)
    } ?: return null
    return susun(jenis, keadaan, sekarang)
}

// Alasan tiap jenis: pengingat hanya keluar kalau ada kejadian nyata di baliknya.
private fun syaratTerpenuhi(
    jenis: JenisPengingat,
    k: Keadaan,
    riwayat: RiwayatPengingat,
    sekarang: LocalDateTime,
    undian: Int,
): Boolean = when (jenis) {
    JenisPengingat.Izin ->
        k.jumlahTerkunci > 0 && (!k.izinPemakaian || !k.izinTampilDiAtas)

    // Hanya kalau waktu layar sudah naik ke kelipatan 2 jam yang belum pernah diberitahukan hari ini.
    JenisPengingat.DuaJam ->
        tingkatWaktuLayar(k.totalHariIniMs) > riwayat.tingkatLayar

    JenisPengingat.AplikasiBoros ->
        k.jumlahTerkunci > 0 && (k.belumDibatasiTerlama?.ms ?: 0L) >= SATU_JAM_MS

    JenisPengingat.BelumAdaKunci ->
        k.jumlahTerkunci == 0

    JenisPengingat.SoalContoh ->
        k.jumlahTerkunci > 0 && k.kuisPakaiSoalContoh

    JenisPengingat.Kemajuan ->
        perluDataKemarin(sekarang) &&
            k.kemarinMs > 0L &&
            k.lusaMs >= PEMBANDING_PALING_SEDIKIT_MS &&
            k.lusaMs - k.kemarinMs >= HEMAT_PALING_SEDIKIT_MS

    JenisPengingat.RekapMingguan ->
        perluDataMinggu(sekarang) && k.rataRataMingguMs > 0L

    JenisPengingat.Motivasi ->
        !k.mimpi.isNullOrBlank() && undian == 0
}

private val KALIMAT_MOTIVASI = listOf(
    "Satu langkah kecil hari ini tetap berarti.",
    "Tidak perlu sempurna. Cukup sedikit lebih baik dari kemarin.",
    "Waktu yang kamu jaga hari ini adalah bekal untuk mimpimu.",
    "Pelan-pelan saja, yang penting tetap berjalan.",
)

// Menyusun tulisan notifikasi. Tiap notifikasi menyebut kejadian yang memicunya.
private fun susun(jenis: JenisPengingat, k: Keadaan, sekarang: LocalDateTime): Pengingat = when (jenis) {
    JenisPengingat.Izin -> {
        // Kalau izin pemakaian sudah ada, berarti yang kurang adalah izin tampil di atas.
        // Kalau dua-duanya kurang, izin pemakaian disebut lebih dulu karena itu dasarnya.
        val kurangIzinTampil = k.izinPemakaian
        Pengingat(
            jenis = jenis,
            judul = "🔒 Kunci Tempora belum bekerja",
            isi = if (kurangIzinTampil) {
                "Izin \"Tampil di atas aplikasi lain\" belum aktif, jadi layar kuis tidak muncul " +
                    "saat waktu habis. Aktifkan supaya batasmu berlaku."
            } else {
                "Izin \"Akses penggunaan\" belum aktif, jadi Tempora tidak bisa menghitung waktu " +
                    "pemakaian. Aktifkan supaya batasmu berlaku."
            },
            buka = BUKA_ATUR,
            tombol = listOf(
                TombolPengingat(
                    teks = "Aktifkan izin",
                    pengaturanHp = if (kurangIzinTampil) PENGATURAN_TAMPIL_DI_ATAS else PENGATURAN_AKSES_PENGGUNAAN,
                ),
            ),
        )
    }

    JenisPengingat.DuaJam -> Pengingat(
        jenis = jenis,
        judul = "⏳ Ayo batasi waktu yang kamu buang",
        isi = "Waktu layarmu hari ini sudah ${formatBatas(menit(k.totalHariIniMs))}." +
            (k.terlamaHariIni?.let { " Paling lama di ${it.nama}." } ?: ""),
        teksBesar = formatDurasi(k.totalHariIniMs),
        teksKecil = "waktu layar hari ini",
        tombol = listOf(
            TombolPengingat("Atur batas", buka = BUKA_APLIKASI_TERKUNCI),
            TombolPengingat("Lihat progres", buka = BUKA_PROGRES),
        ),
    )

    JenisPengingat.AplikasiBoros -> {
        val aplikasi = k.belumDibatasiTerlama ?: PemakaianNama("Aplikasi", 0L)
        Pengingat(
            jenis = jenis,
            judul = "📱 ${aplikasi.nama} belum punya batas",
            isi = "Sudah dipakai ${formatBatas(menit(aplikasi.ms))} hari ini. Beri batas supaya tidak kebablasan.",
            teksBesar = formatDurasi(aplikasi.ms),
            teksKecil = "${aplikasi.nama} hari ini",
            buka = BUKA_APLIKASI_TERKUNCI,
            tombol = listOf(TombolPengingat("Beri batas", buka = BUKA_APLIKASI_TERKUNCI)),
        )
    }

    JenisPengingat.BelumAdaKunci -> {
        // Kalau ada aplikasi yang sudah lama dipakai hari ini, sebut namanya supaya lebih jelas.
        val aplikasi = k.belumDibatasiTerlama?.takeIf { it.ms >= HEMAT_PALING_SEDIKIT_MS }
        Pengingat(
            jenis = jenis,
            judul = "🎯 Belum ada aplikasi yang dibatasi",
            isi = if (aplikasi != null) {
                "${aplikasi.nama} sudah ${formatBatas(menit(aplikasi.ms))} hari ini. Atur batasnya dengan Tempora."
            } else {
                "Pilih aplikasi yang paling sering menyita waktumu, lalu atur batasnya dengan Tempora."
            },
            teksBesar = aplikasi?.let { formatDurasi(it.ms) },
            teksKecil = aplikasi?.let { "${it.nama} hari ini, belum dibatasi" },
            buka = BUKA_APLIKASI_TERKUNCI,
            tombol = listOf(TombolPengingat("Pilih aplikasi", buka = BUKA_APLIKASI_TERKUNCI)),
        )
    }

    JenisPengingat.SoalContoh -> Pengingat(
        jenis = jenis,
        judul = "📚 Kuis masih memakai soal contoh",
        isi = if (k.adaMateri) {
            "Soal untuk materimu belum dibuat. Buka Materi, lalu ketuk \"Buat soal dengan AI\"."
        } else {
            "Tambahkan PDF materimu supaya soal kuncinya berasal dari pelajaranmu sendiri."
        },
        buka = BUKA_MATERI,
        tombol = listOf(TombolPengingat("Buka Materi", buka = BUKA_MATERI)),
    )

    JenisPengingat.Kemajuan -> {
        val hemat = k.lusaMs - k.kemarinMs
        Pengingat(
            jenis = jenis,
            judul = "🌱 Kemarin lebih hemat ${formatBatas(menit(hemat))}",
            isi = "Waktu layar kemarin ${formatDurasi(k.kemarinMs)}, turun dari ${formatDurasi(k.lusaMs)}. Pertahankan.",
            teksBesar = "-${formatDurasi(hemat)}",
            teksKecil = "dibanding hari sebelumnya",
            buka = BUKA_PROGRES,
            tombol = listOf(TombolPengingat("Lihat progres", buka = BUKA_PROGRES)),
        )
    }

    JenisPengingat.RekapMingguan -> Pengingat(
        jenis = jenis,
        judul = "📊 Rekap 7 hari terakhir",
        isi = "Rata-rata waktu layar ${formatDurasi(k.rataRataMingguMs)} per hari, ${k.soalMingguIni} soal dijawab.",
        teksBesar = formatDurasi(k.rataRataMingguMs),
        teksKecil = "rata-rata per hari, ${k.soalMingguIni} soal dijawab",
        buka = BUKA_PROGRES,
        tombol = listOf(TombolPengingat("Lihat progres", buka = BUKA_PROGRES)),
    )

    JenisPengingat.Motivasi -> Pengingat(
        jenis = jenis,
        judul = "✨ Ingat mimpimu",
        // Kalimatnya berganti tiap hari, bergiliran.
        isi = "“${k.mimpi.orEmpty()}”\n" + KALIMAT_MOTIVASI[sekarang.dayOfYear % KALIMAT_MOTIVASI.size],
        tombol = listOf(TombolPengingat("Buka Tempora")),
    )
}

private fun tanggal(ms: Long, zona: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate()

private fun menit(ms: Long): Int = (ms / 60_000L).toInt()
