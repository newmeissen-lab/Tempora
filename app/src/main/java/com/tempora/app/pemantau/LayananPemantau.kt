package com.tempora.app.pemantau

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import com.tempora.app.MainActivity
import com.tempora.app.R
import com.tempora.app.TemporaApplication
import com.tempora.app.data.AplikasiDibatasi
import com.tempora.app.data.Jawaban
import com.tempora.app.kuis.LapisanKuis
import com.tempora.app.kuis.muatBahanKuis
import com.tempora.app.ui.formatBatas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val KANAL_PEMANTAU = "pemantau"
private const val KANAL_PERINGATAN = "peringatan"
private const val ID_NOTIFIKASI_PEMANTAU = 1

// Ada dua macam pemeriksaan selama layar HP menyala:
// - "intip" tiap 1,5 detik: murah, hanya melihat apakah ada aplikasi yang baru dibuka/ditutup;
// - "periksa" penuh: menghitung pemakaian hari ini. Dijalankan kalau intip menemukan
//   pergantian, atau paling lambat tiap 10 detik.
private const val JEDA_INTIP_MS = 1_500L
private const val JEDA_PERIKSA_MS = 10_000L

// Tiap intip melihat sedikit ke belakang, supaya peristiwa yang terlambat dicatat Android
// tidak terlewat.
private const val MUNDUR_INTIP_MS = 2_000L

// Setelah pengguna memilih "Tidak jadi", kuis tidak dimunculkan lagi selama ini,
// memberi waktu bagi layar utama HP untuk tampil.
private const val TUNDA_SETELAH_BATAL_MS = 3_000L

// Selama HP dipakai, layanan ini juga menanyakan apakah ada notifikasi pengingat yang layak
// dikirim, paling sering tiap 5 menit. Seberapa sering pengingat benar-benar keluar diatur
// di AturanPengingat.kt (berjarak 2 sampai 4 jam).
private const val JEDA_PERIKSA_PENGINGAT_MS = 5 * 60_000L

// Peringatan muncul 5 menit sebelum batas habis. Untuk batas yang pendek (10 menit atau
// kurang) dan untuk tambahan waktu dari kuis, peringatannya 1 menit sebelum habis.
private const val PERINGATAN_MS = 5 * 60_000L
private const val PERINGATAN_BATAS_PENDEK_MS = 60_000L
private const val BATAS_PENDEK_MENIT = 10

