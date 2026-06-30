plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.naviapp.agent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.naviapp.agent"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // HERE SDK credentials from local.properties (not committed to git)
        val localProps = rootProject.file("local.properties")
        val props = java.util.Properties()
        if (localProps.exists()) {
            props.load(localProps.inputStream())
        }
        manifestPlaceholders["HERE_ACCESS_KEY_ID"] = props.getProperty("HERE_ACCESS_KEY_ID", "")
        manifestPlaceholders["HERE_ACCESS_KEY_SECRET"] = props.getProperty("HERE_ACCESS_KEY_SECRET", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DataStore for settings persistence
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // HERE SDK for Android (Lite Edition — map rendering)
    implementation("com.here.sdk:here-sdk-lite:4.17.4.0")

    // Compose interop for AndroidView (used to embed HERE MapView)
    implementation("androidx.compose.ui:ui-viewbinding")
}
