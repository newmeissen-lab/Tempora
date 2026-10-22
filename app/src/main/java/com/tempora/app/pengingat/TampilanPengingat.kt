package com.tempora.app.pengingat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import com.tempora.app.MainActivity
import com.tempora.app.R

// Menggambar dan menampilkan notifikasi pengingat: warna, lencana bulat, gambar gradasi
// berisi angka besar, dan tombol-tombolnya.

// Data titipan di dalam niat (Intent) yang membuka Tempora dari notifikasi:
// halaman yang diminta, dan nomor notifikasi yang perlu dihapus setelah dibuka.
const val EXTRA_BUKA = "com.tempora.app.BUKA"
const val EXTRA_ID_NOTIFIKASI = "com.tempora.app.ID_NOTIFIKASI"

private const val KANAL_PENGINGAT = "pengingat"
private const val ID_NOTIFIKASI_AWAL = 100

// Pengingat yang tidak dibuka akan hilang sendiri setelah 6 jam, supaya tidak menumpuk.
private const val HILANG_SETELAH_MS = 6 * 60 * 60_000L

// Warna tulisan dan ikon di atas gradasi (sama dengan TeksDiAtasTerang di tema).
private const val TINTA = 0xFF1A0B14.toInt()

private const val LEBAR_SPANDUK = 1200
private const val TINGGI_SPANDUK = 600

fun tampilkanPengingat(context: Context, pengingat: Pengingat) {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    // DEFAULT: berbunyi sekali tetapi tidak menyembul menutupi layar.
    notificationManager.createNotificationChannel(
        NotificationChannel(KANAL_PENGINGAT, "Pengingat", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Ajakan mengatur waktu layar. Paling banyak 2 kali sehari."
        },
    )

    val jenis = pengingat.jenis
    val idNotifikasi = ID_NOTIFIKASI_AWAL + jenis.nomor
    val builder = Notification.Builder(context, KANAL_PENGINGAT)
        .setSmallIcon(ikon(jenis))
        .setColor(jenis.warnaAwal)
        .setLargeIcon(lencana(context, jenis))
        .setContentTitle(pengingat.judul)
        .setContentText(pengingat.isi)
        .setContentIntent(niatBukaTempora(context, pengingat.buka, idNotifikasi, kode = idNotifikasi * 10))
        .setAutoCancel(true)
        .setTimeoutAfter(HILANG_SETELAH_MS)

    if (pengingat.teksBesar != null) {
        // Saat notifikasi dibentangkan, tampil gambar gradasi berisi angka besar.
        val gambar = spanduk(jenis, pengingat.teksBesar, pengingat.teksKecil.orEmpty())
        builder.setStyle(Notification.BigPictureStyle().bigPicture(gambar).bigLargeIcon(null as Bitmap?))
    } else {
        builder.setStyle(Notification.BigTextStyle().bigText(pengingat.isi))
    }

    pengingat.tombol.forEachIndexed { urutan, tombol ->
        val kode = idNotifikasi * 10 + urutan + 1
        val niat = if (tombol.pengaturanHp != null) {
            niatPengaturanHp(context, tombol.pengaturanHp, kode)
        } else {
            niatBukaTempora(context, tombol.buka, idNotifikasi, kode)
        }
        builder.addAction(Notification.Action.Builder(null as Icon?, tombol.teks, niat).build())
    }

    notificationManager.notify(idNotifikasi, builder.build())
}

// Ikon kecil tiap jenis, juga dipakai di dalam lencana bulat.
private fun ikon(jenis: JenisPengingat): Int = when (jenis) {
    JenisPengingat.Izin, JenisPengingat.BelumAdaKunci -> R.drawable.ic_gembok
    JenisPengingat.DuaJam, JenisPengingat.AplikasiBoros -> R.drawable.ic_jam
    JenisPengingat.SoalContoh -> R.drawable.ic_tab_materi
    JenisPengingat.Kemajuan -> R.drawable.ic_centang
    JenisPengingat.RekapMingguan -> R.drawable.ic_tab_progres
    JenisPengingat.Motivasi -> R.drawable.ic_bintang
}

