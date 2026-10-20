import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun prop(key: String, default: String): String =
    localProps.getProperty(key, System.getenv(key.uppercase().replace('.', '_')) ?: default)

android {
    namespace = "app.homelab.tvlauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.homelab.tvlauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 2
        versionName = (findProperty("appVersionName") as String?) ?: "1.1.0"

        buildConfigField("String", "DEFAULT_JELLYFIN_URL", "\"${prop("jellyfin.url", "")}\"")
        buildConfigField("String", "DEFAULT_JELLYFIN_USER_ID", "\"${prop("jellyfin.userId", "")}\"")
        buildConfigField("String", "DEFAULT_JELLYFIN_API_KEY", "\"${prop("jellyfin.apiKey", "")}\"")
        buildConfigField("String", "DEFAULT_TORRENT_STREAM_URL", "\"${prop("torrentStream.url", "")}\"")
        buildConfigField("String", "DEFAULT_TAILSCALE_IP", "\"${prop("tailscale.ip", "")}\"")
    }

    signingConfigs {
        create("release") {
            val ksFile = rootProject.file("keystore/release.jks")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = prop("signing.storePassword", "homelab123")
                keyAlias = prop("signing.keyAlias", "homelab")
                keyPassword = prop("signing.keyPassword", "homelab123")
            } else {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
}
