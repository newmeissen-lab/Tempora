package com.tempora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tempora.app.R
import com.tempora.app.ui.components.JudulLayar
import com.tempora.app.ui.components.KartuIzin
import com.tempora.app.ui.components.LabelBagian
import com.tempora.app.ui.formatBulan
import com.tempora.app.ui.formatDurasi
import com.tempora.app.ui.formatTanggalPanjang
import com.tempora.app.ui.namaHari
import com.tempora.app.ui.theme.Garis
import com.tempora.app.ui.theme.GarisTebal
import com.tempora.app.ui.theme.HurufAngka
import com.tempora.app.ui.theme.HurufIsi
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Permukaan
import com.tempora.app.ui.theme.Teks
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ProgresScreen(viewModel: ProgresViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.muatUlang() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        JudulLayar("Progres")

        when {
            state.memuat -> Unit
            !state.izinDiberikan -> KartuIzin()
            state.hari.isNotEmpty() -> BagianWaktuLayar(state.hari)
        }

        HorizontalDivider(color = Garis)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LabelBagian("Soal dijawab hari ini")
            Text(
                text = buildAnnotatedString {
                    append(state.soalHariIni.toString())
                    withStyle(
                        SpanStyle(
                            fontFamily = HurufIsi,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = TeksRedup,
                        ),
                    ) {
                        append(" soal")
                    }
                },
                fontFamily = HurufAngka,
                fontSize = 46.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        HorizontalDivider(color = Garis)

        KalenderSoal(
            bulan = state.bulan,
            soalPerTanggal = state.soalPerTanggal,
            bisaMaju = state.bisaMaju,
            onSebelumnya = viewModel::bulanSebelumnya,
            onBerikutnya = viewModel::bulanBerikutnya,
        )
    }
}

// Huruf pertama nama hari, mulai Senin.
private val HURUF_HARI = listOf("S", "S", "R", "K", "J", "S", "M")

// Kalender satu bulan. Tiap kotak berisi tanggal dan jumlah soal yang dijawab hari itu.
@Composable
private fun KalenderSoal(
    bulan: YearMonth,
    soalPerTanggal: Map<LocalDate, Int>,
    bisaMaju: Boolean,
    onSebelumnya: () -> Unit,
    onBerikutnya: () -> Unit,
) {
    val hariIni = LocalDate.now()
    // Kotak kosong di depan supaya tanggal 1 jatuh di kolom harinya (Senin = kolom pertama),
    // lalu dipotong per 7 menjadi baris-baris minggu.
    val minggu = remember(bulan) {
        val kosongDiDepan = bulan.atDay(1).dayOfWeek.value - 1
        val kotak = List<LocalDate?>(kosongDiDepan) { null } +
            (1..bulan.lengthOfMonth()).map { bulan.atDay(it) }
        kotak.chunked(7).map { baris -> baris + List<LocalDate?>(7 - baris.size) { null } }
    }
    val totalSoal = soalPerTanggal.values.sum()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelBagian("Soal dijawab per hari")

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSebelumnya) {
                Icon(painterResource(R.drawable.ic_panah_kiri), contentDescription = "Bulan sebelumnya")
            }
            Text(
                text = formatBulan(bulan),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = onBerikutnya, enabled = bisaMaju) {
                Icon(
                    painter = painterResource(R.drawable.ic_panah_kanan),
                    contentDescription = "Bulan berikutnya",
                    tint = if (bisaMaju) Teks else GarisTebal,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HURUF_HARI.forEach { huruf ->
                Text(
                    text = huruf,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = TeksRedup,
                )
            }
        }

        minggu.forEach { baris ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                baris.forEach { tanggal ->
                    if (tanggal == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        KotakTanggal(
                            tanggal = tanggal,
                            jumlahSoal = soalPerTanggal[tanggal] ?: 0,
                            iniHariIni = tanggal == hariIni,
                        )
                    }
                }
            }
        }

        Text(
            text = if (totalSoal > 0) {
                "$totalSoal soal dijawab dalam ${soalPerTanggal.size} hari di bulan ini."
            } else {
                "Belum ada soal yang dijawab di bulan ini."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TeksRedup,
        )
    }
}

// Satu hari di kalender. Hari yang ada soalnya berwarna kuning dan menampilkan jumlahnya;
// hari ini diberi garis tepi.
@Composable
private fun RowScope.KotakTanggal(tanggal: LocalDate, jumlahSoal: Int, iniHariIni: Boolean) {
    val bentuk = RoundedCornerShape(10.dp)
    val adaSoal = jumlahSoal > 0
    val garisHariIni = if (iniHariIni) Modifier.border(2.dp, Teks, bentuk) else Modifier
    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(bentuk)
            .background(if (adaSoal) Kuning else Permukaan)
            .then(garisHariIni)
            .semantics(mergeDescendants = true) {
                contentDescription = "${formatTanggalPanjang(tanggal)}, $jumlahSoal soal"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tanggal.dayOfMonth.toString(),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = if (adaSoal) TeksDiAtasTerang else TeksRedup,
        )
        Text(
            text = if (adaSoal) jumlahSoal.toString() else "",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TeksDiAtasTerang,
        )
    }
}

@Composable
private fun BagianWaktuLayar(hari: List<BatangHari>) {
    val hariIni = LocalDate.now()
    val rataRata = hari.sumOf { it.totalMs } / hari.size
    val terlama = hari.maxBy { it.totalMs }
    val bentukBatang = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LabelBagian("Waktu layar 7 hari terakhir")
            Text(
                text = buildAnnotatedString {
                    append("rata-rata ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatDurasi(rataRata)) }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Tinggi tiap batang dibanding hari yang paling lama. Batang paling pendek tetap
        // diberi sedikit tinggi supaya hari tanpa pemakaian masih terlihat.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            hari.forEach { batang ->
                val fraksi = if (terlama.totalMs > 0) batang.totalMs.toFloat() / terlama.totalMs else 0f
                val garisHariIni = if (batang.tanggal == hariIni) {
                    Modifier.border(2.dp, Teks, bentukBatang)
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraksi.coerceIn(0.03f, 1f))
                        .clip(bentukBatang)
                        .background(Kuning)
                        .then(garisHariIni)
                        .semantics {
                            contentDescription = "${namaHari(batang.tanggal)} ${formatDurasi(batang.totalMs)}"
                        },
                )
            }
        }

        // Huruf pertama nama hari di bawah tiap batang; hari ini ditebalkan.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            hari.forEach { batang ->
                val iniHariIni = batang.tanggal == hariIni
                Text(
                    text = namaHari(batang.tanggal).take(1),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = if (iniHariIni) Teks else TeksRedup,
                    fontWeight = if (iniHariIni) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        Text(
            text = "Hari ini ${formatDurasi(hari.last().totalMs)}. " +
                "Paling lama ${namaHari(terlama.tanggal)}, ${formatDurasi(terlama.totalMs)}.",
            style = MaterialTheme.typography.bodySmall,
            color = TeksRedup,
        )
    }
}
