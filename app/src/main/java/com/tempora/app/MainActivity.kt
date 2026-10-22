package com.tempora.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tempora.app.pemantau.LayananPemantau
import com.tempora.app.pengingat.EXTRA_BUKA
import com.tempora.app.pengingat.EXTRA_ID_NOTIFIKASI
import com.tempora.app.ui.TemporaApp
import com.tempora.app.ui.theme.TemporaTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Pintu masuk aplikasi: layar pertama yang dibuat Android saat ikon Tempora diketuk.
class MainActivity : ComponentActivity() {
    // Menampilkan dialog izin notifikasi. Jawabannya tidak perlu diolah: status izin
    // dibaca ulang di layar Atur.
    private val mintaIzinNotifikasi =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Halaman yang diminta notifikasi yang baru diketuk, atau null kalau Tempora dibuka biasa.
    private var bukaDariNotifikasi by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tampilan melebar sampai ke balik status bar. Gaya "dark" membuat ikon status bar
        // berwarna terang, karena latar Tempora selalu gelap walau HP memakai mode terang.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // savedInstanceState null = Tempora baru dibuka, bukan dibuat ulang karena layar diputar.
        // Dengan begitu permintaan notifikasi tidak dijalankan dua kali.
        if (savedInstanceState == null) {
            bacaPermintaanNotifikasi(intent)
            mintaIzinNotifikasiBilaPerlu()
        }
        setContent {
            TemporaTheme {
                TemporaApp(
                    bukaDariNotifikasi = bukaDariNotifikasi,
                    onSudahDibuka = { bukaDariNotifikasi = null },
                )
            }
        }
        jalankanPemantauBilaPerlu()
    }

    // Dipanggil kalau notifikasi diketuk saat Tempora sudah terbuka.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        bacaPermintaanNotifikasi(intent)
    }

    // Notifikasi pengingat menitipkan halaman yang harus dibuka dan nomor notifikasinya.
    // Notifikasi itu dihapus karena sudah ditanggapi.
    private fun bacaPermintaanNotifikasi(niat: Intent) {
        val idNotifikasi = niat.getIntExtra(EXTRA_ID_NOTIFIKASI, -1)
        if (idNotifikasi == -1) return
        bukaDariNotifikasi = niat.getStringExtra(EXTRA_BUKA)
        getSystemService(NotificationManager::class.java).cancel(idNotifikasi)
    }

    // Selama Tempora tampil, daftar aplikasi terkunci diamati. Begitu ada aplikasi yang
    // dikunci (atau Tempora dibuka lagi saat sudah ada), layanan pemantau dihidupkan.
    // Layanan berhenti sendiri kalau daftarnya kosong.
    private fun jalankanPemantauBilaPerlu() {
        val app = application as TemporaApplication
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.database.aplikasiDibatasiDao().semua()
                    .map { it.isNotEmpty() }
                    .distinctUntilChanged()
                    .collect { adaTerkunci ->
                        if (adaTerkunci && app.pemakaian.izinDiberikan()) {
                            LayananPemantau.mulai(this@MainActivity)
                        }
                    }
            }
        }
    }

    // Izin notifikasi diminta saat Tempora pertama dibuka, karena pengingat bisa muncul
    // walau belum ada aplikasi yang dikunci.
    private fun mintaIzinNotifikasiBilaPerlu() {
        // Sebelum Android 13 notifikasi diizinkan tanpa dialog.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val diizinkan = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!diizinkan) mintaIzinNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
