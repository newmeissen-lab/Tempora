package com.tempora.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tempora.app.pengingat.BUKA_APLIKASI_TERKUNCI
import com.tempora.app.pengingat.BUKA_ATUR
import com.tempora.app.pengingat.BUKA_MATERI
import com.tempora.app.pengingat.BUKA_PROGRES
import com.tempora.app.ui.screens.ARGUMEN_ID_MATERI
import com.tempora.app.ui.screens.AplikasiTerkunciScreen
import com.tempora.app.ui.screens.AturScreen
import com.tempora.app.ui.screens.BerandaScreen
import com.tempora.app.ui.screens.MateriScreen
import com.tempora.app.ui.screens.MemuatScreen
import com.tempora.app.ui.screens.MotivasiScreen
import com.tempora.app.ui.screens.MotivasiViewModel
import com.tempora.app.ui.screens.ProgresScreen
import com.tempora.app.ui.screens.RangkumanScreen
import com.tempora.app.ui.theme.BilahNavigasi
import com.tempora.app.ui.theme.Garis
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Latar
import com.tempora.app.ui.theme.Teks
import com.tempora.app.ui.theme.TeksDiAtasTerang
import com.tempora.app.ui.theme.TeksRedup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Layar di luar keempat tab. Layar ini menutupi bilah tab dan punya tombol kembali sendiri.
private const val RUTE_APLIKASI_TERKUNCI = "aplikasi-terkunci"
private const val RUTE_RANGKUMAN = "rangkuman"
private const val RUTE_MOTIVASI = "motivasi"

private const val LAMA_LAYAR_MEMUAT_MS = 1500L

// bukaDariNotifikasi: halaman yang diminta notifikasi yang baru diketuk (null kalau tidak ada).
// onSudahDibuka dipanggil setelah halaman itu dibuka, supaya tidak dibuka dua kali.
@Composable
fun TemporaApp(bukaDariNotifikasi: String? = null, onSudahDibuka: () -> Unit = {}) {
    // rememberSaveable: kalau layar diputar, layar memuat tidak diulang dari awal.
    var sedangMemuat by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(LAMA_LAYAR_MEMUAT_MS)
        sedangMemuat = false
    }

    val motivasiViewModel: MotivasiViewModel = viewModel()
    val perluTanyaMotivasi by motivasiViewModel.perluDitanya.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        // Rangka utama sudah bekerja di belakang layar memuat, jadi data Beranda
        // sudah siap saat layar memuat hilang.
        RangkaUtama(motivasiViewModel, bukaDariNotifikasi, onSudahDibuka)
        // Pertanyaan alasan dan mimpi: hanya tampil selama belum pernah diisi.
        if (perluTanyaMotivasi == true) MotivasiScreen(onSimpan = motivasiViewModel::simpan)
        if (sedangMemuat) MemuatScreen()
    }
}

