plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // for Room
}

android {
    namespace = "com.example.budgetplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.budgetplanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true   // <- add this
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
    }

    // enable java.time (YearMonth, LocalDate) on API < 26
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Core + Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    // Vico charts for Compose + Material3
    implementation(libs.core.v213)
    implementation(libs.compose.v213)
    implementation(libs.compose.m3.v213) // f
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.animated.vector.drawable)
    implementation(libs.androidx.foundation)
    // If your catalog doesn’t have a compiler alias, add a direct coord:
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.text.recognition) // base artifact
    implementation(libs.kotlinx.coroutines.play.services)

   // implementation(libs.mlkit.bom) // any recent 3x works
    implementation(libs.text.recognition)           // ← no '-latin', no version
    implementation(libs.kotlinx.coroutines.play.services) // for await()
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Desugaring for java.time on API < 26
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom.v20240600))
    implementation(libs.androidx.compose.material3.material3)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Retrofit
    implementation(libs.retrofit.v2110)
    implementation(libs.converter.scalars)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)

// Moshi (JSON library Retrofit uses)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

// DataStore
    implementation(libs.androidx.datastore.preferences.v111)

// Google Fonts loader for Compose
    implementation(libs.androidx.ui.text.google.fonts)

// System bars control (status/navigation) for pure black
    implementation(libs.accompanist.systemuicontroller)

    // (tests / debug tooling as you had)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // CameraX
    val camerax = "1.3.4"
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view) // PreviewView
    implementation(libs.kotlinx.coroutines.play.services)

    // ML Kit Text Recognition v2
    implementation(libs.text.recognition)
    // Optional Latin script model (usually pulled automatically)
    //implementation(libs.text.recognition.latin)

    // Jetpack
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose.v1101)
}
