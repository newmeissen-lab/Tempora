package com.tempora.app.kuis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempora.app.R
import com.tempora.app.data.CatatanSalah
import com.tempora.app.ui.components.TombolBesar
import com.tempora.app.ui.formatBatas
import com.tempora.app.ui.theme.Garis
import com.tempora.app.ui.theme.Hijau
import com.tempora.app.ui.theme.HurufAngka
import com.tempora.app.ui.theme.HurufJudul
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Latar
import com.tempora.app.ui.theme.LatarLencana
import com.tempora.app.ui.theme.Merah
import com.tempora.app.ui.theme.Permukaan
import com.tempora.app.ui.theme.PermukaanKedua
import com.tempora.app.ui.theme.Teks
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup
import com.tempora.app.ui.theme.TombolUtama
import kotlinx.coroutines.delay

// Lama tambahan waktu yang bisa dipilih setelah menjawab benar, dalam menit.
private val PILIHAN_MENIT = listOf(2, 5, 10, 15)
private const val MENIT_BAWAAN = 5

// Lama menunggu sebagai ganti menjawab soal, dalam menit.
// Untuk menguji dengan cepat, ubah sementara menjadi 1, lalu kembalikan ke 30.
private const val LAMA_TUNGGU_MENIT = 30

// Setelah salah sekian kali berturut-turut, soal berikutnya baru bisa dikerjakan setelah jeda.
// Untuk menguji dengan cepat, ubah sementara LAMA_JEDA_MENIT menjadi 1, lalu kembalikan ke 5.
private const val BATAS_SALAH_BERUNTUN = 3
private const val LAMA_JEDA_MENIT = 5

private val HURUF_PILIHAN = listOf("A", "B", "C", "D")
private val GarisPilihan = Teks.copy(alpha = 0.28f)
private val GarisKotak = Teks.copy(alpha = 0.04f)

// Layar kuis punya enam keadaan; hanya satu yang tampil pada satu waktu.
private sealed interface Tahap {
    data object Soal : Tahap
    data class Salah(val jawaban: Int) : Tahap
    data object Benar : Tahap
    data object Rangkuman : Tahap
    data object Tunggu : Tahap
    data object Jeda : Tahap
}

