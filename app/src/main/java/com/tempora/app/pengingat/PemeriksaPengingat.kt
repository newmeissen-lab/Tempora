package com.tempora.app.pengingat

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.tempora.app.TemporaApplication
import com.tempora.app.kuis.muatBahanKuis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

// Mengumpulkan keadaan saat ini (pemakaian, izin, materi), menanyakan ke aturan di
// AturanPengingat.kt apakah ada pengingat yang layak dikirim, lalu menampilkannya.
// Dipanggil dari dua tempat: layanan pemantau (saat pengguna mulai memakai HP) dan
// PengingatWorker (pemeriksaan berkala, juga saat belum ada aplikasi yang dikunci).
class PemeriksaPengingat(private val app: TemporaApplication) {
    // Gembok supaya dua pemeriksaan yang kebetulan bersamaan tidak mengirim dua pengingat.
    private val gembok = Mutex()

    // Aman dipanggil dari mana saja: kesalahan dicatat ke Logcat, tidak menutup aplikasi.
    suspend fun periksa() {
        try {
            gembok.withLock { periksaSekarang() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TemporaPengingat", "Pemeriksaan pengingat gagal", e)
        }
    }

    private suspend fun periksaSekarang() {
        // Pengguna bisa mematikan pengingat di layar Atur, atau menolak izin notifikasi.
        if (!app.pengaturan.pengingatAktif.first()) return
        if (!app.getSystemService(NotificationManager::class.java).areNotificationsEnabled()) return
        // Pengingat hanya dikirim saat pengguna sedang memakai HP: layar menyala dan tidak terkunci.
        if (!penggunaSedangAktif()) return

        val sekarang = LocalDateTime.now()
        val zona = ZoneId.systemDefault()
        val riwayat = app.pengaturan.riwayatPengingat()
        // Batas umum diperiksa lebih dulu supaya data tidak dikumpulkan sia-sia.
        if (!bolehMengirim(riwayat, sekarang, zona)) return

        val keadaan = kumpulkan(sekarang, zona)
        val pengingat = pilihPengingat(
            keadaan = keadaan,
            riwayat = riwayat,
            sekarang = sekarang,
            zona = zona,
            undian = Random.nextInt(PELUANG_MOTIVASI),
        ) ?: return

        tampilkanPengingat(app, pengingat)
        app.pengaturan.catatPengingat(
            jenis = pengingat.jenis.name,
            waktu = System.currentTimeMillis(),
            // Untuk pengingat waktu layar, kelipatan 2 jam yang baru diberitahukan ikut dicatat
            // supaya tidak diulang sebelum naik ke kelipatan berikutnya.
            tingkatLayar = if (pengingat.jenis == JenisPengingat.DuaJam) {
                tingkatWaktuLayar(keadaan.totalHariIniMs)
            } else {
                null
            },
        )
    }

    private fun penggunaSedangAktif(): Boolean {
        val layarMenyala = app.getSystemService(PowerManager::class.java).isInteractive
        val terkunci = app.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        return layarMenyala && !terkunci
    }

    private suspend fun kumpulkan(sekarang: LocalDateTime, zona: ZoneId): Keadaan {
        val dibatasi = app.database.aplikasiDibatasiDao().semua().first()
        val paketDibatasi = dibatasi.map { it.paket }.toSet()
        val izinPemakaian = app.pemakaian.izinDiberikan()

        // Sama seperti di Beranda: hanya aplikasi yang punya ikon yang dihitung.
        val berikon = if (izinPemakaian) app.aplikasiTerpasang.paketBerikon() else emptySet()
        val hariIni = if (izinPemakaian) {
            app.pemakaian.hariIni().filterKeys { it in berikon }
        } else {
            emptyMap()
        }
        val terlama = hariIni.maxByOrNull { it.value }
        // Tempora sendiri tidak bisa dikunci, jadi tidak disarankan untuk dibatasi.
        val belumDibatasi = hariIni
            .filterKeys { it !in paketDibatasi && it != app.packageName }
            .maxByOrNull { it.value }

        var kemarinMs = 0L
        var lusaMs = 0L
        if (izinPemakaian && perluDataKemarin(sekarang)) {
            // Urutannya: dua hari lalu, kemarin, hari ini.
            val tigaHari = app.pemakaian.perHari(3).map { hari ->
                hari.perPaket.filterKeys { it in berikon }.values.sum()
            }
            lusaMs = tigaHari[0]
            kemarinMs = tigaHari[1]
        }

        var rataRataMingguMs = 0L
        var soalMingguIni = 0
        if (perluDataMinggu(sekarang)) {
            if (izinPemakaian) {
                // Hari tanpa catatan pemakaian tidak ikut dirata-rata.
                val perHari = app.pemakaian.perHari(7)
                    .map { hari -> hari.perPaket.filterKeys { it in berikon }.values.sum() }
                    .filter { it > 0L }
                if (perHari.isNotEmpty()) rataRataMingguMs = perHari.sum() / perHari.size
            }
            val awalMinggu = sekarang.toLocalDate().minusDays(6).atStartOfDay(zona).toInstant().toEpochMilli()
            soalMingguIni = app.database.jawabanDao().semuaWaktu().first().count { it >= awalMinggu }
        }

        return Keadaan(
            jumlahTerkunci = dibatasi.size,
            izinPemakaian = izinPemakaian,
            izinTampilDiAtas = Settings.canDrawOverlays(app),
            totalHariIniMs = hariIni.values.sum(),
            terlamaHariIni = terlama?.let { PemakaianNama(nama(it.key), it.value) },
            belumDibatasiTerlama = belumDibatasi?.let { PemakaianNama(nama(it.key), it.value) },
            kuisPakaiSoalContoh = muatBahanKuis(app).contoh,
            adaMateri = app.materi.semua.first().isNotEmpty(),
            kemarinMs = kemarinMs,
            lusaMs = lusaMs,
            rataRataMingguMs = rataRataMingguMs,
            soalMingguIni = soalMingguIni,
            mimpi = app.pengaturan.motivasi.first()?.mimpi,
        )
    }

    private fun nama(paket: String): String = try {
        val pm = app.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(paket, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        paket
    }
}
