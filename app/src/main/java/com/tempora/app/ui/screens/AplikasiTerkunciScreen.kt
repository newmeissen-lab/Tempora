package com.tempora.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tempora.app.R
import com.tempora.app.data.Motivasi
import com.tempora.app.ui.components.IkonAplikasi
import com.tempora.app.ui.components.KolomCari
import com.tempora.app.ui.components.warnaSakelar
import com.tempora.app.ui.formatBatas
import com.tempora.app.ui.theme.Garis
import com.tempora.app.ui.theme.GarisTebal
import com.tempora.app.ui.theme.HurufAngka
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Permukaan
import com.tempora.app.ui.theme.Teks
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup

private const val LANGKAH_MENIT = 5
private val PILIHAN_CEPAT_MENIT = listOf(15, 30, 60, 120)

@Composable
fun AplikasiTerkunciScreen(
    onKembali: () -> Unit,
    viewModel: AplikasiTerkunciViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Isi kolom cari disimpan di layar supaya ketikan langsung tampil tanpa jeda.
    var kataCari by rememberSaveable { mutableStateOf("") }
    // Paket aplikasi yang batas waktunya sedang diatur lewat dialog. null = dialog tertutup.
    var paketDiatur by rememberSaveable { mutableStateOf<String?>(null) }
    // Paket aplikasi yang kuncinya hendak dimatikan dan sedang menunggu jawaban di dialog pengingat.
    var paketAkanDibuka by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKembali, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(painterResource(R.drawable.ic_kembali), contentDescription = "Kembali")
            }
            Text(
                text = "Aplikasi terkunci",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "${state.jumlahTerkunci} aplikasi dikunci. Ketuk nama aplikasi yang dikunci untuk mengubah batas waktunya.",
            style = MaterialTheme.typography.bodyMedium,
            color = TeksRedup,
        )

        KolomCari(
            nilai = kataCari,
            onUbah = {
                kataCari = it
                viewModel.ubahCari(it)
            },
            petunjuk = "Cari aplikasi",
        )

        when {
            state.memuat -> Text(
                text = "Membaca daftar aplikasi di HP...",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )

            state.daftar.isEmpty() -> Text(
                text = "Tidak ada aplikasi yang cocok.",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )

            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.daftar, key = { it.paket }) { baris ->
                    BarisAplikasiTerpasang(
                        baris = baris,
                        onUbahKunci = { dikunci ->
                            // Mematikan kunci tidak langsung dijalankan: pengguna diingatkan
                            // dulu pada alasan dan mimpinya.
                            if (!dikunci && state.motivasi != null) {
                                paketAkanDibuka = baris.paket
                            } else {
                                viewModel.ubahKunci(baris.paket, dikunci)
                            }
                        },
                        onAturBatas = { paketDiatur = baris.paket },
                    )
                }
            }
        }
    }

    val sedangDiatur = state.daftar.firstOrNull { it.paket == paketDiatur }
    val batasSekarang = sedangDiatur?.batasMenit
    if (sedangDiatur != null && batasSekarang != null) {
        DialogBatas(
            namaAplikasi = sedangDiatur.nama,
            batasAwal = batasSekarang,
            onSimpan = { menit ->
                viewModel.ubahBatas(sedangDiatur.paket, menit)
                paketDiatur = null
            },
            onTutup = { paketDiatur = null },
        )
    }

    val akanDibuka = state.daftar.firstOrNull { it.paket == paketAkanDibuka }
    val motivasi = state.motivasi
    if (akanDibuka != null && motivasi != null) {
        DialogPengingat(
            namaAplikasi = akanDibuka.nama,
            motivasi = motivasi,
            onTetapKunci = { paketAkanDibuka = null },
            onNonaktifkan = {
                viewModel.ubahKunci(akanDibuka.paket, false)
                paketAkanDibuka = null
            },
        )
    }
}

// Kalimat penutup dialog pengingat; satu dipilih acak tiap dialog muncul.
private val KALIMAT_PENGINGAT = listOf(
    "Kamu sendiri yang menulis ini. Kunci ini ada untuk membantumu ke sana.",
    "Tidak perlu sempurna. Bertahan sedikit lebih lama hari ini sudah cukup.",
    "Pelan-pelan saja. Tiap kali kamu menahan diri, mimpimu sedikit lebih dekat.",
    "Kalau memang perlu, tidak apa-apa. Tapi ingat lagi kenapa kamu memulai.",
)

