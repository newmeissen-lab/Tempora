package com.tempora.app.pengingat

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tempora.app.TemporaApplication
import java.util.concurrent.TimeUnit

private const val NAMA_PEKERJAAN = "pengingat"

// 15 menit adalah jarak terpendek yang diizinkan Android untuk pekerjaan berkala.
private const val JEDA_PERIKSA_MENIT = 15L

// Pekerjaan berkala yang dijalankan WorkManager kira-kira tiap 15 menit, juga saat Tempora
// tidak dibuka dan saat belum ada aplikasi yang dikunci. Tiap kali jalan, ia hanya memeriksa;
// pengingat baru dikirim kalau pengguna sedang memakai HP dan aturannya terpenuhi
// (lihat AturanPengingat.kt), jadi sebagian besar pemeriksaan tidak menghasilkan apa-apa.
class PengingatWorker(context: Context, parameter: WorkerParameters) : CoroutineWorker(context, parameter) {

    override suspend fun doWork(): Result {
        (applicationContext as TemporaApplication).pemeriksaPengingat.periksa()
        return Result.success()
    }

    companion object {
        // Aman dipanggil berkali-kali. UPDATE berarti jadwal yang sudah ada disesuaikan dengan
        // aturan terbaru tanpa mengulang hitungannya dari awal.
        // Pemeriksaan pertama ditunda 30 menit, supaya pengingat tidak muncul tepat saat
        // Tempora baru dipasang.
        fun jadwalkan(context: Context) {
            val permintaan = PeriodicWorkRequestBuilder<PengingatWorker>(JEDA_PERIKSA_MENIT, TimeUnit.MINUTES)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAMA_PEKERJAAN, ExistingPeriodicWorkPolicy.UPDATE, permintaan,
            )
        }
    }
}
