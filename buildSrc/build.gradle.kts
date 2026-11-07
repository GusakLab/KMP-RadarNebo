// Build logic shared by the projects: see src/main/kotlin.
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