// Layar kuis yang menutupi aplikasi terkunci.
// batasMenit: batas harian aplikasi itu, ditampilkan di kepala layar soal.
// bahan: soal dan rangkuman yang dipakai, dari materi pengguna atau soal contoh.
// tungguSampaiAwal: jam selesainya waktu tunggu yang sudah berjalan, atau null kalau tidak ada.
// catatanSalahAwal: jumlah salah beruntun dan jeda yang masih berjalan dari kuis sebelumnya.
// onDijawab dipanggil tiap sebuah soal dijawab, membawa catatan salah yang terbaru untuk disimpan.
// onMulaiTunggu dipanggil saat pengguna memilih menunggu, dengan jam selesainya.
// onBuka dipanggil setelah jawaban benar atau waktu tunggu selesai, dengan lama yang dipilih.
// onBatal dipanggil kalau pengguna memilih tidak jadi memakai aplikasinya.
@Composable
fun LayarKuis(
    namaAplikasi: String,
    batasMenit: Int,
    bahan: BahanKuis,
    tungguSampaiAwal: Long?,
    catatanSalahAwal: CatatanSalah,
    onDijawab: (benar: Boolean, catatanSalah: CatatanSalah) -> Unit,
    onMulaiTunggu: (sampaiMs: Long) -> Unit,
    onBuka: (menit: Int) -> Unit,
    onBatal: () -> Unit,
) {
    // Urutan soal diacak sekali tiap kuis muncul. "Coba soal lain" maju ke soal berikutnya.
    val urutan = remember { bahan.soal.shuffled() }
    var nomor by remember { mutableIntStateOf(0) }
    var tungguSampai by remember { mutableStateOf(tungguSampaiAwal) }
    var catatanSalah by remember { mutableStateOf(catatanSalahAwal) }
    // Yang tampil pertama: hitungan waktu tunggu kalau pengguna sedang menunggu, hitungan jeda
    // kalau jeda salah beruntun masih berjalan, selain itu langsung soal.
    var tahap by remember {
        mutableStateOf<Tahap>(
            when {
                tungguSampaiAwal != null -> Tahap.Tunggu
                System.currentTimeMillis() < catatanSalahAwal.jedaSampai -> Tahap.Jeda
                else -> Tahap.Soal
            },
        )
    }
    val soal = urutan[nomor % urutan.size]
    val sisaJedaDetik = hitungMundur(catatanSalah.jedaSampai)
    val sedangJeda = sisaJedaDetik > 0
    // Selama jeda masih berjalan, permintaan soal berikutnya dialihkan ke layar jeda.
    val soalLain = {
        if (System.currentTimeMillis() < catatanSalah.jedaSampai) {
            tahap = Tahap.Jeda
        } else {
            nomor++
            tahap = Tahap.Soal
        }
    }
    val keWaktuTunggu = {
        // Waktu tunggu yang sudah berjalan tidak diulang dari awal.
        if (tungguSampai == null) {
            val sampai = System.currentTimeMillis() + LAMA_TUNGGU_MENIT * 60_000L
            tungguSampai = sampai
            onMulaiTunggu(sampai)
        }
        tahap = Tahap.Tunggu
    }

    // Surface memberi latar gelap dan warna tulisan terang ke semua isi di dalamnya.
    Surface(modifier = Modifier.fillMaxSize(), color = Latar) {
        // key: tiap ganti tahap atau soal, isi dibuat baru sehingga gulirannya kembali ke atas.
        key(tahap, nomor) {
            when (val sekarang = tahap) {
                Tahap.Soal -> TahapSoal(
                    namaAplikasi = namaAplikasi,
                    batasMenit = batasMenit,
                    judulMateri = bahan.judulMateri,
                    soalContoh = bahan.contoh,
                    soal = soal,
                    onJawab = { pilihan ->
                        val benar = pilihan == soal.indeksBenar
                        val salahBeruntun = if (benar) 0 else catatanSalah.beruntun + 1
                        catatanSalah = if (salahBeruntun >= BATAS_SALAH_BERUNTUN) {
                            // Jeda dimulai, dan hitungan salah kembali ke nol untuk kesempatan berikutnya.
                            CatatanSalah(
                                beruntun = 0,
                                jedaSampai = System.currentTimeMillis() + LAMA_JEDA_MENIT * 60_000L,
                            )
                        } else {
                            catatanSalah.copy(beruntun = salahBeruntun)
                        }
                        onDijawab(benar, catatanSalah)
                        tahap = if (benar) Tahap.Benar else Tahap.Salah(pilihan)
                    },
                    onBatal = onBatal,
                )

                is Tahap.Salah -> TahapSalah(
                    namaAplikasi = namaAplikasi,
                    batasMenit = batasMenit,
                    judulMateri = bahan.judulMateri,
                    soalContoh = bahan.contoh,
                    soal = soal,
                    jawaban = sekarang.jawaban,
                    sedangJeda = sedangJeda,
                    sedangMenunggu = tungguSampai != null,
                    onRangkuman = { tahap = Tahap.Rangkuman },
                    onSoalLain = soalLain,
                    onTunggu = keWaktuTunggu,
                    onBatal = onBatal,
                )

                Tahap.Benar -> TahapPilihLama(
                    namaAplikasi = namaAplikasi,
                    judul = "Benar",
                    keterangan = soal.penjelasan,
                    onBuka = onBuka,
                    onBatal = onBatal,
                )

                Tahap.Rangkuman -> TahapRangkuman(
                    namaAplikasi = namaAplikasi,
                    judulMateri = bahan.judulMateri,
                    rangkuman = bahan.rangkuman,
                    sedangJeda = sedangJeda,
                    onSoalLain = soalLain,
                    onBatal = onBatal,
                )

                Tahap.Tunggu -> TahapTunggu(
                    namaAplikasi = namaAplikasi,
                    sampaiMs = tungguSampai ?: 0L,
                    onJawabSoal = soalLain,
                    onBuka = onBuka,
                    onBatal = onBatal,
                )

                Tahap.Jeda -> TahapJeda(
                    namaAplikasi = namaAplikasi,
                    sisaDetik = sisaJedaDetik,
                    sedangMenunggu = tungguSampai != null,
                    onRangkuman = { tahap = Tahap.Rangkuman },
                    onSelesai = soalLain,
                    onTunggu = keWaktuTunggu,
                    onBatal = onBatal,
                )
            }
        }
    }
}

// Garis kotak-kotak tipis di latar layar kuis, seperti di desain.
private fun Modifier.latarKotak(): Modifier = drawBehind {
    val jarak = 28.dp.toPx()
    var x = jarak
    while (x < size.width) {
        drawLine(GarisKotak, Offset(x, 0f), Offset(x, size.height))
        x += jarak
    }
    var y = jarak
    while (y < size.height) {
        drawLine(GarisKotak, Offset(0f, y), Offset(size.width, y))
        y += jarak
    }
}

