package com.tempora.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// DataStore menyimpan pengaturan kecil sebagai pasangan kunci dan nilai di file "pengaturan".
private val Context.dataStore by preferencesDataStore(name = "pengaturan")
private val ID_MATERI_KUNCI = longPreferencesKey("id_materi_kunci")
private val TAMBAHAN_WAKTU = stringPreferencesKey("tambahan_waktu")
private val WAKTU_TUNGGU = stringPreferencesKey("waktu_tunggu")
private val ALASAN = stringPreferencesKey("alasan")
private val MIMPI = stringPreferencesKey("mimpi")

private val SALAH_BERUNTUN = intPreferencesKey("salah_beruntun")
private val JEDA_SOAL_SAMPAI = longPreferencesKey("jeda_soal_sampai")
private val PENGINGAT_AKTIF = booleanPreferencesKey("pengingat_aktif")
private const val AWALAN_PENGINGAT = "pengingat_terakhir_"
private val PENGINGAT_HARI = stringPreferencesKey("pengingat_hari")
private val PENGINGAT_JUMLAH = intPreferencesKey("pengingat_jumlah")
private val PENGINGAT_TINGKAT_LAYAR = intPreferencesKey("pengingat_tingkat_layar")

// Jawaban pengguna atas dua pertanyaan di awal: kenapa ingin membatasi, dan apa mimpinya.
data class Motivasi(val alasan: String, val mimpi: String)

// Catatan jawaban salah di layar kuis. beruntun = jumlah jawaban salah berturut-turut saat ini.
// jedaSampai = jam berakhirnya jeda setelah terlalu banyak salah (milidetik sejak 1 Januari 1970);
// 0 berarti tidak ada jeda. Disimpan supaya tidak hilang saat layar kuis ditutup lalu dibuka lagi.
data class CatatanSalah(val beruntun: Int = 0, val jedaSampai: Long = 0L)

// Riwayat notifikasi pengingat.
// terakhir = kapan tiap jenis terakhir dikirim (nama jenis -> milidetik).
// terkirimHariIni = jumlah pengingat yang sudah dikirim hari ini.
// tingkatLayar = kelipatan 2 jam waktu layar yang sudah diberitahukan hari ini (0 = belum ada).
data class RiwayatPengingat(
    val terakhir: Map<String, Long> = emptyMap(),
    val terkirimHariIni: Int = 0,
    val tingkatLayar: Int = 0,
)

class Pengaturan(private val context: Context) {
    // id materi yang dipakai untuk soal kunci. null berarti belum ada yang dipilih.
    val idMateriKunci: Flow<Long?> = context.dataStore.data.map { it[ID_MATERI_KUNCI] }

    suspend fun pilihMateriKunci(id: Long?) {
        context.dataStore.edit { isi ->
            if (id == null) isi.remove(ID_MATERI_KUNCI) else isi[ID_MATERI_KUNCI] = id
        }
    }

    // null berarti pengguna belum pernah mengisinya.
    val motivasi: Flow<Motivasi?> = context.dataStore.data.map { isi ->
        val alasan = isi[ALASAN]
        val mimpi = isi[MIMPI]
        if (alasan.isNullOrBlank() || mimpi.isNullOrBlank()) null else Motivasi(alasan, mimpi)
    }

    suspend fun simpanMotivasi(alasan: String, mimpi: String) {
        context.dataStore.edit { isi ->
            isi[ALASAN] = alasan
            isi[MIMPI] = mimpi
        }
    }

    val catatanSalah: Flow<CatatanSalah> = context.dataStore.data.map { isi ->
        CatatanSalah(beruntun = isi[SALAH_BERUNTUN] ?: 0, jedaSampai = isi[JEDA_SOAL_SAMPAI] ?: 0L)
    }

    suspend fun simpanCatatanSalah(catatan: CatatanSalah) {
        context.dataStore.edit { isi ->
            isi[SALAH_BERUNTUN] = catatan.beruntun
            isi[JEDA_SOAL_SAMPAI] = catatan.jedaSampai
        }
    }

    // Notifikasi pengingat boleh dimatikan pengguna dari layar Atur. Bawaannya menyala.
    val pengingatAktif: Flow<Boolean> = context.dataStore.data.map { it[PENGINGAT_AKTIF] ?: true }

    suspend fun aturPengingat(aktif: Boolean) {
        context.dataStore.edit { it[PENGINGAT_AKTIF] = aktif }
    }