// Lencana bulat bergradasi dengan ikon di tengahnya, tampil di sisi notifikasi.
private fun lencana(context: Context, jenis: JenisPengingat): Bitmap {
    val ukuran = 192
    val bitmap = Bitmap.createBitmap(ukuran, ukuran, Bitmap.Config.ARGB_8888)
    val kanvas = Canvas(bitmap)
    val kuas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, ukuran.toFloat(), ukuran.toFloat(),
            jenis.warnaAwal, jenis.warnaAkhir, Shader.TileMode.CLAMP,
        )
    }
    kanvas.drawCircle(ukuran / 2f, ukuran / 2f, ukuran / 2f, kuas)
    context.getDrawable(ikon(jenis))?.mutate()?.apply {
        setTint(TINTA)
        setBounds(ukuran / 4, ukuran / 4, ukuran * 3 / 4, ukuran * 3 / 4)
        draw(kanvas)
    }
    return bitmap
}

// Gambar lebar bergradasi: tulisan "TEMPORA", angka besar, dan keterangan kecil di bawahnya.
private fun spanduk(jenis: JenisPengingat, teksBesar: String, teksKecil: String): Bitmap {
    val lebar = LEBAR_SPANDUK.toFloat()
    val tinggi = TINGGI_SPANDUK.toFloat()
    val tepi = 80f
    val bitmap = Bitmap.createBitmap(LEBAR_SPANDUK, TINGGI_SPANDUK, Bitmap.Config.ARGB_8888)
    val kanvas = Canvas(bitmap)
    val kuas = Paint(Paint.ANTI_ALIAS_FLAG)

    kuas.shader = LinearGradient(0f, 0f, lebar, tinggi, jenis.warnaAwal, jenis.warnaAkhir, Shader.TileMode.CLAMP)
    kanvas.drawRect(0f, 0f, lebar, tinggi, kuas)
    kuas.shader = null

    // Hiasan: dua lingkaran putih tembus pandang di sisi kanan.
    kuas.color = 0x40FFFFFF
    kanvas.drawCircle(1040f, 90f, 230f, kuas)
    kuas.color = 0x2BFFFFFF
    kanvas.drawCircle(1160f, 540f, 300f, kuas)

    kuas.color = TINTA
    kuas.typeface = Typeface.DEFAULT_BOLD
    kuas.textSize = 40f
    kuas.letterSpacing = 0.25f
    kanvas.drawText("TEMPORA", tepi, 130f, kuas)

    kuas.letterSpacing = 0f
    kuas.textSize = 210f
    kecilkanBilaTerlaluLebar(kuas, teksBesar, lebar - 2 * tepi)
    kanvas.drawText(teksBesar, tepi, 370f, kuas)

    kuas.typeface = Typeface.DEFAULT
    kuas.textSize = 58f
    kecilkanBilaTerlaluLebar(kuas, teksKecil, lebar - 2 * tepi)
    kanvas.drawText(teksKecil, tepi, 470f, kuas)

    return bitmap
}

// Tulisan yang lebih lebar dari tempatnya dikecilkan ukurannya sampai muat.
private fun kecilkanBilaTerlaluLebar(kuas: Paint, teks: String, lebarTersedia: Float) {
    val lebarTeks = kuas.measureText(teks)
    if (lebarTeks > lebarTersedia) kuas.textSize = kuas.textSize * lebarTersedia / lebarTeks
}

// Niat membuka Tempora di halaman tertentu. kode harus berbeda untuk tiap tombol, supaya
// Android tidak menganggapnya niat yang sama dan menimpa isinya.
private fun niatBukaTempora(context: Context, buka: String?, idNotifikasi: Int, kode: Int): PendingIntent {
    val niat = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(EXTRA_BUKA, buka)
        .putExtra(EXTRA_ID_NOTIFIKASI, idNotifikasi)
    return PendingIntent.getActivity(
        context, kode, niat, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

// Niat membuka halaman izin di Pengaturan HP.
private fun niatPengaturanHp(context: Context, halaman: String, kode: Int): PendingIntent {
    val niat = if (halaman == PENGATURAN_TAMPIL_DI_ATAS) {
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null))
    } else {
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
    niat.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(
        context, kode, niat, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
