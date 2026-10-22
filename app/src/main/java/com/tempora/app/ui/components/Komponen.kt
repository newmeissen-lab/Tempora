package com.tempora.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempora.app.R
import com.tempora.app.ui.theme.GarisTebal
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Permukaan
import com.tempora.app.ui.theme.PermukaanKedua
import com.tempora.app.ui.theme.SakelarMati
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup
import com.tempora.app.ui.theme.TombolUtama

// Potongan tampilan yang dipakai di lebih dari satu layar.

@Composable
fun JudulLayar(teks: String, modifier: Modifier = Modifier) {
    Text(
        text = teks,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
}

// Tulisan kecil redup di atas sebuah bagian, misalnya "Waktu layar hari ini".
@Composable
fun LabelBagian(teks: String, modifier: Modifier = Modifier) {
    Text(
        text = teks,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = TeksRedup,
    )
}

@Composable
fun TombolBesar(teks: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TombolUtama,
            contentColor = TeksDiAtasTerang,
        ),
    ) {
        Text(text = teks, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun KolomCari(
    nilai: String,
    onUbah: (String) -> Unit,
    petunjuk: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onUbah,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(petunjuk) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_cari),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = warnaKolomTeks(),
    )
}

// Warna kolom isian yang sama untuk semua layar.
@Composable
fun warnaKolomTeks(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Permukaan,
    unfocusedContainerColor = Permukaan,
    focusedBorderColor = Kuning,
    unfocusedBorderColor = GarisTebal,
    cursorColor = Kuning,
    focusedLeadingIconColor = TeksRedup,
    unfocusedLeadingIconColor = TeksRedup,
    focusedPlaceholderColor = TeksRedup,
    unfocusedPlaceholderColor = TeksRedup,
)

// Satu baris di layar Atur: judul di kiri, nilai sekarang dan panah di kanan.
@Composable
fun BarisAtur(
    judul: String,
    nilai: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ditonjolkan: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = judul,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = nilai,
            modifier = Modifier.widthIn(max = 150.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (ditonjolkan) Kuning else TeksRedup,
            fontWeight = if (ditonjolkan) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_panah_kanan),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = TeksRedup,
        )
    }
}

// Satu baris di layar Atur yang berisi sakelar: judul dan keterangan di kiri, sakelar di kanan.
@Composable
fun BarisSakelar(
    judul: String,
    keterangan: String,
    aktif: Boolean,
    onUbah: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUbah(!aktif) }
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = judul,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = keterangan,
                style = MaterialTheme.typography.bodySmall,
                color = TeksRedup,
            )
        }
        Switch(checked = aktif, onCheckedChange = onUbah, colors = warnaSakelar())
    }
}

// Ditampilkan selama izin "Akses penggunaan" belum aktif.
@Composable
fun KartuIzin(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    KartuPeringatan(
        judul = "Tempora belum boleh membaca waktu pemakaian",
        isi = "Cari Tempora di daftar \"Akses penggunaan\", lalu aktifkan. Setelah itu kembali ke sini.",
        teksTombol = "Buka pengaturan izin",
        onClick = { bukaPengaturanAksesPenggunaan(context) },
        modifier = modifier,
    )
}

// Ditampilkan selama izin "Tampil di atas aplikasi lain" belum aktif.
@Composable
fun KartuIzinTampilDiAtas(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    KartuPeringatan(
        judul = "Layar kuis belum bisa muncul",
        isi = "Cari Tempora di daftar \"Tampil di atas aplikasi lain\", lalu aktifkan. " +
            "Tanpa izin ini aplikasi terkunci tidak tertutup kuis saat waktunya habis.",
        teksTombol = "Buka pengaturan izin",
        onClick = { bukaPengaturanTampilDiAtas(context) },
        modifier = modifier,
    )
}

// Kartu berisi judul, penjelasan, dan satu tombol.
@Composable
fun KartuPeringatan(
    judul: String,
    isi: String,
    teksTombol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bentuk = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(bentuk)
            .background(Permukaan)
            .border(1.dp, GarisTebal, bentuk)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = judul,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = isi,
            style = MaterialTheme.typography.bodyMedium,
            color = TeksRedup,
        )
        TombolBesar(teks = teksTombol, onClick = onClick)
    }
}

// Batang kemajuan sederhana. fraksi 0f = kosong, 1f = penuh.
@Composable
fun BilahKemajuan(fraksi: Float, modifier: Modifier = Modifier, warna: Color = Kuning) {
    val bentuk = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(bentuk)
            .background(PermukaanKedua),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraksi.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(bentuk)
                .background(warna),
        )
    }
}

@Composable
fun IkonAplikasi(ikon: ImageBitmap, modifier: Modifier = Modifier) {
    Image(bitmap = ikon, contentDescription = null, modifier = modifier.size(40.dp))
}

@Composable
fun warnaSakelar(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = TeksDiAtasTerang,
    checkedTrackColor = Kuning,
    uncheckedThumbColor = TeksRedup,
    uncheckedTrackColor = SakelarMati,
    uncheckedBorderColor = Color.Transparent,
)

// Membuka halaman "Akses penggunaan" di Pengaturan HP. Di banyak HP halaman Tempora langsung
// terbuka; kalau HP tidak mendukungnya, yang dibuka adalah daftar semua aplikasi.
fun bukaPengaturanAksesPenggunaan(context: Context) {
    val halamanTempora = Intent(
        Settings.ACTION_USAGE_ACCESS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    try {
        context.startActivity(halamanTempora)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

// Membuka halaman "Tampil di atas aplikasi lain" di Pengaturan HP. Di Android 11 ke atas
// yang terbuka adalah daftar semua aplikasi; pengguna perlu mencari Tempora di sana.
fun bukaPengaturanTampilDiAtas(context: Context) {
    val halamanTempora = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", context.packageName, null),
    )
    try {
        context.startActivity(halamanTempora)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }
}

// Membuka halaman notifikasi Tempora di Pengaturan HP.
fun bukaPengaturanNotifikasi(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