// Susunan yang sama untuk semua tahap: isi di atas, tombol menempel di bawah layar.// Kalau layar pendek (misalnya HP dimiringkan), semuanya bisa digulir.
@Composable
private fun Rangka(
    atas: @Composable ColumnScope.() -> Unit,
    bawah: @Composable ColumnScope.() -> Unit,
) {
    // safeDrawingPadding menjauhkan isi dari status bar, poni kamera, dan bilah navigasi.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .latarKotak()
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp), content = atas)
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = bawah,
            )
        }
    }
}

// Kepala layar soal: aplikasi mana yang ditutup dan kenapa, lalu ajakan menjawab.
@Composable
private fun KepalaSoal(namaAplikasi: String, batasMenit: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Kotak terang bergambar gembok, dengan titik merah di sudut kanan bawahnya.
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TombolUtama),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gembok),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TeksDiAtasTerang,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Latar)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Merah),
                )
            }
            Text(
                text = buildAnnotatedString {
                    append("$namaAplikasi ditutup. Batas ")
                    withStyle(SpanStyle(color = Teks, fontWeight = FontWeight.Bold)) {
                        append(formatBatas(batasMenit))
                    }
                    append(" hari ini sudah tercapai.")
                },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TeksRedup,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TeksJudulBesar("Jawab")
                // Bagian yang disorot kuning.
                TeksJudulBesar(
                    teks = "1 soal",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Kuning)
                        .padding(horizontal = 10.dp),
                    warna = TeksDiAtasTerang,
                )
            }
            TeksJudulBesar("untuk membuka kunci")
        }
    }
}

@Composable
private fun TeksJudulBesar(teks: String, modifier: Modifier = Modifier, warna: Color = Teks) {
    Text(
        text = teks,
        modifier = modifier,
        fontFamily = HurufJudul,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        color = warna,
    )
}

// Kartu dengan garis berwarna di tepi atasnya. Dipakai untuk soal dan rangkuman.
@Composable
private fun KartuKuis(warnaAtas: Color, isi: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Permukaan),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(warnaAtas),
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = isi,
        )
    }
}

// Isi kartu soal: keterangan materi, pertanyaan, dan empat pilihan.
// jawaban = null berarti soal belum dijawab, jadi pilihannya bisa diketuk. Kalau sudah dijawab,
// pilihan yang benar diberi warna hijau dan pilihan pengguna yang salah diberi warna merah.
@Composable
private fun IsiSoal(
    judulMateri: String,
    soalContoh: Boolean,
    soal: Soal,
    jawaban: Int?,
    onJawab: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Soal kunci · $judulMateri",
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = TeksRedup,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // Penanda ini hanya muncul selama soal dari materi pengguna belum ada.
        if (soalContoh) {
            Text(
                text = "Soal contoh",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LatarLencana)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Kuning,
            )
        }
    }

    Text(
        text = soal.pertanyaan,
        fontFamily = HurufJudul,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        soal.pilihan.forEachIndexed { indeks, teks ->
            BarisPilihan(
                huruf = HURUF_PILIHAN[indeks],
                teks = teks,
                keadaan = when {
                    jawaban == null -> KeadaanPilihan.Biasa
                    indeks == soal.indeksBenar -> KeadaanPilihan.Benar
                    indeks == jawaban -> KeadaanPilihan.Salah
                    else -> KeadaanPilihan.Redup
                },
                onClick = if (jawaban == null) {
                    { onJawab(indeks) }
                } else {
                    null
                },
            )
        }
    }
}

// Keadaan satu baris pilihan. Biasa = belum dijawab. Tiga lainnya dipakai setelah dijawab:
// Benar = jawaban yang benar, Salah = pilihan pengguna yang keliru, Redup = pilihan lainnya.
private enum class KeadaanPilihan { Biasa, Redup, Benar, Salah }