// Rangka aplikasi: bilah tab di bawah, dan di atasnya layar yang sedang dipilih.
@Composable
private fun RangkaUtama(
    motivasiViewModel: MotivasiViewModel,
    bukaDariNotifikasi: String?,
    onSudahDibuka: () -> Unit,
) {
    // navController mengingat layar mana yang sedang dibuka dan mengurus perpindahannya.
    val navController = rememberNavController()
    val entriSekarang by navController.currentBackStackEntryAsState()
    val ruteSekarang = entriSekarang?.destination?.route
    val sedangDiTab = ruteSekarang == null || Tujuan.entries.any { it.rute == ruteSekarang }

    val bukaTab: (Tujuan) -> Unit = { tujuan ->
        navController.navigate(tujuan.rute) {
            // Kembali ke Beranda dulu, supaya tombol Back tidak perlu ditekan
            // berkali-kali setelah bolak-balik pindah tab.
            popUpTo(Tujuan.Beranda.rute) { saveState = true }
            // Mengetuk tab yang sedang aktif tidak membuat layar kedua.
            launchSingleTop = true
            restoreState = true
        }
    }
    val bukaAplikasiTerkunci: () -> Unit = {
        navController.navigate(RUTE_APLIKASI_TERKUNCI) { launchSingleTop = true }
    }

    // Notifikasi pengingat bisa meminta halaman tertentu langsung dibuka.
    LaunchedEffect(bukaDariNotifikasi) {
        if (bukaDariNotifikasi == null) return@LaunchedEffect
        // Menunggu sampai navigasi siap, yaitu halaman pertama sudah terpasang.
        navController.currentBackStackEntryFlow.first()
        when (bukaDariNotifikasi) {
            BUKA_APLIKASI_TERKUNCI -> bukaAplikasiTerkunci()
            BUKA_MATERI -> bukaTab(Tujuan.Materi)
            BUKA_PROGRES -> bukaTab(Tujuan.Progres)
            BUKA_ATUR -> bukaTab(Tujuan.Atur)
        }
        onSudahDibuka()
    }

    Scaffold(
        bottomBar = { if (sedangDiTab) BilahTab(ruteSekarang, bukaTab) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tujuan.Beranda.rute,
            // innerPadding = ruang yang sudah terpakai status bar dan bilah tab.
            // imePadding = ruang untuk keyboard, supaya daftar tidak tertutup saat mengetik.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            // Pindah layar tanpa animasi.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Tujuan.Beranda.rute) {
                Halaman {
                    BerandaScreen(
                        onBukaAplikasiTerkunci = bukaAplikasiTerkunci,
                        onBukaMateri = { bukaTab(Tujuan.Materi) },
                    )
                }
            }
            composable(Tujuan.Materi.rute) {
                Halaman {
                    MateriScreen(
                        onBacaRangkuman = { idMateri ->
                            navController.navigate("$RUTE_RANGKUMAN/$idMateri") { launchSingleTop = true }
                        },
                    )
                }
            }
            // {idMateri} adalah bagian alamat yang berubah-ubah: id materi yang dibuka.
            // RangkumanViewModel membacanya lewat SavedStateHandle.
            composable(
                route = "$RUTE_RANGKUMAN/{$ARGUMEN_ID_MATERI}",
                arguments = listOf(navArgument(ARGUMEN_ID_MATERI) { type = NavType.LongType }),
            ) {
                Halaman { RangkumanScreen(onKembali = { navController.popBackStack() }) }
            }
            composable(Tujuan.Progres.rute) { Halaman { ProgresScreen() } }
            composable(Tujuan.Atur.rute) {
                Halaman {
                    AturScreen(
                        onBukaAplikasiTerkunci = bukaAplikasiTerkunci,
                        onBukaMateri = { bukaTab(Tujuan.Materi) },
                        onBukaMotivasi = {
                            navController.navigate(RUTE_MOTIVASI) { launchSingleTop = true }
                        },
                    )
                }
            }
            // Mengubah jawaban alasan dan mimpi. Layarnya sama dengan pertanyaan pembuka,
            // hanya sudah terisi jawaban yang tersimpan.
            composable(RUTE_MOTIVASI) {
                Halaman {
                    val tersimpan by motivasiViewModel.motivasi.collectAsStateWithLifecycle()
                    // Menunggu jawaban selesai dibaca, supaya kolom tidak tampil kosong dulu.
                    tersimpan?.let { motivasi ->
                        MotivasiScreen(
                            onSimpan = { alasan, mimpi ->
                                motivasiViewModel.simpan(alasan, mimpi)
                                navController.popBackStack()
                            },
                            awal = motivasi,
                            onKembali = { navController.popBackStack() },
                        )
                    }
                }
            }
            composable(RUTE_APLIKASI_TERKUNCI) {
                Halaman {
                    AplikasiTerkunciScreen(onKembali = { navController.popBackStack() })
                }
            }
        }
    }
}

// Tiap halaman diberi latar sendiri yang tidak tembus pandang. Saat berpindah, halaman lama
// masih digambar sekejap; tanpa latar ini isinya terlihat menumpuk dengan halaman baru.
@Composable
private fun Halaman(isi: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Latar),
    ) {
        isi()
    }
}

@Composable
private fun BilahTab(ruteSekarang: String?, onPilih: (Tujuan) -> Unit) {
    Column {
        HorizontalDivider(color = Garis)
        NavigationBar(containerColor = BilahNavigasi) {
            Tujuan.entries.forEach { tujuan ->
                NavigationBarItem(
                    selected = ruteSekarang == tujuan.rute,
                    onClick = { onPilih(tujuan) },
                    icon = { Icon(painterResource(tujuan.ikon), contentDescription = null) },
                    label = { Text(tujuan.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TeksDiAtasTerang,
                        selectedTextColor = Teks,
                        indicatorColor = Kuning,
                        unselectedIconColor = TeksRedup,
                        unselectedTextColor = TeksRedup,
                    ),
                )
            }
        }
    }
}
