import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// API key Gemini dibaca dari local.properties (file yang tidak ikut dibagikan), lalu
// dimasukkan ke aplikasi saat build sebagai BuildConfig.GEMINI_API_KEY.
// Kalau barisnya belum diisi, nilainya kosong dan aplikasi menampilkan pesan.
val propertiLokal = Properties().apply {
    val berkas = rootProject.file("local.properties")
    if (berkas.exists()) berkas.inputStream().use { load(it) }
}
val kunciGemini: String = propertiLokal.getProperty("GEMINI_API_KEY", "").trim()

android {
    namespace = "com.tempora.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tempora.app"
        // Android 8.0. Mulai versi ini overlay layar kuis dan kanal notifikasi bisa dipakai
        // tanpa kode khusus untuk HP lama.
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "0.12"

        buildConfigField("String", "GEMINI_API_KEY", "\"$kunciGemini\"")
    }

    // Kunci tanda tangan APK. Android hanya mau memperbarui aplikasi yang sudah terpasang kalau
    // APK barunya ditandatangani dengan kunci yang sama; kalau kuncinya berbeda, aplikasi lama
    // harus dihapus dulu dan datanya ikut hilang.
    // kunci/tempora.keystore adalah salinan kunci yang sejak awal dipakai Android Studio di
    // laptop ini (kata sandinya kata sandi bawaan Android Studio), jadi Tempora yang sudah ada
    // di HP bisa diperbarui tanpa kehilangan data. Simpan salinan file itu di tempat aman.
    signingConfigs {
        create("tempora") {
            storeFile = rootProject.file("kunci/tempora.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        // debug = versi yang dipasang tombol Run di Android Studio. Dipakai selama mengerjakan.
        debug {
            signingConfig = signingConfigs.getByName("tempora")
        }
        // release = versi untuk dibagikan. R8 membuang kode dan gambar yang tidak terpakai lalu
        // mengoptimalkan sisanya, sehingga APK lebih kecil dan aplikasi lebih cepat.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("tempora")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        // Membuat kelas BuildConfig, tempat API key di atas disimpan.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime)
}
