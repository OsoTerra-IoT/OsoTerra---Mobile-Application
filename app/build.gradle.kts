plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.osoterra.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.osoterra.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // false (por defecto): RESTful API real. true: repositorios mock (demo sin Backend).
        // Cambiar sin tocar código: ./gradlew assembleDebug -Posoterra.fakeApi=true
        val useFakeApi = (project.findProperty("osoterra.fakeApi") as String?) ?: "false"
        buildConfigField("boolean", "USE_FAKE_API", useFakeApi)

        // Base URL del RESTful API. Por defecto, el Backend desplegado en Railway.
        // Backend local desde el emulador: -Posoterra.apiUrl=http://10.0.2.2:8080/api/v1/
        val apiUrl = (project.findProperty("osoterra.apiUrl") as String?)
            ?: "https://osoterra-backend-production.up.railway.app/api/v1/"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
    }

    buildTypes {
        debug {
            // El Backend local corre en HTTP; solo el build de desarrollo lo permite.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // El idioma se cambia dentro de la app (LocaleStore): el bundle debe traer los dos idiomas.
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // AndroidX core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore (token / preferencias)
    implementation(libs.androidx.datastore.preferences)

    // Networking (RESTful API — capa lista, hoy detrás de repos mock)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