@Composable
private fun BarisPilihan(huruf: String, teks: String, keadaan: KeadaanPilihan, onClick: (() -> Unit)?) {
    // Warna penanda; null untuk pilihan yang tidak ditandai.
    val warna = when (keadaan) {
        KeadaanPilihan.Benar -> Hijau
        KeadaanPilihan.Salah -> Merah
        else -> null
    }
    val bentuk = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(bentuk)
            .background(Latar)
            .background(warna?.copy(alpha = 0.14f) ?: Color.Transparent)
            .border(
                width = 1.5.dp,
                color = warna ?: if (keadaan == KeadaanPilihan.Redup) Garis else GarisPilihan,
                shape = bentuk,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(warna ?: PermukaanKedua),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = huruf,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    warna != null -> TeksDiAtasTerang
                    keadaan == KeadaanPilihan.Redup -> TeksRedup
                    else -> Teks
                },
            )
        }
        Text(
            text = teks,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            color = if (keadaan == KeadaanPilihan.Redup) TeksRedup else Teks,
        )
        if (warna != null) {
            Icon(
                painter = painterResource(
                    if (keadaan == KeadaanPilihan.Benar) R.drawable.ic_centang else R.drawable.ic_silang,
                ),
                contentDescription = if (keadaan == KeadaanPilihan.Benar) "Jawaban benar" else "Jawabanmu",
                modifier = Modifier.size(18.dp),
                tint = warna,
            )
        }
    }
}

// Kotak berwarna berisi hasil jawaban dan penjelasannya.
@Composable
private fun KotakPenjelasan(warna: Color, judul: String, isi: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(warna)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = judul, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TeksDiAtasTerang)
        if (isi.isNotBlank()) {
            Text(text = isi, fontSize = 15.sp, lineHeight = 22.sp, color = TeksDiAtasTerang)
        }
    }
}

@Composable
private fun TahapSoal(
    namaAplikasi: String,
    batasMenit: Int,
    judulMateri: String,
    soalContoh: Boolean,
    soal: Soal,
    onJawab: (Int) -> Unit,
    onBatal: () -> Unit,
) {
    Rangka(
        atas = {
            KepalaSoal(namaAplikasi, batasMenit)
            KartuKuis(warnaAtas = Merah) {
                IsiSoal(judulMateri, soalContoh, soal, jawaban = null, onJawab = onJawab)
            }
        },
        bawah = {
            Button(
                onClick = onBatal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PermukaanKedua,
                    contentColor = Teks,
                ),
            ) {
                Text(
                    text = "Tidak jadi buka $namaAplikasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

// Jawaban salah. Soalnya tetap terlihat: pilihan pengguna ditandai merah, jawaban yang benar
// ditandai hijau, dan penjelasannya ada di kotak berwarna di bawahnya.
@Composable
private fun TahapSalah(
    namaAplikasi: String,
    batasMenit: Int,
    judulMateri: String,
    soalContoh: Boolean,
    soal: Soal,
    jawaban: Int,
    sedangJeda: Boolean,
    sedangMenunggu: Boolean,
    onRangkuman: () -> Unit,
    onSoalLain: () -> Unit,
    onTunggu: () -> Unit,
    onBatal: () -> Unit,
) {
    Rangka(
        atas = {
            KepalaSoal(namaAplikasi, batasMenit)
            KartuKuis(warnaAtas = Merah) {
                IsiSoal(judulMateri, soalContoh, soal, jawaban = jawaban, onJawab = {})
                KotakPenjelasan(warna = Merah, judul = "Belum tepat.", isi = soal.penjelasan)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gembok),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TeksRedup,
                )
                Text(
                    text = if (sedangJeda) {
                        "Kamu salah $BATAS_SALAH_BERUNTUN kali berturut-turut. Soal berikutnya baru bisa " +
                            "dikerjakan setelah jeda $LAMA_JEDA_MENIT menit. Pakai jeda itu untuk membaca rangkuman."
                    } else {
                        "$namaAplikasi tetap terkunci. Baca rangkumannya dulu, coba soal lain, " +
                            "atau tunggu $LAMA_TUNGGU_MENIT menit."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TeksRedup,
                )
            }
        },
        bawah = {
            TombolBesar(teks = "Baca rangkuman", onClick = onRangkuman)
            // Selama jeda, tombol ini membuka layar hitungan jeda, bukan soal baru.
            TombolGaris(teks = if (sedangJeda) "Lihat sisa jeda" else "Coba soal lain", onClick = onSoalLain)
            TombolTeks(
                teks = if (sedangMenunggu) {
                    "Kembali ke waktu tunggu"
                } else {
                    "Tidak bisa menjawab? Tunggu $LAMA_TUNGGU_MENIT menit"
                },
                onClick = onTunggu,
                warna = Teks,
            )
            TombolTeks(teks = "Tidak jadi, tutup $namaAplikasi", onClick = onBatal)
        },
    )
}

// Sisa waktu sampai jam sampaiMs, dalam detik, yang diperbarui sendiri tiap detik.
// Hasilnya 0 kalau jam itu sudah lewat. Hitungannya memakai jam HP, jadi tetap berjalan
// walau layar kuis ditutup.
@Composable
private fun hitungMundur(sampaiMs: Long): Long {
    var sekarang by remember(sampaiMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sampaiMs) {
        while (sekarang < sampaiMs) {
            delay(1_000)
            sekarang = System.currentTimeMillis()
        }
    }
    return ((sampaiMs - sekarang + 999) / 1_000).coerceAtLeast(0)
}

// Lingkaran berisi sisa waktu, misalnya 4:05 = 4 menit 5 detik.
@Composable
private fun LingkaranHitungMundur(sisaDetik: Long) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .size(208.dp)
                .border(6.dp, Kuning, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = "${sisaDetik / 60}:${(sisaDetik % 60).toString().padStart(2, '0')}",
                fontFamily = HurufAngka,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "menit lagi", fontSize = 14.sp, color = TeksRedup)
        }
    }
}

