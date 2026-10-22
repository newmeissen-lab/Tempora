# Aturan tambahan untuk R8, alat yang membuang kode tak terpakai dan mengoptimalkan aplikasi
# saat build rilis. Aturan untuk pustaka (Compose, Room, WorkManager, dan lainnya) sudah dibawa
# pustakanya masing-masing; file ini hanya berisi yang khusus untuk Tempora.

# Nama kelas dan fungsi tidak disamarkan. Dengan begitu pesan error di Logcat dari versi rilis
# tetap menyebut nama kelas, nama file, dan nomor baris yang asli, sama seperti versi debug.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
