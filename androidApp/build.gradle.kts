plugins {
    // AGP 9 ya incluye Kotlin: no hace falta el plugin org.jetbrains.kotlin.android.
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "es.dam.entrena"
    compileSdk = 36

    defaultConfig {
        applicationId = "es.dam.entrena"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}

