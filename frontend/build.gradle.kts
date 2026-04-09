plugins {
    kotlin("multiplatform") version "2.3.20"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    wasmJs {
        moduleName = "frontend"
        browser()
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
    }
}