// Jeda setelah beberapa jawaban salah berturut-turut. Soal berikutnya belum bisa dikerjakan,
// tetapi rangkuman tetap bisa dibaca. Begitu jeda habis, soal berikutnya langsung tampil.
@Composable
private fun TahapJeda(
    namaAplikasi: String,
    sisaDetik: Long,
    sedangMenunggu: Boolean,
    onRangkuman: () -> Unit,
    onSelesai: () -> Unit,
    onTunggu: () -> Unit,
    onBatal: () -> Unit,
) {
    val jedaHabis = sisaDetik == 0L
    LaunchedEffect(jedaHabis) {
        if (jedaHabis) onSelesai()
    }

    Rangka(
        atas = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Jeda sebelum soal berikutnya", fontSize = 14.sp, color = TeksRedup)
                Text(
                    text = "$BATAS_SALAH_BERUNTUN jawaban salah berturut-turut",
                    fontFamily = HurufJudul,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            LingkaranHitungMundur(sisaDetik)

            Text(
                text = "Pakai jeda ini untuk membaca rangkuman, supaya soal berikutnya lebih mudah " +
                    "dijawab. Hitungan tetap berjalan walau layar ini ditutup.",
                fontSize = 15.sp,
                color = TeksRedup,
            )
        },
        bawah = {
            TombolBesar(teks = "Baca rangkuman", onClick = onRangkuman)
            TombolTeks(
                teks = if (sedangMenunggu) "Kembali ke waktu tunggu" else "Tunggu $LAMA_TUNGGU_MENIT menit saja",
                onClick = onTunggu,
                warna = Teks,
            )
            TombolTeks(teks = "Tidak jadi, tutup $namaAplikasi", onClick = onBatal)
        },
    )
}

// Hitungan mundur waktu tunggu. Begitu selesai, tampil pilihan lama membuka seperti setelah
// menjawab benar.
@Composable
private fun TahapTunggu(
    namaAplikasi: String,
    sampaiMs: Long,
    onJawabSoal: () -> Unit,
    onBuka: (menit: Int) -> Unit,
    onBatal: () -> Unit,
) {
    val sisaDetik = hitungMundur(sampaiMs)

    if (sisaDetik == 0L) {
        TahapPilihLama(
            namaAplikasi = namaAplikasi,
            judul = "Waktu tunggu selesai",
            keterangan = "Kamu sudah menunggu. Sekarang $namaAplikasi boleh dibuka tanpa menjawab soal.",
            onBuka = onBuka,
            onBatal = onBatal,
        )
        return
    }

    Rangka(
        atas = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Waktu tunggu untuk", fontSize = 14.sp, color = TeksRedup)
                Text(
                    text = namaAplikasi,
                    fontFamily = HurufJudul,
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            LingkaranHitungMundur(sisaDetik)

            Text(
                text = "Setelah waktu tunggu selesai, $namaAplikasi bisa dibuka tanpa menjawab soal. " +
                    "Hitungan tetap berjalan walau layar ini ditutup.",
                fontSize = 15.sp,
                color = TeksRedup,
            )
        },
        bawah = {
            TombolBesar(teks = "Tutup $namaAplikasi", onClick = onBatal)
            TombolGaris(teks = "Jawab soal saja", onClick = onJawabSoal)
        },
    )
}

