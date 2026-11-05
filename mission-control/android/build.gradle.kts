import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// MissionControl-Nebo: an Android monitor for a RadarNebo server, reading its /health report.

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// The release key lives outside the repo, in a folder of its own with a keystore.properties next to the .jks
// (see docs/mission-control-android-release.md). Without it the release build is left unsigned.
val keystoreDir = providers.gradleProperty("missionControl.keystoreDir")
    .orElse(providers.environmentVariable("MISSIONCONTROL_KEYSTORE_DIR"))
    .orElse(System.getProperty("user.home") + "/Keys/MissionControl-Nebo")
    .map(::File)
    .get()
val keystoreProperties = keystoreDir.resolve("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

// The server the app watches until one is set in the app: client.properties' server.url, else the public server.
val defaultServerUrl = rootProject.file("client.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) }.getProperty("server.url")?.trim() }
    ?.takeIf { it.isNotEmpty() }
    ?: "https://radar-nebo.worxbend.com"

android {
    namespace = "com.gusak.lab.radarnebo.missioncontrol"
    // Compose 1.13 (from material3 1.13.0-beta01) needs API 37.1 or later to compile against.
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.gusak.lab.radarnebo.missioncontrol"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // The Google Play deploy workflow passes both, so a release needs no version commit.
        versionCode = providers.gradleProperty("missionControl.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("missionControl.versionName").orNull ?: "1.0.0"
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$defaultServerUrl\"")
    }
    androidResources {
        // English, Ukrainian and Dutch only; libraries' other translations would otherwise show up as supported.
        localeFilters += listOf("en", "uk", "nl")
        // The per-app language picker (Android 13+) reads the generated locale config.
        generateLocaleConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = keystoreDir.resolve(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}
