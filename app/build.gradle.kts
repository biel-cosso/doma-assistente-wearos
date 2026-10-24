plugins {
    // O AGP 9 já compila Kotlin sozinho (sem o plugin org.jetbrains.kotlin.android)
    id("com.android.application")
}

android {
    namespace = "com.example.domaassistente"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.domaassistente"
        minSdk = 30            // Wear OS 3 / Android 11 (R), como pede o roteiro
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
