// File build tingkat atas. Di sini plugin hanya didaftarkan; yang memakainya adalah app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
