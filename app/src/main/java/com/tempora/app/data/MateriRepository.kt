package com.tempora.app.data

import android.content.Context
import android.database.sqlite.SQLiteException
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

// Batas PDF yang dikirim ke AI. Gemini sendiri menerima sampai 1000 halaman dan 50 MB, tetapi
// PDF sebesar itu lama diunggah dari HP, menghabiskan jatah gratis, dan hasilnya terlalu dangkal.
const val MAKS_HALAMAN_AI = 100
const val MAKS_UKURAN_AI_MB = 20

// Jumlah soal yang diminta mengikuti panjang materi: 3 soal per halaman, antara 5 dan 20.
private const val SOAL_PER_HALAMAN = 3
private const val SOAL_PALING_SEDIKIT = 5
private const val SOAL_PALING_BANYAK = 20

// Mengurus PDF materi: menyalin file ke penyimpanan Tempora, mencatatnya di database,
// dan meminta AI membuat soal serta rangkumannya.
class MateriRepository(
    private val context: Context,
    private val dao: MateriDao,
    private val soalDao: SoalDao,
    private val pengaturan: Pengaturan,
    private val gemini: GeminiApi,
    // Lingkup milik aplikasi: pekerjaan AI tetap berjalan walau pengguna pindah layar.
    private val lingkup: CoroutineScope,
) {
    val semua: Flow<List<Materi>> = dao.semua()

    // id materi -> jumlah soal yang sudah tersimpan.
    val jumlahSoal: Flow<Map<Long, Int>> =
        soalDao.jumlahPerMateri().map { daftar -> daftar.associate { it.idMateri to it.jumlah } }

    // Keadaan pekerjaan AI hanya disimpan di memori: materi yang sedang diproses,
    // dan pesan gagal terakhir per materi.
    private val _sedangDibuat = MutableStateFlow<Set<Long>>(emptySet())
    val sedangDibuat: StateFlow<Set<Long>> = _sedangDibuat.asStateFlow()
    private val _gagalDibuat = MutableStateFlow<Map<Long, String>>(emptyMap())
    val gagalDibuat: StateFlow<Map<Long, String>> = _gagalDibuat.asStateFlow()

    // Permintaan ke AI dijalankan satu per satu supaya tidak melewati batas per menit.
    private val antrean = Mutex()

    private val folder: File
        get() = File(context.filesDir, "materi").apply { mkdirs() }

    // Mengembalikan null kalau berhasil, atau kalimat penjelasan kalau gagal.
    // PDF disalin supaya tetap bisa dibaca walau file aslinya dipindah atau dihapus.
    suspend fun tambahDariPdf(uri: Uri): String? = withContext(Dispatchers.IO) {
        val salinan = File(folder, "${UUID.randomUUID()}.pdf")

        try {
            val sumber = context.contentResolver.openInputStream(uri)
                ?: return@withContext "File tidak bisa dibuka."
            sumber.use { masuk -> salinan.outputStream().use { keluar -> masuk.copyTo(keluar) } }
        } catch (e: IOException) {
            salinan.delete()
            return@withContext "File tidak bisa disalin. Coba pilih lagi."
        } catch (e: SecurityException) {
            salinan.delete()
            return@withContext "Tempora tidak diizinkan membuka file itu."
        }

        val jumlahHalaman = try {
            hitungHalaman(salinan)
        } catch (e: IOException) {
            salinan.delete()
            return@withContext "File ini bukan PDF yang bisa dibaca."
        } catch (e: SecurityException) {
            // PdfRenderer melempar SecurityException untuk PDF yang dikunci kata sandi.
            salinan.delete()
            return@withContext "PDF ini dikunci kata sandi, jadi tidak bisa dibuka."
        }

        val materi = Materi(
            judul = judulDari(uri),
            namaFile = salinan.name,
            jumlahHalaman = jumlahHalaman,
            ditambahkanPada = System.currentTimeMillis(),
        )
        val id = dao.tambah(materi)
        // Materi pertama langsung dipakai untuk soal kunci, supaya selalu ada yang terpilih.
        if (pengaturan.idMateriKunci.first() == null) pengaturan.pilihMateriKunci(id)
        // Soal dan rangkuman langsung dibuatkan AI tanpa perlu diminta.
        buatSoal(materi.copy(id = id))
        null
    }

    suspend fun hapus(materi: Materi) = withContext(Dispatchers.IO) {
        dao.hapus(materi)
        File(folder, materi.namaFile).delete()
        _gagalDibuat.update { it - materi.id }
        if (pengaturan.idMateriKunci.first() == materi.id) pengaturan.pilihMateriKunci(null)
    }

    // Meminta AI membuat soal dan rangkuman untuk satu materi. Fungsi ini langsung kembali;
    // kemajuannya bisa diamati lewat sedangDibuat, gagalDibuat, dan jumlahSoal.
    fun buatSoal(materi: Materi) {
        if (materi.id in _sedangDibuat.value) return
        _sedangDibuat.update { it + materi.id }
        _gagalDibuat.update { it - materi.id }
        lingkup.launch {
            val pesanGagal = try {
                antrean.withLock { prosesDenganAi(materi) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Kesalahan yang tidak diperkirakan tidak boleh membuat aplikasi tertutup.
                Log.w("TemporaGemini", "Membuat soal gagal", e)
                "Terjadi kesalahan saat membuat soal. Coba lagi."
            } finally {
                _sedangDibuat.update { it - materi.id }
            }
            if (pesanGagal != null) _gagalDibuat.update { it + (materi.id to pesanGagal) }
        }
    }

    // Mengembalikan null kalau berhasil, atau kalimat penjelasan kalau gagal.
    private suspend fun prosesDenganAi(materi: Materi): String? = withContext(Dispatchers.IO) {
        val pdf = File(folder, materi.namaFile)
        if (!pdf.exists()) return@withContext "File PDF materi ini sudah tidak ada. Hapus lalu tambahkan lagi."
        if (materi.jumlahHalaman > MAKS_HALAMAN_AI) {
            return@withContext "PDF ini ${materi.jumlahHalaman} halaman. Paling banyak $MAKS_HALAMAN_AI halaman; " +
                "pisahkan per bab dulu."
        }
        if (pdf.length() > MAKS_UKURAN_AI_MB * 1024L * 1024L) {
            return@withContext "Ukuran PDF ini lebih dari $MAKS_UKURAN_AI_MB MB. Pakai PDF yang lebih kecil."
        }

        val jumlahDiminta = (materi.jumlahHalaman * SOAL_PER_HALAMAN)
            .coerceIn(SOAL_PALING_SEDIKIT, SOAL_PALING_BANYAK)
        val bahan = when (val hasil = gemini.buatBahan(pdf, jumlahDiminta)) {
            is HasilAi.Gagal -> return@withContext hasil.pesan
            is HasilAi.Berhasil -> hasil.bahan
        }

        // Materinya bisa saja dihapus pengguna selagi AI bekerja.
        if (dao.cari(materi.id) == null) return@withContext null
        try {
            soalDao.ganti(materi.id, bahan.soal.map { it.keBaris(materi.id) })
            dao.simpanRangkuman(materi.id, bahan.rangkuman)
        } catch (e: SQLiteException) {
            // Materi terhapus tepat saat hasilnya disimpan.
            return@withContext null
        }
        null
    }

    // Urutan pilihan diacak sebelum disimpan, karena AI cenderung menaruh jawaban benar
    // di posisi yang sama.
    private fun SoalAi.keBaris(idMateri: Long): SoalMateri {
        val jawabanBenar = pilihan[indeksBenar]
        val acak = pilihan.shuffled()
        return SoalMateri(
            idMateri = idMateri,
            pertanyaan = pertanyaan,
            pilihanA = acak[0],
            pilihanB = acak[1],
            pilihanC = acak[2],
            pilihanD = acak[3],
            indeksBenar = acak.indexOf(jawabanBenar),
            penjelasan = penjelasan,
        )
    }

    private fun hitungHalaman(file: File): Int =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { berkas ->
            PdfRenderer(berkas).use { it.pageCount }
        }

    // Judul diambil dari nama file tanpa ".pdf".
    private fun judulDari(uri: Uri): String {
        val namaFile = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { kursor -> if (kursor.moveToFirst()) kursor.getString(0) else null }
        return namaFile?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Materi tanpa judul"
    }
}
