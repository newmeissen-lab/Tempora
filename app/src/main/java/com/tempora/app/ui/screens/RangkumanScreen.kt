package com.tempora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tempora.app.R
import com.tempora.app.ui.components.JudulLayar
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.LatarLencana
import com.tempora.app.ui.theme.TeksRedup

// Halaman untuk membaca rangkuman satu materi. Dibuka dari tautan "Baca rangkuman" di tab Materi.
@Composable
fun RangkumanScreen(
    onKembali: () -> Unit,
    viewModel: RangkumanViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val materi = state.materi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onKembali, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(painterResource(R.drawable.ic_kembali), contentDescription = "Kembali ke Materi")
            }
            if (state.dipakaiUntukKunci) {
                Text(
                    text = "Dipakai untuk soal kunci",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LatarLencana)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Kuning,
                )
            }
        }

        when {
            state.memuat -> Unit

            materi == null -> Text(
                text = "Materi ini sudah tidak ada.",
                style = MaterialTheme.typography.bodyMedium,
                color = TeksRedup,
            )

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    JudulLayar(materi.judul)
                    Text(
                        text = "${materi.jumlahHalaman} halaman · ${state.jumlahSoal} soal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeksRedup,
                    )
                }
                IsiRangkuman(materi.rangkuman.orEmpty())
            }
        }
    }
}

@Composable
private fun IsiRangkuman(rangkuman: String) {
    if (rangkuman.isBlank()) {
        Text(
            text = "Rangkuman materi ini belum dibuat. Kembali ke Materi, lalu ketuk " +
                "\"Buat soal dengan AI\".",
            style = MaterialTheme.typography.bodyMedium,
            color = TeksRedup,
        )
        return
    }

    // Rangkuman dari AI berupa beberapa paragraf yang dipisah baris kosong.
    // Tiap paragraf ditampilkan dengan penanda kotak kuning, seperti di mockup.
    val paragraf = remember(rangkuman) {
        rangkuman.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = "Inti materi, disusun AI dari PDF kamu", fontSize = 13.sp, color = TeksRedup)
        paragraf.forEach { satu ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 9.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Kuning),
                )
                Text(text = satu, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
        Text(
            text = "Rangkuman dibuat AI dan bisa keliru. Cocokkan dengan PDF aslinya untuk hal yang penting.",
            style = MaterialTheme.typography.bodySmall,
            color = TeksRedup,
        )
    }
}