// Pilihan lama membuka aplikasi. Dipakai setelah jawaban benar dan setelah waktu tunggu selesai.
@Composable
private fun TahapPilihLama(
    namaAplikasi: String,
    judul: String,
    keterangan: String,
    onBuka: (menit: Int) -> Unit,
    onBatal: () -> Unit,
) {
    var menit by remember { mutableIntStateOf(MENIT_BAWAAN) }

    Rangka(
        atas = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Kuning),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_centang),
                            contentDescription = null,
                            tint = TeksDiAtasTerang,
                        )
                    }
                    Text(text = judul, fontFamily = HurufJudul, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = keterangan, fontSize = 16.sp, color = TeksRedup)
            }

            HorizontalDivider(color = Garis)

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Berapa lama mau di $namaAplikasi?",
                    fontFamily = HurufJudul,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                )
                // Empat pilihan disusun dua baris, dua kolom.
                PILIHAN_MENIT.chunked(2).forEach { baris ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        baris.forEach { pilihan ->
                            PilihanMenit(
                                menit = pilihan,
                                dipilih = menit == pilihan,
                                onClick = { menit = pilihan },
                            )
                        }
                    }
                }
                Text(
                    text = "Saat waktunya habis, soal baru muncul.",
                    fontSize = 14.sp,
                    color = TeksRedup,
                )
            }
        },
        bawah = {
            TombolBesar(teks = "Buka $namaAplikasi, $menit menit", onClick = { onBuka(menit) })
            TombolTeks(teks = "Tidak jadi, cukup soalnya saja", onClick = onBatal, warna = Teks)
        },
    )
}

@Composable
private fun RowScope.PilihanMenit(menit: Int, dipilih: Boolean, onClick: () -> Unit) {
    val bentuk = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 92.dp)
            .clip(bentuk)
            .background(if (dipilih) Kuning else Permukaan)
            .border(1.5.dp, if (dipilih) Kuning else GarisPilihan, bentuk)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = menit.toString(),
            fontFamily = HurufAngka,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (dipilih) TeksDiAtasTerang else Teks,
        )
        Text(
            text = "menit",
            fontSize = 14.sp,
            color = if (dipilih) TeksDiAtasTerang else TeksRedup,
        )
    }
}

// Rangkuman materi, dibaca tanpa keluar dari layar kuis. Isinya ditaruh di dalam kartu
// bergaris kuning supaya menjadi pusat perhatian layar ini.
@Composable
private fun TahapRangkuman(
    namaAplikasi: String,
    judulMateri: String,
    rangkuman: String,
    sedangJeda: Boolean,
    onSoalLain: () -> Unit,
    onBatal: () -> Unit,
) {
    // Rangkuman dari AI berupa beberapa paragraf yang dipisah baris kosong.
    val paragraf = remember(rangkuman) {
        rangkuman.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    Rangka(
        atas = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Baca dulu, lalu coba lagi", fontSize = 14.sp, color = TeksRedup)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TeksJudulBesar(
                        teks = "Rangkuman",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Kuning)
                            .padding(horizontal = 10.dp),
                        warna = TeksDiAtasTerang,
                    )
                    TeksJudulBesar("materi")
                }
            }

            KartuKuis(warnaAtas = Kuning) {
                Text(
                    text = judulMateri,
                    fontFamily = HurufJudul,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider(color = Garis)
                if (paragraf.isEmpty()) {
                    Text(
                        text = "Rangkuman untuk materi ini belum tersedia.",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = TeksRedup,
                    )
                }
                // Tiap paragraf diberi penanda kotak kuning.
                paragraf.forEach { satu ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 9.dp)
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Kuning),
                        )
                        Text(text = satu, fontSize = 16.sp, lineHeight = 25.sp)
                    }
                }
            }
        },
        bawah = {
            // Selama jeda, tombol ini kembali ke layar hitungan jeda.
            TombolBesar(teks = if (sedangJeda) "Kembali ke jeda" else "Coba soal lain", onClick = onSoalLain)
            TombolTeks(teks = "Tidak jadi, tutup $namaAplikasi", onClick = onBatal)
        },
    )
}

@Composable
private fun TombolGaris(teks: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, Teks),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teks),
    ) {
        Text(text = teks, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun TombolTeks(teks: String, onClick: () -> Unit, warna: Color = TeksRedup) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
    ) {
        Text(
            text = teks,
            fontSize = 14.sp,
            color = warna,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
