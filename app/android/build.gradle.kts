import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Firebase Analytics and Crashlytics read their project from google-services.json (gitignored; see
// docs/android-release-build.md). Without it, as in CI, the app still builds and runs, with Firebase inactive.
if (file("google-services.json").exists()) {
    pluginManager.apply(libs.plugins.googleServices.get().pluginId)
    pluginManager.apply(libs.plugins.firebaseCrashlytics.get().pluginId)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        allWarningsAsErrors.set(true)
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.uiautomator)
}

// The app ships on the Google Play listing created for MissionControl-Nebo, whose package name and upload key can't
// change, so release signing uses that key's folder (see docs/android-release-build.md). Without it the release build is
// left unsigned rather than signed with a key Play would reject.
val keystoreDir = providers.gradleProperty("missionControl.keystoreDir")
    .orElse(providers.environmentVariable("MISSIONCONTROL_KEYSTORE_DIR"))
    .orElse(System.getProperty("user.home") + "/Keys/MissionControl-Nebo")
    .map(::File)
    .get()
val keystoreProperties = keystoreDir.resolve("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

// tools/capture-play-screenshots.sh: instruments the release build, which has the UI Play users get, signed with the debug
// key so it installs without the upload key. Nothing changes without this property.
val storeCapture = providers.gradleProperty("radarNebo.capture").isPresent

android {
    if (storeCapture) testBuildType = "release"
    namespace = "com.gusak.lab.radarnebo"
    // Compose 1.13 (from material3 1.13.0-beta01) needs API 37.1 or later to compile against.
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) { minorApiLevel = 2 }
    }

    defaultConfig {
        // Fixed by the Play listing. The namespace above stays, so code and resources keep their package.
        applicationId = "com.gusak.lab.radarnebo.missioncontrol"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // The Google Play deploy workflow passes both. Play already has 1 (1.0.0), 2 (1.0.1) and 3 (1.0.2) on this listing.
        versionCode = providers.gradleProperty("radarNebo.versionCode").orNull?.toInt() ?: 4
        versionName = providers.gradleProperty("radarNebo.versionName").orNull ?: "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Only release builds report to Firebase (see the manifest), so development runs stay out of the statistics.
        manifestPlaceholders["firebaseCollection"] = "false"
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
            signingConfig = if (storeCapture) signingConfigs.getByName("debug") else signingConfigs.findByName("release")
            // Store screenshot runs drive the release build and must not count as real use.
            manifestPlaceholders["firebaseCollection"] = (!storeCapture).toString()
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}