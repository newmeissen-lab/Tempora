package com.tempora.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempora.app.R
import com.tempora.app.ui.theme.HurufLogo
import com.tempora.app.ui.theme.Kuning
import com.tempora.app.ui.theme.Latar
import com.tempora.app.ui.theme.PermukaanKedua
import com.tempora.app.ui.theme.TeksRedup

// Layar pembuka: maskot, tulisan Tempora, dan garis yang bergerak.
// Surface menutupi seluruh layar dan menahan sentuhan supaya tidak tembus ke layar di belakangnya.
@Composable
fun MemuatScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = Latar) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                // Gambar maskot punya latar yang sama persis dengan warna Latar, jadi menyatu.
                Image(
                    painter = painterResource(R.drawable.maskot_tempora),
                    contentDescription = "Maskot Tempora",
                    modifier = Modifier.size(width = 156.dp, height = 227.dp),
                )
                Text(text = "Tempora", fontFamily = HurufLogo, fontSize = 46.sp, lineHeight = 52.sp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GarisMemuat()
                Text(
                    text = "Menyiapkan data hari ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeksRedup,
                )
            }
        }
    }
}

// Batang kuning pendek yang bolak-balik di dalam garis sepanjang 120dp.
@Composable
private fun GarisMemuat() {
    val transisi = rememberInfiniteTransition(label = "garis memuat")
    val posisi by transisi.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "posisi batang",
    )
    val bentuk = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(4.dp)
            .clip(bentuk)
            .background(PermukaanKedua),
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = (72.dp * posisi).roundToPx(), y = 0) }
                .width(48.dp)
                .fillMaxHeight()
                .clip(bentuk)
                .background(Kuning),
        )
    }
}
