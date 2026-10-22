package com.tempora.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AplikasiTerpasang(val paket: String, val nama: String, val ikon: ImageBitmap)

// Daftar aplikasi di HP yang punya ikon (bisa dibuka pengguna), kecuali layar utama HP.
class AplikasiTerpasangRepository(private val context: Context) {
    // Membaca nama dan ikon semua aplikasi cukup lama, jadi hasilnya disimpan di memori.
    private var tersimpan: List<AplikasiTerpasang>? = null
    private val gembok = Mutex()

    suspend fun semua(muatUlang: Boolean = false): List<AplikasiTerpasang> = gembok.withLock {
        val lama = tersimpan
        if (lama != null && !muatUlang) {
            lama
        } else {
            withContext(Dispatchers.IO) { baca(lama.orEmpty().associateBy { it.paket }) }
                .also { tersimpan = it }
        }
    }

    // Hanya nama paketnya, tanpa membaca nama dan ikon, jadi jauh lebih ringan.
    // Dipakai pekerjaan di latar belakang (notifikasi pengingat).
    suspend fun paketBerikon(): Set<String> = withContext(Dispatchers.IO) {
        infoBerikon().map { it.packageName }.toSet()
    }

    // Aplikasi yang punya ikon di daftar aplikasi HP, kecuali layar utama HP.
    private fun infoBerikon(): List<ApplicationInfo> {
        val pm = context.packageManager
        val layarUtama = pm
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        return pm
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName !in layarUtama }
    }

    // sudahDibaca: aplikasi yang nama dan ikonnya sudah pernah dibaca. Saat memuat ulang,
    // hanya aplikasi yang baru dipasang yang dibaca, sehingga memuat ulang hampir tidak terasa.
    private fun baca(sudahDibaca: Map<String, AplikasiTerpasang>): List<AplikasiTerpasang> {
        val pm = context.packageManager
        val ukuranIkon = (40 * context.resources.displayMetrics.density).toInt()

        return infoBerikon()
            .map { info ->
                sudahDibaca[info.packageName] ?: AplikasiTerpasang(
                    paket = info.packageName,
                    nama = info.loadLabel(pm).toString(),
                    ikon = info.loadIcon(pm).keBitmap(ukuranIkon).asImageBitmap(),
                )
            }
            .sortedBy { it.nama.lowercase() }
    }
}

// Ikon aplikasi dari Android berupa Drawable; Compose butuh bitmap, jadi digambar ulang ke bitmap.
private fun Drawable.keBitmap(ukuran: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(ukuran, ukuran, Bitmap.Config.ARGB_8888)
    setBounds(0, 0, ukuran, ukuran)
    draw(Canvas(bitmap))
    return bitmap
}
