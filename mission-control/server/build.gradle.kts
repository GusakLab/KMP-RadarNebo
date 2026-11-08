import com.gusak.lab.radarnebo.buildlogic.launchAs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("com.gusak.lab.radarnebo.server.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        optIn.add("kotlin.time.ExperimentalTime")
        allWarningsAsErrors.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.rateLimit)
    implementation(libs.ktor.server.forwardedHeader)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.logback.classic)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.hikari)
    // Local container in ServerComponents. Never GlobalContext.startKoin.
    implementation(libs.koin.core)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.ktor)
    implementation(libs.opentelemetry.logback.mdc)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.micrometer)
    implementation(libs.arrow.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    // kotlin.test classes are JUnit 4. Vintage keeps them on the JUnit Platform next to Kotest.
    testImplementation(libs.junit.vintage.engine)
}

tasks.named<org.gradle.api.tasks.testing.Test>("test") {
    useJUnitPlatform()
}

// Local secrets for `./gradlew :mission-control:server:run`: KEY=value lines from mission-control/server/.env
// (git-ignored). Variables already set in the environment win.
tasks.named<JavaExec>("run") {
    val envFile = file(".env")
    if (envFile.exists()) {
        envFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && "=" in it }
            .map { it.substringBefore("=").trim() to it.substringAfter("=").trim().removeSurrounding("\"") }
            .filter { (key, value) -> value.isNotEmpty() && System.getenv(key) == null }
            .forEach { (key, value) -> environment(key, value) }
    }
}

// The process is called "RadarNebo Server" rather than "java"
// (buildSrc/src/main/kotlin/com/gusak/lab/radarnebo/buildlogic/LaunchAs.kt).
launchAs("RadarNebo Server")