// Layanan latar depan (foreground service): tetap berjalan walau Tempora ditutup, dengan
// syarat menampilkan notifikasi tetap. Tugasnya memeriksa pemakaian aplikasi terkunci,
// mengirim peringatan saat batasnya hampir habis, dan memunculkan layar kuis saat habis.
class LayananPemantau : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tugasPantau: Job? = null

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private var teksPemantau = ""

    private val lapisanKuis by lazy { LapisanKuis(this) }

    // Waktu memakai jam "sejak HP menyala" (elapsedRealtime), yang tidak terpengaruh
    // perubahan jam oleh pengguna.
    private var periksaLagiPada = 0L
    private var tundaKuisSampai = 0L
    private var periksaPengingatLagiPada = 0L

    // Mencatat notifikasi yang sudah dikirim supaya tidak berulang tiap pemeriksaan.
    // Isinya paket aplikasi -> "tanggal/batas"; besok atau saat batas berubah, dikirim lagi.
    private val sudahDiperingatkan = HashMap<String, String>()
    private val sudahHabis = HashMap<String, String>()
    private val namaAplikasi = HashMap<String, String>()

    // Layanan ini tidak dihubungkan (bind) ke layar mana pun.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Kanal = kelompok notifikasi yang bisa diatur pengguna di Pengaturan HP.
        // LOW tidak berbunyi; HIGH muncul di atas layar.
        notificationManager.createNotificationChannel(
            NotificationChannel(KANAL_PEMANTAU, "Pemantau berjalan", NotificationManager.IMPORTANCE_LOW),
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(KANAL_PERINGATAN, "Peringatan batas waktu", NotificationManager.IMPORTANCE_HIGH),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!jadikanLatarDepan()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (tugasPantau == null) tugasPantau = scope.launch { pantau() }
        // Tempora baru saja dibuka atau daftar aplikasi terkunci berubah: periksa segera.
        periksaLagiPada = 0L
        // STICKY: kalau Android mematikan layanan karena kehabisan memori, nanti dihidupkan lagi.
        return START_STICKY
    }

    override fun onDestroy() {
        lapisanKuis.sembunyikan()
        scope.cancel()
        super.onDestroy()
    }

    private fun jadikanLatarDepan(): Boolean = try {
        if (teksPemantau.isEmpty()) teksPemantau = "Memantau aplikasi terkunci"
        val notifikasi = notifikasiPemantau(teksPemantau)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ID_NOTIFIKASI_PEMANTAU, notifikasi, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(ID_NOTIFIKASI_PEMANTAU, notifikasi)
        }
        true
    } catch (e: IllegalStateException) {
        // Android 12 ke atas kadang menolak layanan yang dihidupkan saat Tempora tidak tampil.
        // Layanan akan dimulai lagi saat Tempora dibuka.
        false
    }

    private suspend fun pantau() {
        val app = application as TemporaApplication
        val powerManager = getSystemService(PowerManager::class.java)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        var terakhirIntip = System.currentTimeMillis()
        while (true) {
            val dibatasi = app.database.aplikasiDibatasiDao().semua().first()
            // Tidak ada yang perlu dipantau: layanan berhenti dan notifikasinya hilang.
            if (dibatasi.isEmpty() || !app.pemakaian.izinDiberikan()) {
                lapisanKuis.sembunyikan()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            // Saat layar mati tidak ada aplikasi yang dipakai, jadi tidak perlu memeriksa.
            if (powerManager.isInteractive) {
                val sekarang = System.currentTimeMillis()
                val adaPergantian =
                    app.pemakaian.adaPergantianLayar(terakhirIntip - MUNDUR_INTIP_MS, sekarang)
                terakhirIntip = sekarang
                if (adaPergantian || SystemClock.elapsedRealtime() >= periksaLagiPada) {
                    val jeda = periksa(app, dibatasi)
                    periksaLagiPada = SystemClock.elapsedRealtime() + jeda
                }
                // Layar menyala dan tidak terkunci = pengguna sedang memakai HP. Itulah saat
                // yang tepat untuk pengingat, jadi ditanyakan begitu pengguna mulai aktif.
                if (!keyguardManager.isKeyguardLocked &&
                    SystemClock.elapsedRealtime() >= periksaPengingatLagiPada
                ) {
                    periksaPengingatLagiPada = SystemClock.elapsedRealtime() + JEDA_PERIKSA_PENGINGAT_MS
                    scope.launch { app.pemeriksaPengingat.periksa() }
                }
            }
            delay(JEDA_INTIP_MS)
        }
    }

    // Menghitung pemakaian aplikasi terkunci yang sedang tampil, lalu memutuskan: tidak ada
    // apa-apa, kirim peringatan, atau munculkan kuis. Mengembalikan berapa lama lagi
    // pemeriksaan penuh berikutnya perlu dilakukan (milidetik).
    private suspend fun periksa(app: TemporaApplication, dibatasi: List<AplikasiDibatasi>): Long {
        val pemakaian = app.pemakaian.sekarang()
        val aktif = dibatasi.firstOrNull { it.paket in pemakaian.sedangTampil }
        if (aktif == null) {
            lapisanKuis.sembunyikan()
            perbaruiNotifikasiPemantau("Memantau ${dibatasi.size} aplikasi terkunci")
            return JEDA_PERIKSA_MS
        }

        val nama = nama(aktif.paket)
        val terpakaiMs = pemakaian.perPaket[aktif.paket] ?: 0L
        // Batas yang berlaku: batas harian, atau batas dari tambahan waktu kalau lebih besar.
        val batasHarianMs = aktif.batasMenit * 60_000L
        val batasTambahanMs = app.pengaturan.tambahanWaktu.first()[aktif.paket] ?: 0L
        val pakaiTambahan = batasTambahanMs > batasHarianMs
        val batasMs = maxOf(batasHarianMs, batasTambahanMs)
        val sisaMs = batasMs - terpakaiMs
        val tanda = "${LocalDate.now()}/$batasMs"

        if (sisaMs <= 0L) {
            perbaruiNotifikasiPemantau("$nama: waktu habis")
            return munculkanKuis(aktif.paket, nama, aktif.batasMenit, tanda)
        }

        lapisanKuis.sembunyikan()
        // Dibulatkan ke atas: sisa 4 menit 10 detik ditulis "5 menit".
        val sisaMenit = ((sisaMs + 59_999L) / 60_000L).toInt()
        perbaruiNotifikasiPemantau("$nama: sisa ${formatBatas(sisaMenit)}")

        val ambangPeringatan = if (!pakaiTambahan && aktif.batasMenit > BATAS_PENDEK_MENIT) {
            PERINGATAN_MS
        } else {
            PERINGATAN_BATAS_PENDEK_MS
        }
        if (sisaMs <= ambangPeringatan && sudahDiperingatkan.put(aktif.paket, tanda) != tanda) {
            kirimPeringatan(
                paket = aktif.paket,
                judul = "Sisa ${formatBatas(sisaMenit)} untuk $nama",
                isi = "Setelah itu kamu perlu menjawab soal untuk lanjut.",
            )
        }
        // Kalau sisanya kurang dari 10 detik, periksa lagi tepat saat waktunya habis.
        return sisaMs.coerceAtMost(JEDA_PERIKSA_MS)
    }

    private suspend fun munculkanKuis(paket: String, nama: String, batasMenit: Int, tanda: String): Long {
        // Tanpa izin "Tampil di atas aplikasi lain" kuis tidak bisa muncul: beri tahu lewat notifikasi.
        if (!Settings.canDrawOverlays(this)) {
            if (sudahHabis.put(paket, tanda) != tanda) {
                kirimPeringatan(
                    paket = paket,
                    judul = "Waktu $nama habis",
                    isi = "Kuis belum bisa muncul. Buka Tempora dan aktifkan izin tampil di atas aplikasi lain.",
                )
            }
            return JEDA_PERIKSA_MS
        }
        if (SystemClock.elapsedRealtime() < tundaKuisSampai) return JEDA_INTIP_MS
        // Kuis untuk aplikasi ini sudah tampil: soalnya tidak perlu dimuat lagi.
        if (lapisanKuis.sedangTampil(paket)) return JEDA_PERIKSA_MS

        val app = application as TemporaApplication
        val pengaturan = app.pengaturan
        lapisanKuis.tampilkan(
            paket = paket,
            namaAplikasi = nama,
            batasMenit = batasMenit,
            // Soal dan rangkuman dari materi yang dipilih, atau soal contoh kalau belum ada.
            bahan = muatBahanKuis(app),
            // Kalau pengguna sedang menunggu, kuis langsung menampilkan hitungan waktu tunggunya.
            tungguSampai = pengaturan.waktuTunggu.first()[paket],
            // Jumlah salah beruntun dan jeda yang masih berjalan dibawa dari kuis sebelumnya.
            catatanSalah = pengaturan.catatanSalah.first(),
            // Tiap soal yang dijawab dicatat untuk hitungan di halaman Progres, dan catatan
            // salah beruntun disimpan supaya tidak hilang saat layar kuis ditutup.
            onDijawab = { benar, catatanSalah ->
                scope.launch {
                    app.database.jawabanDao().catat(
                        Jawaban(dijawabPada = System.currentTimeMillis(), benar = benar),
                    )
                    pengaturan.simpanCatatanSalah(catatanSalah)
                }
            },
            onMulaiTunggu = { sampaiMs -> scope.launch { pengaturan.mulaiTunggu(paket, sampaiMs) } },
            onBuka = { menit -> scope.launch { beriTambahanWaktu(paket, menit) } },
            onBatal = { keLayarUtama() },
        )
        return JEDA_PERIKSA_MS
    }

    // Jawaban benar atau waktu tunggu selesai: aplikasi boleh dipakai lagi selama menit yang
    // dipilih, dihitung dari pemakaian saat ini. Waktu tunggu yang sudah terpakai dihapus,
    // jadi lain kali perlu menjawab soal atau menunggu lagi.
    private suspend fun beriTambahanWaktu(paket: String, menit: Int) {
        val app = application as TemporaApplication
        val terpakaiMs = app.pemakaian.sekarang().perPaket[paket] ?: 0L
        app.pengaturan.beriTambahanWaktu(paket, terpakaiMs + menit * 60_000L)
        app.pengaturan.hapusTunggu(paket)
        lapisanKuis.sembunyikan()
        periksaLagiPada = 0L
    }

    // "Tidak jadi": pindah ke layar utama HP, lalu kuis dilepas.
    private fun keLayarUtama() {
        tundaKuisSampai = SystemClock.elapsedRealtime() + TUNDA_SETELAH_BATAL_MS
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: RuntimeException) {
            // Sebagian HP melarang layanan membuka layar utama. Kuis tetap dilepas; kalau
            // aplikasinya masih tampil setelah 3 detik, kuis muncul lagi.
        }
        lapisanKuis.sembunyikan()
        periksaLagiPada = 0L
    }

    private fun nama(paket: String): String = namaAplikasi.getOrPut(paket) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(paket, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            paket
        }
    }

    private fun perbaruiNotifikasiPemantau(teks: String) {
        if (teks == teksPemantau) return
        teksPemantau = teks
        notificationManager.notify(ID_NOTIFIKASI_PEMANTAU, notifikasiPemantau(teks))
    }

    private fun notifikasiPemantau(teks: String): Notification {
        val builder = Notification.Builder(this, KANAL_PEMANTAU)
            .setSmallIcon(R.drawable.ic_gembok)
            .setContentTitle("Tempora aktif")
            .setContentText(teks)
            .setContentIntent(bukaTempora())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Tanpa ini Android 12 ke atas menunda tampilnya notifikasi sampai 10 detik.
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun kirimPeringatan(paket: String, judul: String, isi: String) {
        val notifikasi = Notification.Builder(this, KANAL_PERINGATAN)
            .setSmallIcon(R.drawable.ic_gembok)
            // Warna kuning Tempora untuk ikon dan nama aplikasi di notifikasi.
            .setColor(0xFFFFDD4A.toInt())
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(Notification.BigTextStyle().bigText(isi))
            .setContentIntent(bukaTempora())
            .setAutoCancel(true)
            .build()
        // Nomor notifikasi dibuat dari nama paket: satu aplikasi satu notifikasi, sehingga
        // notifikasi baru menggantikan notifikasi lama milik aplikasi yang sama.
        notificationManager.notify(paket.hashCode(), notifikasi)
    }

    // Mengetuk notifikasi membuka Tempora.
    private fun bukaTempora(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        fun mulai(context: Context) {
            try {
                context.startForegroundService(Intent(context, LayananPemantau::class.java))
            } catch (e: IllegalStateException) {
                // Ditolak Android karena Tempora sedang tidak tampil; dicoba lagi saat dibuka.
            }
        }
    }
}
