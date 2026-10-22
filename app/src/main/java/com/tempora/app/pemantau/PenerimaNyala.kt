package com.tempora.app.pemantau

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Dipanggil Android setelah HP selesai dinyalakan, supaya pemantau jalan lagi tanpa
// harus membuka Tempora. Kalau tidak ada aplikasi terkunci, layanan berhenti sendiri.
class PenerimaNyala : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) LayananPemantau.mulai(context)
    }
}
