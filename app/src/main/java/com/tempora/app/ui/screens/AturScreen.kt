package com.tempora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tempora.app.pengingat.JAM_MULAI
import com.tempora.app.pengingat.JAM_SELESAI
import com.tempora.app.pengingat.MAKS_PER_HARI
import com.tempora.app.ui.components.BarisAtur
import com.tempora.app.ui.components.BarisSakelar
import com.tempora.app.ui.components.JudulLayar
import com.tempora.app.ui.components.LabelBagian
import com.tempora.app.ui.components.bukaPengaturanAksesPenggunaan
import com.tempora.app.ui.components.bukaPengaturanNotifikasi
import com.tempora.app.ui.components.bukaPengaturanTampilDiAtas
import com.tempora.app.ui.theme.Garis

@Composable
fun AturScreen(
    onBukaAplikasiTerkunci: () -> Unit,
    onBukaMateri: () -> Unit,
    onBukaMotivasi: () -> Unit,
    viewModel: AturViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.periksaIzin() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        JudulLayar("Atur")

        Column {
            LabelBagian("Kunci")
            BarisAtur(
                judul = "Aplikasi terkunci",
                nilai = "${state.jumlahTerkunci} aplikasi",
                onClick = onBukaAplikasiTerkunci,
            )
            HorizontalDivider(color = Garis)
            BarisAtur(
                judul = "Materi untuk soal kunci",
                nilai = state.judulMateriKunci ?: "Belum dipilih",
                onClick = onBukaMateri,
            )
        }

        Column {
            LabelBagian("Tentang kamu")
            BarisAtur(
                judul = "Alasan dan mimpi",
                nilai = "Ubah",
                onClick = onBukaMotivasi,
            )
        }

        Column {
            LabelBagian("Perangkat")
            BarisAtur(
                judul = "Akses penggunaan",
                nilai = if (state.izin.aksesPenggunaan) "Aktif" else "Belum aktif",
                onClick = { bukaPengaturanAksesPenggunaan(context) },
                ditonjolkan = !state.izin.aksesPenggunaan,
            )
            HorizontalDivider(color = Garis)
            BarisAtur(
                judul = "Tampil di atas aplikasi lain",
                nilai = if (state.izin.tampilDiAtas) "Aktif" else "Belum aktif",
                onClick = { bukaPengaturanTampilDiAtas(context) },
                ditonjolkan = !state.izin.tampilDiAtas,
            )
            HorizontalDivider(color = Garis)
            BarisAtur(
                judul = "Notifikasi",
                nilai = if (state.izin.notifikasi) "Aktif" else "Belum aktif",
                onClick = { bukaPengaturanNotifikasi(context) },
                ditonjolkan = !state.izin.notifikasi,
            )
        }

        Column {
            LabelBagian("Pengingat")
            BarisSakelar(
                judul = "Notifikasi pengingat",
                keterangan = "Muncul saat HP sedang dipakai, berjarak 2 sampai 4 jam, paling banyak " +
                    "$MAKS_PER_HARI per hari, antara pukul ${JAM_MULAI.toString().padStart(2, '0')}.00 " +
                    "dan $JAM_SELESAI.00",
                aktif = state.pengingatAktif,
                onUbah = viewModel::aturPengingat,
            )
        }
    }
}
