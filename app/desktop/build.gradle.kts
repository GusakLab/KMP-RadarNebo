import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// The name and icon macOS shows in the Dock and app switcher, instead of "MainKt" and the Java cup.
// -Xdock is macOS-only; other JVMs refuse to start with it.
val dockArgs = if (System.getProperty("os.name").startsWith("Mac")) {
    listOf("-Xdock:name=RadarNebo", "-Xdock:icon=${file("icons/radarnebo.png").absolutePath}")
} else {
    emptyList()
}

// Every way of starting the app needs these too, not only `run` (which gets compose.desktop's jvmArgs):
// Compose Hot Reload's hotRun/hotDev, which the IDE's "[hot]" run configuration uses.
tasks.withType<JavaExec>().configureEach {
    jvmArgs("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
    jvmArgs(dockArgs)
    // Turns on the request inspector. Packaged apps only get compose.desktop's jvmArgs, so they never have this.
    // A system property, not a jvmArg: Compose replaces the jvmArgs of `run` after this.
    if (name != "runRelease") systemProperty("radarnebo.debug", "true")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.androidx.lifecycle.viewmodelCompose)
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(libs.koin.test)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    jvmArgs("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
    // Where the headless snapshot test writes its PNGs.
    systemProperty("snapshot.dir", layout.buildDirectory.dir("snapshots").get().asFile.absolutePath)
    // Previews are found from their source, since @Preview isn't visible to reflection.
    systemProperty("preview.sources", rootProject.file("shared/src/commonMain/kotlin").absolutePath)
    // ./gradlew :app:desktop:test -PupdateReadmeScreenshots regenerates the README's screenshots.
    if (providers.gradleProperty("updateReadmeScreenshots").isPresent) {
        systemProperty("readme.screenshots.dir", rootProject.file("docs/screenshots").absolutePath)
        outputs.upToDateWhen { false }
    }
}

// A Mac App Store package: ./gradlew :app:desktop:packagePkg -PmacAppStore -PclientProperties=client.ios.properties
// -PmacProfile=<embedded.provisionprofile> -PmacKeychain=<keychain> -PmacBuildNumber=<n>. Without -PmacAppStore nothing changes.
val macAppStore = providers.gradleProperty("macAppStore").isPresent

compose.desktop {
    application {
        mainClass = "com.gusak.lab.radarnebo.desktop.MainKt"
        jvmArgs("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
        // dockArgs name a file in this checkout, which a shipped app doesn't have; its bundle sets the name and icon.
        if (!macAppStore) jvmArgs(*dockArgs.toTypedArray())
        System.getProperty("radarnebo.settings")?.let { jvmArgs("-Dradarnebo.settings=$it") }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RadarNebo"
            packageVersion = "1.0.0"
            // The packaged app ships a trimmed JDK. `suggestRuntimeModules` finds the first three; logback needs
            // java.naming (loaded reflectively, so not detected) and HTTPS tile servers need jdk.crypto.ec.
            modules("java.instrument", "java.management", "jdk.unsupported", "java.naming", "jdk.crypto.ec")
            // Regenerate with tools/make-icons.sh.
            macOS {
                iconFile.set(project.file("icons/radarnebo.icns"))
                bundleID = "com.gusak.lab.radarnebo"
                if (macAppStore) {
                    appStore = true
                    infoPlist {
                        // Standard HTTPS only, as in the iOS app's Info.plist.
                        extraKeysRawXml = "<key>ITSAppUsesNonExemptEncryption</key><false/>"
                    }
                    // The same App ID as the iOS app, so one App Store listing covers both.
                    bundleID = "com.gusak.lab.radarnebo.RadarNebo"
                    iconFile.set(project.file("icons/radarnebo-appstore.icns"))
                    appCategory = "public.app-category.utilities"
                    minimumSystemVersion = "12.0"
                    packageBuildVersion = providers.gradleProperty("macBuildNumber").getOrElse("1")
                    entitlementsFile.set(project.file("macos/entitlements.plist"))
                    runtimeEntitlementsFile.set(project.file("macos/runtime-entitlements.plist"))
                    provisioningProfile.set(file(providers.gradleProperty("macProfile").get()))
                    runtimeProvisioningProfile.set(file(providers.gradleProperty("macProfile").get()))
                    signing {
                        sign.set(true)
                        identity.set("GUSAK LAB LLC")
                        keychain.set(providers.gradleProperty("macKeychain").get())
                    }
                }
            }
            windows { iconFile.set(project.file("icons/radarnebo.ico")) }
            linux { iconFile.set(project.file("icons/radarnebo.png")) }
        }
    }
}

// The process is called "RadarNebo" rather than "java" (buildSrc/src/main/kotlin/LaunchAs.kt).
launchAs("RadarNebo")
