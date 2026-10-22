package com.tempora.app.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

// Pemakaian satu hari: lama tiap aplikasi tampil di layar, dalam milidetik.
data class PemakaianHarian(val tanggal: LocalDate, val perPaket: Map<String, Long>)

// Pemakaian hari ini sampai detik ini, ditambah aplikasi yang sedang tampil di layar.
data class PemakaianSekarang(val perPaket: Map<String, Long>, val sedangTampil: Set<String>)

// Membaca lama pemakaian aplikasi dari catatan Android (UsageStatsManager).
class PemakaianRepository(private val context: Context) {

    // "Akses penggunaan" bukan izin biasa: tidak bisa diminta lewat dialog, pengguna harus
    // mengaktifkannya sendiri di Pengaturan. Fungsi ini hanya memeriksa statusnya.
    fun izinDiberikan(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Pemakaian sejak pukul 00.00 hari ini sampai sekarang.
    suspend fun hariIni(): Map<String, Long> = withContext(Dispatchers.IO) {
        dalamRentang(awalHari(LocalDate.now()), System.currentTimeMillis())
    }

    // Dipakai layanan pemantau: pemakaian hari ini dan aplikasi yang sedang tampil saat ini.
    suspend fun sekarang(): PemakaianSekarang = withContext(Dispatchers.IO) {
        hitung(awalHari(LocalDate.now()), System.currentTimeMillis())
    }

    // Pemeriksaan ringan untuk layanan pemantau: apakah dalam rentang waktu pendek ini ada
    // layar aplikasi yang baru tampil atau tertutup. Kalau ada, barulah pemakaian dihitung ulang.
    suspend fun adaPergantianLayar(mulai: Long, selesai: Long): Boolean = withContext(Dispatchers.IO) {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val peristiwa = usageStats.queryEvents(mulai, selesai) ?: return@withContext false
        val satu = UsageEvents.Event()
        while (peristiwa.hasNextEvent()) {
            peristiwa.getNextEvent(satu)
            if (satu.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                satu.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            ) {
                return@withContext true
            }
        }
        false
    }

    // Pemakaian beberapa hari terakhir, dari yang paling lama sampai hari ini.
    suspend fun perHari(jumlahHari: Int): List<PemakaianHarian> = withContext(Dispatchers.IO) {
        val hariIni = LocalDate.now()
        val sekarang = System.currentTimeMillis()
        (jumlahHari - 1 downTo 0).map { mundur ->
            val tanggal = hariIni.minusDays(mundur.toLong())
            val perPaket = if (mundur == 0) {
                dalamRentang(awalHari(tanggal), sekarang)
            } else {
                // Pemakaian hari yang sudah lewat tidak berubah lagi, jadi cukup dihitung sekali.
                // Hasil kosong tidak disimpan: bisa jadi izinnya waktu itu belum aktif.
                hariLalu[tanggal] ?: dalamRentang(awalHari(tanggal), awalHari(tanggal.plusDays(1)))
                    .also { if (it.isNotEmpty()) hariLalu[tanggal] = it }
            }
            PemakaianHarian(tanggal, perPaket)
        }
    }

    private val hariLalu = ConcurrentHashMap<LocalDate, Map<String, Long>>()

    private fun awalHari(tanggal: LocalDate): Long =
        tanggal.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Android mencatat peristiwa "layar aplikasi tampil" dan "layar aplikasi tertutup".
    // Lama pemakaian dihitung dari jarak waktu antara dua peristiwa itu, per aplikasi.
    private fun dalamRentang(mulai: Long, selesai: Long): Map<String, Long> =
        hitung(mulai, selesai).perPaket

    private fun hitung(mulai: Long, selesai: Long): PemakaianSekarang {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val peristiwa = usageStats.queryEvents(mulai, selesai)
            ?: return PemakaianSekarang(emptyMap(), emptySet())

        val total = HashMap<String, Long>()
        // Layar (activity) tiap aplikasi yang sedang tampil, dan kapan aplikasi itu mulai tampil.
        val layarTampil = HashMap<String, MutableSet<String>>()
        val mulaiTampil = HashMap<String, Long>()
        val sudahMuncul = HashSet<String>()

        fun tutup(paket: String, waktu: Long) {
            val awal = mulaiTampil.remove(paket) ?: return
            total[paket] = (total[paket] ?: 0L) + (waktu - awal).coerceAtLeast(0L)
        }

        val satu = UsageEvents.Event()
        while (peristiwa.hasNextEvent()) {
            peristiwa.getNextEvent(satu)
            val paket = satu.packageName ?: continue
            val layar = satu.className ?: ""

            when (satu.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val tampil = layarTampil.getOrPut(paket) { HashSet() }
                    if (tampil.isEmpty()) mulaiTampil[paket] = satu.timeStamp
                    tampil += layar
                    sudahMuncul += paket
                }

                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val tampil = layarTampil[paket]
                    if (tampil != null && tampil.remove(layar)) {
                        if (tampil.isEmpty()) tutup(paket, satu.timeStamp)
                    } else if (satu.eventType == UsageEvents.Event.ACTIVITY_PAUSED && paket !in sudahMuncul) {
                        // Aplikasi sudah terbuka sejak sebelum rentang dimulai,
                        // misalnya dipakai melewati tengah malam.
                        total[paket] = (total[paket] ?: 0L) + (satu.timeStamp - mulai)
                    }
                    sudahMuncul += paket
                }

                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    mulaiTampil.keys.toList().forEach { tutup(it, satu.timeStamp) }
                    layarTampil.clear()
                }

                // HP mati mendadak tanpa catatan penutup: waktu yang masih terbuka tidak dihitung.
                UsageEvents.Event.DEVICE_STARTUP -> {
                    mulaiTampil.clear()
                    layarTampil.clear()
                }
            }
        }

        // Aplikasi yang masih tampil sampai akhir rentang, termasuk yang sedang dibuka sekarang.
        val sedangTampil = mulaiTampil.keys.toSet()
        sedangTampil.forEach { tutup(it, selesai) }
        return PemakaianSekarang(total, sedangTampil)
    }
}
