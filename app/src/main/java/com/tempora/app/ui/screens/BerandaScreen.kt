package com.tempora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tempora.app.R
import com.tempora.app.ui.components.BilahKemajuan
import com.tempora.app.ui.components.IkonAplikasi
import com.tempora.app.ui.components.KartuIzin
import com.tempora.app.ui.components.KartuIzinTampilDiAtas
import com.tempora.app.ui.components.LabelBagian
import com.tempora.app.ui.formatBatas
import com.tempora.app.ui.formatDurasi
import com.tempora.app.ui.formatTanggal
import com.tempora.app.ui.theme.Garis
import com.tempora.app.ui.theme.GarisTebal
import com.tempora.app.ui.theme.HurufAngka
import com.tempora.app.ui.theme.HurufLogo
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Permukaan
import com.tempora.app.ui.theme.Salah
import com.tempora.app.ui.theme.Teks
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup
import java.time.LocalDate

@Composable
fun BerandaScreen(
    onBukaAplikasiTerkunci: () -> Unit,
    onBukaMateri: () -> Unit,
    viewModel: BerandaViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ON_RESUME terjadi tiap kali layar ini tampil lagi, termasuk sepulang dari Pengaturan HP.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.muatUlang() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Kepala()

        when {
            state.memuat -> Unit
            state.izinDiberikan -> WaktuLayar(state.totalMs)
            else -> KartuIzin()
        }

        // Hanya perlu kalau sudah ada aplikasi yang dikunci.
        if (!state.memuat && state.jumlahTerkunci > 0 && !state.izinTampilDiAtas) {
            KartuIzinTampilDiAtas()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KartuPintasan(
                ikon = R.drawable.ic_gembok,
                judul = "Aplikasi terkunci",
                keterangan = "${state.jumlahTerkunci} aplikasi",
                menonjol = false,
                onClick = onBukaAplikasiTerkunci,
                modifier = Modifier.weight(1f),
            )
            KartuPintasan(
                ikon = R.drawable.ic_tambah,
                judul = "Tambah materi",
                keterangan = "Unggah PDF",
                menonjol = true,
                onClick = onBukaMateri,
                modifier = Modifier.weight(1f),
            )
        }

        if (!state.memuat) {
            HorizontalDivider(color = Garis)
            BagianTerkunci(state.terkunci)

            if (state.palingLama.isNotEmpty()) {
                HorizontalDivider(color = Garis)
                BagianPalingLama(state.palingLama)
            }
        }
    }
}

@Composable
private fun Kepala() {
    val tanggal = remember { formatTanggal(LocalDate.now()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BatangLogo(Teks)
                BatangLogo(Kuning)
            }
            Text(text = "Tempora", fontFamily = HurufLogo, fontSize = 26.sp)
        }
        Text(text = tanggal, style = MaterialTheme.typography.bodyMedium, color = TeksRedup)
    }
}

@Composable
private fun BatangLogo(warna: Color) {
    Box(
        modifier = Modifier
            .size(width = 9.dp, height = 26.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(warna),
    )
}

@Composable
private fun WaktuLayar(totalMs: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabelBagian("Waktu layar hari ini")
        Text(
            text = durasiBesar(totalMs),
            fontFamily = HurufAngka,
            fontSize = 46.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// Angka besar dengan huruf satuan kecil dan redup, seperti di mockup: 2j 14m.
private fun durasiBesar(ms: Long): AnnotatedString {
    val menit = ms / 60_000
    val satuan = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal, color = TeksRedup)
    return buildAnnotatedString {
        if (menit >= 60) {
            append((menit / 60).toString())
            withStyle(satuan) { append("j ") }
        }
        append((menit % 60).toString())
        withStyle(satuan) { append("m") }
    }
}

@Composable
private fun KartuPintasan(
    ikon: Int,
    judul: String,
    keterangan: String,
    menonjol: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bentuk = RoundedCornerShape(18.dp)
    val warnaIsi = if (menonjol) TeksDiAtasTerang else Teks
    Column(
        modifier = modifier
            .heightIn(min = 104.dp)
            .clip(bentuk)
            .background(if (menonjol) Kuning else Permukaan)
            .border(1.dp, if (menonjol) Kuning else GarisTebal, bentuk)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(ikon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = warnaIsi,
        )
        Text(
            text = judul,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = warnaIsi,
        )
        Text(
            text = keterangan,
            style = MaterialTheme.typography.labelMedium,
            color = if (menonjol) TeksDiAtasTerang else TeksRedup,
        )
    }
}

@Composable
private fun BagianTerkunci(daftar: List<PemakaianAplikasi>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LabelBagian("Aplikasi terkunci hari ini")
        if (daftar.isEmpty()) {
            Text(
                text = "Belum ada aplikasi yang dikunci.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        daftar.forEach { BarisTerkunci(it) }
    }
}

@Composable
private fun BarisTerkunci(aplikasi: PemakaianAplikasi) {
    val batasMenit = aplikasi.batasMenit ?: return
    val batasMs = batasMenit * 60_000L
    val lewatBatas = aplikasi.terpakaiMs >= batasMs

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IkonAplikasi(aplikasi.ikon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = aplikasi.nama,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatDurasi(aplikasi.terpakaiMs)} dari ${formatBatas(batasMenit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lewatBatas) Salah else TeksRedup,
                )
            }
            BilahKemajuan(
                fraksi = aplikasi.terpakaiMs.toFloat() / batasMs,
                warna = if (lewatBatas) Salah else Kuning,
            )
        }
    }
}

@Composable
private fun BagianPalingLama(daftar: List<PemakaianAplikasi>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LabelBagian("Paling lama dipakai hari ini")
        daftar.forEach { aplikasi ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IkonAplikasi(aplikasi.ikon)
                Text(
                    text = aplikasi.nama,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatDurasi(aplikasi.terpakaiMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeksRedup,
                )
            }
        }
    }
}
