import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String, fallback: String): String {
    return localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
}

android {
    namespace = "com.udchemistry.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.udchemistry.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperty("SUPABASE_URL", "https://placeholder.supabase.co")}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperty("SUPABASE_PUBLISHABLE_KEY", "placeholder-key")}\"",
        )
        buildConfigField(
            "String",
            "APP_PUBLIC_URL",
            "\"${localProperty("APP_PUBLIC_URL", "http://localhost:5173")}\"",
        )
        buildConfigField(
            "long",
            "SESSION_MAX_AGE_MS",
            "${24L * 60L * 60L * 1000L}L",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.datastore:datastore-preferences:1.1.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.ktor:ktor-client-okhttp:3.4.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.1")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.17")

    implementation("androidx.camera:camera-core:1.5.3")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.zxing:core:3.5.4")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
