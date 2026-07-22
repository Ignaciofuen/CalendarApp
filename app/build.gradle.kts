plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.calendarapp"
    compileSdk = 36 // VOLVEMOS A 36 OBLIGATORIAMENTE

    defaultConfig {
        applicationId = "com.example.calendarapp"
        minSdk = 24
        targetSdk = 36 // CAMBIADO A 36 PARA COINCIDIR
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

    compileOptions {
        // ESTO ES LO QUE EVITA QUE LA APP SE CIERRE (CRASH)
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- LIBRERÍAS BASE (Usando tus alias actuales) ---
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Dentro de dependencies { ... }
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Para usar Corrutinas y Flow
// IMPORTANTE: Usamos KSP (más rápido que KAPT y compatible con Kotlin 2.0+)
    ksp("androidx.room:room-compiler:$room_version")

    // --- ICONOS EXTENDIDOS (Necesario para ThemeBuilder) ---
    // Sin esto, Icons.Default.Refresh o Image darán error
    implementation("androidx.compose.material:material-icons-extended")

    // --- GOOGLE FONTS (Fuentes descargables para el selector de tipografía) ---
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.2")

    // --- SOPORTE DE FECHAS (Desugaring) ---
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // --- NAVEGACIÓN Y VIEWMODEL ---
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // --- IMÁGENES (Coil) - Solo una versión ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- CALENDARIO ---
    implementation("com.kizitonwose.calendar:compose:2.6.0-beta01")

    // --- UTILIDADES ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Red y Datos
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Google Sign-In y Calendar OAuth
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // --- TESTING ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}