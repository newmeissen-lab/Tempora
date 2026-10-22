package com.tempora.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tempora.app.R
import com.tempora.app.data.MAKS_HALAMAN_AI
import com.tempora.app.data.MAKS_UKURAN_AI_MB
import com.tempora.app.data.Materi
import com.tempora.app.ui.components.JudulLayar
import com.tempora.app.ui.components.KolomCari
import com.tempora.app.ui.components.LabelBagian
import com.tempora.app.ui.formatKapan
import com.tempora.app.ui.theme.Garis
import com.tempora.app.ui.theme.HurufJudul
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.LatarLencana
import com.tempora.app.ui.theme.Permukaan
import com.tempora.app.ui.theme.PermukaanKedua
import com.tempora.app.ui.theme.Salah
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup
import com.tempora.app.ui.theme.TombolUtama

// onBacaRangkuman dipanggil dengan id materi yang rangkumannya hendak dibaca.
@Composable
fun MateriScreen(
    onBacaRangkuman: (idMateri: Long) -> Unit,
    viewModel: MateriViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var kataCari by rememberSaveable { mutableStateOf("") }
    var akanDihapus by remember { mutableStateOf<Materi?>(null) }

    // Pemilih file bawaan Android. Hasilnya (uri) datang setelah pengguna memilih satu PDF,
    // atau null kalau pengguna membatalkan.
    val pemilihPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.tambah(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        JudulLayar("Materi", modifier = Modifier.padding(top = 24.dp))

        KolomCari(
            nilai = kataCari,
            onUbah = {
                kataCari = it
                viewModel.ubahCari(it)
            },
            petunjuk = "Cari materi",
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TombolTambahMateri(
                sedangMenambah = state.sedangMenambah,
                onClick = { pemilihPdf.launch(arrayOf("application/pdf")) },
            )
            // Pengguna perlu tahu bahwa PDF-nya dikirim ke luar HP.
            Text(
                text = "PDF dikirim ke Google Gemini untuk dibuatkan soal dan rangkuman. " +
                    "Paling banyak $MAKS_HALAMAN_AI halaman dan $MAKS_UKURAN_AI_MB MB.",
                style = MaterialTheme.typography.bodySmall,
                color = TeksRedup,
            )
        }

        state.pesanGagal?.let { pesan ->
            Text(text = pesan, style = MaterialTheme.typography.bodyMedium, color = Salah)
        }

        when {
            state.memuat -> Unit

            !state.adaMateri -> Text(
                text = "Belum ada materi. Tambahkan PDF pertamamu.",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )

            state.daftar.isEmpty() -> Text(
                text = "Tidak ada materi yang cocok.",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )

            else -> Column(modifier = Modifier.weight(1f)) {
                LabelBagian("Pilih satu materi untuk soal kunci, atau baca rangkumannya")
                LazyColumn {
                    items(state.daftar, key = { it.materi.id }) { baris ->
                        BarisDaftarMateri(
                            baris = baris,
                            onPilih = { viewModel.pilihUntukKunci(baris.materi.id) },
                            onBuatSoal = { viewModel.buatSoal(baris.materi) },
                            onBacaRangkuman = { onBacaRangkuman(baris.materi.id) },
                            onHapus = { akanDihapus = baris.materi },
                        )
                    }
                }
            }
        }
    }

    akanDihapus?.let { materi ->
        AlertDialog(
            onDismissRequest = { akanDihapus = null },
            containerColor = Permukaan,
            title = { Text("Hapus materi ini?") },
            text = {
                Text("\"${materi.judul}\" dihapus dari Tempora. File aslinya di HP tidak ikut terhapus.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hapus(materi)
                        akanDihapus = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Salah),
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { akanDihapus = null }) { Text("Batal") } },
        )
    }
}

// Keadaan soal buatan AI untuk satu materi: sedang dibuat, gagal, belum dibuat, atau siap.
@Composable
private fun KeadaanSoalAi(baris: BarisMateri, onBuatSoal: () -> Unit, onBacaRangkuman: () -> Unit) {
    when {
        baris.sedangDibuat -> Text(
            text = "AI sedang membuat soal dan rangkuman. Bisa sampai beberapa menit.",
            style = MaterialTheme.typography.bodyMedium,
            color = Kuning,
        )

        baris.pesanGagalAi != null -> {
            Text(
                text = baris.pesanGagalAi,
                style = MaterialTheme.typography.bodyMedium,
                color = Salah,
            )
            TautanAksi(teks = "Coba lagi", onClick = onBuatSoal)
        }

        baris.jumlahSoal == 0 -> TautanAksi(teks = "Buat soal dengan AI", onClick = onBuatSoal)

        else -> {
            Text(
                text = "${baris.jumlahSoal} soal dan rangkuman siap",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )
            if (!baris.materi.rangkuman.isNullOrBlank()) {
                TautanAksi(teks = "Baca rangkuman", onClick = onBacaRangkuman)
            }
        }
    }
}

// Tulisan yang bisa diketuk, untuk aksi kecil di dalam baris.
@Composable
private fun TautanAksi(teks: String, onClick: () -> Unit) {
    Text(
        text = teks,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = Kuning,
    )
}

@Composable
private fun TombolTambahMateri(sedangMenambah: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !sedangMenambah,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, TombolUtama),
        colors = ButtonDefaults.buttonColors(
            containerColor = Kuning,
            contentColor = TeksDiAtasTerang,
            disabledContainerColor = PermukaanKedua,
            disabledContentColor = TeksRedup,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tambah),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (sedangMenambah) "Menyalin PDF..." else "Tambah materi",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
    }
}

@Composable
private fun BarisDaftarMateri(
    baris: BarisMateri,
    onPilih: () -> Unit,
    onBuatSoal: () -> Unit,
    onBacaRangkuman: () -> Unit,
    onHapus: () -> Unit,
) {
    val materi = baris.materi
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = baris.dipakaiUntukKunci,
            onClick = onPilih,
            colors = RadioButtonDefaults.colors(selectedColor = Kuning, unselectedColor = TeksRedup),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPilih),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = materi.judul,
                fontFamily = HurufJudul,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${materi.jumlahHalaman} halaman · ${formatKapan(materi.ditambahkanPada)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )
            KeadaanSoalAi(baris = baris, onBuatSoal = onBuatSoal, onBacaRangkuman = onBacaRangkuman)
            if (baris.dipakaiUntukKunci) {
                Text(
                    text = "Dipakai untuk soal kunci",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LatarLencana)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Kuning,
                )
            }
        }
        IconButton(onClick = onHapus) {
            Icon(
                painter = painterResource(R.drawable.ic_hapus),
                contentDescription = "Hapus ${materi.judul}",
                tint = TeksRedup,
            )
        }
    }
    HorizontalDivider(color = Garis)
}