    // Riwayat pengingat, dipakai untuk memastikan pengingat tidak terlalu sering.
    suspend fun riwayatPengingat(): RiwayatPengingat {
        val isi = context.dataStore.data.first()
        // Hitungan harian hanya berlaku kalau memang dicatat hari ini.
        val dicatatHariIni = isi[PENGINGAT_HARI] == LocalDate.now().toString()
        return RiwayatPengingat(
            terakhir = isi.asMap().mapNotNull { (kunci, nilai) ->
                if (kunci.name.startsWith(AWALAN_PENGINGAT) && nilai is Long) {
                    kunci.name.removePrefix(AWALAN_PENGINGAT) to nilai
                } else {
                    null
                }
            }.toMap(),
            terkirimHariIni = if (dicatatHariIni) isi[PENGINGAT_JUMLAH] ?: 0 else 0,
            tingkatLayar = if (dicatatHariIni) isi[PENGINGAT_TINGKAT_LAYAR] ?: 0 else 0,
        )
    }

    // Dipanggil tiap satu pengingat dikirim. tingkatLayar diisi hanya untuk pengingat waktu
    // layar: kelipatan 2 jam yang baru saja diberitahukan.
    suspend fun catatPengingat(jenis: String, waktu: Long, tingkatLayar: Int?) {
        context.dataStore.edit { isi ->
            val tanggal = LocalDate.now().toString()
            // Hari berganti: hitungan harian dimulai dari nol lagi.
            if (isi[PENGINGAT_HARI] != tanggal) {
                isi[PENGINGAT_HARI] = tanggal
                isi[PENGINGAT_JUMLAH] = 0
                isi[PENGINGAT_TINGKAT_LAYAR] = 0
            }
            isi[longPreferencesKey(AWALAN_PENGINGAT + jenis)] = waktu
            isi[PENGINGAT_JUMLAH] = (isi[PENGINGAT_JUMLAH] ?: 0) + 1
            if (tingkatLayar != null) isi[PENGINGAT_TINGKAT_LAYAR] = tingkatLayar
        }
    }

    // Tambahan waktu hasil menjawab soal dengan benar, hanya berlaku hari ini.
    // Isinya paket aplikasi -> batas baru: aplikasi boleh dipakai sampai total pemakaiannya
    // hari ini mencapai angka itu (dalam milidetik).
    val tambahanWaktu: Flow<Map<String, Long>> =
        context.dataStore.data.map { bacaHariIni(it[TAMBAHAN_WAKTU]) }

    suspend fun beriTambahanWaktu(paket: String, sampaiMs: Long) {
        ubahHariIni(TAMBAHAN_WAKTU) { it + (paket to sampaiMs) }
    }

    // Waktu tunggu sebagai ganti menjawab soal, hanya berlaku hari ini.
    // Isinya paket aplikasi -> jam selesainya waktu tunggu (milidetik sejak 1 Januari 1970).
    val waktuTunggu: Flow<Map<String, Long>> =
        context.dataStore.data.map { bacaHariIni(it[WAKTU_TUNGGU]) }

    suspend fun mulaiTunggu(paket: String, sampaiMs: Long) {
        ubahHariIni(WAKTU_TUNGGU) { it + (paket to sampaiMs) }
    }

    suspend fun hapusTunggu(paket: String) {
        ubahHariIni(WAKTU_TUNGGU) { it - paket }
    }

    private suspend fun ubahHariIni(
        kunci: Preferences.Key<String>,
        ubah: (Map<String, Long>) -> Map<String, Long>,
    ) {
        context.dataStore.edit { isi ->
            val baru = ubah(bacaHariIni(isi[kunci]))
            isi[kunci] = (listOf(LocalDate.now().toString()) + baru.map { "${it.key}=${it.value}" })
                .joinToString("\n")
        }
    }

    // Disimpan sebagai tulisan: baris pertama tanggal, baris berikutnya "paket=angka".
    // Kalau tanggalnya bukan hari ini, semua isinya sudah hangus.
    private fun bacaHariIni(teks: String?): Map<String, Long> {
        val baris = teks?.lines().orEmpty()
        if (baris.firstOrNull() != LocalDate.now().toString()) return emptyMap()
        return baris.drop(1).mapNotNull { satu ->
            val bagian = satu.split("=")
            val angka = bagian.getOrNull(1)?.toLongOrNull()
            if (bagian.size == 2 && angka != null) bagian[0] to angka else null
        }.toMap()
    }
}
