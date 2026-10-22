package com.tempora.app.kuis

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.tempora.app.data.CatatanSalah
import com.tempora.app.ui.theme.TemporaTheme

// Memasang dan melepas layar kuis di atas aplikasi lain (overlay). Android tidak mengizinkan
// Tempora menutup aplikasi lain, jadi aplikasi yang waktunya habis ditutupi layar ini.
// Butuh izin "Tampil di atas aplikasi lain".
class LapisanKuis(layanan: Context) {
    // Layanan tidak punya layar sendiri. Di Android 11 ke atas, tampilan di luar Activity
    // sebaiknya dibuat dari "konteks jendela" supaya ukuran dan kerapatan layarnya benar.
    private val context: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val layar = layanan.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        layanan.createDisplayContext(layar)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else {
        layanan
    }
    private val windowManager = context.getSystemService(WindowManager::class.java)

    private var tampilan: View? = null
    private var pemilik: PemilikLapisan? = null
    private var paketTampil: String? = null

    fun sedangTampil(paket: String): Boolean = paketTampil == paket

    fun tampilkan(
        paket: String,
        namaAplikasi: String,
        batasMenit: Int,
        bahan: BahanKuis,
        tungguSampai: Long?,
        catatanSalah: CatatanSalah,
        onDijawab: (benar: Boolean, catatanSalah: CatatanSalah) -> Unit,
        onMulaiTunggu: (sampaiMs: Long) -> Unit,
        onBuka: (menit: Int) -> Unit,
        onBatal: () -> Unit,
    ) {
        // Kuis untuk aplikasi ini sudah tampil: biarkan, supaya soalnya tidak berganti sendiri.
        if (paketTampil == paket) return
        sembunyikan()

        val pemilikBaru = PemilikLapisan()
        val tampilanBaru = ComposeView(context).apply {
            setViewTreeLifecycleOwner(pemilikBaru)
            setViewTreeSavedStateRegistryOwner(pemilikBaru)
            setContent {
                TemporaTheme {
                    LayarKuis(
                        namaAplikasi, batasMenit, bahan, tungguSampai, catatanSalah,
                        onDijawab, onMulaiTunggu, onBuka, onBatal,
                    )
                }
            }
        }
        try {
            windowManager.addView(tampilanBaru, parameterJendela())
        } catch (e: RuntimeException) {
            // Izin dicabut tepat sebelum layar dipasang.
            pemilikBaru.tutup()
            return
        }
        tampilan = tampilanBaru
        pemilik = pemilikBaru
        paketTampil = paket
    }

    fun sembunyikan() {
        val lama = tampilan ?: return
        tampilan = null
        paketTampil = null
        try {
            windowManager.removeView(lama)
        } catch (e: IllegalArgumentException) {
            // Layar sudah terlepas lebih dulu.
        }
        pemilik?.tutup()
        pemilik = null
    }

    // Jendela selebar layar penuh di atas aplikasi lain, termasuk di balik status bar.
    // Jendela ini menerima semua sentuhan, jadi aplikasi di bawahnya tidak bisa disentuh.
    @Suppress("DEPRECATION")
    private fun parameterJendela(): WindowManager.LayoutParams {
        val parameter = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            PixelFormat.TRANSLUCENT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 0 = jendela tidak dikecilkan untuk memberi tempat status bar dan bilah navigasi.
            parameter.fitInsetsTypes = 0
            parameter.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            parameter.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return parameter
    }
}

// Compose butuh "pemilik siklus hidup" untuk tahu kapan tampilannya aktif. Di layar biasa
// pemiliknya Activity; overlay tidak punya Activity, jadi dibuatkan pemilik kecil sendiri.
private class PemilikLapisan : SavedStateRegistryOwner {
    private val siklus = LifecycleRegistry(this)
    private val pengendali = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = siklus
    override val savedStateRegistry: SavedStateRegistry get() = pengendali.savedStateRegistry

    init {
        pengendali.performRestore(null)
        siklus.currentState = Lifecycle.State.RESUMED
    }

    fun tutup() {
        siklus.currentState = Lifecycle.State.DESTROYED
    }
}