// Muncul saat sakelar kunci dimatikan: menampilkan lagi alasan dan mimpi pengguna.
// Pilihan yang ditonjolkan adalah tetap mengunci.
@Composable
private fun DialogPengingat(
    namaAplikasi: String,
    motivasi: Motivasi,
    onTetapKunci: () -> Unit,
    onNonaktifkan: () -> Unit,
) {
    val kalimat = remember { KALIMAT_PENGINGAT.random() }

    AlertDialog(
        onDismissRequest = onTetapKunci,
        containerColor = Permukaan,
        title = { Text("Buka kunci $namaAplikasi?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                JawabanPengguna(label = "Alasanmu membatasi", isi = motivasi.alasan)
                JawabanPengguna(label = "Mimpimu", isi = motivasi.mimpi)
                Text(text = kalimat, color = Teks)
            }
        },
        confirmButton = {
            Button(
                onClick = onTetapKunci,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Kuning,
                    contentColor = TeksDiAtasTerang,
                ),
            ) { Text("Tetap kunci", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(
                onClick = onNonaktifkan,
                colors = ButtonDefaults.textButtonColors(contentColor = TeksRedup),
            ) { Text("Nonaktifkan") }
        },
    )
}

@Composable
private fun JawabanPengguna(label: String, isi: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TeksRedup)
        Text(
            text = "“$isi”",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Kuning,
        )
    }
}

@Composable
private fun BarisAplikasiTerpasang(
    baris: BarisAplikasi,
    onUbahKunci: (Boolean) -> Unit,
    onAturBatas: () -> Unit,
) {
    val batasMenit = baris.batasMenit
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IkonAplikasi(baris.ikon)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = batasMenit != null, onClick = onAturBatas)
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = baris.nama,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (batasMenit != null) "Batas ${formatBatas(batasMenit)} per hari" else "Bebas dibuka",
                style = MaterialTheme.typography.bodySmall,
                color = if (batasMenit != null) Kuning else TeksRedup,
            )
        }
        Switch(
            checked = batasMenit != null,
            onCheckedChange = onUbahKunci,
            colors = warnaSakelar(),
        )
    }
    HorizontalDivider(color = Garis)
}

@Composable
private fun DialogBatas(
    namaAplikasi: String,
    batasAwal: Int,
    onSimpan: (Int) -> Unit,
    onTutup: () -> Unit,
) {
    var menit by remember { mutableIntStateOf(batasAwal) }

    AlertDialog(
        onDismissRequest = onTutup,
        containerColor = Permukaan,
        title = { Text("Batas waktu $namaAplikasi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Lama pemakaian yang diizinkan per hari.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TombolLangkah(
                        ikon = R.drawable.ic_kurang,
                        keterangan = "Kurangi $LANGKAH_MENIT menit",
                        onClick = { menit = (menit - LANGKAH_MENIT).coerceAtLeast(BATAS_TERKECIL_MENIT) },
                    )
                    Text(
                        text = batasPendek(menit),
                        fontFamily = HurufAngka,
                        fontSize = 26.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Teks,
                    )
                    TombolLangkah(
                        ikon = R.drawable.ic_tambah,
                        keterangan = "Tambah $LANGKAH_MENIT menit",
                        onClick = { menit = (menit + LANGKAH_MENIT).coerceAtMost(BATAS_TERBESAR_MENIT) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PILIHAN_CEPAT_MENIT.forEach { pilihan ->
                        PilihanCepat(
                            teks = batasPendek(pilihan),
                            dipilih = menit == pilihan,
                            onClick = { menit = pilihan },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSimpan(menit) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } },
    )
}

// Misalnya 90 menjadi "1j 30m", 45 menjadi "45m".
private fun batasPendek(menit: Int): String = when {
    menit >= 60 && menit % 60 == 0 -> "${menit / 60}j"
    menit >= 60 -> "${menit / 60}j ${menit % 60}m"
    else -> "${menit}m"
}

@Composable
private fun TombolLangkah(ikon: Int, keterangan: String, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, Teks),
    ) {
        Icon(painterResource(ikon), contentDescription = keterangan, tint = Teks)
    }
}

@Composable
private fun RowScope.PilihanCepat(teks: String, dipilih: Boolean, onClick: () -> Unit) {
    val bentuk = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(bentuk)
            .background(if (dipilih) Kuning else Color.Transparent)
            .border(1.5.dp, if (dipilih) Kuning else GarisTebal, bentuk)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = teks,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (dipilih) TeksDiAtasTerang else Teks,
        )
    }
}
