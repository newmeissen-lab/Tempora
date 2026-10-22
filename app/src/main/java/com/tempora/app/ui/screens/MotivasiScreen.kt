package com.tempora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tempora.app.R
import com.tempora.app.data.Motivasi
import com.tempora.app.ui.components.JudulLayar
import com.tempora.app.ui.components.TombolBesar
import com.tempora.app.ui.components.warnaKolomTeks
import com.tempora.app.ui.theme.Latar
import com.tempora.app.ui.theme.TeksRedup

private const val PANJANG_TERBANYAK = 200

// Dipakai di dua tempat:
// 1. Saat alasan dan mimpi belum pernah diisi (awal = null, onKembali = null): pertanyaan pembuka.
// 2. Dari layar Atur (awal = jawaban tersimpan, onKembali diisi): mengubah jawaban.
// Surface menutupi seluruh layar dan menahan sentuhan supaya tidak tembus ke layar di belakangnya.
@Composable
fun MotivasiScreen(
    onSimpan: (alasan: String, mimpi: String) -> Unit,
    modifier: Modifier = Modifier,
    awal: Motivasi? = null,
    onKembali: (() -> Unit)? = null,
) {
    // rememberSaveable: ketikan tidak hilang saat layar diputar.
    var alasan by rememberSaveable { mutableStateOf(awal?.alasan.orEmpty()) }
    var mimpi by rememberSaveable { mutableStateOf(awal?.mimpi.orEmpty()) }
    val sedangMengubah = onKembali != null
    val adaPerubahan = alasan.trim() != awal?.alasan || mimpi.trim() != awal?.mimpi

    Surface(modifier = modifier.fillMaxSize(), color = Latar) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                // imePadding memberi ruang untuk keyboard; isi bisa digulir kalau tertutup.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (onKembali != null) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onKembali, modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(painterResource(R.drawable.ic_kembali), contentDescription = "Kembali")
                    }
                    Text(
                        text = "Alasan dan mimpi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!sedangMengubah) JudulLayar("Sebelum mulai")
                Text(
                    text = if (sedangMengubah) {
                        "Jawaban ini Tempora tampilkan lagi saat kamu ingin membuka kunci sebuah " +
                            "aplikasi. Ubah kalau alasan atau mimpimu sudah berbeda."
                    } else {
                        "Dua pertanyaan singkat. Jawabanmu hanya disimpan di HP ini, dan akan " +
                            "Tempora tampilkan lagi saat kamu ingin membuka kunci sebuah aplikasi."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeksRedup,
                )
            }

            KolomJawaban(
                pertanyaan = "Kenapa ingin membatasi penggunaan aplikasi ini?",
                nilai = alasan,
                onUbah = { alasan = it },
                petunjuk = "Misalnya: supaya lebih fokus belajar",
            )
            KolomJawaban(
                pertanyaan = "Apa mimpimu?",
                nilai = mimpi,
                onUbah = { mimpi = it },
                petunjuk = "Misalnya: diterima di jurusan yang kuinginkan",
            )

            if (alasan.isBlank() || mimpi.isBlank()) {
                Text(
                    text = if (sedangMengubah) {
                        "Kedua jawaban harus terisi supaya bisa disimpan."
                    } else {
                        "Isi kedua jawaban untuk melanjutkan."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeksRedup,
                )
            } else if (!sedangMengubah) {
                TombolBesar(teks = "Simpan dan mulai", onClick = { onSimpan(alasan, mimpi) })
            } else if (adaPerubahan) {
                TombolBesar(teks = "Simpan perubahan", onClick = { onSimpan(alasan, mimpi) })
            } else {
                Text(
                    text = "Belum ada yang diubah.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeksRedup,
                )
            }
        }
    }
}

@Composable
private fun KolomJawaban(
    pertanyaan: String,
    nilai: String,
    onUbah: (String) -> Unit,
    petunjuk: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = pertanyaan,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = nilai,
            onValueChange = { onUbah(it.take(PANJANG_TERBANYAK)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(petunjuk) },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(16.dp),
            colors = warnaKolomTeks(),
        )
    }
}
