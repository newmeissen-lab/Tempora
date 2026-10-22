package com.tempora.app

import android.app.Application
import com.tempora.app.data.AplikasiTerpasangRepository
import com.tempora.app.data.GeminiApi
import com.tempora.app.data.MateriRepository
import com.tempora.app.data.PemakaianRepository
import com.tempora.app.data.Pengaturan
import com.tempora.app.data.TemporaDatabase
import com.tempora.app.pengingat.PemeriksaPengingat
import com.tempora.app.pengingat.PengingatWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Dibuat Android satu kali saat aplikasi mulai. Menyimpan satu database dan satu set
// "repository" (pengambil data), supaya semua layar memakai objek yang sama.
// "by lazy" berarti tiap objek baru dibuat saat pertama kali dipakai.
class TemporaApplication : Application() {
    // Lingkup untuk pekerjaan yang harus tetap jalan walau pengguna pindah layar,
    // misalnya menunggu jawaban AI.
    val lingkup = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { TemporaDatabase.buat(this) }
    val pengaturan by lazy { Pengaturan(this) }
    val pemakaian by lazy { PemakaianRepository(this) }
    val aplikasiTerpasang by lazy { AplikasiTerpasangRepository(this) }

    // API key diambil dari local.properties saat build (lihat app/build.gradle.kts).
    private val gemini by lazy { GeminiApi(BuildConfig.GEMINI_API_KEY) }
    val materi by lazy {
        MateriRepository(this, database.materiDao(), database.soalDao(), pengaturan, gemini, lingkup)
    }

    // Satu pemeriksa pengingat untuk seluruh aplikasi, dipakai layanan pemantau dan PengingatWorker.
    val pemeriksaPengingat by lazy { PemeriksaPengingat(this) }

    override fun onCreate() {
        super.onCreate()
        // Pemeriksaan pengingat berkala; WorkManager mengingat jadwalnya walau HP dinyalakan ulang.
        PengingatWorker.jadwalkan(this)
    }
}
